package com.tomady.nutrition.demo

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.tomady.nutrition.data.AppDatabase
import com.tomady.nutrition.data.local.diet.DietDatabase
import com.tomady.nutrition.data.local.foodb.FooDBLocalDatabase
import com.tomady.nutrition.service.diet.DailySummary
import com.tomady.nutrition.service.diet.DailyTargets
import com.tomady.nutrition.service.diet.DietAPIService
import com.tomady.nutrition.service.diet.NutritionSummary
import com.tomady.nutrition.service.foodb.FooDBDataAPIService
import com.tomady.nutrition.service.gemma.GemmaAndroidService
import com.tomady.nutrition.service.gemma.GemmaAnswerResult
import com.tomady.nutrition.service.gemma.GemmaRecipeResult
import com.tomady.nutrition.worker.DailySuggestionWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * WebView JavaScript interface that wraps all Tomady backend services.
 *
 * Exposed to JavaScript via [android.webkit.WebView.addJavascriptInterface].
 * All methods run on [Dispatchers.IO] and use JSON for data exchange.
 *
 * @param context Application context for database initialization.
 */
class DemoJSBridge(context: Context) {

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val workManager = WorkManager.getInstance(context.applicationContext)

    // ── Lazy service initialisation ────────────────────────────────────

    private val foodbService: FooDBDataAPIService by lazy {
        val db = AppDatabase.getInstance(context.applicationContext)
        val localDb = FooDBLocalDatabase(
            foodItemDao = db.foodItemDao(),
            nutrientPropertyDao = db.nutrientPropertyDao()
        )
        FooDBDataAPIService(localDatabase = localDb)
    }

