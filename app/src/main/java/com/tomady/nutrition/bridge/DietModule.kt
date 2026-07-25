package com.tomady.nutrition.bridge

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.tomady.nutrition.data.AppDatabase
import com.tomady.nutrition.data.local.diet.DietDatabase
import com.tomady.nutrition.data.local.foodb.FooDBLocalDatabase
import com.tomady.nutrition.service.diet.DailySummary
import com.tomady.nutrition.service.diet.DailyTargets
import com.tomady.nutrition.service.diet.DietAPIService
import com.tomady.nutrition.service.diet.NutritionSummary
import com.tomady.nutrition.service.diet.ProfileValidationResult
import com.tomady.nutrition.service.foodb.FooDBDataAPIService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * React Native bridge module exposing diet-planning capabilities to the RN host.
 *
 * Provides methods for user/profile management, meal logging, nutrition analysis,
 * and daily planning.
 *
 * All public methods follow the RN Promise pattern:
 * - On success: [promise.resolve] with a [WritableMap] or [WritableArray]
 * - On error: [promise.reject] with an error code and message
 */
class DietModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val moduleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val service: DietAPIService by lazy {
        val db = AppDatabase.getInstance(reactContext.applicationContext)
        val dietDb = DietDatabase(
            userDao = db.userDao(),
            profileDao = db.profileDao(),
            bioRecordDao = db.bioRecordDao(),
            dishDao = db.dishDao(),
            recipeDao = db.recipeDao(),
            recipeIngredientDao = db.recipeIngredientDao(),
            dishHistoryDao = db.dishHistoryDao()
        )
        val foodbDb = FooDBLocalDatabase(
            foodItemDao = db.foodItemDao(),
            nutrientPropertyDao = db.nutrientPropertyDao()
        )
        val foodbService = FooDBDataAPIService(localDatabase = foodbDb)
        DietAPIService(dietDatabase = dietDb, foodbService = foodbService)
    }

    override fun getName(): String = "TomadyDiet"

    // ══════════════════════════════════════════════════════════════════════
    // User & Profile
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Creates a new user account.
     *
     * @param id       Unique user identifier.
     * @param username Display name.
     * @param email    Email address.
     * @param promise  Resolves with the created user as a WritableMap.
     */
    @ReactMethod
    fun createUser(id: String, username: String, email: String, promise: Promise) {
        moduleScope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    service.createUser(id, username, email)
                }
                promise.resolve(userToWritableMap(user))
            } catch (e: Exception) {
                handleError("CREATE_USER_ERROR", e, promise)
            }
        }
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param userId  User identifier.
     * @param promise Resolves with the user as a WritableMap, or null.
     */
    @ReactMethod
    fun getUser(userId: String, promise: Promise) {
        moduleScope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    service.getUser(userId)
                }
                if (user != null) {
                    promise.resolve(userToWritableMap(user))
                } else {
                    promise.resolve(null)
                }
            } catch (e: Exception) {
                handleError("GET_USER_ERROR", e, promise)
            }
        }
    }

    /**
     * Retrieves the profile for a user.
     *
     * @param userId  User identifier.
     * @param promise Resolves with the profile as a WritableMap, or null.
     */
    @ReactMethod
    fun getProfile(userId: String, promise: Promise) {
        moduleScope.launch {
            try {
                val profile = withContext(Dispatchers.IO) {
                    service.getProfile(userId)
                }
                if (profile != null) {
                    promise.resolve(profileToWritableMap(profile))
                } else {
                    promise.resolve(null)
                }
            } catch (e: Exception) {
                handleError("GET_PROFILE_ERROR", e, promise)
            }
        }
    }

    /**
     * Creates or updates a user's health profile.
     *
     * @param params  ReadableMap with profile fields.
     * @param promise Resolves with the updated profile.
     */
    @ReactMethod
    fun updateProfile(params: ReadableMap, promise: Promise) {
        moduleScope.launch {
            try {
                val userId = params.getString("userId") ?: run {
                    promise.reject("INVALID_PARAMS", "userId is required")
                    return@launch
                }

                val existingProfile = withContext(Dispatchers.IO) {
                    service.getProfile(userId)
                }

                if (existingProfile != null) {
                    val updated = existingProfile.copy(
                        displayName = params.getString("displayName") ?: existingProfile.displayName,
                        dateOfBirth = params.getString("dateOfBirth") ?: existingProfile.dateOfBirth,
                        heightCm = if (params.hasKey("heightCm")) params.getDouble("heightCm") else existingProfile.heightCm,
                        weightKg = if (params.hasKey("weightKg")) params.getDouble("weightKg") else existingProfile.weightKg,
                        dailyCalorieTarget = if (params.hasKey("dailyCalorieTarget")) params.getInt("dailyCalorieTarget") else existingProfile.dailyCalorieTarget,
                        proteinGramsTarget = if (params.hasKey("proteinGramsTarget")) params.getInt("proteinGramsTarget") else existingProfile.proteinGramsTarget,
                        carbsGramsTarget = if (params.hasKey("carbsGramsTarget")) params.getInt("carbsGramsTarget") else existingProfile.carbsGramsTarget,
                        fatGramsTarget = if (params.hasKey("fatGramsTarget")) params.getInt("fatGramsTarget") else existingProfile.fatGramsTarget,
                        goal = params.getString("goal") ?: existingProfile.goal
                    )
                    withContext(Dispatchers.IO) { service.updateProfile(updated) }
                    promise.resolve(profileToWritableMap(updated))
                } else {
                    val created = withContext(Dispatchers.IO) {
                        service.createProfile(
                            userId = userId,
                            displayName = params.getString("displayName"),
                            dateOfBirth = params.getString("dateOfBirth"),
                            heightCm = if (params.hasKey("heightCm")) params.getDouble("heightCm") else null,
                            weightKg = if (params.hasKey("weightKg")) params.getDouble("weightKg") else null,
                            dailyCalorieTarget = if (params.hasKey("dailyCalorieTarget")) params.getInt("dailyCalorieTarget") else null,
                            proteinGramsTarget = if (params.hasKey("proteinGramsTarget")) params.getInt("proteinGramsTarget") else null,
                            carbsGramsTarget = if (params.hasKey("carbsGramsTarget")) params.getInt("carbsGramsTarget") else null,
                            fatGramsTarget = if (params.hasKey("fatGramsTarget")) params.getInt("fatGramsTarget") else null,
                            goal = params.getString("goal")
                        )
                    }
                    promise.resolve(profileToWritableMap(created))
                }
            } catch (e: Exception) {
                handleError("UPDATE_PROFILE_ERROR", e, promise)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Bio Records
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Records a biometric measurement for a user.
     *
     * @param userId   User identifier.
     * @param date     Date string (yyyy-MM-dd).
     * @param weightKg Optional weight in kg.
     * @param bodyFat  Optional body fat percentage.
     * @param systolic Optional systolic BP.
     * @param diastolic Optional diastolic BP.
     * @param notes    Optional notes.
     * @param promise  Resolves with the created BioRecord.
     */
    @ReactMethod
    fun recordBio(
        userId: String,
        date: String,
        weightKg: Double,
        bodyFat: Double,
        systolic: Int,
        diastolic: Int,
        notes: String,
        promise: Promise
    ) {
        moduleScope.launch {
            try {
                val record = withContext(Dispatchers.IO) {
                    service.recordBio(
                        userId = userId,
                        date = date,
                        weightKg = if (weightKg.isNaN()) null else weightKg,
                        bodyFatPercentage = if (bodyFat.isNaN()) null else bodyFat,
                        systolicBp = if (systolic == 0) null else systolic,
                        diastolicBp = if (diastolic == 0) null else diastolic,
                        notes = notes.ifBlank { null }
                    )
                }
                promise.resolve(bioRecordToWritableMap(record))
            } catch (e: Exception) {
                handleError("RECORD_BIO_ERROR", e, promise)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Dish History / Meal Logging
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Logs a dish consumption event (a meal).
     *
     * @param userId   User identifier.
     * @param dishId   Dish identifier (optional, can be null).
     * @param date     Date string (yyyy-MM-dd).
     * @param mealType Meal type (e.g., "Breakfast", "Lunch", "Dinner", "Snack").
     * @param servings Number of servings (default 1.0).
     * @param notes    Optional notes.
     * @param promise  Resolves with the created DishHistory entry.
     */
    @ReactMethod
    fun logMeal(
        userId: String,
        dishId: String,
        date: String,
        mealType: String,
        servings: Double,
        notes: String,
        promise: Promise
    ) {
        moduleScope.launch {
            try {
                val history = withContext(Dispatchers.IO) {
                    service.logMealConsumption(
                        userId = userId,
                        dishId = dishId.ifBlank { null },
                        date = date,
                        mealType = mealType.ifBlank { null },
                        servings = if (servings.isNaN()) 1.0 else servings,
                        notes = notes.ifBlank { null }
                    )
                }
                promise.resolve(dishHistoryToWritableMap(history))
            } catch (e: Exception) {
                handleError("LOG_MEAL_ERROR", e, promise)
            }
        }
    }

    /**
     * Retrieves meal history for a user on a specific date.
     *
     * @param userId  User identifier.
     * @param date    Date string (yyyy-MM-dd).
     * @param promise Resolves with a WritableArray of DishHistory entries.
     */
    @ReactMethod
    fun getDishHistory(userId: String, date: String, promise: Promise) {
        moduleScope.launch {
            try {
                val history = withContext(Dispatchers.IO) {
                    service.getDishHistoryByDate(userId, date)
                }
                val array = Arguments.createArray()
                for (entry in history) {
                    array.pushMap(dishHistoryToWritableMap(entry))
                }
                promise.resolve(array)
            } catch (e: Exception) {
                handleError("GET_HISTORY_ERROR", e, promise)
            }
        }
    }

    /**
     * Retrieves meal history for a user over a date range.
     *
     * @param userId    User identifier.
     * @param startDate Start date (yyyy-MM-dd).
     * @param endDate   End date (yyyy-MM-dd).
     * @param promise   Resolves with a WritableArray of DishHistory entries.
     */
    @ReactMethod
    fun getHistory(userId: String, startDate: String, endDate: String, promise: Promise) {
        moduleScope.launch {
            try {
                val history = withContext(Dispatchers.IO) {
                    service.getDishHistoryInRange(userId, startDate, endDate)
                }
                val array = Arguments.createArray()
                for (entry in history) {
                    array.pushMap(dishHistoryToWritableMap(entry))
                }
                promise.resolve(array)
            } catch (e: Exception) {
                handleError("GET_HISTORY_RANGE_ERROR", e, promise)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Nutrition Analysis
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Computes the full nutritional breakdown of a dish.
     *
     * @param dishId  Dish identifier.
     * @param promise Resolves with a NutritionSummary WritableMap.
     */
    @ReactMethod
    fun getDishNutrition(dishId: String, promise: Promise) {
        moduleScope.launch {
            try {
                val summary = withContext(Dispatchers.IO) {
                    service.computeDishNutrition(dishId)
                }
                if (summary != null) {
                    promise.resolve(nutritionSummaryToWritableMap(summary))
                } else {
                    promise.resolve(null)
                }
            } catch (e: Exception) {
                handleError("DISH_NUTRITION_ERROR", e, promise)
            }
        }
    }

    /**
     * Validates a dish against the user's health profile.
     *
     * @param dishId  Dish identifier.
     * @param userId  User identifier (to look up their profile).
     * @param promise Resolves with a ProfileValidationResult WritableMap.
     */
    @ReactMethod
    fun validateDish(dishId: String, userId: String, promise: Promise) {
        moduleScope.launch {
            try {
                val profile = withContext(Dispatchers.IO) { service.getProfile(userId) }
                if (profile == null) {
                    promise.reject("PROFILE_NOT_FOUND", "Profile not found for user $userId")
                    return@launch
                }
                val validation = withContext(Dispatchers.IO) {
                    service.validateDishForProfile(dishId, profile)
                }
                promise.resolve(validationResultToWritableMap(validation))
            } catch (e: Exception) {
                handleError("VALIDATE_DISH_ERROR", e, promise)
            }
        }
    }

    /**
     * Computes daily calorie/macro targets for a user based on their profile.
     *
     * @param userId  User identifier.
     * @param promise Resolves with a DailyTargets WritableMap.
     */
    @ReactMethod
    fun getDailyPlan(userId: String, promise: Promise) {
        moduleScope.launch {
            try {
                val profile = withContext(Dispatchers.IO) { service.getProfile(userId) }
                if (profile == null) {
                    promise.reject("PROFILE_NOT_FOUND", "Profile not found for user $userId")
                    return@launch
                }
                val latestBio = withContext(Dispatchers.IO) {
                    // Get today's bio or latest available
                    null
                }
                val targets = withContext(Dispatchers.IO) {
                    service.computeDailyTargets(profile, latestBio)
                }
                if (targets != null) {
                    promise.resolve(dailyTargetsToWritableMap(targets))
                } else {
                    promise.resolve(null)
                }
            } catch (e: Exception) {
                handleError("DAILY_PLAN_ERROR", e, promise)
            }
        }
    }

    /**
     * Computes today's aggregate nutrition summary.
     *
     * @param userId  User identifier.
     * @param date    Today's date (yyyy-MM-dd).
     * @param promise Resolves with a DailySummary WritableMap.
     */
    @ReactMethod
    fun getTodaySummary(userId: String, date: String, promise: Promise) {
        moduleScope.launch {
            try {
                val summary = withContext(Dispatchers.IO) {
                    service.getDailySummary(userId, date)
                }
                if (summary != null) {
                    promise.resolve(dailySummaryToWritableMap(summary))
                } else {
                    promise.resolve(null)
                }
            } catch (e: Exception) {
                handleError("TODAY_SUMMARY_ERROR", e, promise)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Mapping Helpers
    // ══════════════════════════════════════════════════════════════════════

    private fun userToWritableMap(user: com.tomady.nutrition.data.local.diet.entity.User): WritableMap {
        val map = Arguments.createMap()
        map.putString("id", user.id)
        map.putString("username", user.username)
        map.putString("email", user.email)
        map.putDouble("createdAt", user.createdAt.toDouble())
        map.putDouble("updatedAt", user.updatedAt.toDouble())
        return map
    }

    private fun profileToWritableMap(profile: com.tomady.nutrition.data.local.diet.entity.Profile): WritableMap {
        val map = Arguments.createMap()
        map.putString("id", profile.id)
        map.putString("userId", profile.userId)
        profile.displayName?.let { map.putString("displayName", it) }
        profile.dateOfBirth?.let { map.putString("dateOfBirth", it) }
        profile.heightCm?.let { map.putDouble("heightCm", it) }
        profile.weightKg?.let { map.putDouble("weightKg", it) }
        profile.dailyCalorieTarget?.let { map.putInt("dailyCalorieTarget", it) }
        profile.proteinGramsTarget?.let { map.putInt("proteinGramsTarget", it) }
        profile.carbsGramsTarget?.let { map.putInt("carbsGramsTarget", it) }
        profile.fatGramsTarget?.let { map.putInt("fatGramsTarget", it) }
        profile.goal?.let { map.putString("goal", it) }
        return map
    }

    private fun bioRecordToWritableMap(record: com.tomady.nutrition.data.local.diet.entity.BioRecord): WritableMap {
        val map = Arguments.createMap()
        map.putString("id", record.id)
        map.putString("userId", record.userId)
        map.putString("date", record.date)
        record.weightKg?.let { map.putDouble("weightKg", it) }
        record.bodyFatPercentage?.let { map.putDouble("bodyFatPercentage", it) }
        record.systolicBp?.let { map.putInt("systolicBp", it) }
        record.diastolicBp?.let { map.putInt("diastolicBp", it) }
        record.notes?.let { map.putString("notes", it) }
        return map
    }

    private fun dishHistoryToWritableMap(history: com.tomady.nutrition.data.local.diet.entity.DishHistory): WritableMap {
        val map = Arguments.createMap()
        map.putString("id", history.id)
        map.putString("userId", history.userId)
        history.dishId?.let { map.putString("dishId", it) }
        map.putString("date", history.date)
        history.mealType?.let { map.putString("mealType", it) }
        map.putDouble("servings", history.servings)
        history.notes?.let { map.putString("notes", it) }
        map.putDouble("createdAt", history.createdAt.toDouble())
        return map
    }

    private fun nutritionSummaryToWritableMap(summary: NutritionSummary): WritableMap {
        val map = Arguments.createMap()
        map.putString("dishId", summary.dishId)
        map.putString("dishName", summary.dishName)
        map.putDouble("totalCalories", summary.totalCalories)
        map.putDouble("totalProteinG", summary.totalProteinG)
        map.putDouble("totalCarbsG", summary.totalCarbsG)
        map.putDouble("totalFatG", summary.totalFatG)
        map.putDouble("totalFiberG", summary.totalFiberG)
        map.putDouble("totalSugarG", summary.totalSugarG)
        map.putDouble("totalSodiumMg", summary.totalSodiumMg)

        val detailsArray = Arguments.createArray()
        for (detail in summary.nutrientDetails) {
            val detailMap = Arguments.createMap()
            detailMap.putString("nutrientName", detail.nutrientName)
            detailMap.putDouble("amount", detail.amount)
            detailMap.putString("unit", detail.unit)
            detailMap.putDouble("sourceFoodId", detail.sourceFoodId.toDouble())
            detailMap.putString("sourceFoodName", detail.sourceFoodName)
            detailsArray.pushMap(detailMap)
        }
        map.putArray("nutrientDetails", detailsArray)
        return map
    }

    private fun validationResultToWritableMap(result: ProfileValidationResult): WritableMap {
        val map = Arguments.createMap()
        map.putString("dishId", result.dishId)
        map.putBoolean("isCompatible", result.isCompatible)
        val warningsArray = Arguments.createArray()
        for (warning in result.warnings) {
            warningsArray.pushString(warning)
        }
        map.putArray("warnings", warningsArray)
        return map
    }

    private fun dailyTargetsToWritableMap(targets: DailyTargets): WritableMap {
        val map = Arguments.createMap()
        map.putInt("calories", targets.calories)
        map.putInt("proteinG", targets.proteinG)
        map.putInt("carbsG", targets.carbsG)
        map.putInt("fatG", targets.fatG)
        return map
    }

    private fun dailySummaryToWritableMap(summary: DailySummary): WritableMap {
        val map = Arguments.createMap()
        map.putString("userId", summary.userId)
        map.putString("date", summary.date)
        map.putDouble("totalCalories", summary.totalCalories)
        map.putDouble("totalProteinG", summary.totalProteinG)
        map.putDouble("totalCarbsG", summary.totalCarbsG)
        map.putDouble("totalFatG", summary.totalFatG)

        val mealsArray = Arguments.createArray()
        for (meal in summary.meals) {
            val mealMap = Arguments.createMap()
            mealMap.putString("mealType", meal.mealType)
            mealMap.putDouble("totalCalories", meal.totalCalories)
            mealMap.putDouble("totalProteinG", meal.totalProteinG)
            mealMap.putDouble("totalCarbsG", meal.totalCarbsG)
            mealMap.putDouble("totalFatG", meal.totalFatG)
            mealsArray.pushMap(mealMap)
        }
        map.putArray("meals", mealsArray)
        return map
    }

    // ══════════════════════════════════════════════════════════════════════
    // Error Handling
    // ══════════════════════════════════════════════════════════════════════

    private fun handleError(code: String, error: Exception, promise: Promise) {
        when (error) {
            is IOException -> {
                promise.reject("NETWORK_ERROR", "Network request failed: ${error.message}", error)
            }
            else -> {
                promise.reject(code, error.message ?: "An unexpected error occurred", error)
            }
        }
    }
}
