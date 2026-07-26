package com.tomady.nutrition.worker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tomady.nutrition.data.AppDatabase
import com.tomady.nutrition.data.local.diet.DietDatabase
import com.tomady.nutrition.data.local.diet.entity.DishHistory
import com.tomady.nutrition.data.local.foodb.FooDBLocalDatabase
import com.tomady.nutrition.service.diet.DietAPIService
import com.tomady.nutrition.service.foodb.FooDBDataAPIService
import com.tomady.nutrition.service.gemma.GemmaAndroidService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * WorkManager worker for generating daily personalized food suggestions.
 *
 * Scheduled as a [androidx.work.PeriodicWorkRequest] running daily at 00:00,
 * this worker:
 * 1. Loads all users from the DietDatabase
 * 2. For each user, fetches their [Profile], today's [BioRecord], and recent
 *    [DishHistory] to build a rich context for the LLM
 * 3. Calls [GemmaAndroidService.computeRecipe] to generate a personalized
 *    dish suggestion
 * 4. Persists the generated [Dish] and logs it as a [DishHistory] entry
 * 5. Posts a [SuggestionEvent] to [SuggestionEventBus] so the RN bridge
 *    layer can notify the host via [DeviceEventEmitter]
 *
 * @param appContext  Application context (used to initialise Room DB, services).
 * @param workerParams Standard WorkManager parameters.
 */
class DailySuggestionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    /** ISO date format used for all date-based queries and storage. */
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Persists the last run timestamp to avoid duplicate runs on the same day. */
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Lazy service initialisation (same pattern as all bridge modules) ─

    private val dietDb: DietDatabase by lazy {
        val db = AppDatabase.getInstance(applicationContext)
        DietDatabase(
            userDao = db.userDao(),
            profileDao = db.profileDao(),
            bioRecordDao = db.bioRecordDao(),
            dishDao = db.dishDao(),
            recipeDao = db.recipeDao(),
            recipeIngredientDao = db.recipeIngredientDao(),
            dishHistoryDao = db.dishHistoryDao()
        )
    }

    private val gemmaService: GemmaAndroidService by lazy {
        val db = AppDatabase.getInstance(applicationContext)
        val foodbDb = FooDBLocalDatabase(
            foodItemDao = db.foodItemDao(),
            nutrientPropertyDao = db.nutrientPropertyDao()
        )
        val foodbService = FooDBDataAPIService(localDatabase = foodbDb)
        val dietService = DietAPIService(dietDatabase = dietDb, foodbService = foodbService)

        GemmaAndroidService(
            dietDatabase = dietDb,
            dietService = dietService,
            foodbService = foodbService
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    // doWork — Entry point
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Executes the daily suggestion generation pipeline.
     *
     * @return [Result.success] if suggestions were generated for at least one
     *         user, or [Result.failure] on unrecoverable errors.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Guard against duplicate runs on the same calendar day
            val today = dateFormat.format(Date())
            val lastRunDate = prefs.getString(KEY_LAST_RUN_DATE, null)
            if (lastRunDate == today) {
                // Already ran today — skip to avoid duplicate suggestions
                return@withContext Result.success()
            }

            // 1. Load the Gemma model (simulated 2s load in dev)
            val modelLoaded = gemmaService.loadModel(null)
            if (!modelLoaded) {
                Log.e(TAG, "Failed to load Gemma model")
                return@withContext Result.failure()
            }

            // 2. Fetch all registered users via the UserDao
            val users = dietDb.userDao.observeAll().let { flow ->
                // Collect the first emission from the reactive Flow
                var result = emptyList<com.tomady.nutrition.data.local.diet.entity.User>()
                flow.first { emitted ->
                    result = emitted
                    true  // stop after first emission
                }
                result
            }

            if (users.isEmpty()) {
                // No users to generate suggestions for
                gemmaService.release()
                Log.w(TAG, "No users found — skipping suggestion generation")
                return@withContext Result.success()
            }

            var suggestionCount = 0
            var errorCount = 0

            // 3. For each user, generate a personalised suggestion
            for (user in users) {
                try {
                    val suggestion = generateUserSuggestion(
                        userId = user.id,
                        today = today
                    )
                    if (suggestion != null) {
                        suggestionCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to generate suggestion for user ${user.id}: ${e.message}", e)
                    errorCount++
                }
            }

            // 5. Release model resources
            gemmaService.release()

            // 4. Persist the run date ONLY if at least one suggestion was generated
            if (suggestionCount > 0) {
                prefs.edit().putString(KEY_LAST_RUN_DATE, today).apply()
            }

            Log.i(TAG, "Daily suggestion worker completed: $suggestionCount suggestions, $errorCount errors")

            if (errorCount > 0 && suggestionCount == 0) {
                // All users failed — return failure so WorkManager can retry
                Result.failure()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Daily suggestion worker failed with unrecoverable error: ${e.message}", e)
            Result.failure()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Suggestion generation for a single user
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Generates a personalised dish suggestion for a single user.
     *
     * Pipeline:
     * 1. Fetch the user's [Profile] and today's [BioRecord] for context
     * 2. Fetch the last 7 days of [DishHistory] for dietary pattern awareness
     * 3. Build a prompt that includes the user's goals, restrictions, and
     *    recent eating patterns
     * 4. Call [GemmaAndroidService.computeRecipe] to produce a dish/recipe
     * 5. Log the generated dish as a [DishHistory] entry (meal type: "suggestion")
     * 6. Post a [SuggestionEvent] so the bridge layer can notify the RN host
     *
     * @param userId The target user.
     * @param today  Today's date in "yyyy-MM-dd" format.
     * @return The generated [Dish] name if successful, null otherwise.
     */
    private suspend fun generateUserSuggestion(
        userId: String,
        today: String
    ): String? {
        // ── Fetch user context ──────────────────────────────────────────

        val profile = dietDb.getProfileByUserId(userId)
        val bioRecord = dietDb.getBioRecordByUserAndDate(userId, today)
        val recentMeals = dietDb.getDishHistoryByUserInRange(
            userId = userId,
            startDate = dateFormat.format(
                Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
            ),
            endDate = today
        )

        // ── Build the personalised prompt ───────────────────────────────

        val prompt = buildSuggestionPrompt(
            profile = profile,
            bioRecord = bioRecord,
            recentMeals = recentMeals
        )

        // ── Generate via Gemma ──────────────────────────────────────────

        val recipeResult = gemmaService.computeRecipe(prompt, userId)

        // ── Persist the suggestion ──────────────────────────────────────

        // If the dish was already created by computeRecipe, just log it.
        // computeRecipe already calls dietService.createDish internally.
        val dishHistory = DishHistory(
            id = UUID.randomUUID().toString(),
            userId = userId,
            dishId = recipeResult.dishId,
            date = today,
            mealType = MEAL_TYPE_SUGGESTION,
            servings = 1.0,
            notes = "Daily suggestion — ${recipeResult.dishName}"
        )
        dietDb.insertDishHistory(dishHistory)

        // ── Notify the RN bridge layer ──────────────────────────────────

        SuggestionEventBus.post(
            SuggestionEvent(
                userId = userId,
                dishId = recipeResult.dishId,
                dishName = recipeResult.dishName,
                date = today,
                isCompatible = recipeResult.isCompatible,
                warnings = recipeResult.warnings
            )
        )

        return recipeResult.dishName
    }

    // ══════════════════════════════════════════════════════════════════════
    // Prompt building
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Builds a natural-language prompt for the LLM that includes the user's
     * health profile, today's biometrics, and recent meal history so the
     * generated suggestion is genuinely personalised.
     */
    private fun buildSuggestionPrompt(
        profile: com.tomady.nutrition.data.local.diet.entity.Profile?,
        bioRecord: com.tomady.nutrition.data.local.diet.entity.BioRecord?,
        recentMeals: List<DishHistory>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Generate a healthy meal suggestion for a user with the following context:")
        sb.appendLine()

        // Profile context
        if (profile != null) {
            sb.appendLine("--- User Profile ---")
            sb.appendLine("Goal: ${profile.goal ?: "Not specified"}")
            profile.dailyCalorieTarget?.let { sb.appendLine("Daily calorie target: ${it}kcal") }
            profile.proteinGramsTarget?.let { sb.appendLine("Protein target: ${it}g") }
            profile.carbsGramsTarget?.let { sb.appendLine("Carbs target: ${it}g") }
            profile.fatGramsTarget?.let { sb.appendLine("Fat target: ${it}g") }
            profile.weightKg?.let { sb.appendLine("Weight: ${it}kg") }
            profile.heightCm?.let { sb.appendLine("Height: ${it}cm") }
            profile.age?.let { sb.appendLine("Age: ${it}") }
            profile.activityLevel?.let { sb.appendLine("Activity level: ${it}") }
            if (!profile.allergies.isNullOrBlank() && profile.allergies != "[]") {
                sb.appendLine("Allergies: ${profile.allergies}")
            }
            if (!profile.conditions.isNullOrBlank() && profile.conditions != "[]") {
                sb.appendLine("Medical conditions: ${profile.conditions}")
            }
            if (!profile.restrictedFoods.isNullOrBlank() && profile.restrictedFoods != "[]") {
                sb.appendLine("Restricted foods: ${profile.restrictedFoods}")
            }
            if (!profile.forbiddenByDoctor.isNullOrBlank() && profile.forbiddenByDoctor != "[]") {
                sb.appendLine("Forbidden by doctor: ${profile.forbiddenByDoctor}")
            }
            sb.appendLine()
        }

        // Bio record context
        if (bioRecord != null) {
            sb.appendLine("--- Today's Biometrics ---")
            bioRecord.weightKg?.let { sb.appendLine("Weight: ${it}kg") }
            bioRecord.bodyFatPercentage?.let { sb.appendLine("Body fat: ${it}%") }
            bioRecord.systolicBp?.let { sb.appendLine("Blood pressure: ${it}/${bioRecord.diastolicBp ?: "?"}") }
            sb.appendLine()
        }

        // Recent meal context
        if (recentMeals.isNotEmpty()) {
            sb.appendLine("--- Recent Meals (last 7 days) ---")
            val grouped = recentMeals.groupBy { it.date }
            for ((date, meals) in grouped) {
                sb.appendLine("$date: ${meals.joinToString(", ") { m -> m.dishId ?: "unknown" }}")
            }
            sb.appendLine()
        }

        sb.appendLine("Suggest a single dish that is nutritious, fits the user's goals,")
        sb.appendLine("and adds variety to their recent meals. Consider any health")
        sb.appendLine("restrictions implied by the profile (e.g., low sugar for diabetes,")
        sb.appendLine("low sodium for hypertension, high protein for muscle gain).")
        sb.appendLine()
        sb.appendLine("Respond with a structured recipe as instructed.")

        return sb.toString()
    }

    companion object {
        private const val TAG = "DailySuggestionWorker"

        /** SharedPreferences file name. */
        private const val PREFS_NAME = "daily_suggestion_worker"

        /** Key for the last run date. */
        private const val KEY_LAST_RUN_DATE = "last_run_date"

        /** Meal type used for suggestion entries in dish_history. */
        const val MEAL_TYPE_SUGGESTION = "suggestion"

        /**
         * Unique work name for the periodic work request. Used to avoid
         * duplicate scheduling.
         */
        const val WORK_NAME = "daily_suggestion_generation"
    }
}
