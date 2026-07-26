package com.tomady.nutrition.server

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tomady.nutrition.BuildConfig
import com.tomady.nutrition.data.local.diet.DietDatabase
import com.tomady.nutrition.service.diet.DietAPIService
import com.tomady.nutrition.service.foodb.FooDBDataAPIService
import com.tomady.nutrition.service.gemma.GemmaAndroidService
import com.tomady.nutrition.worker.DailySuggestionWorker
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Embedded REST API server for Tomady Nutrition services.
 *
 * Binds to **0.0.0.0** on **port 7777** so it is accessible from any device
 * on the same local network (WiFi). An external React Native (or any HTTP)
 * client can call these endpoints instead of using native bridge modules.
 *
 * ## Endpoints
 *
 * ### Health
 * - `GET /api/v1/health` — Service health check
 *
 * ### FooDB
 * - `GET /api/v1/foodb/search?q=<query>` — Search foods
 * - `GET /api/v1/foodb/food/<id>` — Get food details (cache-first)
 * - `GET /api/v1/foodb/food/<id>/nutrients` — Get nutrients for a food
 * - `GET /api/v1/foodb/groups` — List food groups
 *
 * ### Diet / Profile
 * - `GET  /api/v1/diet/profile/<userId>` — Get user profile
 * - `PUT  /api/v1/diet/profile` — Create or update profile (JSON body)
 * - `POST /api/v1/diet/bio` — Record biometric measurement
 * - `GET  /api/v1/diet/bio/<userId>?date=<yyyy-MM-dd>` — Get bio record
 * - `GET  /api/v1/diet/targets/<userId>` — Compute daily calorie/macro targets
 *
 * ### Diet / Meals
 * - `POST /api/v1/diet/meal` — Log a meal (DishHistory entry)
 * - `GET  /api/v1/diet/meals/<userId>?date=<yyyy-MM-dd>` — Get meal history for a date
 * - `GET  /api/v1/diet/summary/<userId>?date=<yyyy-MM-dd>` — Get daily nutrition summary
 * - `GET  /api/v1/diet/nutrition/<dishId>` — Compute nutritional profile of a dish
 * - `GET  /api/v1/diet/validate/<dishId>/<userId>` — Validate dish against user health profile
 *
 * ### Diet / Dish & Recipe CRUD
 * - `POST /api/v1/diet/dish` — Create a dish
 * - `GET  /api/v1/diet/dish/<dishId>` — Get dish by ID
 * - `POST /api/v1/diet/recipe` — Create a recipe with optional ingredients
 *
 * ### Gemma (AI)
 * - `POST /api/v1/gemma/load` — Load the Gemma model
 * - `POST /api/v1/gemma/ask` — Ask a nutrition question (JSON: {question, userId})
 * - `POST /api/v1/gemma/compute-recipe` — Compute a recipe (JSON: {prompt, userId})
 *
 * ### Worker
 * - `POST /api/v1/worker/trigger-daily-suggestion` — Force-run the daily suggestion worker
 *
 * @param foodbService   FooDB data access service.
 * @param dietService    Diet planning and CRUD service.
 * @param gemmaService   Gemma on-device LLM service.
 * @param dietDatabase   Diet database wrapper (for direct profile/history queries).
 * @param context        Android application context (for WorkManager).
 * @param port           HTTP port to bind to (default 8910).
 * @param gson           Gson instance for JSON serialization.
 */
