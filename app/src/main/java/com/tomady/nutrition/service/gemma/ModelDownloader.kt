package com.tomady.nutrition.service.gemma

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import com.tomady.nutrition.config.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility for downloading and caching the on-device Gemma `.task` model file.
 *
 * MediaPipe LLM Inference (`com.google.mediapipe.tasks.genai.llminference.LlmInference`)
 * loads a `.task` bundle, NOT a llama.cpp GGUF file — model file name and download
 * URLs are sourced from [ConfigManager] ([com.tomady.nutrition.config.GemmaConfig])
 * so they can be corrected/overridden without a code change, e.g. via
 * `POST /api/v1/config`.
 *
 * Quantized `.task` models are ~500MB-1GB — too large to bundle in the APK.
 * This class downloads the model at first use and caches it in the app's
 * internal storage (`context.filesDir/models/`).
 *
 * ## Usage
 * ```kotlin
 * val downloader = ModelDownloader(context)
 * downloader.setKaggleCredentials("your-username", "your-api-key")
 * val path = downloader.ensureModelDownloaded()
 * ```
 *
 * ## Authentication
 * The default download URL is a Hugging Face mirror; the Gemma license is
 * gated there, so a Hugging Face access token is required (set via config's
 * `gemma.huggingfaceToken`). Setting Kaggle credentials instead switches the
 * effective URL to the Kaggle API mirror.
 *
 * To get your Kaggle API key:
 * 1. Go to https://www.kaggle.com/settings
 * 2. Scroll to "API" section
 * 3. Click "Create New Token" — downloads kaggle.json containing username + key
 *
 * ## Model File Verification
 * The downloaded file is verified by checking its size (must be > 1 MB).
 */
