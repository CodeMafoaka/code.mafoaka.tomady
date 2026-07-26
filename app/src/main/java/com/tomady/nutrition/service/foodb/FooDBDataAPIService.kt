package com.tomady.nutrition.service.foodb

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.tomady.nutrition.data.local.foodb.FooDBLocalDatabase
import com.tomady.nutrition.data.local.foodb.entity.FoodItem
import com.tomady.nutrition.data.local.foodb.entity.NutrientProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Data access facade over the local FooDB Room database with a remote API fallback.
 *
 * Implements a **cache-first** strategy:
 * 1. Check the local [FooDBLocalDatabase] for the requested data.
 * 2. If present, return it immediately.
 * 3. If missing, fetch from the remote FooDB API, persist the result locally,
 *    and return the freshly cached data.
 *
 * @param localDatabase Wrapper around the local FooDB Room database.
 * @param baseUrl       Base URL for the remote FooDB API (defaults to the public instance).
 * @param gson          Gson instance for JSON deserialization.
 */
class FooDBDataAPIService(
    private val localDatabase: FooDBLocalDatabase,
    private val baseUrl: String = FOO_DB_API_BASE_URL,
    private val gson: Gson = Gson()
) {

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Retrieves detailed information for a food item by its FooDB ID.
     *
     * **Cache-first:** checks the local database before making a network request.
     * If a cache miss occurs, the food and its nutrient properties are fetched
     * from the remote API and persisted locally before being returned.
     *
     * @param foodId The FooDB food identifier.
     * @return [FoodDetailResult] containing the [FoodItem] and its [NutrientProperty] list,
     *         or null if the food cannot be found either locally or remotely.
     * @throws FooDbApiException if the remote API returns a non-2xx status.
     * @throws IOException if a network error occurs during the remote fetch.
     */
    suspend fun getFoodDetails(foodId: Long): FoodDetailResult? = withContext(Dispatchers.IO) {
        // 1. Check local cache first
        val cachedFood = localDatabase.getFoodById(foodId)
        if (cachedFood != null) {
            val cachedNutrients = localDatabase.getNutrientsByFoodId(foodId)
            return@withContext FoodDetailResult(cachedFood, cachedNutrients)
        }

        // 2. Cache miss — fetch from remote API
        val response = fetchRemoteFoodDetail(foodId) ?: return@withContext null

        // 3. Persist to local database
        localDatabase.insertFood(response.food)
        if (response.nutrients.isNotEmpty()) {
            localDatabase.insertNutrientProperties(response.nutrients)
        }

        // 4. Return result
        FoodDetailResult(response.food, response.nutrients)
    }

    /**
     * Searches the local food catalog by name or scientific name.
     *
     * Implements a **cache-first** strategy: checks the local database first.
     * If the local result is empty, falls back to the remote FooDB API, caches
     * the results locally, and returns them.
     *
     * @param query Free-text search string.
     * @return List of matching [FoodItem] entries (may be empty if both local and remote return nothing).
     */
    suspend fun searchFood(query: String): List<FoodItem> = withContext(Dispatchers.IO) {
        // 1. Check local cache first
        val localResults = localDatabase.searchFood(query)
        if (localResults.isNotEmpty()) {
            return@withContext localResults
        }

        // 2. Cache miss — try remote API
        try {
            val remoteResults = searchRemoteFood(query)
            // 3. Cache remote results locally
            if (remoteResults.isNotEmpty()) {
                localDatabase.insertFoods(remoteResults)
            }
            remoteResults
        } catch (e: Exception) {
            // Remote failed (network error, API error, etc.) — return empty
            emptyList()
        }
    }

    /**
     * Retrieves all nutrient properties for a given food item from the local cache.
     *
     * If the food has never been fetched and cached, returns an empty list.
     * Call [getFoodDetails] first to populate the cache for that food.
     *
     * @param foodId The FooDB food identifier.
     * @return List of [NutrientProperty] entries for the food item.
     */
    suspend fun getNutrients(foodId: Long): List<NutrientProperty> = withContext(Dispatchers.IO) {
        localDatabase.getNutrientsByFoodId(foodId)
    }

    /**
     * Returns all distinct food groups available in the local catalog.
     */
    suspend fun getFoodGroups(): List<String> = withContext(Dispatchers.IO) {
        localDatabase.getFoodGroups()
    }

    /**
     * Returns all distinct nutrient names available in the local database.
     */
    suspend fun getNutrientNames(): List<String> = withContext(Dispatchers.IO) {
        localDatabase.getNutrientNames()
    }

    // ── Remote API client ───────────────────────────────────────────────

    /**
     * Fetches food details from the remote FooDB API.
     *
     * @param foodId The FooDB food identifier.
     * @return A pair of [FoodItem] and list of [NutrientProperty], or null if 404.
     * @throws FooDbApiException on non-2xx / non-404 response.
     * @throws IOException on network failure.
     */
    private suspend fun fetchRemoteFoodDetail(foodId: Long): RemoteFoodResponse? {
        val url = URL("$baseUrl/foods/$foodId")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")

        return try {
            val responseCode = connection.responseCode
            when {
                responseCode == HttpURLConnection.HTTP_NOT_FOUND -> null
                responseCode in 200..299 -> {
                    val body = readResponseBody(connection)
                    parseFoodDetailResponse(foodId, body)
                }
                else -> {
                    val errorBody = try {
                        readResponseBody(connection)
                    } catch (_: IOException) {
                        "Unknown error"
                    }
                    throw FooDbApiException(
                        statusCode = responseCode,
                        message = "FooDB API returned status $responseCode: $errorBody"
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Searches the remote FooDB API for foods matching a query.
     *
     * @param query Free-text search string.
     * @return List of [FoodItem] results (may be empty).
     * @throws FooDbApiException on non-2xx response.
     * @throws IOException on network failure.
     */
    suspend fun searchRemoteFood(query: String): List<FoodItem> = withContext(Dispatchers.IO) {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = URL("$baseUrl/foods/search?q=$encodedQuery")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")

        try {
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val body = readResponseBody(connection)
                parseFoodSearchResponse(body)
            } else {
                val errorBody = try {
                    readResponseBody(connection)
                } catch (_: IOException) {
                    "Unknown error"
                }
                throw FooDbApiException(
                    statusCode = responseCode,
                    message = "FooDB search API returned status $responseCode: $errorBody"
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    // ── JSON parsing helpers ────────────────────────────────────────────

    /**
     * Parses the JSON response from `/foods/{id}` into a [RemoteFoodResponse].
     *
     * Expected JSON structure (simplified):
     * ```json
     * {
     *   "food": { ... FoodItem fields ... },
     *   "nutrients": [ ... NutrientProperty fields ... ]
     * }
     * ```
     */
    private fun parseFoodDetailResponse(foodId: Long, body: String): RemoteFoodResponse? {
        val root = gson.fromJson(body, JsonObject::class.java) ?: return null

        val foodJson = root.getAsJsonObject("food") ?: return null
        val food = FoodItem(
            id = foodJson.get("id")?.asLong ?: foodId,
            publicId = foodJson.get("public_id")?.asString,
            name = foodJson.get("name")?.asString,
            nameScientific = foodJson.get("name_scientific")?.asString,
            description = foodJson.get("description")?.asString,
            foodGroup = foodJson.get("food_group")?.asString,
            foodSubgroup = foodJson.get("food_subgroup")?.asString,
            foodType = foodJson.get("food_type")?.asString,
            category = foodJson.get("category")?.asString,
            itisId = foodJson.get("itis_id")?.asString,
            wikipediaId = foodJson.get("wikipedia_id")?.asString,
            ncbiTaxonomyId = foodJson.get("ncbi_taxonomy_id")?.asLong,
            pictureFileName = foodJson.get("picture_file_name")?.asString
        )

        val nutrients = mutableListOf<NutrientProperty>()
        val nutrientsArray = root.getAsJsonArray("nutrients")
        if (nutrientsArray != null) {
            for (element in nutrientsArray) {
                val obj = element.asJsonObject
                val nutrient = NutrientProperty(
                    id = obj.get("id")?.asLong ?: 0L,
                    foodItemId = foodId,
                    nutrientName = obj.get("nutrient_name")?.asString,
                    amount = obj.get("amount")?.asDouble,
                    unit = obj.get("unit")?.asString,
                    standardContent = obj.get("standard_content")?.asDouble,
                    preparationType = obj.get("preparation_type")?.asString,
                    citation = obj.get("citation")?.asString,
                    origContent = obj.get("orig_content")?.asString
                )
                nutrients.add(nutrient)
            }
        }

        return RemoteFoodResponse(food, nutrients)
    }

    /**
     * Parses the JSON search response array into a list of [FoodItem].
     *
     * Expected JSON structure:
     * ```json
     * { "foods": [ ... FoodItem fields ... ] }
     * ```
     */
    private fun parseFoodSearchResponse(body: String): List<FoodItem> {
        val root = gson.fromJson(body, JsonObject::class.java) ?: return emptyList()
        val foodsArray = root.getAsJsonArray("foods") ?: return emptyList()

        val listType = object : TypeToken<List<FoodItemMap>>() {}.type
        val foodMaps: List<FoodItemMap> = gson.fromJson(foodsArray, listType)

        return foodMaps.map { it.toFoodItem() }
    }

    // ── Utility ─────────────────────────────────────────────────────────

    private fun readResponseBody(connection: HttpURLConnection): String {
        val stream = connection.inputStream
        val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
        return reader.use { it.readText() }
    }

    companion object {
        /** Default base URL for the public FooDB API. */
        private const val FOO_DB_API_BASE_URL = "https://foodb.ca/api/v1"

        /** Connection timeout in milliseconds. */
        private const val CONNECT_TIMEOUT_MS = 10_000

        /** Read timeout in milliseconds. */
        private const val READ_TIMEOUT_MS = 15_000
    }
}

// ── Data types ─────────────────────────────────────────────────────────────

/**
 * Result of a [FooDBDataAPIService.getFoodDetails] call.
 *
 * @property food The [FoodItem] details.
 * @property nutrients The associated list of [NutrientProperty] entries.
 */
data class FoodDetailResult(
    val food: FoodItem,
    val nutrients: List<NutrientProperty>
)

/**
 * Internal representation of a remote FooDB API response for food details.
 */
internal data class RemoteFoodResponse(
    val food: FoodItem,
    val nutrients: List<NutrientProperty>
)

/**
 * Lightweight map used for Gson deserialization of search results.
 */
internal data class FoodItemMap(
    val id: Long?,
    val public_id: String?,
    val name: String?,
    val name_scientific: String?,
    val description: String?,
    val food_group: String?,
    val food_subgroup: String?,
    val food_type: String?,
    val category: String?,
    val itis_id: String?,
    val wikipedia_id: String?,
    val ncbi_taxonomy_id: Long?,
    val picture_file_name: String?
) {
    fun toFoodItem() = FoodItem(
        id = id ?: 0L,
        publicId = public_id,
        name = name,
        nameScientific = name_scientific,
        description = description,
        foodGroup = food_group,
        foodSubgroup = food_subgroup,
        foodType = food_type,
        category = category,
        itisId = itis_id,
        wikipediaId = wikipedia_id,
        ncbiTaxonomyId = ncbi_taxonomy_id,
        pictureFileName = picture_file_name
    )
}

/**
 * Exception thrown when the FooDB remote API returns an error status code.
 *
 * @property statusCode The HTTP status code returned by the API.
 * @property message A human-readable error description.
 */
class FooDbApiException(
    val statusCode: Int,
    override val message: String
) : IOException(message)