    private val dietService: DietAPIService by lazy {
        val db = AppDatabase.getInstance(context.applicationContext)
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

    private val gemmaService: GemmaAndroidService by lazy {
        val db = AppDatabase.getInstance(context.applicationContext)
        val dietDb = DietDatabase(
            userDao = db.userDao(),
            profileDao = db.profileDao(),
            bioRecordDao = db.bioRecordDao(),
            dishDao = db.dishDao(),
            recipeDao = db.recipeDao(),
            recipeIngredientDao = db.recipeIngredientDao(),
            dishHistoryDao = db.dishHistoryDao()
        )
        GemmaAndroidService(
            context = context.applicationContext,
            dietDatabase = dietDb,
            dietService = dietService,
            foodbService = foodbService
        )
    }

    // ── Helper ─────────────────────────────────────────────────────────

    private fun <T> runIO(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }

    // ════════════════════════════════════════════════════════════════════
    // SECTION 1 — FooDB
    // ════════════════════════════════════════════════════════════════════

    @JavascriptInterface
    fun searchFood(query: String): String = runIO {
        try {
            val results = foodbService.searchFood(query)
            gson.toJson(mapOf("ok" to true, "data" to results))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun getFoodDetails(foodId: Long): String = runIO {
        try {
            val detail = foodbService.getFoodDetails(foodId)
            if (detail != null) {
                gson.toJson(mapOf("ok" to true, "data" to detail))
            } else {
                gson.toJson(mapOf("ok" to false, "error" to "Food not found"))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun getFoodGroups(): String = runIO {
        try {
            val groups = foodbService.getFoodGroups()
            gson.toJson(mapOf("ok" to true, "data" to groups))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun getNutrientNames(): String = runIO {
        try {
            val names = foodbService.getNutrientNames()
            gson.toJson(mapOf("ok" to true, "data" to names))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // SECTION 2 — Diet Service (User, Profile, Bio, Meals, Nutrition)
    // ════════════════════════════════════════════════════════════════════

    @JavascriptInterface
    fun createUser(json: String): String = runIO {
        try {
            val params = gson.fromJson(json, Map::class.java)
            val user = dietService.createUser(
                id = params["id"] as? String ?: "",
                username = params["username"] as? String ?: "",
                email = params["email"] as? String ?: ""
            )
            gson.toJson(mapOf("ok" to true, "data" to user))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun getUser(userId: String): String = runIO {
        try {
            val user = dietService.getUser(userId)
            gson.toJson(mapOf("ok" to true, "data" to user))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun getProfile(userId: String): String = runIO {
        try {
            val profile = dietService.getProfile(userId)
            gson.toJson(mapOf("ok" to true, "data" to profile))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun updateProfile(json: String): String = runIO {
        try {
            val params = gson.fromJson(json, Map::class.java)
            val userId = params["userId"] as? String ?: return@runIO gson.toJson(
                mapOf("ok" to false, "error" to "userId required")
            )
            val existing = dietService.getProfile(userId)
            if (existing != null) {
                val updated = existing.copy(
                    goal = params["goal"] as? String ?: existing.goal,
                    displayName = params["displayName"] as? String ?: existing.displayName,
                    age = (params["age"] as? Number)?.toInt() ?: existing.age,
                    heightCm = (params["heightCm"] as? Number)?.toDouble() ?: existing.heightCm,
                    weightKg = (params["weightKg"] as? Number)?.toDouble() ?: existing.weightKg,
                    dailyCalorieTarget = (params["dailyCalorieTarget"] as? Number)?.toInt() ?: existing.dailyCalorieTarget,
                    activityLevel = params["activityLevel"] as? String ?: existing.activityLevel,
                    allergies = params["allergies"] as? String ?: existing.allergies,
                    intolerances = params["intolerances"] as? String ?: existing.intolerances,
                    conditions = params["conditions"] as? String ?: existing.conditions,
                    restrictedFoods = params["restrictedFoods"] as? String ?: existing.restrictedFoods,
                    forbiddenByDoctor = params["forbiddenByDoctor"] as? String ?: existing.forbiddenByDoctor
                )
                dietService.updateProfile(updated)
                gson.toJson(mapOf("ok" to true, "data" to updated))
            } else {
                val created = dietService.createProfile(
                    userId = userId,
                    goal = params["goal"] as? String,
                    displayName = params["displayName"] as? String,
                    age = (params["age"] as? Number)?.toInt(),
                    heightCm = (params["heightCm"] as? Number)?.toDouble(),
                    weightKg = (params["weightKg"] as? Number)?.toDouble(),
                    dailyCalorieTarget = (params["dailyCalorieTarget"] as? Number)?.toInt(),
                    activityLevel = params["activityLevel"] as? String,
                    allergies = params["allergies"] as? String,
                    intolerances = params["intolerances"] as? String,
                    conditions = params["conditions"] as? String,
                    restrictedFoods = params["restrictedFoods"] as? String,
                    forbiddenByDoctor = params["forbiddenByDoctor"] as? String
                )
                gson.toJson(mapOf("ok" to true, "data" to created))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun recordBio(json: String): String = runIO {
        try {
            val params = gson.fromJson(json, Map::class.java)
            val record = dietService.recordBio(
                userId = params["userId"] as? String ?: "",
                date = params["date"] as? String ?: "",
                weightKg = (params["weightKg"] as? Number)?.toDouble(),
                bodyFatPercentage = (params["bodyFatPercentage"] as? Number)?.toDouble(),
                systolicBp = (params["systolicBp"] as? Number)?.toInt(),
                diastolicBp = (params["diastolicBp"] as? Number)?.toInt(),
                notes = params["notes"] as? String
            )
            gson.toJson(mapOf("ok" to true, "data" to record))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun logMeal(json: String): String = runIO {
        try {
            val params = gson.fromJson(json, Map::class.java)
            val history = dietService.logMealConsumption(
                userId = params["userId"] as? String ?: "",
                dishId = params["dishId"] as? String,
                date = params["date"] as? String ?: "",
                mealType = params["mealType"] as? String,
                servings = (params["servings"] as? Number)?.toDouble() ?: 1.0,
                notes = params["notes"] as? String
            )
            gson.toJson(mapOf("ok" to true, "data" to history))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun getDishHistory(userId: String, date: String): String = runIO {
        try {
            val history = dietService.getDishHistoryByDate(userId, date)
            gson.toJson(mapOf("ok" to true, "data" to history))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun getHistoryRange(userId: String, startDate: String, endDate: String): String = runIO {
        try {
            val history = dietService.getDishHistoryInRange(userId, startDate, endDate)
            gson.toJson(mapOf("ok" to true, "data" to history))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun getDishNutrition(dishId: String): String = runIO {
        try {
            val nutrition = dietService.computeDishNutrition(dishId)
            gson.toJson(mapOf("ok" to true, "data" to nutrition))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun validateDish(dishId: String, userId: String): String = runIO {
        try {
            val profile = dietService.getProfile(userId)
            if (profile != null) {
                val validation = dietService.validateDishForProfile(dishId, profile)
                gson.toJson(mapOf("ok" to true, "data" to validation))
            } else {
                gson.toJson(mapOf("ok" to false, "error" to "Profile not found"))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun getDailyPlan(userId: String): String = runIO {
        try {
            val profile = dietService.getProfile(userId)
            if (profile != null) {
                val targets = dietService.computeDailyTargets(profile, null)
                gson.toJson(mapOf("ok" to true, "data" to targets))
            } else {
                gson.toJson(mapOf("ok" to false, "error" to "Profile not found"))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun getTodaySummary(userId: String, date: String): String = runIO {
        try {
            val summary = dietService.getDailySummary(userId, date)
            gson.toJson(mapOf("ok" to true, "data" to summary))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // SECTION 3 — Gemma LLM
    // ════════════════════════════════════════════════════════════════════

    @JavascriptInterface
    fun loadModel(): String = runIO {
        try {
            val result = gemmaService.loadModel(null)
            gson.toJson(mapOf("ok" to true, "data" to result))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun isModelLoaded(): String = runIO {
        gson.toJson(mapOf("ok" to true, "data" to gemmaService.isModelLoaded()))
    }

    @JavascriptInterface
    fun releaseModel(): String = runIO {
        try {
            gemmaService.release()
            gson.toJson(mapOf("ok" to true, "data" to "Model released"))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun computeRecipe(prompt: String, userId: String): String = runIO {
        try {
            if (!gemmaService.isModelLoaded()) {
                gemmaService.loadModel(null)
            }
            val result = gemmaService.computeRecipe(prompt, userId)
            gson.toJson(mapOf("ok" to true, "data" to result))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun askQuestion(question: String, userId: String): String = runIO {
        try {
            if (!gemmaService.isModelLoaded()) {
                gemmaService.loadModel(null)
            }
            val result = gemmaService.askQuestion(question, userId)
            gson.toJson(mapOf("ok" to true, "data" to result))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }

    @JavascriptInterface
    fun triggerDailyWorker(): String {
        return try {
            val request = OneTimeWorkRequestBuilder<DailySuggestionWorker>().build()
            workManager.enqueue(request)
            gson.toJson(mapOf("ok" to true, "data" to "Daily suggestion worker queued via WorkManager"))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to e.message))
        }
    }
}