class TomadyRestApiServer(
    private val foodbService: FooDBDataAPIService,
    private val dietService: DietAPIService,
    private val gemmaService: GemmaAndroidService,
    private val dietDatabase: DietDatabase,
    private val context: android.content.Context,
    private val port: Int = DEFAULT_PORT,
    private val gson: Gson = Gson()
) : NanoHTTPD(port) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Lifecycle ───────────────────────────────────────────────────────

    /**
     * Starts the server. Binds to 0.0.0.0 so it's reachable on all network
     * interfaces (WiFi, hotspot, Ethernet).
     */
    override fun start() {
        super.start()
        android.util.Log.i(TAG, "Tomady REST API server started on port $port")
        android.util.Log.i(TAG, "Local network URL: http://${getLocalIpAddress()}:$port/api/v1/health")
    }

    /**
     * Stops the server gracefully.
     */
    fun shutdown() {
        stop()
        android.util.Log.i(TAG, "Tomady REST API server stopped")
    }

    // ── Request routing ─────────────────────────────────────────────────

    override fun serve(session: IHTTPSession): Response {
        return try {
            val method = session.method.name.uppercase()
            val uri = session.uri
            val params = session.parms ?: emptyMap()
            val body = readBody(session)

            // Add CORS headers to every response
            val response = route(method, uri, params, body)
            response.addHeader("Access-Control-Allow-Origin", "*")
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
            response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
            response
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error serving ${session.uri}", e)
            jsonError(500, "Internal server error: ${e.message}")
        }
    }

    private fun route(method: String, uri: String, params: Map<String, String>, body: String?): Response {
        // CORS preflight
        if (method == "OPTIONS") {
            return jsonOk(mapOf("ok" to true))
        }

        return when {
            // ── Ping ────────────────────────────────────────────────
            method == "GET" && uri == "/ping" -> handlePing()

            // ── Health ─────────────────────────────────────────────
            method == "GET" && uri == "/api/v1/health" -> handleHealth()

            // ── FooDB ──────────────────────────────────────────────
            method == "GET" && uri == "/api/v1/foodb/search" -> handleFoodSearch(params)
            uri.matches(Regex("/api/v1/foodb/food/(\\d+)/nutrients")) -> {
                val id = Regex("/api/v1/foodb/food/(\\d+)/nutrients").find(uri)!!.groupValues[1].toLong()
                handleFoodNutrients(id)
            }
            uri.matches(Regex("/api/v1/foodb/food/(\\d+)")) -> {
                val id = Regex("/api/v1/foodb/food/(\\d+)").find(uri)!!.groupValues[1].toLong()
                handleFoodDetails(id)
            }
            method == "GET" && uri == "/api/v1/foodb/groups" -> handleFoodGroups()

            // ── Diet / Profile ─────────────────────────────────────
            uri.matches(Regex("/api/v1/diet/profile/(.+)")) -> {
                val userId = Regex("/api/v1/diet/profile/(.+)").find(uri)!!.groupValues[1]
                handleGetProfile(userId)
            }
            method == "PUT" && uri == "/api/v1/diet/profile" -> handleUpdateProfile(body)
            method == "POST" && uri == "/api/v1/diet/bio" -> handleRecordBio(body)
            uri.matches(Regex("/api/v1/diet/bio/(.+)")) -> {
                val userId = Regex("/api/v1/diet/bio/(.+)").find(uri)!!.groupValues[1]
                handleGetBioRecord(userId, params)
            }
            uri.matches(Regex("/api/v1/diet/targets/(.+)")) -> {
                val userId = Regex("/api/v1/diet/targets/(.+)").find(uri)!!.groupValues[1]
                handleDailyTargets(userId)
            }

            // ── Diet / Meals ───────────────────────────────────────
            method == "POST" && uri == "/api/v1/diet/meal" -> handleLogMeal(body)
            uri.matches(Regex("/api/v1/diet/meals/(.+)")) -> {
                val userId = Regex("/api/v1/diet/meals/(.+)").find(uri)!!.groupValues[1]
                handleGetMeals(userId, params)
            }
            uri.matches(Regex("/api/v1/diet/summary/(.+)")) -> {
                val userId = Regex("/api/v1/diet/summary/(.+)").find(uri)!!.groupValues[1]
                handleDailySummary(userId, params)
            }
            uri.matches(Regex("/api/v1/diet/nutrition/(.+)")) -> {
                val dishId = Regex("/api/v1/diet/nutrition/(.+)").find(uri)!!.groupValues[1]
                handleDishNutrition(dishId)
            }
            uri.matches(Regex("/api/v1/diet/validate/(.+)/(.+)")) -> {
                val match = Regex("/api/v1/diet/validate/(.+)/(.+)").find(uri)!!
                handleDishValidation(match.groupValues[1], match.groupValues[2])
            }

            // ── Diet / Dish & Recipe CRUD ──────────────────────────
            method == "POST" && uri == "/api/v1/diet/dish" -> handleCreateDish(body)
            uri.matches(Regex("/api/v1/diet/dish/(.+)")) -> {
                val dishId = Regex("/api/v1/diet/dish/(.+)").find(uri)!!.groupValues[1]
                handleGetDish(dishId)
            }
            method == "POST" && uri == "/api/v1/diet/recipe" -> handleCreateRecipe(body)

            // ── Gemma ──────────────────────────────────────────────
            method == "POST" && uri == "/api/v1/gemma/load" -> handleGemmaLoad()
            method == "POST" && uri == "/api/v1/gemma/ask" -> handleGemmaAsk(body)
            method == "POST" && uri == "/api/v1/gemma/compute-recipe" -> handleGemmaComputeRecipe(body)

            // ── Worker ─────────────────────────────────────────────
            method == "POST" && uri == "/api/v1/worker/trigger-daily-suggestion" -> handleTriggerWorker()

            // ── 404 ────────────────────────────────────────────────
            else -> jsonError(404, "Not found: $method $uri")
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Handler implementations
    // ══════════════════════════════════════════════════════════════════════

    // ── Ping ───────────────────────────────────────────────────────────

    private fun handlePing(): Response {
        return jsonOk(mapOf("ok" to true, "message" to "Tomady server is running on port $port"))
    }

    // ── Health ──────────────────────────────────────────────────────────

    private fun handleHealth(): Response {
        return jsonOk(mapOf(
            "status" to "ok",
            "service" to "tomady-nutrition",
            "version" to BuildConfig.VERSION_NAME,
            "gemmaLoaded" to gemmaService.isModelLoaded(),
            "ipAddress" to getLocalIpAddress(),
            "port" to port
        ))
    }

    // ── FooDB ───────────────────────────────────────────────────────────

    private fun handleFoodSearch(params: Map<String, String>): Response {
        val query = params["q"] ?: return jsonError(400, "Missing 'q' query parameter")
        val results = runBlocking { foodbService.searchFood(query) }
        return jsonOk(mapOf("query" to query, "results" to results))
    }

    private fun handleFoodDetails(foodId: Long): Response {
        val result = runBlocking { foodbService.getFoodDetails(foodId) }
        if (result == null) return jsonError(404, "Food not found for id: $foodId")
        return jsonOk(mapOf(
            "food" to result.food,
            "nutrients" to result.nutrients,
            "source" to "local-cache"
        ))
    }

    private fun handleFoodNutrients(foodId: Long): Response {
        val nutrients = runBlocking { foodbService.getNutrients(foodId) }
        return jsonOk(mapOf("foodId" to foodId, "nutrients" to nutrients))
    }

    private fun handleFoodGroups(): Response {
        val groups = runBlocking { foodbService.getFoodGroups() }
        return jsonOk(mapOf("groups" to groups))
    }

    // ── Diet / Profile ──────────────────────────────────────────────────

    private fun handleGetProfile(userId: String): Response {
        val profile = runBlocking { dietService.getProfile(userId) }
        if (profile == null) return jsonError(404, "Profile not found for user: $userId")
        return jsonOk(mapOf("profile" to profile))
    }

    private fun handleUpdateProfile(body: String?): Response {
        if (body == null) return jsonError(400, "Request body required")
        val json = tryParseJson(body) ?: return jsonError(400, "Invalid JSON")
        val userId = json.get("userId")?.asString ?: return jsonError(400, "Missing 'userId'")
        val profile = runBlocking {
            val existing = dietService.getProfile(userId)
            if (existing != null) {
                existing.copy(
                    displayName = json.get("displayName")?.asString ?: existing.displayName,
                    dateOfBirth = json.get("dateOfBirth")?.asString ?: existing.dateOfBirth,
                    heightCm = json.get("heightCm")?.asDouble ?: existing.heightCm,
                    weightKg = json.get("weightKg")?.asDouble ?: existing.weightKg,
                    dailyCalorieTarget = json.get("dailyCalorieTarget")?.asInt ?: existing.dailyCalorieTarget,
                    proteinGramsTarget = json.get("proteinGramsTarget")?.asInt ?: existing.proteinGramsTarget,
                    carbsGramsTarget = json.get("carbsGramsTarget")?.asInt ?: existing.carbsGramsTarget,
                    fatGramsTarget = json.get("fatGramsTarget")?.asInt ?: existing.fatGramsTarget,
                    goal = json.get("goal")?.asString ?: existing.goal,
                    updatedAt = System.currentTimeMillis()
                ).also { dietService.updateProfile(it) }
            } else {
                dietService.createProfile(
                    userId = userId,
                    displayName = json.get("displayName")?.asString,
                    dateOfBirth = json.get("dateOfBirth")?.asString,
                    heightCm = json.get("heightCm")?.asDouble,
                    weightKg = json.get("weightKg")?.asDouble,
                    dailyCalorieTarget = json.get("dailyCalorieTarget")?.asInt,
                    proteinGramsTarget = json.get("proteinGramsTarget")?.asInt,
                    carbsGramsTarget = json.get("carbsGramsTarget")?.asInt,
                    fatGramsTarget = json.get("fatGramsTarget")?.asInt,
                    goal = json.get("goal")?.asString
                )
            }
        }
        return jsonOk(mapOf("profile" to profile))
    }

    // ── Diet / Bio Records ──────────────────────────────────────────────

    private fun handleRecordBio(body: String?): Response {
        if (body == null) return jsonError(400, "Request body required")
        val json = tryParseJson(body) ?: return jsonError(400, "Invalid JSON")
        val userId = json.get("userId")?.asString ?: return jsonError(400, "Missing 'userId'")
        val date = json.get("date")?.asString ?: return jsonError(400, "Missing 'date' (yyyy-MM-dd)")
        val record = runBlocking {
            dietService.recordBio(
                userId = userId,
                date = date,
                weightKg = json.get("weightKg")?.asDouble,
                bodyFatPercentage = json.get("bodyFatPercentage")?.asDouble,
                systolicBp = json.get("systolicBp")?.asInt,
                diastolicBp = json.get("diastolicBp")?.asInt,
                notes = json.get("notes")?.asString
            )
        }
        return jsonOk(mapOf("record" to record))
    }

    private fun handleGetBioRecord(userId: String, params: Map<String, String>): Response {
        val date = params["date"] ?: return jsonError(400, "Missing 'date' query parameter (yyyy-MM-dd)")
        val record = runBlocking { dietService.getBioRecord(userId, date) }
        if (record == null) return jsonError(404, "No bio record found for user $userId on $date")
        return jsonOk(mapOf("record" to record))
    }

    private fun handleDailyTargets(userId: String): Response {
        val targets = runBlocking {
            val profile = dietService.getProfile(userId) ?: return@runBlocking null
            val latestBio = dietDatabase.getBioRecordByUserAndDate(
                userId, java.time.LocalDate.now().toString()
            )
            dietService.computeDailyTargets(profile, latestBio)
        }
        if (targets == null) return jsonError(404, "Could not compute targets for user: $userId")
        return jsonOk(mapOf("targets" to targets))
    }

    // ── Diet / Meals ────────────────────────────────────────────────────

    private fun handleLogMeal(body: String?): Response {
        if (body == null) return jsonError(400, "Request body required")
        val json = tryParseJson(body) ?: return jsonError(400, "Invalid JSON")
        val userId = json.get("userId")?.asString ?: return jsonError(400, "Missing 'userId'")
        val date = json.get("date")?.asString ?: return jsonError(400, "Missing 'date' (yyyy-MM-dd)")
        val history = runBlocking {
            dietService.logMealConsumption(
                userId = userId,
                dishId = json.get("dishId")?.asString,
                date = date,
                mealType = json.get("mealType")?.asString,
                servings = json.get("servings")?.asDouble ?: 1.0,
                notes = json.get("notes")?.asString
            )
        }
        return jsonOk(mapOf("entry" to history))
    }

    private fun handleGetMeals(userId: String, params: Map<String, String>): Response {
        val date = params["date"] ?: return jsonError(400, "Missing 'date' query parameter (yyyy-MM-dd)")
        val meals = runBlocking { dietService.getDishHistoryByDate(userId, date) }
        return jsonOk(mapOf("userId" to userId, "date" to date, "meals" to meals))
    }

    private fun handleDailySummary(userId: String, params: Map<String, String>): Response {
        val date = params["date"] ?: java.time.LocalDate.now().toString()
        val summary = runBlocking { dietService.getDailySummary(userId, date) }
        if (summary == null) return jsonError(404, "No meal data for user $userId on $date")
        return jsonOk(mapOf("summary" to summary))
    }

    private fun handleDishNutrition(dishId: String): Response {
        val nutrition = runBlocking { dietService.computeDishNutrition(dishId) }
        if (nutrition == null) return jsonError(404, "Dish not found: $dishId")
        return jsonOk(mapOf("nutrition" to nutrition))
    }

    private fun handleDishValidation(dishId: String, userId: String): Response {
        val result = runBlocking {
            val profile = dietService.getProfile(userId) ?: return@runBlocking null
            dietService.validateDishForProfile(dishId, profile)
        }
        if (result == null) return jsonError(404, "Profile not found for user: $userId")
        return jsonOk(mapOf("validation" to result))
    }

    // ── Diet / Dish & Recipe CRUD ───────────────────────────────────────

    private fun handleCreateDish(body: String?): Response {
        if (body == null) return jsonError(400, "Request body required")
        val json = tryParseJson(body) ?: return jsonError(400, "Invalid JSON")
        val name = json.get("name")?.asString ?: return jsonError(400, "Missing 'name'")
        val dish = runBlocking {
            dietService.createDish(
                name = name,
                description = json.get("description")?.asString,
                category = json.get("category")?.asString,
                imageUrl = json.get("imageUrl")?.asString
            )
        }
        return jsonOk(mapOf("dish" to dish))
    }

    private fun handleGetDish(dishId: String): Response {
        val dish = runBlocking { dietService.getDish(dishId) }
        if (dish == null) return jsonError(404, "Dish not found: $dishId")
        return jsonOk(mapOf("dish" to dish))
    }

    private fun handleCreateRecipe(body: String?): Response {
        if (body == null) return jsonError(400, "Request body required")
        val json = tryParseJson(body) ?: return jsonError(400, "Invalid JSON")
        val name = json.get("name")?.asString ?: return jsonError(400, "Missing 'name'")
        val recipe = runBlocking {
            dietService.createRecipe(
                name = name,
                description = json.get("description")?.asString,
                instructions = json.get("instructions")?.asString,
                prepTimeMinutes = json.get("prepTimeMinutes")?.asInt,
                cookTimeMinutes = json.get("cookTimeMinutes")?.asInt,
                servings = json.get("servings")?.asInt,
                imageUrl = json.get("imageUrl")?.asString
            )
        }
        return jsonOk(mapOf("recipe" to recipe))
    }

    // ── Gemma ───────────────────────────────────────────────────────────

    private fun handleGemmaLoad(): Response {
        scope.launch {
            gemmaService.loadModel()
        }
        return jsonOk(mapOf("message" to "Model loading initiated", "currentlyLoaded" to gemmaService.isModelLoaded()))
    }

    private fun handleGemmaAsk(body: String?): Response {
        if (body == null) return jsonError(400, "Request body required")
        val json = tryParseJson(body) ?: return jsonError(400, "Invalid JSON")
        val question = json.get("question")?.asString ?: return jsonError(400, "Missing 'question'")
        val userId = json.get("userId")?.asString ?: "default"

        val result = runBlocking {
            // Auto-load model if needed
            if (!gemmaService.isModelLoaded()) gemmaService.loadModel()
            gemmaService.askQuestion(question, userId)
        }
        return jsonOk(mapOf("result" to result))
    }

    private fun handleGemmaComputeRecipe(body: String?): Response {
        if (body == null) return jsonError(400, "Request body required")
        val json = tryParseJson(body) ?: return jsonError(400, "Invalid JSON")
        val prompt = json.get("prompt")?.asString ?: return jsonError(400, "Missing 'prompt'")
        val userId = json.get("userId")?.asString ?: "default"

        val result = runBlocking {
            if (!gemmaService.isModelLoaded()) gemmaService.loadModel()
            gemmaService.computeRecipe(prompt, userId)
        }
        return jsonOk(mapOf("result" to result))
    }

    // ── Worker ──────────────────────────────────────────────────────────

    private fun handleTriggerWorker(): Response {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<DailySuggestionWorker>()
            .addTag("manual_trigger")
            .build()
        androidx.work.WorkManager.getInstance(context).enqueue(workRequest)

        return jsonOk(mapOf(
            "message" to "DailySuggestionWorker triggered",
            "workId" to workRequest.id.toString()
        ))
    }

    // ══════════════════════════════════════════════════════════════════════
    // Utility helpers
    // ══════════════════════════════════════════════════════════════════════

    private fun readBody(session: IHTTPSession): String? {
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            files["postData"]
        } catch (e: Exception) {
            null
        }
    }

    private fun tryParseJson(body: String): JsonObject? {
        return try {
            JsonParser.parseString(body).asJsonObject
        } catch (e: Exception) {
            null
        }
    }

    private fun jsonOk(data: Map<*, *>): Response {
        val json = gson.toJson(data)
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun jsonOk(data: Any): Response {
        val json = gson.toJson(data)
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun jsonError(statusCode: Int, message: String): Response {
        val json = gson.toJson(mapOf("error" to message))
        return newFixedLengthResponse(
            Response.Status.lookup(statusCode),
            "application/json",
            json
        )
    }

    /**
     * Runs a suspend function synchronously for NanoHTTPd's non-coroutine handler.
     */
    private fun <T> runBlocking(block: suspend CoroutineScope.() -> T): T {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO, block)
    }

    companion object {
        /** Default HTTP port for the REST API. */
        const val DEFAULT_PORT = 7777

        private const val TAG = "TomadyRestApi"

        /**
         * Returns the device's local WiFi IP address, or "127.0.0.1" if not found.
         */
        fun getLocalIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val intf = interfaces.nextElement()
                    val addresses = intf.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            return addr.hostAddress ?: "127.0.0.1"
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to get local IP", e)
            }
            return "127.0.0.1"
        }
    }
}
