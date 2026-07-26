package com.tomady.nutrition.service.gemma

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.tomady.nutrition.data.local.diet.DietDatabase
import com.tomady.nutrition.data.local.diet.entity.Profile
import com.tomady.nutrition.service.diet.DietAPIService
import com.tomady.nutrition.service.diet.ProfileValidationResult
import com.tomady.nutrition.service.foodb.FooDBDataAPIService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * On-device LLM service for natural-language meal analysis, recipe computation,
 * nutrition Q&A, and daily insight generation via **Google Gemma 4** running on
 * **MediaPipe LLM Inference**.
 *
 * ## Architecture
 * ```
 *                     ┌─────────────────────────────┐
 *                     │     GemmaAndroidService      │
 *                     │  (prompt engineering +       │
 *                     │   response parsing +         │
 *                     │   validation pipeline +      │
 *                     │   true token streaming)      │
 *                     └──────────┬──────────────────┘
 *                                │ calls
 *                     ┌──────────▼──────────────────┐
 *                     │    LlmInference (MediaPipe)  │
 *                     │  .generateResponse(prompt)   │
 *                     │  .generateResponseAsync(...) │ ← true streaming
 *                     └─────────────────────────────┘
 * ```
 *
 * ## Model Lifecycle
 * 1. [loadModel] — Locates the GGUF model file (via [ModelDownloader]) and
 *    initialises a [LlmInference] instance.
 * 2. [computeRecipe] / [askQuestion] / [generateTokens] / [generateDailyInsight] — Inference calls.
 * 3. [release] — Closes the [LlmInference] and frees native resources.
 *
 * ## Streaming
 * [generateTokens] uses [LlmInference.generateResponseAsync] with a callback
 * to emit tokens as they are produced by the model, enabling true token-by-token
 * streaming to the React Native UI.
 *
 * ## Fallback Behaviour
 * If no real model file is available (not downloaded yet), the service falls
 * back to [MockLLMEngine] so the app remains demonstrable without the ~2 GB
 * model download.
 *
 * @param context       Android context (application) for file access and MediaPipe init.
 * @param dietDatabase  Wrapper around diet DAOs (used for profile lookup).
 * @param dietService   Diet service for dish/recipe creation and validation.
 * @param foodbService  FooDB service for cross-referencing nutrient data.
 */