class ModelDownloader(
    private val context: Context,
    private val configManager: ConfigManager = ConfigManager(context)
) {

    /** Directory where model files are cached. */
    private val modelDir: File
        get() = File(context.filesDir, MODEL_DIR_NAME).also { it.mkdirs() }

    private fun modelFileName(): String = configManager.get().gemma.modelFileName

    /**
     * Sets Kaggle API credentials for authenticated model download.
     *
     * Required when using the Kaggle download URL. Without credentials, the
     * effective download falls back to the configured Hugging Face mirror.
     *
     * @param username Your Kaggle username.
     * @param apiKey   Your Kaggle API key (from kaggle.json).
     */
    fun setKaggleCredentials(username: String, apiKey: String) {
        val partial = JsonObject().apply {
            add("gemma", JsonObject().apply {
                addProperty("kaggleUsername", username)
                addProperty("kaggleApiKey", apiKey)
            })
        }
        configManager.update(partial)
    }

    /**
     * Returns whether Kaggle credentials have been configured.
     */
    fun hasKaggleCredentials(): Boolean {
        val gemma = configManager.get().gemma
        return !gemma.kaggleUsername.isNullOrBlank() && !gemma.kaggleApiKey.isNullOrBlank()
    }

    private fun hasHuggingFaceToken(): Boolean =
        !configManager.get().gemma.huggingfaceToken.isNullOrBlank()

    /**
     * The primary download URL. Uses the Kaggle API if credentials are available,
     * otherwise uses the configured Hugging Face mirror.
     */
    fun getEffectiveDownloadUrl(): String {
        val gemma = configManager.get().gemma
        return if (hasKaggleCredentials()) {
            gemma.kaggleModelDownloadUrl
        } else {
            gemma.modelDownloadUrl
        }
    }

    /**
     * Ensures the Gemma model file is downloaded and returns its absolute path.
     *
     * @param forceReDownload If true, deletes any existing model file and re-downloads.
     * @return Absolute path to the model file, or null if download fails.
     */
    suspend fun ensureModelDownloaded(forceReDownload: Boolean = false): String? = withContext(Dispatchers.IO) {
        val modelFile = File(modelDir, modelFileName())

        // Check if model already exists
        if (modelFile.exists() && !forceReDownload) {
            if (modelFile.length() > MIN_VALID_FILE_SIZE) {
                Log.i(TAG, "Model file already exists: ${modelFile.absolutePath} (${modelFile.length()} bytes)")
                return@withContext modelFile.absolutePath
            } else {
                Log.w(TAG, "Model file exists but is too small (${modelFile.length()} bytes), re-downloading")
                modelFile.delete()
            }
        }

        // Download the model
        return@withContext downloadModel(modelFile)
    }

    /**
     * Downloads the Gemma model from a remote URL.
     *
     * @param urlStr URL of the `.task` model file to download; defaults to the
     * effective configured URL (Kaggle if credentials are set, else Hugging Face).
     * @return Absolute path to the downloaded file, or null on failure.
     */
    suspend fun downloadFromUrl(urlStr: String? = null): String? = withContext(Dispatchers.IO) {
        val modelFile = File(modelDir, modelFileName())
        if (modelFile.exists()) modelFile.delete()
        downloadModel(modelFile, urlStr ?: getEffectiveDownloadUrl())
    }

    /**
     * Returns the cached model file path, or null if not yet downloaded.
     */
    fun getCachedModelPath(): String? {
        val modelFile = File(modelDir, modelFileName())
        return if (modelFile.exists() && modelFile.length() > MIN_VALID_FILE_SIZE) {
            modelFile.absolutePath
        } else null
    }

    /**
     * Returns the download progress as a float (0.0 to 1.0), or null if no download is in progress.
     */
    @Volatile
    var downloadProgress: Float? = null
        private set

    /**
     * Deletes the cached model file to free up storage space.
     */
    fun deleteCachedModel() {
        val modelFile = File(modelDir, modelFileName())
        if (modelFile.exists()) {
            modelFile.delete()
            Log.i(TAG, "Deleted cached model file")
        }
    }

    /**
     * Returns the size of the cached model file in bytes, or 0 if not cached.
     */
    fun getCachedModelSizeBytes(): Long {
        val modelFile = File(modelDir, modelFileName())
        return if (modelFile.exists()) modelFile.length() else 0L
    }

    // ── Private helpers ────────────────────────────────────────────────

    /**
     * Downloads the model file from a URL to the specified file path.
     *
     * Automatically selects the best URL based on available credentials:
     * - If Kaggle credentials are set, uses the Kaggle API URL
     * - Otherwise, uses the HuggingFace mirror
     *
     * @param targetFile The file to write the downloaded data to.
     * @param urlStr The URL of the model file (auto-selected if not specified).
     * @return The absolute path of the downloaded file, or null on failure.
     */
    private suspend fun downloadModel(
        targetFile: File,
        urlStr: String? = null
    ): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            // Select the best URL
            val effectiveUrl = urlStr ?: getEffectiveDownloadUrl()
            Log.i(TAG, "Starting model download from $effectiveUrl")
            downloadProgress = 0.0f

            val url = URL(effectiveUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/octet-stream")

            // Add authentication if available — Kaggle basic auth or HF bearer token
            val gemmaConfig = configManager.get().gemma
            if (hasKaggleCredentials() && effectiveUrl.contains("kaggle.com")) {
                val combined = "${gemmaConfig.kaggleUsername}:${gemmaConfig.kaggleApiKey}"
                val basicAuth = "Basic " + android.util.Base64.encodeToString(
                    combined.toByteArray(), android.util.Base64.NO_WRAP
                )
                connection.setRequestProperty("Authorization", basicAuth)
            } else if (hasHuggingFaceToken() && effectiveUrl.contains("huggingface.co")) {
                connection.setRequestProperty("Authorization", "Bearer ${gemmaConfig.huggingfaceToken}")
            }

            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorMsg = when (responseCode) {
                    HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN ->
                        "Authentication required or rejected (HTTP $responseCode) for $effectiveUrl. " +
                            "Set gemma.kaggleUsername/kaggleApiKey or gemma.huggingfaceToken via POST /api/v1/config " +
                            "(the Hugging Face Gemma repo is gated — the token's account must have accepted the license)."
                    else -> "Download failed with HTTP $responseCode"
                }
                Log.e(TAG, errorMsg)
                downloadProgress = null
                return@withContext null
            }

            val contentLength = connection.contentLengthLong
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                if (contentLength > 0) {
                    downloadProgress = totalBytesRead.toFloat() / contentLength.toFloat()
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            downloadProgress = null

            // Verify downloaded file
            if (targetFile.exists() && targetFile.length() > MIN_VALID_FILE_SIZE) {
                Log.i(TAG, "Model downloaded successfully: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                targetFile.absolutePath
            } else {
                Log.e(TAG, "Downloaded file is invalid or too small")
                targetFile.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Model download failed: ${e.message}", e)
            downloadProgress = null
            if (targetFile.exists()) targetFile.delete()
            null
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val TAG = "ModelDownloader"

        /** Directory name inside filesDir where models are cached. */
        internal const val MODEL_DIR_NAME = "models"

        /** Minimum valid file size (1 MB) — real quantized `.task` models are several hundred MB+. */
        private const val MIN_VALID_FILE_SIZE = 1_000_000L

        /** Connection timeout in milliseconds. */
        private const val CONNECT_TIMEOUT_MS = 30_000

        /** Read timeout in milliseconds. */
        private const val READ_TIMEOUT_MS = 60_000

        /** Download buffer size (8 KB). */
        private const val BUFFER_SIZE = 8 * 1024
    }
}
