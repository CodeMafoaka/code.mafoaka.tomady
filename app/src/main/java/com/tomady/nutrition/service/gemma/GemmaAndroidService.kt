package com.tomady.nutrition.service.gemma

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
import java.util.concurrent.atomic.AtomicReference

/**
 * On-device LLM service for natural-language meal analysis, recipe computation,
 * and nutrition Q&A via Google Gemma (or a mock engine for development).
 *
 * ## Lifecycle
 * 1. [loadModel] — initialise the LLM (download/locate quantized weights).
 * 2. [computeRecipe] / [askQuestion] / [generateTokens] — inference calls.
 * 3. [release] — free model resources.
 *
 * ## Recipe Computation & Validation Pipeline
 * ```
 * user prompt → LLM inference → structured Dish/Recipe JSON
 *     → parse → create Dish/Recipe via DietAPIService
 *     → validate via DietAPIService.validateDishForProfile
 *     → return result with warnings
 * ```
 *
 * @param dietDatabase  Wrapper around diet DAOs (used for profile lookup).
 * @param dietService   Diet service for dish/recipe creation and validation.
 * @param foodbService  FooDB service for cross-referencing nutrient data.
 */
class GemmaAndroidService(
    private val dietDatabase: DietDatabase,
    private val dietService: DietAPIService,
    private val foodbService: FooDBDataAPIService
) {

    // ── Model lifecycle state ───────────────────────────────────────────

    private val isLoaded = AtomicBoolean(false)
    private val isLoading = AtomicBoolean(false)
    private val modelVersion = AtomicReference<String?>(null)

    /** Tracks active streaming sessions so they can be cancelled on release. */
    private val activeSessions = ConcurrentHashMap.newKeySet<String>()

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 1 — LLM Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Loads the Gemma model into memory.
     *
     * In production this would:
     * 1. Check for a downloaded model file in the app's internal storage.
     * 2. If missing, download the quantized `.gguf` or TFLite model.
     * 3. Initialise the inference runtime (MediaPipe / XNNPACK / etc.).
     *
     * For development, this simulates a 2-second load and sets a mock version.
     *
     * @param modelPath Optional path to a pre-downloaded model file. If null,
     *                  the service attempts to locate a bundled default model.
     * @return `true` if loading succeeded, `false` otherwise.
     */
    suspend fun loadModel(modelPath: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded.get()) return@withContext true
        if (isLoading.get()) return@withContext false

        isLoading.set(true)
        try {
            // Simulate model loading (2s delay)
            delay(MODEL_LOAD_SIMULATED_MS)

            // In production, this would initialise the native Gemma runtime:
            //   GemmaRuntime.initialize(context, modelPath ?: DEFAULT_MODEL_PATH)
            //   isLoaded.set(GemmaRuntime.isReady())

            isLoaded.set(true)
            modelVersion.set(modelPath ?: DEFAULT_MODEL_VERSION)
            true
        } catch (e: Exception) {
            // Log error: failed to load model
            isLoaded.set(false)
            false
        } finally {
            isLoading.set(false)
        }
    }

    /**
     * Returns whether the Gemma model is currently loaded and ready for inference.
     */
    fun isModelLoaded(): Boolean = isLoaded.get()

    /**
     * Returns whether the model is currently being loaded.
     */
    fun isLoadingModel(): Boolean = isLoading.get()

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

        // In production:
        //   GemmaRuntime.close()
        //   modelFile?.delete()

        isLoaded.set(false)
        modelVersion.set(null)
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
            description = recipeData.description,
            instructions = recipeData.instructions,
            prepTimeMinutes = recipeData.prepTimeMinutes,
            cookTimeMinutes = recipeData.cookTimeMinutes,
            servings = recipeData.servings,
            imageUrl = null
        )

        // 3. Add RecipeIngredients (food items from FooDB lookup)
        for (item in recipeData.ingredients) {
            // Try to find matching food items in FooDB
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
    // SECTION 4 — Streaming Tokens
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Generates LLM tokens as a [Flow] for real-time streaming to the RN host
     * via [DeviceEventEmitter].
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
            val fullResponse = llmGenerate(query)
            // Simulate token-by-token streaming with realistic delays
            val tokens = tokenizeResponse(fullResponse)
            for (token in tokens) {
                // Check if the session was cancelled
                if (!activeSessions.contains(sessionId)) break
                emit(token)
                delay(token.length * TOKEN_DELAY_MS_PER_CHAR.toLong())
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
    // SECTION 5 — LLM Engine (Mock Implementation)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Core LLM inference call.
     *
     * In production, this would call the native Gemma runtime:
     * ```kotlin
     * return GemmaRuntime.generate(prompt, maxTokens = 512)
     * ```
     *
     * For development, uses a [MockLLMEngine] that returns sensible
     * structured responses for known prompt patterns.
     */
    private suspend fun llmGenerate(prompt: String): String = withContext(Dispatchers.IO) {
        ensureModelLoaded()

        // Simulate inference latency (production: actual model inference time)
        delay(MODEL_INFERENCE_SIMULATED_MS)

        MockLLMEngine.generate(prompt)
    }

    /**
     * Splits a full response into tokens for streaming simulation.
     * In production, the native runtime would provide tokens one at a time.
     */
    private fun tokenizeResponse(response: String): List<String> {
        // Split on word boundaries for realistic streaming
        val tokens = mutableListOf<String>()
        val words = response.split(" ")
        for ((i, word) in words.withIndex()) {
            tokens.add(if (i < words.size - 1) "$word " else word)
        }
        return tokens
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 6 — Prompt Engineering
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Builds a structured prompt for recipe computation.
     */
    private fun buildRecipePrompt(userPrompt: String): String {
        return """
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
            """
            |User profile context:
            |- Goal: ${profile.goal ?: "Not specified"}
            |- Daily calorie target: ${profile.dailyCalorieTarget ?: "Not set"}
            |- Weight: ${profile.weightKg?.let { "${it}kg" } ?: "Unknown"}
            |- Height: ${profile.heightCm?.let { "${it}cm" } ?: "Unknown"}
            """.trimMargin()
        } else {
            "User profile: No profile available."
        }

        return """
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

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 7 — Response Parsing & Validation
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Parses the LLM's JSON recipe response into a [ParsedRecipe].
     *
     * @throws GemmaException if the response is malformed.
     */
    private fun parseRecipeResponse(raw: String): ParsedRecipe {
        // Extract JSON from the response (handle markdown code fences)
        val jsonStr = extractJson(raw)

        // In production, use Gson to deserialize. For the mock, the engine
        // already returns clean JSON so we parse it directly here.
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
     *
     * @return A pair of (dishId, dishName) if a match is found, null otherwise.
     */
    private suspend fun extractDishFromAnswer(answer: String): Pair<String, String>? {
        // Try to find dish names mentioned in the answer
        val lines = answer.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.length < 3) continue

            // Search the local dish catalog for a matching name
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
        // Remove markdown code fences if present
        val cleaned = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return cleaned
    }

    /**
     * Ensures the model is loaded before inference.
     *
     * @throws GemmaException if the model is not loaded.
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
     * Ensures the model is loaded inside a Flow builder (where we can't use `suspend`).
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
        /** Simulated model load time in milliseconds. */
        private const val MODEL_LOAD_SIMULATED_MS = 2_000L

        /** Simulated inference time in milliseconds. */
        private const val MODEL_INFERENCE_SIMULATED_MS = 800L

        /** Delay per character when streaming tokens (milliseconds). */
        private const val TOKEN_DELAY_MS_PER_CHAR = 15

        /** Default model version identifier. */
        private const val DEFAULT_MODEL_VERSION = "gemma-2b-it-q4_k_m"
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
 *
 * @property message Human-readable error description.
 * @property rawResponse The raw LLM output that caused the error, if available.
 */
class GemmaException(
    override val message: String,
    val rawResponse: String?
) : Exception(message)

// ═══════════════════════════════════════════════════════════════════════════
// Mock LLM Engine (development only)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Development mock for the Gemma LLM inference engine.
 *
 * Returns hardcoded structured responses for known prompt patterns.
 * In production, this would be replaced by a native Gemma runtime call.
 */
internal object MockLLMEngine {

    /**
     * Generates a mock response for the given prompt.
     */
    fun generate(prompt: String): String {
        return when {
            // Recipe generation prompts
            prompt.contains("dishName", ignoreCase = true) &&
            prompt.contains("ingredients", ignoreCase = true) -> {
                generateMockRecipe(prompt)
            }
            // Diabetes-related questions
            prompt.contains("diabetes", ignoreCase = true) ||
            prompt.contains("coca-cola", ignoreCase = true) ||
            prompt.contains("sugar", ignoreCase = true) -> {
                generateMockDiabetesAnswer(prompt)
            }
            // General nutrition questions
            prompt.contains("question", ignoreCase = true) ||
            prompt.contains("Can I", ignoreCase = true) ||
            prompt.contains("Answer:", ignoreCase = true) -> {
                generateMockGeneralAnswer(prompt)
            }
            // Default generic response
            else -> {
                "I'm Tomady, your nutrition assistant. I can help you plan meals, " +
                        "analyze recipes, and answer nutrition questions. " +
                        "Please provide more details about what you'd like to know."
            }
        }
    }

    private fun generateMockRecipe(prompt: String): String {
        val requestLower = prompt.lowercase()

        return when {
            requestLower.contains("low sugar") || requestLower.contains("diabetes") -> {
                """{
                    "dishName": "Sugar-Free Berry Oatmeal Bowl",
                    "description": "A heart-healthy, low-glycemic breakfast packed with fiber and antioxidants. Sweetened naturally with berries.",
                    "category": "Breakfast",
                    "instructions": "1. Cook rolled oats in water or unsweetened almond milk for 5 minutes.\n2. Top with fresh mixed berries (blueberries, strawberries, raspberries).\n3. Add a tablespoon of chopped almonds for healthy fats.\n4. Sprinkle with cinnamon and a drizzle of sugar-free maple syrup alternative.",
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
                    "instructions": "1. Season chicken breast with herbs and grill for 6-7 minutes per side.\n2. Cook quinoa according to package directions.\n3. Chop vegetables (cherry tomatoes, cucumber, bell pepper).\n4. Assemble bowl with quinoa base, sliced chicken, and vegetables.\n5. Drizzle with lemon-tahini dressing.",
                    "prepTimeMinutes": 15,
                    "cookTimeMinutes": 20,
                    "servings": 2,
                    "ingredients": [
                        {"name": "Chicken breast", "quantity": 200.0, "unit": "g"},
                        {"name": "Quinoa", "quantity": 100.0, "unit": "g"},
                        {"name": "Cherry tomatoes", "quantity": 100.0, "unit": "g"},
                        {"name": "Cucumber", "quantity": 80.0, "unit": "g"},
                        {"name": "Bell pepper", "quantity": 60.0, "unit": "g"},
                        {"name": "Lemon juice", "quantity": 15.0, "unit": "ml"},
                        {"name": "Tahini", "quantity": 20.0, "unit": "g"}
                    ]
                }"""
            }
            requestLower.contains("vegan") || requestLower.contains("plant") -> {
                """{
                    "dishName": "Vegan Lentil & Sweet Potato Curry",
                    "description": "A hearty plant-based curry rich in protein, fiber, and vitamins. Perfect for a satisfying dinner.",
                    "category": "Dinner",
                    "instructions": "1. Sauté onion and garlic in coconut oil until soft.\n2. Add curry powder and cumin, cook 1 minute.\n3. Add diced sweet potato, lentils, and coconut milk.\n4. Simmer for 25 minutes until lentils are tender.\n5. Season with salt and serve over brown rice.",
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