class GemmaAndroidService(
    private val context: Context,
    private val dietDatabase: DietDatabase,
    private val dietService: DietAPIService,
    private val foodbService: FooDBDataAPIService
) {

    // ── Model lifecycle state ───────────────────────────────────────────

    private val isLoaded = AtomicBoolean(false)
    private val isLoading = AtomicBoolean(false)
    private val modelVersion = AtomicReference<String?>(null)

    /** The MediaPipe LLM inference instance (non-null when loaded). */
    private var llmInference: LlmInference? = null

    /** Tracks active streaming sessions so they can be cancelled on release. */
    private val activeSessions = ConcurrentHashMap.newKeySet<String>()

    private val modelDownloader = ModelDownloader(context)

    /** Whether we're using a real model or the mock fallback. */
    private val isUsingMock = AtomicBoolean(false)

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 1 — LLM Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Loads the Gemma model into memory via MediaPipe.
     *
     * **Timeout-safe**: This method NEITHER triggers a download NOR blocks for
     * one. It only checks for an already-cached GGUF file (instant check via
     * [ModelDownloader.getCachedModelPath]). If no cached file is found,
     * the service falls back to [MockLLMEngine] immediately.
     *
     * To trigger an actual download, call [downloadModelIfNeeded] separately
     * (e.g. from a background worker or a dedicated API endpoint).
     *
     * Pipeline:
     * 1. Check for a cached GGUF model file via [ModelDownloader.getCachedModelPath].
     * 2. If found, initialise a [LlmInference] instance with it.
     * 3. If not found, fall back to [MockLLMEngine] instantly — no download.
     *
     * @param modelPath Optional explicit path to a GGUF model file. If null,
     *                  the service checks the default cache location.
     * @return `true` if the model (real or mock) is ready for inference.
     */
    suspend fun loadModel(modelPath: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded.get()) return@withContext true
        if (isLoading.get()) return@withContext false

        isLoading.set(true)
        try {
            // 1. Resolve the model file path — INSTANT check, no download
            val resolvedPath = modelPath ?: modelDownloader.getCachedModelPath()

            if (resolvedPath != null) {
                // 2. Found a real model file — initialise MediaPipe LlmInference
                try {
                    val options = LlmInferenceOptions.builder()
                        .setModelPath(resolvedPath)
                        .setMaxTokens(MAX_TOKENS)
                        .build()
                    llmInference = LlmInference.createFromOptions(context, options)
                    isLoaded.set(true)
                    isUsingMock.set(false)
                    modelVersion.set("gemma-4-2b-it (MediaPipe, QAT 4-bit)")
                    Log.i(TAG, "Gemma model loaded via MediaPipe from: $resolvedPath")
                    return@withContext true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialise MediaPipe LlmInference: ${e.message}", e)
                    // Fall through to mock fallback
                }
            }

            // 3. Model file not cached — skip download, use mock fallback
            //    Download is handled separately by downloadModelIfNeeded().
            Log.w(TAG, "Gemma model file not cached — falling back to MockLLMEngine")
            isLoaded.set(true)
            isUsingMock.set(true)
            modelVersion.set("mock (model not downloaded yet)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Gemma model: ${e.message}", e)
            isLoaded.set(false)
            false
        } finally {
            isLoading.set(false)
        }
    }

    /**
     * Downloads the Gemma model if it is not already cached.
     *
     * This is a **potentially long-running** operation (minutes for ~1.5 GB).
     * Call it from:
     * - A background [androidx.work.WorkManager] worker
     * - A dedicated API endpoint (e.g., `/v1/gemma/download`)
     * - A coroutine scope that is NOT on the main thread
     *
     * After the download completes, call [loadModel] again to switch from
     * mock to real inference.
     *
     * @return The absolute path to the downloaded model file, or null if the
     *         download fails or the model is already cached.
     */
    suspend fun downloadModelIfNeeded(): String? = withContext(Dispatchers.IO) {
        // Check cache first (instant)
        val cached = modelDownloader.getCachedModelPath()
        if (cached != null) {
            Log.i(TAG, "Model already cached at $cached — skipping download")
            return@withContext cached
        }
        // Download (slow)
        modelDownloader.downloadFromUrl()
    }

    /**
     * Returns `true` if a GGUF model file is already cached on disk
     * (instant check — no network involved).
     */
    fun isModelCached(): Boolean = modelDownloader.getCachedModelPath() != null

    /**
     * Returns the current download progress (0.0 to 1.0), or null if no
     * download is in progress.
     */
    fun getDownloadProgress(): Float? = modelDownloader.downloadProgress

    /**
     * Returns whether the Gemma model is currently loaded and ready for inference.
     */
    fun isModelLoaded(): Boolean = isLoaded.get()

    /**
     * Returns whether the model is currently being loaded.
     */
    fun isLoadingModel(): Boolean = isLoading.get()

    /**
     * Returns whether the service is currently using the mock fallback
     * (i.e., no real model file was available).
     */
    fun isUsingMockFallback(): Boolean = isUsingMock.get()

    /**
     * Returns the loaded model version string, or null if not loaded.
     */
    fun getModelVersion(): String? = modelVersion.get()

    /**
     * Releases all model resources and cancels any active streaming sessions.
     *
     * Call this when the service is no longer needed (e.g., on app destroy).
     */
    fun release() {
        // Cancel all active streaming sessions
        for (sessionId in activeSessions) {
            activeSessions.remove(sessionId)
        }

        // Close the MediaPipe inference instance
        llmInference?.close()
        llmInference = null

        isLoaded.set(false)
        isUsingMock.set(false)
        modelVersion.set(null)
        Log.i(TAG, "Gemma model resources released")
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 2 — Recipe Computation & Validation Pipeline
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Computes a recipe/dish recommendation based on a natural-language prompt.
     *
     * Pipeline:
     * 1. Send the prompt to the LLM and parse the structured response.
     * 2. Create a [Dish] and [Recipe] in the local database.
     * 3. Validate the result against the user's [Profile] restrictions.
     * 4. Return the computed [GemmaRecipeResult] with any health warnings.
     *
     * @param prompt Natural-language request (e.g., "Suggest a low-sugar breakfast").
     * @param userId The target user (to look up their [Profile] for validation).
     * @return A [GemmaRecipeResult] with the created dish/recipe and validation.
     * @throws GemmaException if the LLM fails to produce a valid response.
     */
    suspend fun computeRecipe(prompt: String, userId: String): GemmaRecipeResult = withContext(Dispatchers.IO) {
        ensureModelLoaded()

        // 1. LLM inference — generate structured recipe JSON
        val llmResponse = llmGenerate(buildRecipePrompt(prompt))
        val recipeData = parseRecipeResponse(llmResponse)

        // 2. Create the Dish and Recipe via DietAPIService
        val dish = dietService.createDish(
            name = recipeData.dishName,
            description = recipeData.description,
            category = recipeData.category ?: "AI-Generated"
        )

        val recipe = dietService.createRecipe(
            name = recipeData.dishName,
            dishId = dish.id,
            description = recipeData.description,
            instructions = recipeData.instructions,
            prepTimeMinutes = recipeData.prepTimeMinutes,
            cookTimeMinutes = recipeData.cookTimeMinutes,
            servings = recipeData.servings,
            imageUrl = null
        )

        // 3. Add RecipeIngredients (food items from FooDB lookup)
        for (item in recipeData.ingredients) {
            val searchResults = foodbService.searchFood(item.name)
            val foodItemId = searchResults.firstOrNull()?.id?.toString()

            dietService.addRecipeIngredient(
                recipeId = recipe.id,
                foodItemId = foodItemId,
                name = item.name,
                quantity = item.quantity,
                unit = item.unit
            )
        }

        // 4. Validate against user profile
        val profile = dietDatabase.getProfileByUserId(userId)
        val validation = if (profile != null) {
            dietService.validateDishForProfile(dish.id, profile)
        } else {
            ProfileValidationResult(
                dishId = dish.id,
                isCompatible = true,
                warnings = emptyList()
            )
        }

        GemmaRecipeResult(
            dishId = dish.id,
            dishName = dish.name,
            recipeId = recipe.id,
            isCompatible = validation.isCompatible,
            warnings = validation.warnings,
            rawResponse = llmResponse
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 3 — Question Answering
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Answers a natural-language nutrition or diet question.
     *
     * The LLM is given the user's profile context (goals, restrictions) so that
     * responses are personalised.
     *
     * @param question The user's question (e.g., "Can I drink coca-cola with diabetes?").
     * @param userId   The target user (for profile context).
     * @return A [GemmaAnswerResult] with the answer text and optional dish cross-reference.
     * @throws GemmaException if the LLM fails.
     */
    suspend fun askQuestion(question: String, userId: String): GemmaAnswerResult = withContext(Dispatchers.IO) {
        ensureModelLoaded()

        // 1. Build context-aware prompt
        val profile = dietDatabase.getProfileByUserId(userId)
        val contextPrompt = buildQuestionPrompt(question, profile)

        // 2. LLM inference
        val llmResponse = llmGenerate(contextPrompt)

        // 3. Cross-reference: try to find a matching dish in the DB
        val matchingDish = extractDishFromAnswer(llmResponse)

        GemmaAnswerResult(
            answer = llmResponse,
            referencedDishId = matchingDish?.first,
            referencedDishName = matchingDish?.second,
            rawResponse = llmResponse
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 4 — Daily Insight Generation
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Generates a personalised daily nutrition insight for the dashboard.
     *
     * The LLM analyses the user's profile, today's meals, and macro intake
     * to produce a short, actionable insight (1-2 sentences).
     *
     * @param userId   The target user (for profile + meals context).
     * @param today    Today's date in "yyyy-MM-dd" format.
     * @param mealsSummary  Pre-computed summary of today's meals (calories, protein, carbs, fat).
     * @return A [GemmaInsightResult] with the insight text and a category tag.
     * @throws GemmaException if the LLM fails.
     */
    suspend fun generateDailyInsight(
        userId: String,
        today: String,
        mealsSummary: GemmaMealsSummary
    ): GemmaInsightResult = withContext(Dispatchers.IO) {
        ensureModelLoaded()

        val profile = dietDatabase.getProfileByUserId(userId)
        val contextPrompt = buildInsightPrompt(profile, today, mealsSummary)

        val llmResponse = llmGenerate(contextPrompt)

        // Determine category from the response content
        val category = when {
            llmResponse.contains("protéin", ignoreCase = true) &&
                (llmResponse.contains("manque", ignoreCase = true) ||
                 llmResponse.contains("insuffisant", ignoreCase = true) ||
                 llmResponse.contains("augmenter", ignoreCase = true)) -> "protein_low"
            llmResponse.contains("calori", ignoreCase = true) &&
                llmResponse.contains("dépass", ignoreCase = true) -> "calories_over"
            llmResponse.contains("hydrat", ignoreCase = true) ||
                llmResponse.contains("eau", ignoreCase = true) -> "hydration"
            llmResponse.contains("équilibr", ignoreCase = true) ||
                llmResponse.contains("bien", ignoreCase = true) -> "balanced"
            else -> "general"
        }

        GemmaInsightResult(
            text = llmResponse.trim(),
            category = category,
            rawResponse = llmResponse
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 5 — Streaming Tokens
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Generates LLM tokens as a [Flow] for true token-by-token streaming.
     *
     * When the real MediaPipe model is loaded, tokens are streamed via
     * [LlmInference.generateResponseAsync] with a callback that emits each
     * token as it is produced. When the mock fallback is active, tokens are
     * simulated by splitting the full response.
     *
     * Each emission is a partial token string. The flow completes when the
     * full response has been generated.
     *
     * @param query The input prompt.
     * @param sessionId Unique session identifier for cancellation tracking.
     * @return A flow of token strings.
     */
    fun generateTokens(query: String, sessionId: String = UUID.randomUUID().toString()): Flow<String> = flow {
        ensureModelLoadedInFlow()

        activeSessions.add(sessionId)
        try {
            if (isUsingMock.get()) {
                // Mock mode: simulate token-by-token streaming
                val fullResponse = llmGenerate(query)
                val tokens = tokenizeResponse(fullResponse)
                for (token in tokens) {
                    if (!activeSessions.contains(sessionId)) break
                    emit(token)
                    delay(token.length * TOKEN_DELAY_MS_PER_CHAR.toLong())
                }
            } else {
                // True streaming via MediaPipe generateResponseAsync with callback
                val inference = llmInference ?: throw GemmaException(
                    message = "LLM inference instance is null",
                    rawResponse = null
                )

                // Use kotlinx.coroutines channels to bridge callback → Flow
                val channel = kotlinx.coroutines.channels.Channel<String>(capacity = kotlinx.coroutines.channels.Channel.UNLIMITED)

                try {
                    inference.generateResponseAsync(query) { partialResult, done ->
                        if (!activeSessions.contains(sessionId)) {
                            channel.close()
                            return@generateResponseAsync
                        }
                        partialResult?.let { token ->
                            channel.trySend(token)
                        }
                        if (done) {
                            channel.close()
                        }
                    }

                    // Emit tokens from the channel until closed
                    for (token in channel) {
                        emit(token)
                    }
                } catch (e: Exception) {
                    channel.close()
                    throw GemmaException(
                        message = "Streaming failed: ${e.message}",
                        rawResponse = null
                    )
                }
            }
        } finally {
            activeSessions.remove(sessionId)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Cancels an active streaming session.
     */
    fun cancelStreaming(sessionId: String) {
        activeSessions.remove(sessionId)
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 6 — LLM Engine (MediaPipe + Mock Fallback)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Core LLM inference call.
     *
     * Uses the real MediaPipe [LlmInference.generateResponse] when available,
     * falling back to [MockLLMEngine] for development/demo mode.
     */
    private suspend fun llmGenerate(prompt: String): String = withContext(Dispatchers.IO) {
        ensureModelLoaded()

        if (isUsingMock.get()) {
            // Mock fallback
            delay(MODEL_INFERENCE_SIMULATED_MS)
            MockLLMEngine.generate(prompt)
        } else {
            // Real MediaPipe inference
            val inference = llmInference ?: throw GemmaException(
                message = "LLM inference instance is null after successful load",
                rawResponse = null
            )
            val response = inference.generateResponse(prompt)
            response ?: throw GemmaException(
                message = "MediaPipe generateResponse returned null",
                rawResponse = null
            )
        }
    }

    /**
     * Splits a full response into tokens for mock streaming simulation.
     */
    private fun tokenizeResponse(response: String): List<String> {
        val tokens = mutableListOf<String>()
        val words = response.split(" ")
        for ((i, word) in words.withIndex()) {
            tokens.add(if (i < words.size - 1) "$word " else word)
        }
        return tokens
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 7 — Prompt Engineering
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Builds a structured prompt for recipe computation.
     */
    private fun buildRecipePrompt(userPrompt: String): String {
        return """|
            |You are Tomady, a professional nutritionist and chef AI assistant.
            |
            |Given the following user request, generate a structured recipe.
            |Respond ONLY with a valid JSON object — no markdown, no explanation.
            |
            |User request: "$userPrompt"
            |
            |Respond in this JSON format:
            |{
            |  "dishName": "Name of the dish",
            |  "description": "Short description",
            |  "category": "Breakfast|Lunch|Dinner|Snack|Dessert|Beverage",
            |  "instructions": "Step-by-step cooking instructions",
            |  "prepTimeMinutes": 15,
            |  "cookTimeMinutes": 25,
            |  "servings": 2,
            |  "ingredients": [
            |    { "name": "Ingredient name", "quantity": 100.0, "unit": "g" },
            |    { "name": "Another ingredient", "quantity": 2.0, "unit": "cups" }
            |  ]
            |}
            |
            |Only include ingredients. Use metric units where possible.
        """.trimMargin()
    }

    /**
     * Builds a structured prompt for answering a nutrition question,
     * including the user's profile context for personalisation.
     */
    private fun buildQuestionPrompt(question: String, profile: Profile?): String {
        val profileContext = if (profile != null) {
            """|
                |User profile context:
                |- Goal: ${profile.goal ?: "Not specified"}
                |- Age: ${profile.age ?: "Unknown"}
                |- Daily calorie target: ${profile.dailyCalorieTarget ?: "Not set"}
                |- Weight: ${profile.weightKg?.let { "${it}kg" } ?: "Unknown"}
                |- Height: ${profile.heightCm?.let { "${it}cm" } ?: "Unknown"}
                |- Activity level: ${profile.activityLevel ?: "Unknown"}
                |- Allergies: ${profile.allergies ?: "None"}
                |- Medical conditions: ${profile.conditions ?: "None"}
            """.trimMargin()
        } else {
            "User profile: No profile available."
        }

        return """|
            |You are Tomady, a professional nutritionist and health coach AI assistant.
            |
            |$profileContext
            |
            |Answer the following question concisely and accurately.
            |Focus on evidence-based nutrition science.
            |If the question relates to a medical condition, advise consulting a doctor.
            |Keep the answer under 200 words.
            |
            |Question: "$question"
            |
            |Answer:
        """.trimMargin()
    }

    /**
     * Builds a prompt for generating a daily nutrition insight.
     *
     * The insight should be short (1-2 sentences), personalised, and actionable.
     * It focuses on what the user did well and what they can improve.
     */
    private fun buildInsightPrompt(
        profile: Profile?,
        today: String,
        meals: GemmaMealsSummary
    ): String {
        val profileContext = if (profile != null) {
            """|
                |User profile:
                |- Goal: ${profile.goal ?: "Not specified"}
                |- Daily calorie target: ${profile.dailyCalorieTarget ?: "Not set"}
                |- Weight: ${profile.weightKg?.let { "${it}kg" } ?: "Unknown"}
                |- Activity level: ${profile.activityLevel ?: "Unknown"}
                |- Allergies: ${profile.allergies ?: "None"}
                |- Conditions: ${profile.conditions ?: "None"}
            """.trimMargin()
        } else {
            "User profile: No profile available."
        }

        return """|
            |You are Tomady, a professional nutritionist AI.
            |
            |$profileContext
            |
            |Today's date: $today
            |Today's intake:
            |- Calories consumed: ${meals.totalCalories} kcal (target: ${meals.calorieGoal} kcal)
            |- Protein: ${meals.totalProtein}g (target: ${meals.proteinGoal}g)
            |- Carbs: ${meals.totalCarbs}g (target: ${meals.carbsGoal}g)
            |- Fat: ${meals.totalFat}g (target: ${meals.fatGoal}g)
            |- Number of meals: ${meals.mealCount}
            |
            |Generate a short daily insight (1-2 sentences, in French).
            |Focus on:
            |1. What the user did well today
            |2. One specific, actionable recommendation for improvement
            |Be encouraging but honest. Reference specific numbers.
            |Do NOT use markdown. Write as plain text.
            |
            |Insight:
        """.trimMargin()
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 8 — Response Parsing & Validation
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Parses the LLM's JSON recipe response into a [ParsedRecipe].
     *
     * @throws GemmaException if the response is malformed.
     */
    private fun parseRecipeResponse(raw: String): ParsedRecipe {
        val jsonStr = extractJson(raw)

        return try {
            val json = com.google.gson.JsonParser.parseString(jsonStr).asJsonObject

            val ingredients = mutableListOf<ParsedIngredient>()
            val ingredientsArray = json.getAsJsonArray("ingredients")
            if (ingredientsArray != null) {
                for (element in ingredientsArray) {
                    val obj = element.asJsonObject
                    ingredients.add(
                        ParsedIngredient(
                            name = obj.get("name")?.asString ?: "Unknown",
                            quantity = obj.get("quantity")?.asDouble ?: 1.0,
                            unit = obj.get("unit")?.asString ?: "g"
                        )
                    )
                }
            }

            ParsedRecipe(
                dishName = json.get("dishName")?.asString ?: "AI-Generated Dish",
                description = json.get("description")?.asString,
                category = json.get("category")?.asString,
                instructions = json.get("instructions")?.asString,
                prepTimeMinutes = json.get("prepTimeMinutes")?.asInt,
                cookTimeMinutes = json.get("cookTimeMinutes")?.asInt,
                servings = json.get("servings")?.asInt ?: 2,
                ingredients = ingredients
            )
        } catch (e: Exception) {
            throw GemmaException(
                message = "Failed to parse LLM recipe response: ${e.message}",
                rawResponse = raw
            )
        }
    }

    /**
     * Attempts to extract a dish reference from an LLM answer.
     */
    private suspend fun extractDishFromAnswer(answer: String): Pair<String, String>? {
        val lines = answer.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.length < 3) continue

            val matches = dietService.searchDishes(trimmed)
            if (matches.isNotEmpty()) {
                val dish = matches.first()
                return Pair(dish.id, dish.name)
            }
        }
        return null
    }

    /**
     * Extracts a JSON object from a string that may contain markdown fences.
     */
    private fun extractJson(raw: String): String {
        val cleaned = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return cleaned
    }

    /**
     * Ensures the model is loaded before inference.
     */
    private fun ensureModelLoaded() {
        if (!isLoaded.get()) {
            throw GemmaException(
                message = "Gemma model is not loaded. Call loadModel() first.",
                rawResponse = null
            )
        }
    }

    /**
     * Ensures the model is loaded inside a Flow builder.
     */
    private fun ensureModelLoadedInFlow() {
        if (!isLoaded.get()) {
            throw GemmaException(
                message = "Gemma model is not loaded. Call loadModel() first.",
                rawResponse = null
            )
        }
    }

    companion object {
        private const val TAG = "GemmaAndroidService"

        /** Maximum number of tokens the model can generate. */
        private const val MAX_TOKENS = 1024

        /** Simulated inference delay when using the mock fallback (ms). */
        private const val MODEL_INFERENCE_SIMULATED_MS = 800L

        /** Delay per character when simulating token streaming (ms). */
        private const val TOKEN_DELAY_MS_PER_CHAR = 15
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Data types
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Result of a recipe computation request.
 */
data class GemmaRecipeResult(
    val dishId: String,
    val dishName: String,
    val recipeId: String,
    val isCompatible: Boolean,
    val warnings: List<String>,
    val rawResponse: String
)

/**
 * Result of a question-answering request.
 */
data class GemmaAnswerResult(
    val answer: String,
    val referencedDishId: String?,
    val referencedDishName: String?,
    val rawResponse: String
)

/**
 * Internal parsed recipe structure from LLM output.
 */
internal data class ParsedRecipe(
    val dishName: String,
    val description: String?,
    val category: String?,
    val instructions: String?,
    val prepTimeMinutes: Int?,
    val cookTimeMinutes: Int?,
    val servings: Int,
    val ingredients: List<ParsedIngredient>
)

/**
 * A single ingredient parsed from the LLM response.
 */
internal data class ParsedIngredient(
    val name: String,
    val quantity: Double,
    val unit: String
)

/**
 * Exception thrown when the Gemma service encounters an error.
 */
class GemmaException(
    override val message: String,
    val rawResponse: String?
) : Exception(message)

/**
 * Summary of today's meals, pre-computed by the caller.
 * Used by [GemmaAndroidService.generateDailyInsight] to build the insight prompt.
 */
data class GemmaMealsSummary(
    val totalCalories: Int,
    val calorieGoal: Int,
    val totalProtein: Int,
    val proteinGoal: Int,
    val totalCarbs: Int,
    val carbsGoal: Int,
    val totalFat: Int,
    val fatGoal: Int,
    val mealCount: Int
)

/**
 * Result of a daily insight generation request.
 */
data class GemmaInsightResult(
    val text: String,
    val category: String,
    val rawResponse: String
)

// ═══════════════════════════════════════════════════════════════════════════
// Mock LLM Engine (development/demo fallback)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Development mock for the Gemma LLM inference engine.
 *
 * Returns hardcoded structured responses for known prompt patterns.
 * Used when no real GGUF model file is available on the device.
 */
internal object MockLLMEngine {

    fun generate(prompt: String): String {
        return when {
            prompt.contains("dishName", ignoreCase = true) &&
            prompt.contains("ingredients", ignoreCase = true) -> {
                generateMockRecipe(prompt)
            }
            prompt.contains("Insight:", ignoreCase = true) &&
            prompt.contains("Today's intake", ignoreCase = true) -> {
                generateMockInsight(prompt)
            }
            prompt.contains("diabetes", ignoreCase = true) ||
            prompt.contains("coca-cola", ignoreCase = true) ||
            prompt.contains("sugar", ignoreCase = true) -> {
                generateMockDiabetesAnswer(prompt)
            }
            prompt.contains("question", ignoreCase = true) ||
            prompt.contains("Can I", ignoreCase = true) ||
            prompt.contains("Answer:", ignoreCase = true) -> {
                generateMockGeneralAnswer(prompt)
            }
            else -> {
                "Bonjour, je suis Tomady, votre assistant nutrition. " +
                        "Je peux vous aider à planifier vos repas, analyser vos recettes " +
                        "et répondre à vos questions sur la nutrition. " +
                        "N'hésitez pas à me poser vos questions !"
            }
        }
    }

    private fun generateMockInsight(prompt: String): String {
        // Extract numbers from the prompt for contextual mock insight
        val calorieMatch = Regex("Calories consumed: (\\d+)").find(prompt)
        val proteinMatch = Regex("Protein: (\\d+)g").find(prompt)
        val mealCountMatch = Regex("Number of meals: (\\d+)").find(prompt)

        val calories = calorieMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val protein = proteinMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val mealCount = mealCountMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

        return when {
            calories == 0 -> {
                "Vous n'avez pas encore enregistré de repas aujourd'hui. " +
                "Commencez par ajouter votre petit-déjeuner pour bien démarrer la journée !"
            }
            protein < 50 -> {
                "Bon début avec $calories kcal aujourd'hui ! " +
                "Cependant, vos protéines sont un peu faibles ($protein g). " +
                "Essayez d'ajouter une source de protéines comme du poulet grillé, des œufs ou du poisson à votre prochain repas."
            }
            calories > 2500 -> {
                "Aujourd'hui vous avez consommé $calories kcal, ce qui dépasse votre objectif. " +
                "Pour le reste de la journée, privilégiez les légumes et évitez les aliments riches en calories."
            }
            mealCount <= 1 -> {
                "Vous avez déjà $calories kcal au compteur avec seulement $mealCount repas. " +
                "N'oubliez pas de prendre un déjeuner équilibré avec des protéines et des légumes !"
            }
            else -> {
                "Excellent travail aujourd'hui ! Avec $calories kcal et $protein g de protéines " +
                "répartis sur $mealCount repas, vous êtes sur la bonne voix pour atteindre votre objectif. " +
                "Continuez comme ça !"
            }
        }
    }

    private fun generateMockRecipe(prompt: String): String {
        val requestLower = prompt.lowercase()

        return when {
            requestLower.contains("low sugar") || requestLower.contains("diabetes") -> {
                """{
                    "dishName": "Sugar-Free Berry Oatmeal Bowl",
                    "description": "A heart-healthy, low-glycemic breakfast packed with fiber and antioxidants.",
                    "category": "Breakfast",
                    "instructions": "1. Cook rolled oats in water or unsweetened almond milk for 5 minutes.\n2. Top with fresh mixed berries (blueberries, strawberries, raspberries).\n3. Add a tablespoon of chopped almonds for healthy fats.\n4. Sprinkle with cinnamon.",
                    "prepTimeMinutes": 5,
                    "cookTimeMinutes": 7,
                    "servings": 1,
                    "ingredients": [
                        {"name": "Rolled oats", "quantity": 50.0, "unit": "g"},
                        {"name": "Mixed berries", "quantity": 100.0, "unit": "g"},
                        {"name": "Almonds", "quantity": 15.0, "unit": "g"},
                        {"name": "Cinnamon", "quantity": 1.0, "unit": "g"}
                    ]
                }"""
            }
            requestLower.contains("high protein") || requestLower.contains("muscle") -> {
                """{
                    "dishName": "Grilled Chicken Quinoa Power Bowl",
                    "description": "A protein-packed bowl with lean grilled chicken, quinoa, and fresh vegetables.",
                    "category": "Lunch",
                    "instructions": "1. Season chicken breast with herbs and grill for 6-7 minutes per side.\n2. Cook quinoa according to package directions.\n3. Chop vegetables (cherry tomatoes, cucumber, bell pepper).\n4. Assemble bowl with quinoa base, sliced chicken, and vegetables.",
                    "prepTimeMinutes": 15,
                    "cookTimeMinutes": 20,
                    "servings": 2,
                    "ingredients": [
                        {"name": "Chicken breast", "quantity": 200.0, "unit": "g"},
                        {"name": "Quinoa", "quantity": 100.0, "unit": "g"},
                        {"name": "Cherry tomatoes", "quantity": 100.0, "unit": "g"},
                        {"name": "Cucumber", "quantity": 80.0, "unit": "g"},
                        {"name": "Bell pepper", "quantity": 60.0, "unit": "g"}
                    ]
                }"""
            }
            requestLower.contains("vegan") || requestLower.contains("plant") -> {
                """{
                    "dishName": "Vegan Lentil & Sweet Potato Curry",
                    "description": "A hearty plant-based curry rich in protein, fiber, and vitamins.",
                    "category": "Dinner",
                    "instructions": "1. Sauté onion and garlic in coconut oil until soft.\n2. Add curry powder and cumin, cook 1 minute.\n3. Add diced sweet potato, lentils, and coconut milk.\n4. Simmer for 25 minutes until lentils are tender.",
                    "prepTimeMinutes": 10,
                    "cookTimeMinutes": 30,
                    "servings": 4,
                    "ingredients": [
                        {"name": "Green lentils", "quantity": 200.0, "unit": "g"},
                        {"name": "Sweet potato", "quantity": 300.0, "unit": "g"},
                        {"name": "Coconut milk", "quantity": 400.0, "unit": "ml"},
                        {"name": "Onion", "quantity": 100.0, "unit": "g"},
                        {"name": "Garlic", "quantity": 10.0, "unit": "g"},
                        {"name": "Curry powder", "quantity": 10.0, "unit": "g"},
                        {"name": "Brown rice", "quantity": 200.0, "unit": "g"}
                    ]
                }"""
            }
            else -> {
                """{
                    "dishName": "Mediterranean Avocado Toast with Poached Egg",
                    "description": "A balanced, nutrient-dense meal with healthy fats, protein, and whole grains.",
                    "category": "Breakfast",
                    "instructions": "1. Toast whole grain bread until golden.\n2. Mash half an avocado with lemon juice and spread on toast.\n3. Poach an egg for 3 minutes.\n4. Top with sliced cherry tomatoes and a sprinkle of red pepper flakes.",
                    "prepTimeMinutes": 5,
                    "cookTimeMinutes": 8,
                    "servings": 1,
                    "ingredients": [
                        {"name": "Whole grain bread", "quantity": 2.0, "unit": "slices"},
                        {"name": "Avocado", "quantity": 0.5, "unit": "piece"},
                        {"name": "Egg", "quantity": 1.0, "unit": "piece"},
                        {"name": "Cherry tomatoes", "quantity": 50.0, "unit": "g"},
                        {"name": "Lemon juice", "quantity": 5.0, "unit": "ml"}
                    ]
                }"""
            }
        }
    }

    private fun generateMockDiabetesAnswer(prompt: String): String {
        val lower = prompt.lowercase()

        return if (lower.contains("coca-cola") || lower.contains("coke") || lower.contains("soda") || lower.contains("soft drink")) {
            "Regular Coca-Cola contains approximately 39 grams of sugar per 12oz (355ml) can, " +
                    "which is significantly higher than the American Diabetes Association's recommendation " +
                    "of limiting added sugars. For individuals with diabetes, regular soda can cause rapid " +
                    "spikes in blood glucose levels.\n\n" +
                    "**Recommendation:** It's best to avoid regular Coca-Cola if you have diabetes. " +
                    "Consider these alternatives:\n" +
                    "- Diet Coke or Coke Zero (sugar-free, but be mindful of artificial sweeteners)\n" +
                    "- Sparkling water with a splash of lemon or lime\n" +
                    "- Unsweetened iced tea\n" +
                    "- Infused water with berries and mint\n\n" +
                    "Always consult your healthcare provider for personalized dietary advice."
        } else if (lower.contains("sugar")) {
            "The American Heart Association recommends limiting added sugar intake to no more than " +
                    "25g (6 teaspoons) per day for women and 36g (9 teaspoons) per day for men. " +
                    "For individuals with diabetes or prediabetes, even stricter limits may be beneficial.\n\n" +
                    "**Tips to reduce sugar intake:**\n" +
                    "- Choose whole fruits over fruit juices\n" +
                    "- Read nutrition labels for hidden sugars\n" +
                    "- Use spices like cinnamon or vanilla extract for sweetness\n" +
                    "- Avoid sugary beverages — they're the #1 source of added sugar\n\n" +
                    "Tomady can help you find low-sugar recipe alternatives. Would you like a recipe suggestion?"
        } else {
            "Managing diabetes through diet involves balancing carbohydrates, choosing low-glycemic " +
                    "index foods, and maintaining consistent meal timing.\n\n" +
                    "**Key dietary principles for diabetes management:**\n" +
                    "- Focus on non-starchy vegetables (leafy greens, broccoli, peppers)\n" +
                    "- Choose whole grains over refined grains\n" +
                    "- Include lean protein at every meal\n" +
                    "- Monitor portion sizes of carbohydrate-rich foods\n" +
                    "- Limit added sugars and refined carbohydrates\n\n" +
                    "Would you like me to create a diabetes-friendly recipe or analyze a specific food?"
        }
    }

    private fun generateMockGeneralAnswer(prompt: String): String {
        val lower = prompt.lowercase()

        return when {
            lower.contains("protein") && lower.contains("need") -> {
                "The Recommended Dietary Allowance (RDA) for protein is 0.8g per kilogram of body weight " +
                        "for sedentary adults. However, individual needs vary:\n\n" +
                        "- **Sedentary adults:** 0.8g/kg\n" +
                        "- **Recreational athletes:** 1.2-1.4g/kg\n" +
                        "- **Strength athletes:** 1.6-2.0g/kg\n" +
                        "- **Weight loss:** 1.2-1.6g/kg (to preserve muscle mass)\n\n" +
                        "For a 70kg person, that's 56-140g of protein per day depending on activity level."
            }
            lower.contains("calorie") && (lower.contains("how many") || lower.contains("much")) -> {
                "Daily calorie needs vary based on age, sex, weight, height, and activity level.\n\n" +
                        "**General estimates:**\n" +
                        "- Sedentary women: 1,600-2,000 kcal/day\n" +
                        "- Active women: 2,000-2,400 kcal/day\n" +
                        "- Sedentary men: 2,000-2,400 kcal/day\n" +
                        "- Active men: 2,400-3,000 kcal/day\n\n" +
                        "For a more accurate estimate, use Tomady's Daily Plan feature which calculates " +
                        "your BMR using the Mifflin-St Jeor equation and adjusts for your goals."
            }
            else -> {
                "That's a great question about nutrition! Here's what you should know:\n\n" +
                        "A balanced diet should include a variety of whole foods from all food groups. " +
                        "Focus on:\n" +
                        "- **Vegetables and fruits** — half your plate\n" +
                        "- **Whole grains** — quarter of your plate\n" +
                        "- **Lean proteins** — quarter of your plate\n" +
                        "- **Healthy fats** — in moderation\n\n" +
                        "Would you like me to elaborate on a specific topic or create a personalized meal plan?"
            }
        }
    }
}
