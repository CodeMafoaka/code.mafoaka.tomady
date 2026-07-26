package com.tomady.nutrition.bridge

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tomady.nutrition.data.AppDatabase
import com.tomady.nutrition.data.local.diet.DietDatabase
import com.tomady.nutrition.service.diet.DietAPIService
import com.tomady.nutrition.service.diet.DailyTargets
import com.tomady.nutrition.service.foodb.FooDBDataAPIService
import com.tomady.nutrition.data.local.foodb.FooDBLocalDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * React Native bridge module exposing [DietAPIService] to JavaScript.
 *
 * ## JS Usage
 * ```js
 * import { NativeModules } from 'react-native';
 * const { DietModule } = NativeModules;
 *
 * // Create a user
 * const user = await DietModule.createUser('user-1', 'John Doe', 'john@example.com');
 *
 * // Create/update profile
 * const profile = await DietModule.createProfile('user-1', {
 *   age: 30,
 *   goal: 'Perte de poids',
 *   allergies: '["arachide","lactose"]',
 *   conditions: '["diabetes"]'
 * });
 *
 * // Log a meal
 * const meal = await DietModule.logMeal('user-1', 'dish-1', '2026-07-26', 'dejeuner');
 *
 * // Get daily summary
 * const summary = await DietModule.getDailySummary('user-1', '2026-07-26');
 *
 * // Validate a dish against profile
 * const validation = await DietModule.validateDish('dish-1', 'user-1');
 * // validation = { dishId: '...', isCompatible: true, warnings: [] }
 * ```
 */
class DietModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = MODULE_NAME

    // ── Service initialisation ──────────────────────────────────────────

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val gson = Gson()

    private val dietService: DietAPIService by lazy {
        val db = AppDatabase.getInstance(reactApplicationContext)
        val localDb = FooDBLocalDatabase(
            foodItemDao = db.foodItemDao(),
            nutrientPropertyDao = db.nutrientPropertyDao()
        )
        val foodbService = FooDBDataAPIService(localDatabase = localDb)
        val dietDb = DietDatabase(
            userDao = db.userDao(),
            profileDao = db.profileDao(),
            bioRecordDao = db.bioRecordDao(),
            dishDao = db.dishDao(),
            recipeDao = db.recipeDao(),
            recipeIngredientDao = db.recipeIngredientDao(),
            dishHistoryDao = db.dishHistoryDao()
        )
        DietAPIService(dietDatabase = dietDb, foodbService = foodbService)
    }

    // ════════════════════════════════════════════════════════════════════
    // SECTION 1 — User CRUD
    // ════════════════════════════════════════════════════════════════════

    @ReactMethod
    fun createUser(id: String, username: String, email: String, promise: Promise) {
        scope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    dietService.createUser(id, username, email)
                }
                promise.resolve(mapToWritable(
                    gson.fromJson(gson.toJson(user), Map::class.java)
                ))
            } catch (e: Exception) {
                promise.reject("DIET_USER_CREATE_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun getUser(userId: String, promise: Promise) {
        scope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    dietService.getUser(userId)
                }
                if (user != null) {
                    promise.resolve(mapToWritable(
                        gson.fromJson(gson.toJson(user), Map::class.java)
                    ))
                } else {
                    promise.resolve(null)
                }
            } catch (e: Exception) {
                promise.reject("DIET_USER_GET_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun getUserByEmail(email: String, promise: Promise) {
        scope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    dietService.getUserByEmail(email)
                }
                if (user != null) {
                    promise.resolve(mapToWritable(
                        gson.fromJson(gson.toJson(user), Map::class.java)
                    ))
                } else {
                    promise.resolve(null)
                }
            } catch (e: Exception) {
                promise.reject("DIET_USER_EMAIL_ERROR", e.message, e)
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // SECTION 2 — Profile CRUD
    // ════════════════════════════════════════════════════════════════════

    @ReactMethod
    fun createProfile(
        userId: String,
        displayName: String?,
        dateOfBirth: String?,
        heightCm: Double?,
        weightKg: Double?,
        dailyCalorieTarget: Int?,
        goal: String?,
        age: Int?,
        activityLevel: String?,
        allergies: String?,
        intolerances: String?,
        conditions: String?,
        restrictedFoods: String?,
        forbiddenByDoctor: String?,
        promise: Promise
    ) {
        scope.launch {
            try {
                val profile = withContext(Dispatchers.IO) {
                    dietService.createProfile(
                        userId = userId,
                        displayName = displayName,
                        dateOfBirth = dateOfBirth,
                        heightCm = heightCm,
                        weightKg = weightKg,
                        dailyCalorieTarget = dailyCalorieTarget,
                        goal = goal,
                        age = age,
                        activityLevel = activityLevel,
                        allergies = allergies,
                        intolerances = intolerances,
                        conditions = conditions,
                        restrictedFoods = restrictedFoods,
                        forbiddenByDoctor = forbiddenByDoctor
                    )
                }
                promise.resolve(mapToWritable(
                    gson.fromJson(gson.toJson(profile), Map::class.java)
                ))
            } catch (e: Exception) {
                promise.reject("DIET_PROFILE_CREATE_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun getProfile(userId: String, promise: Promise) {
        scope.launch {
            try {
                val profile = withContext(Dispatchers.IO) {
                    dietService.getProfile(userId)
                }
                if (profile != null) {
                    promise.resolve(mapToWritable(
                        gson.fromJson(gson.toJson(profile), Map::class.java)
                    ))
                } else {
                    promise.resolve(null)
                }
            } catch (e: Exception) {
                promise.reject("DIET_PROFILE_GET_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun updateProfile(userId: String, json: String, promise: Promise) {
        scope.launch {
            try {
                val params: Map<String, Any?> = gson.fromJson(json, Map::class.java)
                val existing = withContext(Dispatchers.IO) {
                    dietService.getProfile(userId)
                }
                if (existing != null) {
                    val updated = existing.copy(
                        displayName = params["displayName"] as? String ?: existing.displayName,
                        dateOfBirth = params["dateOfBirth"] as? String ?: existing.dateOfBirth,
                        heightCm = (params["heightCm"] as? Number)?.toDouble() ?: existing.heightCm,
                        weightKg = (params["weightKg"] as? Number)?.toDouble() ?: existing.weightKg,
                        dailyCalorieTarget = (params["dailyCalorieTarget"] as? Number)?.toInt() ?: existing.dailyCalorieTarget,
                        goal = params["goal"] as? String ?: existing.goal,
                        age = (params["age"] as? Number)?.toInt() ?: existing.age,
                        activityLevel = params["activityLevel"] as? String ?: existing.activityLevel,
                        allergies = params["allergies"] as? String ?: existing.allergies,
                        intolerances = params["intolerances"] as? String ?: existing.intolerances,
                        conditions = params["conditions"] as? String ?: existing.conditions,
                        restrictedFoods = params["restrictedFoods"] as? String ?: existing.restrictedFoods,
                        forbiddenByDoctor = params["forbiddenByDoctor"] as? String ?: existing.forbiddenByDoctor
                    )
                    withContext(Dispatchers.IO) { dietService.updateProfile(updated) }
                    promise.resolve(mapToWritable(
                        gson.fromJson(gson.toJson(updated), Map::class.java)
                    ))
                } else {
                    promise.reject("DIET_PROFILE_NOT_FOUND", "Profile not found for user: $userId")
                }
            } catch (e: Exception) {
                promise.reject("DIET_PROFILE_UPDATE_ERROR", e.message, e)
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // SECTION 3 — Biometric Records
    // ════════════════════════════════════════════════════════════════════

    @ReactMethod
    fun recordBio(
        userId: String,
        date: String,
        weightKg: Double?,
        bodyFatPercentage: Double?,
        systolicBp: Int?,
        diastolicBp: Int?,
        notes: String?,
        promise: Promise
    ) {
        scope.launch {
            try {
                val record = withContext(Dispatchers.IO) {
                    dietService.recordBio(
                        userId = userId,
                        date = date,
                        weightKg = weightKg,
                        bodyFatPercentage = bodyFatPercentage,
                        systolicBp = systolicBp,
                        diastolicBp = diastolicBp,
                        notes = notes
                    )
                }
                promise.resolve(mapToWritable(
                    gson.fromJson(gson.toJson(record), Map::class.java)
                ))
            } catch (e: Exception) {
                promise.reject("DIET_BIO_RECORD_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun getBioRecord(userId: String, date: String, promise: Promise) {
        scope.launch {
            try {
                val record = withContext(Dispatchers.IO) {
                    dietService.getBioRecord(userId, date)
                }
                if (record != null) {
                    promise.resolve(mapToWritable(
                        gson.fromJson(gson.toJson(record), Map::class.java)
                    ))
                } else {
                    promise.resolve(null)
                }
            } catch (e: Exception) {
                promise.reject("DIET_BIO_GET_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun getBioRecordsInRange(userId: String, startDate: String, endDate: String, promise: Promise) {
        scope.launch {
            try {
                val records = withContext(Dispatchers.IO) {
                    dietService.getBioRecordsInRange(userId, startDate, endDate)
                }
                val listType = object : TypeToken<List<Map<String, Any?>>>() {}.type
                val maps: List<Map<String, Any?>> = gson.fromJson(gson.toJson(records), listType)
                val array = Arguments.createArray()
                for (item in maps) {
                    array.pushMap(mapToWritable(item))
                }
                promise.resolve(array)
            } catch (e: Exception) {
                promise.reject("DIET_BIO_RANGE_ERROR", e.message, e)
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // SECTION 4 — Dish / Recipe CRUD
    // ════════════════════════════════════════════════════════════════════

    @ReactMethod
    fun createDish(name: String, description: String?, category: String?, promise: Promise) {
        scope.launch {
            try {
                val dish = withContext(Dispatchers.IO) {
                    dietService.createDish(name, description, category)
                }
                promise.resolve(mapToWritable(
                    gson.fromJson(gson.toJson(dish), Map::class.java)
                ))
            } catch (e: Exception) {
                promise.reject("DIET_DISH_CREATE_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun searchDishes(query: String, promise: Promise) {
        scope.launch {
            try {
                val dishes = withContext(Dispatchers.IO) {
                    dietService.searchDishes(query)
                }
                val listType = object : TypeToken<List<Map<String, Any?>>>() {}.type
                val maps: List<Map<String, Any?>> = gson.fromJson(gson.toJson(dishes), listType)
                val array = Arguments.createArray()
                for (item in maps) {
                    array.pushMap(mapToWritable(item))
                }
                promise.resolve(array)
            } catch (e: Exception) {
                promise.reject("DIET_DISH_SEARCH_ERROR", e.message, e)
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // SECTION 5 — Meal Logging (DishHistory)
    // ════════════════════════════════════════════════════════════════════

    @ReactMethod
    fun logMeal(
        userId: String,
        dishId: String?,
        date: String,
        mealType: String?,
        servings: Double?,
        notes: String?,
        promise: Promise
    ) {
        scope.launch {
            try {
                val history = withContext(Dispatchers.IO) {
                    dietService.logMealConsumption(
                        userId = userId,
                        dishId = dishId,
                        date = date,
                        mealType = mealType,
                        servings = servings ?: 1.0,
                        notes = notes
                    )
                }
                promise.resolve(mapToWritable(
                    gson.fromJson(gson.toJson(history), Map::class.java)
                ))
            } catch (e: Exception) {
                promise.reject("DIET_MEAL_LOG_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun getDishHistory(userId: String, date: String, promise: Promise) {
        scope.launch {
            try {
                val history = withContext(Dispatchers.IO) {
                    dietService.getDishHistoryByDate(userId, date)
                }
                val listType = object : TypeToken<List<Map<String, Any?>>>() {}.type
                val maps: List<Map<String, Any?>> = gson.fromJson(gson.toJson(history), listType)
                val array = Arguments.createArray()
                for (item in maps) {
                    array.pushMap(mapToWritable(item))
                }
                promise.resolve(array)
            } catch (e: Exception) {
                promise.reject("DIET_HISTORY_GET_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun getHistoryInRange(userId: String, startDate: String, endDate: String, promise: Promise) {
        scope.launch {
            try {
                val history = withContext(Dispatchers.IO) {
                    dietService.getDishHistoryInRange(userId, startDate, endDate)
                }
                val listType = object : TypeToken<List<Map<String, Any?>>>() {}.type
                val maps: List<Map<String, Any?>> = gson.fromJson(gson.toJson(history), listType)
                val array = Arguments.createArray()
                for (item in maps) {
                    array.pushMap(mapToWritable(item))
                }
                promise.resolve(array)
            } catch (e: Exception) {
                promise.reject("DIET_HISTORY_RANGE_ERROR", e.message, e)
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // SECTION 6 — Nutrition & Validation
    // ════════════════════════════════════════════════════════════════════

    @ReactMethod
    fun getDishNutrition(dishId: String, promise: Promise) {
        scope.launch {
            try {
                val nutrition = withContext(Dispatchers.IO) {
                    dietService.computeDishNutrition(dishId)
                }
                if (nutrition != null) {
                    promise.resolve(mapToWritable(
                        gson.fromJson(gson.toJson(nutrition), Map::class.java)
                    ))
                } else {
                    promise.resolve(null)
                }
            } catch (e: Exception) {
                promise.reject("DIET_NUTRITION_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun validateDish(dishId: String, userId: String, promise: Promise) {
        scope.launch {
            try {
                val profile = withContext(Dispatchers.IO) {
                    dietService.getProfile(userId)
                }
                if (profile != null) {
                    val validation = withContext(Dispatchers.IO) {
                        dietService.validateDishForProfile(dishId, profile)
                    }
                    promise.resolve(mapToWritable(
                        gson.fromJson(gson.toJson(validation), Map::class.java)
                    ))
                } else {
                    promise.reject("DIET_PROFILE_NOT_FOUND", "Profile not found for user: $userId")
                }
            } catch (e: Exception) {
                promise.reject("DIET_VALIDATION_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun getDailyPlan(userId: String, promise: Promise) {
        scope.launch {
            try {
                val profile = withContext(Dispatchers.IO) {
                    dietService.getProfile(userId)
                }
                if (profile != null) {
                    val targets = withContext(Dispatchers.IO) {
                        dietService.computeDailyTargets(profile, null)
                    }
                    if (targets != null) {
                        promise.resolve(mapToWritable(
                            gson.fromJson(gson.toJson(targets), Map::class.java)
                        ))
                    } else {
                        promise.resolve(null)
                    }
                } else {
                    promise.reject("DIET_PROFILE_NOT_FOUND", "Profile not found for user: $userId")
                }
            } catch (e: Exception) {
                promise.reject("DIET_PLAN_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun getDailySummary(userId: String, date: String, promise: Promise) {
        scope.launch {
            try {
                val summary = withContext(Dispatchers.IO) {
                    dietService.getDailySummary(userId, date)
                }
                if (summary != null) {
                    promise.resolve(mapToWritable(
                        gson.fromJson(gson.toJson(summary), Map::class.java)
                    ))
                } else {
                    promise.resolve(null)
                }
            } catch (e: Exception) {
                promise.reject("DIET_SUMMARY_ERROR", e.message, e)
            }
        }
    }

    // ── Helper (shared with other modules — kept private for encapsulation) ──

    private fun mapToWritable(map: Map<String, Any?>): WritableMap {
        val writable = Arguments.createMap()
        for ((key, value) in map) {
            when (value) {
                null -> writable.putNull(key)
                is Boolean -> writable.putBoolean(key, value)
                is Int -> writable.putInt(key, value)
                is Long -> writable.putDouble(key, value.toDouble())
                is Double -> writable.putDouble(key, value)
                is Float -> writable.putDouble(key, value.toDouble())
                is String -> writable.putString(key, value)
                is List<*> -> {
                    val array = Arguments.createArray()
                    for (item in value) {
                        when (item) {
                            is String -> array.pushString(item)
                            is Number -> array.pushDouble(item.toDouble())
                            is Boolean -> array.pushBoolean(item)
                            is Map<*, *> -> {
                                @Suppress("UNCHECKED_CAST")
                                array.pushMap(mapToWritable(item as Map<String, Any?>))
                            }
                            else -> array.pushString(item?.toString())
                        }
                    }
                    writable.putArray(key, array)
                }
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    writable.putMap(key, mapToWritable(value as Map<String, Any?>))
                }
                else -> writable.putString(key, value.toString())
            }
        }
        return writable
    }

    companion object {
        const val MODULE_NAME = "DietModule"
    }
}
