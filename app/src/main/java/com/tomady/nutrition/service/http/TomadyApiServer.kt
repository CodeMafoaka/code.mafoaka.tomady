package com.tomady.nutrition.service.http

import android.content.Context
import com.google.gson.Gson
import com.tomady.nutrition.BuildConfig
import com.tomady.nutrition.data.local.diet.entity.DishHistory
import com.tomady.nutrition.service.gemma.GemmaAnswerResult
import com.tomady.nutrition.service.gemma.GemmaRecipeResult
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tomady.nutrition.worker.DailySuggestionWorker
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.ResponseException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlinx.coroutines.runBlocking

internal class TomadyApiServer(
    context: Context,
    port: Int = BuildConfig.SERVICE_API_PORT
) : NanoHTTPD(port) {

    private val gson = Gson()
    private val services = TomadyServiceModule(context.applicationContext)
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun startServer() {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
    }

    override fun serve(session: IHTTPSession): Response {
        return try {
            route(session)
        } catch (exception: Exception) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                gson.toJson(mapOf("code" to 500, "message" to exception.message ?: "Internal server error"))
            )
        }
    }

    private fun route(session: IHTTPSession): Response {
        val uri = session.uri.trimEnd('/')
        val method = session.method
        val body = parseJsonBody(session)

        return when {
            uri == "/v1/food/search" && method == Method.GET -> handleFoodSearch(session)
            uri.startsWith("/v1/food/") && method == Method.GET -> handleFoodDetails(uri)
            uri.matches(Regex("^/v1/users/[^/]+/profile$")) && method == Method.GET -> handleGetProfile(uri)
            uri.matches(Regex("^/v1/users/[^/]+/profile$")) && method == Method.PUT -> handleUpdateProfile(uri, body)
            uri.matches(Regex("^/v1/users/[^/]+/biorecords$")) && method == Method.POST -> handleCreateBioRecord(uri, body)
            uri.matches(Regex("^/v1/users/[^/]+/history$")) && method == Method.GET -> handleGetHistory(session, uri)
            uri.matches(Regex("^/v1/users/[^/]+/history$")) && method == Method.POST -> handleLogMeal(uri, body)
            uri.matches(Regex("^/v1/dishes/[^/]+/nutrition$")) && method == Method.GET -> handleDishNutrition(uri)
            uri == "/v1/gemma/compute-recipe" && method == Method.POST -> handleComputeRecipe(body)
            uri == "/v1/gemma/ask" && method == Method.POST -> handleGemmaAsk(body)
            uri == "/v1/worker/suggestions/daily" && method == Method.GET -> handleGetDailySuggestions()
            uri == "/v1/worker/suggestions/daily" && method == Method.POST -> handleTriggerDailySuggestions()
            else -> newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                "application/json",
                gson.toJson(mapOf("code" to 404, "message" to "Endpoint not found"))
            )
        }
    }

    private fun parseJsonBody(session: IHTTPSession): Map<String, Any> {
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            val raw = files["postData"] ?: ""
            if (raw.isBlank()) {
                emptyMap()
            } else {
                gson.fromJson(raw, Map::class.java) as Map<String, Any>
            }
        } catch (exception: IOException) {
            emptyMap()
        } catch (exception: ResponseException) {
            emptyMap()
        }
    }

    private fun <T> runService(block: suspend () -> T): T = runBlocking { block() }

    private fun handleFoodSearch(session: IHTTPSession): Response {
        val query = session.parms["q"]
        if (query.isNullOrBlank()) {
            return badRequest("Missing required query parameter 'q'.")
        }
        val results = runService { services.foodbService.searchFood(query) }
        return ok(mapOf("query" to query, "results" to results))
    }

    private fun handleFoodDetails(uri: String): Response {
        val foodId = uri.removePrefix("/v1/food/").toLongOrNull()
        if (foodId == null) {
            return badRequest("Invalid foodId path parameter.")
        }
        val detail = runService { services.foodbService.getFoodDetails(foodId) }
        return if (detail != null) {
            ok(mapOf("food" to detail.food, "nutrients" to detail.nutrients))
        } else {
            notFound("Food item not found")
        }
    }

    private fun handleGetProfile(uri: String): Response {
        val userId = uri.removePrefix("/v1/users/").removeSuffix("/profile")
        val profile = runService { services.dietService.getProfile(userId) }
        return if (profile != null) {
            ok(profile)
        } else {
            notFound("Profile not found")
        }
    }

    private fun handleUpdateProfile(uri: String, body: Map<String, Any>): Response {
        val userId = uri.removePrefix("/v1/users/").removeSuffix("/profile")
        val existingProfile = runService { services.dietService.getProfile(userId) }
        val displayName = body["displayName"] as? String
        val avatarUrl = body["avatarUrl"] as? String
        val dateOfBirth = body["dateOfBirth"] as? String
        val age = (body["age"] as? Number)?.toInt()
        val heightCm = (body["heightCm"] as? Number)?.toDouble()
        val weightKg = (body["weightKg"] as? Number)?.toDouble()
        val dailyCalorieTarget = (body["dailyCalorieTarget"] as? Number)?.toInt()
        val proteinGramsTarget = (body["proteinGramsTarget"] as? Number)?.toInt()
        val carbsGramsTarget = (body["carbsGramsTarget"] as? Number)?.toInt()
        val fatGramsTarget = (body["fatGramsTarget"] as? Number)?.toInt()
        val goal = body["goal"] as? String
        val activityLevel = body["activityLevel"] as? String
        val allergies = body["allergies"] as? String
        val intolerances = body["intolerances"] as? String
        val conditions = body["conditions"] as? String
        val restrictedFoods = body["restrictedFoods"] as? String
        val forbiddenByDoctor = body["forbiddenByDoctor"] as? String

        val profile = if (existingProfile != null) {
            existingProfile.copy(
                displayName = displayName ?: existingProfile.displayName,
                avatarUrl = avatarUrl ?: existingProfile.avatarUrl,
                dateOfBirth = dateOfBirth ?: existingProfile.dateOfBirth,
                age = age ?: existingProfile.age,
                heightCm = heightCm ?: existingProfile.heightCm,
                weightKg = weightKg ?: existingProfile.weightKg,
                dailyCalorieTarget = dailyCalorieTarget ?: existingProfile.dailyCalorieTarget,
                proteinGramsTarget = proteinGramsTarget ?: existingProfile.proteinGramsTarget,
                carbsGramsTarget = carbsGramsTarget ?: existingProfile.carbsGramsTarget,
                fatGramsTarget = fatGramsTarget ?: existingProfile.fatGramsTarget,
                goal = goal ?: existingProfile.goal,
                activityLevel = activityLevel ?: existingProfile.activityLevel,
                allergies = allergies ?: existingProfile.allergies,
                intolerances = intolerances ?: existingProfile.intolerances,
                conditions = conditions ?: existingProfile.conditions,
                restrictedFoods = restrictedFoods ?: existingProfile.restrictedFoods,
                forbiddenByDoctor = forbiddenByDoctor ?: existingProfile.forbiddenByDoctor
            ).also {
                runService { services.dietService.updateProfile(it) }
            }
        } else {
            runService {
                services.dietService.createProfile(
                    userId = userId,
                    displayName = displayName,
                    dateOfBirth = dateOfBirth,
                    age = age,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    dailyCalorieTarget = dailyCalorieTarget,
                    proteinGramsTarget = proteinGramsTarget,
                    carbsGramsTarget = carbsGramsTarget,
                    fatGramsTarget = fatGramsTarget,
                    goal = goal,
                    activityLevel = activityLevel,
                    allergies = allergies,
                    intolerances = intolerances,
                    conditions = conditions,
                    restrictedFoods = restrictedFoods,
                    forbiddenByDoctor = forbiddenByDoctor
                )
            }
        }

        return ok(profile)
    }

    private fun handleCreateBioRecord(uri: String, body: Map<String, Any>): Response {
        val userId = uri.removePrefix("/v1/users/").removeSuffix("/biorecords")
        val date = body["date"] as? String
        if (date.isNullOrBlank()) {
            return badRequest("Missing required field 'date'.")
        }
        val record = runService {
            services.dietService.recordBio(
                userId = userId,
                date = date,
                weightKg = (body["weightKg"] as? Number)?.toDouble(),
                bodyFatPercentage = (body["bodyFatPercentage"] as? Number)?.toDouble(),
                systolicBp = (body["systolicBp"] as? Number)?.toInt(),
                diastolicBp = (body["diastolicBp"] as? Number)?.toInt(),
                notes = body["notes"] as? String
            )
        }
        return newFixedLengthResponse(Response.Status.CREATED, "application/json", gson.toJson(record))
    }

    private fun handleGetHistory(session: IHTTPSession, uri: String): Response {
        val userId = uri.removePrefix("/v1/users/").removeSuffix("/history")
        val queryParameters = session.parms
        val startDate = queryParameters["startDate"]
        val endDate = queryParameters["endDate"]
        val entries = if (!startDate.isNullOrBlank() && !endDate.isNullOrBlank()) {
            runService { services.dietService.getDishHistoryInRange(userId, startDate, endDate) }
        } else {
            emptyList<DishHistory>()
        }
        return ok(mapOf("userId" to userId, "entries" to entries))
    }

    private fun handleLogMeal(uri: String, body: Map<String, Any>): Response {
        val userId = uri.removePrefix("/v1/users/").removeSuffix("/history")
        val date = body["date"] as? String
        if (date.isNullOrBlank()) {
            return badRequest("Missing required field 'date'.")
        }
        val history = runService {
            services.dietService.logMealConsumption(
                userId = userId,
                dishId = body["dishId"] as? String,
                date = date,
                mealType = body["mealType"] as? String,
                servings = (body["servings"] as? Number)?.toDouble() ?: 1.0,
                notes = body["notes"] as? String
            )
        }
        return newFixedLengthResponse(Response.Status.CREATED, "application/json", gson.toJson(history))
    }

    private fun handleDishNutrition(uri: String): Response {
        val dishId = uri.removePrefix("/v1/dishes/").removeSuffix("/nutrition")
        val nutrition = runService { services.dietService.computeDishNutrition(dishId) }
        return if (nutrition != null) {
            ok(nutrition)
        } else {
            notFound("Dish not found")
        }
    }

    private fun handleComputeRecipe(body: Map<String, Any>): Response {
        val prompt = body["prompt"] as? String
        val userId = body["userId"] as? String
        if (prompt.isNullOrBlank() || userId.isNullOrBlank()) {
            return badRequest("Missing required fields 'prompt' and 'userId'.")
        }
        if (!services.gemmaService.isModelLoaded()) {
            runService { services.gemmaService.loadModel(null) }
        }
        val recipeResult: GemmaRecipeResult = runService { services.gemmaService.computeRecipe(prompt, userId) }
        return ok(recipeResult)
    }

    private fun handleGemmaAsk(body: Map<String, Any>): Response {
        val question = body["question"] as? String
        val userId = body["userId"] as? String ?: ""
        if (question.isNullOrBlank()) {
            return badRequest("Missing required field 'question'.")
        }
        if (!services.gemmaService.isModelLoaded()) {
            runService { services.gemmaService.loadModel(null) }
        }
        val answerResult: GemmaAnswerResult = runService { services.gemmaService.askQuestion(question, userId) }
        return ok(answerResult)
    }

    private fun handleGetDailySuggestions(): Response {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val suggestions = runService { services.dietService.getDailySuggestions(today) }
        return ok(mapOf("date" to today, "suggestions" to suggestions))
    }

    private fun handleTriggerDailySuggestions(): Response {
        val request = OneTimeWorkRequestBuilder<DailySuggestionWorker>().build()
        workManager.enqueue(request)
        return ok(mapOf("status" to "triggered", "message" to "Daily suggestions worker queued."))
    }

    private fun ok(data: Any): Response = newFixedLengthResponse(
        Response.Status.OK,
        "application/json",
        gson.toJson(data)
    )

    private fun badRequest(message: String): Response = newFixedLengthResponse(
        Response.Status.BAD_REQUEST,
        "application/json",
        gson.toJson(mapOf("code" to 400, "message" to message))
    )

    private fun notFound(message: String): Response = newFixedLengthResponse(
        Response.Status.NOT_FOUND,
        "application/json",
        gson.toJson(mapOf("code" to 404, "message" to message))
    )
}
