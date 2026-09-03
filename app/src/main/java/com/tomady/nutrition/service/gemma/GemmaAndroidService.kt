package com.tomady.nutrition.service.gemma

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.tomady.nutrition.config.ConfigManager
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
 * nutrition Q&A, and daily insight generation via **Google Gemma** running on
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
 * 1. [loadModel] — Locates the `.task` model file (via [ModelDownloader]) and
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
 * back to [MockLLMEngine] so the app remains demonstrable without the model
 * download. Trigger the real download via [downloadModelIfNeeded] (wired to
 * `POST /api/v1/gemma/download` on the REST server).
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
    private val isUsingMock = AtomicBoolean(false)
    private val modelVersion = AtomicReference<String?>(null)

    /** The MediaPipe LLM inference instance (non-null when loaded). */
    private var llmInference: LlmInference? = null

    /** Tracks active streaming sessions so they can be cancelled on release. */
    private val activeSessions = ConcurrentHashMap.newKeySet<String>()

    private val configManager = ConfigManager(context)
    private val modelDownloader = ModelDownloader(context, configManager)

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 1 — LLM Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Loads the Gemma model into memory via MediaPipe.
     *
     * If the GGUF file is already cached on disk, this initialises
     * MediaPipe LlmInference with the real model. Otherwise falls back
     * to MockLLMEngine instantly.
     *
     * @param modelPath Optional explicit path to a `.task` model file.
     * @return `true` if the model (real or mock) is ready for inference.
     */
    suspend fun loadModel(modelPath: String? = null): Boolean = withContext(Dispatchers.IO) {
        // Short-circuit only once the REAL model is loaded — if we're still on the
        // mock fallback, keep retrying so a model downloaded after mock-load can
        // still be picked up (otherwise isLoaded=true from the mock would wedge
        // this service on mock responses forever).
        if (isLoaded.get() && !isUsingMock.get()) return@withContext true
        if (isLoading.get()) return@withContext false

        isLoading.set(true)
        try {
            val resolvedPath = modelPath ?: modelDownloader.getCachedModelPath()

            if (resolvedPath != null) {
                try {
                    val gemmaConfig = configManager.get().gemma
                    val options = LlmInferenceOptions.builder()
                        .setModelPath(resolvedPath)
                        .setMaxTokens(gemmaConfig.maxTokens)
                        .build()
                    llmInference = LlmInference.createFromOptions(context, options)
                    isLoaded.set(true)
                    isUsingMock.set(false)
                    modelVersion.set("${gemmaConfig.modelFileName} (MediaPipe LlmInference)")
                    Log.i(TAG, "Gemma model loaded via MediaPipe from: $resolvedPath")
                    return@withContext true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialise MediaPipe LlmInference: ${e.message}", e)
                }
            }

            // Model file not cached — use mock fallback
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

    suspend fun downloadModelIfNeeded(): String? = withContext(Dispatchers.IO) {
        val cached = modelDownloader.getCachedModelPath()
        if (cached != null) {
            Log.i(TAG, "Model already cached at $cached — skipping download")
            return@withContext cached
        }
        modelDownloader.downloadFromUrl()
    }

    fun isModelCached(): Boolean = modelDownloader.getCachedModelPath() != null

    fun getDownloadProgress(): Float? = modelDownloader.downloadProgress

    fun isModelLoaded(): Boolean = isLoaded.get()

    fun isLoadingModel(): Boolean = isLoading.get()

    fun isUsingMockFallback(): Boolean = isUsingMock.get()

    fun getModelVersion(): String? = modelVersion.get()

    fun release() {
        for (sessionId in activeSessions) {
            activeSessions.remove(sessionId)
        }
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

    suspend fun computeRecipe(prompt: String, userId: String): GemmaRecipeResult = withContext(Dispatchers.IO) {
        ensureModelLoaded()

        val llmResponse = llmGenerate(buildRecipePrompt(prompt))
        val recipeData = parseRecipeResponse(llmResponse)

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

        val profile = dietDatabase.getProfileByUserId(userId)
        val validation = if (profile != null) {
            dietService.validateDishForProfile(dish.id, profile)
        } else {
            ProfileValidationResult(dishId = dish.id, isCompatible = true, warnings = emptyList())
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

    suspend fun askQuestion(question: String, userId: String): GemmaAnswerResult = withContext(Dispatchers.IO) {
        ensureModelLoaded()

        val profile = dietDatabase.getProfileByUserId(userId)
        val contextPrompt = buildQuestionPrompt(question, profile)
        val llmResponse = llmGenerate(contextPrompt)
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

    suspend fun generateDailyInsight(
        userId: String,
        today: String,
        mealsSummary: GemmaMealsSummary
    ): GemmaInsightResult = withContext(Dispatchers.IO) {
        ensureModelLoaded()

        val profile = dietDatabase.getProfileByUserId(userId)
        val contextPrompt = buildInsightPrompt(profile, today, mealsSummary)
        val llmResponse = llmGenerate(contextPrompt)

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

        GemmaInsightResult(text = llmResponse.trim(), category = category, rawResponse = llmResponse)
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 5 — Streaming Tokens (TRUE token-by-token)
    // ══════════════════════════════════════════════════════════════════════

    fun generateTokens(query: String, sessionId: String = UUID.randomUUID().toString()): Flow<String> = flow {
        ensureModelLoadedInFlow()

        activeSessions.add(sessionId)
        try {
            if (isUsingMock.get()) {
                val fullResponse = llmGenerate(query)
                val tokens = tokenizeResponse(fullResponse)
                for (token in tokens) {
                    if (!activeSessions.contains(sessionId)) break
                    emit(token)
                    delay(token.length * TOKEN_DELAY_MS_PER_CHAR.toLong())
                }
            } else {
                val inference = llmInference ?: throw GemmaException(
                    message = "LLM inference instance is null",
                    rawResponse = null
                )

                val channel = kotlinx.coroutines.channels.Channel<String>(
                    capacity = kotlinx.coroutines.channels.Channel.UNLIMITED
                )

                try {
                    inference.generateResponseAsync(query) { partialResult, done ->
                        if (!activeSessions.contains(sessionId)) {
                            channel.close()
                            return@generateResponseAsync
                        }
                        partialResult?.let { token -> channel.trySend(token) }
                        if (done) channel.close()
                    }

                    for (token in channel) {
                        emit(token)
                    }
                } catch (e: Exception) {
                    channel.close()
                    throw GemmaException(message = "Streaming failed: ${e.message}", rawResponse = null)
                }
            }
        } finally {
            activeSessions.remove(sessionId)
        }
    }.flowOn(Dispatchers.IO)

    fun cancelStreaming(sessionId: String) {
        activeSessions.remove(sessionId)
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 6 — LLM Engine (MediaPipe + Mock Fallback)
    // ══════════════════════════════════════════════════════════════════════

    private suspend fun llmGenerate(prompt: String): String = withContext(Dispatchers.IO) {
        ensureModelLoaded()

        if (isUsingMock.get()) {
            delay(MODEL_INFERENCE_SIMULATED_MS)
            MockLLMEngine.generate(prompt)
        } else {
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
            throw GemmaException(message = "Failed to parse LLM recipe response: ${e.message}", rawResponse = raw)
        }
    }

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

    private fun extractJson(raw: String): String {
        return raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun ensureModelLoaded() {
        if (!isLoaded.get()) {
            throw GemmaException(message = "Gemma model is not loaded. Call loadModel() first.", rawResponse = null)
        }
    }

    private fun ensureModelLoadedInFlow() {
        if (!isLoaded.get()) {
            throw GemmaException(message = "Gemma model is not loaded. Call loadModel() first.", rawResponse = null)
        }
    }

    companion object {
        private const val TAG = "GemmaAndroidService"
        private const val MODEL_INFERENCE_SIMULATED_MS = 800L
        private const val TOKEN_DELAY_MS_PER_CHAR = 15
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Data types
// ═══════════════════════════════════════════════════════════════════════════

data class GemmaRecipeResult(
    val dishId: String,
    val dishName: String,
    val recipeId: String,
    val isCompatible: Boolean,
    val warnings: List<String>,
    val rawResponse: String
)

data class GemmaAnswerResult(
    val answer: String,
    val referencedDishId: String?,
    val referencedDishName: String?,
    val rawResponse: String
)

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

internal data class ParsedIngredient(
    val name: String,
    val quantity: Double,
    val unit: String
)

class GemmaException(
    override val message: String,
    val rawResponse: String?
) : Exception(message)

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

data class GemmaInsightResult(
    val text: String,
    val category: String,
    val rawResponse: String
)

// ═══════════════════════════════════════════════════════════════════════════
// Mock LLM Engine (development/demo fallback)
// ═══════════════════════════════════════════════════════════════════════════

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
                    "dishName": "Bol d'avoine sans sucre aux baies",
                    "description": "Un petit-déjeuner riche en fibres et antioxydants, sans sucre ajouté.",
                    "category": "Breakfast",
                    "instructions": "1. Cuire les flocons d'avoine dans de l'eau ou du lait d'amande non sucré pendant 5 minutes.\n2. Garnir de baies fraîches (myrtilles, framboises, fraises).\n3. Ajouter une cuillère d'amandes concassées.\n4. Saupoudrer de cannelle.",
                    "prepTimeMinutes": 5,
                    "cookTimeMinutes": 7,
                    "servings": 1,
                    "ingredients": [
                        {"name": "Flocons d'avoine", "quantity": 50.0, "unit": "g"},
                        {"name": "Baies fraîches", "quantity": 100.0, "unit": "g"},
                        {"name": "Amandes", "quantity": 15.0, "unit": "g"},
                        {"name": "Cannelle", "quantity": 1.0, "unit": "g"}
                    ]
                }"""
            }
            requestLower.contains("high protein") || requestLower.contains("muscle") -> {
                """{
                    "dishName": "Bowl poulet grillé et quinoa",
                    "description": "Un bowl riche en protéines avec du poulet maigre, du quinoa et des légumes frais.",
                    "category": "Lunch",
                    "instructions": "1. Assaisonner le poulet avec des herbes et griller 6-7 minutes de chaque côté.\n2. Cuire le quinoa selon les instructions.\n3. Couper les légumes (tomates cerises, concombre, poivron).\n4. Assembler le bowl avec le quinoa, le poulet tranché et les légumes.",
                    "prepTimeMinutes": 15,
                    "cookTimeMinutes": 20,
                    "servings": 2,
                    "ingredients": [
                        {"name": "Blanc de poulet", "quantity": 200.0, "unit": "g"},
                        {"name": "Quinoa", "quantity": 100.0, "unit": "g"},
                        {"name": "Tomates cerises", "quantity": 100.0, "unit": "g"},
                        {"name": "Concombre", "quantity": 80.0, "unit": "g"},
                        {"name": "Poivron", "quantity": 60.0, "unit": "g"}
                    ]
                }"""
            }
            requestLower.contains("vegan") || requestLower.contains("plant") -> {
                """{
                    "dishName": "Curry végétalien lentilles et patate douce",
                    "description": "Un curry végétalien riche en protéines, fibres et vitamines.",
                    "category": "Dinner",
                    "instructions": "1. Faire revenir l'oignon et l'ail dans l'huile de coco.\n2. Ajouter le curry et le cumin, cuire 1 minute.\n3. Ajouter la patate douce, les lentilles et le lait de coco.\n4. Laisser mijoter 25 minutes jusqu'à ce que les lentilles soient tendres.",
                    "prepTimeMinutes": 10,
                    "cookTimeMinutes": 30,
                    "servings": 4,
                    "ingredients": [
                        {"name": "Lentilles vertes", "quantity": 200.0, "unit": "g"},
                        {"name": "Patate douce", "quantity": 300.0, "unit": "g"},
                        {"name": "Lait de coco", "quantity": 400.0, "unit": "ml"},
                        {"name": "Oignon", "quantity": 100.0, "unit": "g"},
                        {"name": "Ail", "quantity": 10.0, "unit": "g"},
                        {"name": "Poudre de curry", "quantity": 10.0, "unit": "g"}
                    ]
                }"""
            }
            else -> {
                """{
                    "dishName": "Toast avocat méditerranéen à l'œuf poché",
                    "description": "Un repas équilibré avec des bonnes graisses, des protéines et des céréales complètes.",
                    "category": "Breakfast",
                    "instructions": "1. Griller le pain complet jusqu'à ce qu'il soit doré.\n2. Écraser la moitié d'avocat avec du jus de citron et étaler sur le pain.\n3. Pocher un œuf pendant 3 minutes.\n4. Garnir de tomates cerises tranchées et de piments.",
                    "prepTimeMinutes": 5,
                    "cookTimeMinutes": 8,
                    "servings": 1,
                    "ingredients": [
                        {"name": "Pain complet", "quantity": 2.0, "unit": "tranches"},
                        {"name": "Avocat", "quantity": 0.5, "unit": "pièce"},
                        {"name": "Œuf", "quantity": 1.0, "unit": "pièce"},
                        {"name": "Tomates cerises", "quantity": 50.0, "unit": "g"},
                        {"name": "Jus de citron", "quantity": 5.0, "unit": "ml"}
                    ]
                }"""
            }
        }
    }

    private fun generateMockDiabetesAnswer(prompt: String): String {
        val lower = prompt.lowercase()
        return if (lower.contains("coca-cola") || lower.contains("coke") || lower.contains("soda")) {
            "Le Coca-Cola contient environ 39 grammes de sucre par canette de 355ml, " +
            "ce qui est bien au-delà des recommandations pour les personnes diabétiques.\n\n" +
            "**Recommandation :** Évitez le Coca-Cola régulier si vous êtes diabétique. " +
            "Alternatives : eau pétillante avec citron, thé glacé non sucré, eau infusée aux baies.\n\n" +
            "Consultez toujours votre médecin pour des conseils nutritionnels personnalisés."
        } else if (lower.contains("sugar")) {
            "L'American Heart Association recommande de limiter les sucres ajoutés à 25g par jour pour les femmes " +
            "et 36g par jour pour les hommes.\n\n" +
            "**Conseils pour réduire les sucres :**\n" +
            "- Privilégiez les fruits entiers aux jus de fruits\n" +
            "- Lisez les étiquettes nutritionnelles\n" +
            "- Utilisez la cannelle ou l'extrait de vanille pour la douceur\n" +
            "- Évitez les boissons sucrées"
        } else {
            "La gestion du diabète par l'alimentation consiste à équilibrer les glucides, " +
            "choisir des aliments à faible index glycémique et maintenir des horaires de repas réguliers.\n\n" +
            "**Principes clés :**\n" +
            "- Privilégiez les légumes non féculents\n" +
            "- Choisissez les céréales complètes plutôt que raffinées\n" +
            "- Incluez des protéines maigres à chaque repas\n" +
            "- Surveillez les portions d'aliments riches en glucides"
        }
    }

    private fun generateMockGeneralAnswer(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("protein") && lower.contains("need") -> {
                "L'apport recommandé en protéines est de 0,8g par kilogramme de poids corporel " +
                "pour les adultes sédentaires. Cependant, les besoins varient :\n\n" +
                "- Adultes sédentaires : 0,8g/kg\n" +
                "- Athlètes récréatifs : 1,2-1,4g/kg\n" +
                "- Athlètes de force : 1,6-2,0g/kg\n" +
                "- Perte de poids : 1,2-1,6g/kg\n\n" +
                "Pour une personne de 70kg, c'est 56-140g de protéines par jour."
            }
            lower.contains("calorie") && (lower.contains("how many") || lower.contains("much")) -> {
                "Les besoins caloriques quotidiens varient selon l'âge, le sexe, le poids, la taille et le niveau d'activité.\n\n" +
                "**Estimations générales :**\n" +
                "- Femmes sédentaires : 1 600-2 000 kcal/jour\n" +
                "- Femmes actives : 2 000-2 400 kcal/jour\n" +
                "- Hommes sédentaires : 2 000-2 400 kcal/jour\n" +
                "- Hommes actifs : 2 400-3 000 kcal/jour\n\n" +
                "Pour une estimation plus précise, utilisez la fonctionnalité Plan Quotidien de Tomady."
            }
            else -> {
                "Excellente question ! Voici ce que vous devez savoir :\n\n" +
                "Un régime équilibré doit inclure une variété d'aliments entiers de tous les groupes alimentaires. " +
                "Concentrez-vous sur :\n" +
                "- **Légumes et fruits** — la moitié de votre assiette\n" +
                "- **Céréales complètes** — le quart de votre assiette\n" +
                "- **Protéines maigres** — le quart de votre assiette\n" +
                "- **Bonnes graisses** — avec modération"
            }
        }
    }
}
