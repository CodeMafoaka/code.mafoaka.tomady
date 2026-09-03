package com.tomady.nutrition.service.foodb

import android.util.Log
import com.tomady.nutrition.config.ConfigManager
import com.tomady.nutrition.data.local.foodb.FooDBLocalDatabase
import com.tomady.nutrition.data.local.foodb.entity.FoodItem
import com.tomady.nutrition.data.local.foodb.entity.NutrientProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Syncs the local FooDB Room cache (`food_item` / `nutrient_property`) from a
 * remote PostgreSQL database holding the full FooDB dataset — same table
 * shape as `init_ressources/foodb_generated_schema_only.sql` (`food`,
 * `content`, `nutrient`, ...), just hosted on Postgres instead of shipped as
 * an empty embedded SQLite schema.
 *
 * ## Why a join is needed
 * The real `content` table is polymorphic: `source_id`/`source_type`
 * reference either `nutrient` or `compound` rows, and the actual value lives
 * in `standard_content`/`orig_content` (stored as text). This flattens that
 * into the `nutrient_name` + `amount` + `unit` shape [NutrientProperty]
 * already assumes, by joining `content` -> `nutrient` where
 * `source_type = 'Nutrient'`.
 *
 * **Unverified assumption**: the `'Nutrient'` literal for `source_type` and
 * the exact column set were inferred from the schema-only SQL dump, not
 * confirmed against live data — recheck once real Postgres credentials are
 * configured and a sync has actually been run.
 */
class FooDBRemoteSyncService(
    private val localDatabase: FooDBLocalDatabase,
    private val configManager: ConfigManager
) {
    private val isSyncing = AtomicBoolean(false)
    private val progress = AtomicReference<Float?>(null)
    private val lastResult = AtomicReference<String?>(null)
    private val lastError = AtomicReference<String?>(null)

    fun isSyncInProgress(): Boolean = isSyncing.get()
    fun getSyncProgress(): Float? = progress.get()
    fun getLastSyncResult(): String? = lastResult.get()
    fun getLastSyncError(): String? = lastError.get()

    fun isConfigured(): Boolean {
        val pg = configManager.get().postgres
        return !pg.host.isNullOrBlank() && !pg.database.isNullOrBlank() && !pg.username.isNullOrBlank()
    }

    /**
     * Connects to the configured Postgres instance and imports `food` and
     * `content`/`nutrient` rows into the local Room cache.
     *
     * @param forceRefresh If true, clears the local food_item/nutrient_property
     *   tables before importing. Inserts otherwise use IGNORE-on-conflict (see
     *   FoodItemDao/NutrientPropertyDao), so remote edits to already-cached
     *   rows won't be picked up without a forced refresh.
     * @return true if the sync completed successfully.
     */
    suspend fun syncFromPostgres(forceRefresh: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (isSyncing.get()) return@withContext false
        if (!isConfigured()) {
            lastError.set("Postgres not configured — set postgres.host/database/username via POST /api/v1/config")
            return@withContext false
        }

        isSyncing.set(true)
        progress.set(0f)
        lastError.set(null)
        var connection: Connection? = null
        try {
            val pg = configManager.get().postgres
            Class.forName("org.postgresql.Driver")
            val url = "jdbc:postgresql://${pg.host}:${pg.port}/${pg.database}?sslmode=${pg.sslMode}"
            connection = DriverManager.getConnection(url, pg.username, pg.password)

            if (forceRefresh) {
                localDatabase.deleteAllFoods()
                localDatabase.deleteAllNutrients()
            }

            val foodCount = syncFoodItems(connection)
            progress.set(0.5f)
            val nutrientCount = syncNutrientProperties(connection)
            progress.set(1f)

            val summary = "Synced $foodCount foods, $nutrientCount nutrient properties from Postgres"
            lastResult.set(summary)
            Log.i(TAG, summary)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Postgres sync failed: ${e.message}", e)
            lastError.set(e.message ?: e::class.simpleName ?: "Unknown error")
            false
        } finally {
            connection?.close()
            isSyncing.set(false)
            progress.set(null)
        }
    }

    // JDBC access below is blocking, but these are only ever called from
    // syncFromPostgres() which already runs under withContext(Dispatchers.IO),
    // so blocking here is fine — no need to hop dispatchers again.
    private suspend fun syncFoodItems(connection: Connection): Int {
        val items = mutableListOf<FoodItem>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery(
                """
                SELECT id, public_id, name, name_scientific, description, food_group,
                       food_subgroup, food_type, category, itis_id, wikipedia_id,
                       ncbi_taxonomy_id, picture_file_name
                FROM food
                """.trimIndent()
            ).use { rs ->
                while (rs.next()) {
                    val ncbiTaxonomyId = rs.getLong("ncbi_taxonomy_id").takeUnless { rs.wasNull() }
                    items.add(
                        FoodItem(
                            id = rs.getLong("id"),
                            publicId = rs.getString("public_id"),
                            name = rs.getString("name"),
                            nameScientific = rs.getString("name_scientific"),
                            description = rs.getString("description"),
                            foodGroup = rs.getString("food_group"),
                            foodSubgroup = rs.getString("food_subgroup"),
                            foodType = rs.getString("food_type"),
                            category = rs.getString("category"),
                            itisId = rs.getString("itis_id"),
                            wikipediaId = rs.getString("wikipedia_id"),
                            ncbiTaxonomyId = ncbiTaxonomyId,
                            pictureFileName = rs.getString("picture_file_name")
                        )
                    )
                }
            }
        }
        localDatabase.insertFoods(items)
        return items.size
    }

    private suspend fun syncNutrientProperties(connection: Connection): Int {
        val properties = mutableListOf<NutrientProperty>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery(
                """
                SELECT c.id AS content_id, c.food_id, n.name AS nutrient_name,
                       COALESCE(c.standard_content, c.orig_content) AS amount_raw,
                       c.orig_unit AS unit, c.standard_content, c.preparation_type,
                       c.citation, c.orig_content
                FROM content c
                JOIN nutrient n ON c.source_id = n.id AND c.source_type = 'Nutrient'
                WHERE c.food_id IS NOT NULL
                """.trimIndent()
            ).use { rs ->
                while (rs.next()) {
                    properties.add(
                        NutrientProperty(
                            id = rs.getLong("content_id"),
                            foodItemId = rs.getLong("food_id"),
                            nutrientName = rs.getString("nutrient_name"),
                            amount = rs.getString("amount_raw")?.toDoubleOrNull(),
                            unit = rs.getString("unit"),
                            standardContent = rs.getString("standard_content")?.toDoubleOrNull(),
                            preparationType = rs.getString("preparation_type"),
                            citation = rs.getString("citation"),
                            origContent = rs.getString("orig_content")
                        )
                    )
                }
            }
        }
        localDatabase.insertNutrientProperties(properties)
        return properties.size
    }

    companion object {
        private const val TAG = "FooDBRemoteSync"
    }
}
