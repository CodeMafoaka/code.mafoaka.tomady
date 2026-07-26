package com.tomady.nutrition.bridge

import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.gson.Gson
import com.tomady.nutrition.data.AppDatabase
import com.tomady.nutrition.data.local.diet.DietDatabase
import com.tomady.nutrition.data.local.foodb.FooDBLocalDatabase
import com.tomady.nutrition.service.diet.DietAPIService
import com.tomady.nutrition.service.foodb.FooDBDataAPIService
import com.tomady.nutrition.service.gemma.GemmaAndroidService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * React Native bridge module exposing [GemmaAndroidService] to JavaScript.
 *
 * Handles the on-device Gemma LLM lifecycle and all AI-powered features:
 * - Model loading/download/status
 * - Recipe computation from natural language
 * - Nutrition Q&A with profile context
 * - Token streaming for real-time responses
 *
 * ## JS Usage
 * ```js
 * import { NativeModules, NativeEventEmitter } from 'react-native';
 * const { GemmaModule } = NativeModules;
 * const gemmaEmitter = new NativeEventEmitter(GemmaModule);
 *
 * // Check model status
 * const status = await GemmaModule.getModelStatus();
 * // status = { loaded: false, usingMock: true, version: 'mock...', progress: null }
 *
 * // Load model (instant check, falls back to mock if not cached)
 * const ready = await GemmaModule.loadModel();
 *
 * // Compute a recipe from natural language
 * const recipe = await GemmaModule.computeRecipe(
 *   'Suggest a low-sugar breakfast with rice and fruits',
 *   'user-1'
 * );
 *
 * // Ask a nutrition question
 * const answer = await GemmaModule.askQuestion(
 *   'Can I eat bananas with diabetes?',
 *   'user-1'
 * );
 *
 * // Stream tokens in real-time
 * const sessionId = await GemmaModule.startStreaming('What is a healthy lunch?');
 * gemmaEmitter.addListener('onToken', (event) => {
 *   console.log('Token:', event.token);
 *   // { sessionId: '...', token: '...', isComplete: false }
 * });
 * gemmaEmitter.addListener('onStreamComplete', (event) => {
 *   console.log('Done:', event.fullResponse);
 * });
 *
 * // Trigger model download (background, ~1.5 GB)
 * await GemmaModule.downloadModel();
 * // Listen for progress
 * gemmaEmitter.addListener('onModelDownloadProgress', (event) => {
 *   console.log('Download:', Math.round(event.progress * 100) + '%');
 * });
 * ```
 */
class GemmaModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = MODULE_NAME

    // ── Service initialisation ──────────────────────────────────────────

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val gson = Gson()

    private val gemmaService: GemmaAndroidService by lazy {
        val ctx = reactApplicationContext
        val db = AppDatabase.getInstance(ctx)
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
        val dietService = DietAPIService(dietDatabase = dietDb, foodbService = foodbService)
        GemmaAndroidService(
            context = ctx,
            dietDatabase = dietDb,
            dietService = dietService,
            foodbService = foodbService
        )
    }

    // ════════════════════════════════════════════════════════════════════
    // SECTION 1 — Model Lifecycle
    // ════════════════════════════════════════════════════════════════════

    /**
     * Loads the Gemma model (instant check — no download).
     *
     * If the model GGUF file is already cached on disk, this initialises
     * MediaPipe LlmInference with the real model. Otherwise falls back
     * to MockLLMEngine instantly — no blocking download.
     *
     * Call [downloadModel] to trigger the actual download separately.
     *
     * @param modelPath Optional explicit path to a GGUF model file.
     * @param promise Resolves with `true` if model (real or mock) is ready.
     */
    @ReactMethod
    fun loadModel(modelPath: String?, promise: Promise) {
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    gemmaService.loadModel(modelPath)
                }
                promise.resolve(result)
            } catch (e: Exception) {
                promise.reject("GEMMA_LOAD_ERROR", e.message, e)
            }
        }
    }

    /**
     * Returns the current model status as a JSON map.
     *
     * @param promise Resolves with:
     *   - `loaded`: boolean — whether the model is loaded
     *   - `usingMock`: boolean — whether mock fallback is active
     *   - `version`: string or null — model version string
     *   - `isLoading`: boolean — whether loading is in progress
     *   - `progress`: float or null — download progress (0.0 to 1.0)
     *   - `hasCachedModel`: boolean — whether a GGUF file exists on disk
     */
    @ReactMethod
    fun getModelStatus(promise: Promise) {
        scope.launch {
            try {
                val status = Arguments.createMap()
                status.putBoolean("loaded", gemmaService.isModelLoaded())
                status.putBoolean("usingMock", gemmaService.isUsingMockFallback())
                status.putString("version", gemmaService.getModelVersion())
                status.putBoolean("isLoading", gemmaService.isLoadingModel())
                status.putDouble(
                    "progress",
                    (gemmaService.getDownloadProgress() ?: 0.0).toDouble()
                )
                promise.resolve(status)
            } catch (e: Exception) {
                promise.reject("GEMMA_STATUS_ERROR", e.message, e)
            }
        }
    }

    /**
     * Triggers the Gemma model download in the background.
     *
     * This is a **long-running** operation (~1.5 GB download). Progress
     * events are emitted via `onModelDownloadProgress`.
     *
     * After download completes, call [loadModel] again to activate the
     * real model.
     *
     * @param promise Resolves with the model file path once download completes.
     */
    @ReactMethod
    fun downloadModel(promise: Promise) {
        scope.launch {
            try {
                // First check if already cached
                val cached = gemmaService.downloadModelIfNeeded()
                if (cached != null) {
                    promise.resolve(cached)
                    return@launch
                }

                // Poll progress while downloading (runs on IO internally)
                withContext(Dispatchers.IO) {
                    kotlinx.coroutines.launch {
                        // Poll progress every 500ms while download is active
                        while (gemmaService.getDownloadProgress() != null) {
                            val progress = gemmaService.getDownloadProgress() ?: 0f
                            emitDownloadProgress(progress)
                            kotlinx.coroutines.delay(500)
                        }
                    }

                    // Trigger the actual download (blocks until complete)
                    val result = gemmaService.downloadModelIfNeeded()

                    // Final progress = 1.0 or signal completion
                    emitDownloadProgress(1.0f)

                    // Reload the model now that the file is cached
                    gemmaService.loadModel(null)

                    promise.resolve(result)
                }
            } catch (e: Exception) {
                promise.reject("GEMMA_DOWNLOAD_ERROR", e.message, e)
            }
        }
    }

    /**
     * Releases the Gemma model from memory.
     *
     * Call this to free ~1.5 GB of RAM when the AI features are not in use.
     *
     * @param promise Resolves when the model is released.
     */
    @ReactMethod
    fun releaseModel(promise: Promise) {
        try {
            gemmaService.release()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("GEMMA_RELEASE_ERROR", e.message, e)
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // SECTION 2 — Recipe Computation
    // ════════════════════════════════════════════════════════════════════

    /**
     * Computes a dish/recipe recommendation from natural language.
     *
     * Pipeline:
     * 1. LLM generates structured recipe data from the prompt
     * 2. Dish + Recipe are created in the local database
     * 3. Ingredients are cross-referenced with FooDB for nutrition
     * 4. Result is validated against the user's health profile
     *
     * @param prompt  Natural-language request (e.g., "Suggest a low-calorie dinner").
     * @param userId  The target user for profile-aware validation.
     * @param promise Resolves with { dishId, dishName, recipeId, isCompatible, warnings, rawResponse }.
     */
    @ReactMethod
    fun computeRecipe(prompt: String, userId: String, promise: Promise) {
        scope.launch {
            try {
                ensureModelLoaded()
                val result = withContext(Dispatchers.IO) {
                    gemmaService.computeRecipe(prompt, userId)
                }
                promise.resolve(mapToWritable(
                    gson.fromJson(gson.toJson(result), Map::class.java)
                ))
            } catch (e: Exception) {
                promise.reject("GEMMA_RECIPE_ERROR", e.message, e)
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // SECTION 3 — Question Answering
    // ════════════════════════════════════════════════════════════════════

    /**
     * Answers a nutrition/diet question with profile context.
     *
     * The LLM is given the user's health profile (goals, allergies,
     * conditions) so responses are personalised.
     *
     * @param question The user's question.
     * @param userId   The target user for profile context.
     * @param promise  Resolves with { answer, referencedDishId, referencedDishName }.
     */
    @ReactMethod
    fun askQuestion(question: String, userId: String, promise: Promise) {
        scope.launch {
            try {
                ensureModelLoaded()
                val result = withContext(Dispatchers.IO) {
                    gemmaService.askQuestion(question, userId)
                }
                promise.resolve(mapToWritable(
                    gson.fromJson(gson.toJson(result), Map::class.java)
                ))
            } catch (e: Exception) {
                promise.reject("GEMMA_ASK_ERROR", e.message, e)
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // SECTION 4 — Token Streaming
    // ════════════════════════════════════════════════════════════════════

    /**
     * Starts a streaming inference session.
     *
     * Tokens are emitted as RN events:
     * - `onGemmaToken`: { sessionId, token, isComplete: false }
     * - `onGemmaStreamComplete`: { sessionId, fullResponse }
     * - `onGemmaStreamError`: { sessionId, error }
     *
     * @param query   The input prompt.
     * @param promise Resolves immediately with the sessionId.
     */
    @ReactMethod
    fun startStreaming(query: String, promise: Promise) {
        val sessionId = UUID.randomUUID().toString()
        scope.launch {
            try {
                val ctx = reactApplicationContext
                gemmaService.generateTokens(query, sessionId).collect { token ->
                    val event = Arguments.createMap()
                    event.putString("sessionId", sessionId)
                    event.putString("token", token)
                    event.putBoolean("isComplete", false)
                    ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                        .emit("onGemmaToken", event)
                }
                // Stream completed
                val completeEvent = Arguments.createMap()
                completeEvent.putString("sessionId", sessionId)
                completeEvent.putBoolean("isComplete", true)
                ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit("onGemmaStreamComplete", completeEvent)
                promise.resolve(sessionId)
            } catch (e: Exception) {
                val errorEvent = Arguments.createMap()
                errorEvent.putString("sessionId", sessionId)
                errorEvent.putString("error", e.message)
                reactApplicationContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit("onGemmaStreamError", errorEvent)
                promise.reject("GEMMA_STREAM_ERROR", e.message, e)
            }
        }
    }

    /**
     * Cancels an active streaming session.
     *
     * @param sessionId The session ID returned by [startStreaming].
     * @param promise   Resolves when streaming is cancelled.
     */
    @ReactMethod
    fun cancelStreaming(sessionId: String, promise: Promise) {
        try {
            gemmaService.cancelStreaming(sessionId)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("GEMMA_CANCEL_ERROR", e.message, e)
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // SECTION 5 — Events (RN-side)
    // ════════════════════════════════════════════════════════════════════

    /**
     * Emits a download progress event to React Native.
     * Called from the native download coroutine.
     */
    internal fun emitDownloadProgress(progress: Float) {
        try {
            val event = Arguments.createMap()
            event.putDouble("progress", progress.toDouble())
            reactApplicationContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("onModelDownloadProgress", event)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to emit download progress: ${e.message}")
        }
    }

    /**
     * Required for NativeEventEmitter support in React Native.
     */
    override fun getConstants(): MutableMap<String, Any> {
        return mutableMapOf(
            "MODULE_NAME" to MODULE_NAME,
            "EVENT_ON_TOKEN" to "onGemmaToken",
            "EVENT_ON_STREAM_COMPLETE" to "onGemmaStreamComplete",
            "EVENT_ON_STREAM_ERROR" to "onGemmaStreamError",
            "EVENT_ON_DOWNLOAD_PROGRESS" to "onModelDownloadProgress"
        )
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * Ensures the model is loaded before performing inference.
     * If not loaded yet, loads it (instant — mock fallback if no cache).
     */
    private suspend fun ensureModelLoaded() {
        if (!gemmaService.isModelLoaded()) {
            gemmaService.loadModel(null)
        }
    }

    private fun mapToWritable(map: Map<String, Any?>): WritableMap {
        return BridgeUtils.mapToWritable(map)
    }

    companion object {
        const val MODULE_NAME = "GemmaModule"
        private const val TAG = "GemmaModule"
    }
}
