package com.tomady.nutrition.bridge

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.tomady.nutrition.data.AppDatabase
import com.tomady.nutrition.data.local.diet.DietDatabase
import com.tomady.nutrition.data.local.foodb.FooDBLocalDatabase
import com.tomady.nutrition.service.diet.DietAPIService
import com.tomady.nutrition.service.foodb.FooDBDataAPIService
import com.tomady.nutrition.service.gemma.GemmaAndroidService
import com.tomady.nutrition.service.gemma.GemmaAnswerResult
import com.tomady.nutrition.service.gemma.GemmaException
import com.tomady.nutrition.service.gemma.GemmaRecipeResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * React Native bridge module exposing Gemma AI/ML capabilities to the RN host.
 *
 * Provides:
 * - [computeRecipe]: Generates a recipe from a natural-language prompt (Promise-based).
 * - [askQuestion]: Answers nutrition/diet questions with profile context (Promise-based).
 * - [streamTokens]: Streams LLM tokens in real-time via [DeviceEventEmitter].
 * - [loadModel] / [isModelLoaded] / [releaseModel]: Model lifecycle management.
 *
 * ## Streaming Event Format
 * Each token emission fires a `"GemmaToken"` event with payload:
 * ```json
 * { "sessionId": "...", "token": "...", "isFinal": false }
 * ```
 * The final emission has `"isFinal": true` and may include the full response.
 */
class GemmaModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val moduleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var streamingJob: Job? = null

    private val service: GemmaAndroidService by lazy {
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
        val dietService = DietAPIService(dietDatabase = dietDb, foodbService = foodbService)

        GemmaAndroidService(
            dietDatabase = dietDb,
            dietService = dietService,
            foodbService = foodbService
        )
    }

    private val eventEmitter: DeviceEventManagerModule.RCTDeviceEventEmitter?
        get() = reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)

    override fun getName(): String = "TomadyGemma"

    // ══════════════════════════════════════════════════════════════════════
    // Model Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Loads the Gemma model. If [modelPath] is provided, attempts to load
     * from that path; otherwise uses the default bundled model.
     *
     * @param modelPath Optional path to a pre-downloaded model file.
     * @param promise   Resolves with `true` when loaded, rejects on failure.
     */
    @ReactMethod
    fun loadModel(modelPath: String, promise: Promise) {
        moduleScope.launch {
            try {
                val path = modelPath.ifBlank { null }
                val result = withContext(Dispatchers.IO) {
                    service.loadModel(path)
                }
                promise.resolve(result)
            } catch (e: Exception) {
                handleError("LOAD_MODEL_ERROR", e, promise)
            }
        }
    }

    /**
     * Checks whether the Gemma model is currently loaded and ready.
     *
     * @param promise Resolves with a boolean.
     */
    @ReactMethod
    fun isModelLoaded(promise: Promise) {
        promise.resolve(service.isModelLoaded())
    }

    /**
     * Releases all Gemma model resources and cancels active streaming sessions.
     */
    @ReactMethod
    fun releaseModel() {
        streamingJob?.cancel()
        streamingJob = null
        service.release()
    }

    // ══════════════════════════════════════════════════════════════════════
    // Recipe Computation
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Computes a recipe/dish recommendation based on a natural-language prompt.
     *
     * The response is validated against the user's profile restrictions and
     * includes any health warnings.
     *
     * @param prompt  Natural-language request (e.g., "Suggest a low-sugar breakfast").
     * @param userId  The target user for profile-based validation.
     * @param promise Resolves with a [GemmaRecipeResult] WritableMap.
     */
    @ReactMethod
    fun computeRecipe(prompt: String, userId: String, promise: Promise) {
        moduleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    service.computeRecipe(prompt, userId)
                }
                promise.resolve(recipeResultToWritableMap(result))
            } catch (e: GemmaException) {
                val errorMap = Arguments.createMap()
                errorMap.putString("message", e.message)
                e.rawResponse?.let { errorMap.putString("rawResponse", it) }
                promise.reject("GEMMA_RECIPE_ERROR", e.message, e, errorMap)
            } catch (e: Exception) {
                handleError("COMPUTE_RECIPE_ERROR", e, promise)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Question Answering
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Answers a natural-language nutrition or diet question.
     *
     * The answer is personalised using the user's profile context (goals,
     * restrictions, biometric data).
     *
     * @param question The user's question (e.g., "Can I drink coca-cola with diabetes?").
     * @param userId   The target user for profile context.
     * @param promise  Resolves with a [GemmaAnswerResult] WritableMap.
     */
    @ReactMethod
    fun askQuestion(question: String, userId: String, promise: Promise) {
        moduleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    service.askQuestion(question, userId)
                }
                promise.resolve(answerResultToWritableMap(result))
            } catch (e: GemmaException) {
                val errorMap = Arguments.createMap()
                errorMap.putString("message", e.message)
                e.rawResponse?.let { errorMap.putString("rawResponse", it) }
                promise.reject("GEMMA_QUESTION_ERROR", e.message, e, errorMap)
            } catch (e: Exception) {
                handleError("ASK_QUESTION_ERROR", e, promise)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Streaming
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Starts streaming LLM tokens for the given query via [DeviceEventEmitter].
     *
     * Events are emitted as `"GemmaToken"` with the following payload:
     * ```json
     * {
     *   "sessionId": "uuid",
     *   "token": "streamed text fragment",
     *   "accumulated": "full text so far",
     *   "isFinal": false
     * }
     * ```
     *
     * When generation completes, an event with `isFinal: true` is emitted
     * containing the full response.
     *
     * @param query  The input prompt to stream from.
     * @param promise Resolves with the `sessionId` string when streaming starts,
     *                or rejects if the model isn't loaded.
     */
    @ReactMethod
    fun streamTokens(query: String, promise: Promise) {
        if (!service.isModelLoaded()) {
            promise.reject("MODEL_NOT_LOADED", "Gemma model is not loaded. Call loadModel() first.")
            return
        }

        val sessionId = UUID.randomUUID().toString()
        streamingJob?.cancel()
        streamingJob = moduleScope.launch {
            val accumulator = StringBuilder()
            try {
                service.generateTokens(query, sessionId)
                    .catch { e ->
                        emitError(sessionId, e.message ?: "Streaming error")
                    }
                    .collect { token ->
                        accumulator.append(token)
                        emitToken(sessionId, token, accumulator.toString(), isFinal = false)
                    }
                // Emit final event
                emitToken(sessionId, "", accumulator.toString(), isFinal = true)
            } catch (e: Exception) {
                emitError(sessionId, e.message ?: "Streaming failed")
            }
        }
        promise.resolve(sessionId)
    }

    /**
     * Cancels an active streaming session.
     */
    @ReactMethod
    fun cancelStreaming() {
        streamingJob?.cancel()
        streamingJob = null
    }

    // ══════════════════════════════════════════════════════════════════════
    // Event Emitters
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Emits a single token event to the React Native JS thread.
     */
    private fun emitToken(sessionId: String, token: String, accumulated: String, isFinal: Boolean) {
        val event = Arguments.createMap().apply {
            putString("sessionId", sessionId)
            putString("token", token)
            putString("accumulated", accumulated)
            putBoolean("isFinal", isFinal)
        }
        eventEmitter?.emit("GemmaToken", event)
    }

    /**
     * Emits an error event to the React Native JS thread.
     */
    private fun emitError(sessionId: String, errorMessage: String) {
        val event = Arguments.createMap().apply {
            putString("sessionId", sessionId)
            putString("error", errorMessage)
            putBoolean("isFinal", true)
        }
        eventEmitter?.emit("GemmaToken", event)
    }

    // ══════════════════════════════════════════════════════════════════════
    // Mapping Helpers
    // ══════════════════════════════════════════════════════════════════════

    private fun recipeResultToWritableMap(result: GemmaRecipeResult): WritableMap {
        val map = Arguments.createMap()
        map.putString("dishId", result.dishId)
        map.putString("dishName", result.dishName)
        map.putString("recipeId", result.recipeId)
        map.putBoolean("isCompatible", result.isCompatible)

        val warningsArray = Arguments.createArray()
        for (warning in result.warnings) {
            warningsArray.pushString(warning)
        }
        map.putArray("warnings", warningsArray)
        map.putString("rawResponse", result.rawResponse)
        return map
    }

    private fun answerResultToWritableMap(result: GemmaAnswerResult): WritableMap {
        val map = Arguments.createMap()
        map.putString("answer", result.answer)
        result.referencedDishId?.let { map.putString("referencedDishId", it) }
        result.referencedDishName?.let { map.putString("referencedDishName", it) }
        map.putString("rawResponse", result.rawResponse)
        return map
    }

    // ══════════════════════════════════════════════════════════════════════
    // Error Handling
    // ══════════════════════════════════════════════════════════════════════

    private fun handleError(code: String, error: Exception, promise: Promise) {
        when (error) {
            is GemmaException -> {
                val errorMap = Arguments.createMap()
                errorMap.putString("message", error.message)
                error.rawResponse?.let { errorMap.putString("rawResponse", it) }
                promise.reject("GEMMA_ERROR", error.message, error, errorMap)
            }
            is IOException -> {
                promise.reject("NETWORK_ERROR", "Network request failed: ${error.message}", error)
            }
            else -> {
                promise.reject(code, error.message ?: "An unexpected error occurred", error)
            }
        }
    }

    /**
     * Cleans up resources when the module is destroyed.
     */
    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        streamingJob?.cancel()
        moduleScope.cancel()
        service.release()
    }
}
