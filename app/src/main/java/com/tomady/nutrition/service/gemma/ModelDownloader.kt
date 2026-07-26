package com.tomady.nutrition.service.gemma

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility for downloading and caching the Gemma 4 GGUF model file on-device.
 *
 * Gemma 4 quantized GGUF models are ~2 GB — too large to bundle in the APK.
 * This class downloads the model at first launch and caches it in the app's
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
 * The default download URL uses the **Kaggle API**, which requires **Kaggle credentials**
 * (username + API key). Set them via [setKaggleCredentials] before downloading.
 *
 * To get your Kaggle API key:
 * 1. Go to https://www.kaggle.com/settings
 * 2. Scroll to "API" section
 * 3. Click "Create New Token" — downloads kaggle.json containing username + key
 *
 * If no credentials are set, the service falls back to a **HuggingFace mirror**
 * which doesn't require authentication but may have slower download speeds.
 *
 * ## Model File Verification
 * The downloaded file is verified by checking its size (must be > 1 MB).
 * Real Gemma 4 4-bit GGUF files are approximately 2 GB.
 */
class ModelDownloader(private val context: Context) {

    /** Directory where model files are cached. */
    private val modelDir: File
        get() = File(context.filesDir, MODEL_DIR_NAME).also { it.mkdirs() }

    /**
     * Sets Kaggle API credentials for authenticated model download.
     *
     * Required when using the default Kaggle download URL.
     * Without credentials, the download will fall back to the HuggingFace mirror.
     *
     * @param username Your Kaggle username.
     * @param apiKey   Your Kaggle API key (from kaggle.json).
     */
    fun setKaggleCredentials(username: String, apiKey: String) {
        this.kaggleUsername = username
        this.kaggleApiKey = apiKey
    }

    /**
     * Returns whether Kaggle credentials have been configured.
     */
    fun hasKaggleCredentials(): Boolean = kaggleUsername != null && kaggleApiKey != null

    /**
     * The primary download URL. Uses Kaggle API if credentials are available,
     * otherwise uses the HuggingFace mirror.
     */
    fun getEffectiveDownloadUrl(): String {
        return if (hasKaggleCredentials()) {
            KAGGLE_DOWNLOAD_URL
        } else {
            HF_MIRROR_DOWNLOAD_URL
        }
    }

    /**
     * Ensures the Gemma model file is downloaded and returns its absolute path.
     *
     * @param forceReDownload If true, deletes any existing model file and re-downloads.
     * @return Absolute path to the model file, or null if download fails.
     */
    suspend fun ensureModelDownloaded(forceReDownload: Boolean = false): String? = withContext(Dispatchers.IO) {
        val modelFile = File(modelDir, MODEL_FILE_NAME)

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
     * @param urlStr URL of the GGUF model file to download.
     * @return Absolute path to the downloaded file, or null on failure.
     */
    suspend fun downloadFromUrl(urlStr: String = MODEL_DOWNLOAD_URL): String? = withContext(Dispatchers.IO) {
        val modelFile = File(modelDir, MODEL_FILE_NAME)
        if (modelFile.exists()) modelFile.delete()
        downloadModel(modelFile, urlStr)
    }

    /**
     * Returns the cached model file path, or null if not yet downloaded.
     */
    fun getCachedModelPath(): String? {
        val modelFile = File(modelDir, MODEL_FILE_NAME)
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
        val modelFile = File(modelDir, MODEL_FILE_NAME)
        if (modelFile.exists()) {
            modelFile.delete()
            Log.i(TAG, "Deleted cached model file")
        }
    }

    /**
     * Returns the size of the cached model file in bytes, or 0 if not cached.
     */
    fun getCachedModelSizeBytes(): Long {
        val modelFile = File(modelDir, MODEL_FILE_NAME)
        return if (modelFile.exists()) modelFile.length() else 0L
    }

    // ── Internal state ────────────────────────────────────────────────

    private var kaggleUsername: String? = null
    private var kaggleApiKey: String? = null

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

            // Add Kaggle authentication if available
            if (hasKaggleCredentials() && effectiveUrl.contains("kaggle.com")) {
                val combined = "$kaggleUsername:$kaggleApiKey"
                val basicAuth = "Basic " + android.util.Base64.encodeToString(
                    combined.toByteArray(), android.util.Base64.NO_WRAP
                )
                connection.setRequestProperty("Authorization", basicAuth)
            }

            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorMsg = if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    "Kaggle API requires authentication (HTTP 401). Call setKaggleCredentials(username, apiKey) before downloading, or use the HuggingFace mirror."
                } else {
                    "Download failed with HTTP $responseCode"
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

        /** Name of the Gemma 4 GGUF model file. */
        internal const val MODEL_FILE_NAME = "gemma-4-2b-it-qat-int4.gguf"

        /**
         * Kaggle API download URL for the MediaPipe-ready Gemma 4 GGUF model.
         * Requires Kaggle credentials (username + API key) via [setKaggleCredentials].
         */
        internal const val KAGGLE_DOWNLOAD_URL =
            "https://www.kaggle.com/api/v1/models/google/gemma-4/tfLite/gemma-4-2b-it-qat-int4/1/download"

        /**
         * HuggingFace mirror URL for Gemma 4 — no authentication required.
         * May be slower but works out of the box.
         */
        internal const val HF_MIRROR_DOWNLOAD_URL =
            "https://huggingface.co/google/gemma-4-2b-it-gguf/resolve/main/gemma-4-2b-it-qat-int4.gguf"

        /**
         * Default download URL used when determining which source to use.
         * This is dynamically selected based on whether Kaggle credentials are set.
         * For raw downloadFromUrl() calls, this defaults to HuggingFace mirror.
         */
        internal val MODEL_DOWNLOAD_URL: String
            get() = HF_MIRROR_DOWNLOAD_URL

        /** Minimum valid file size (1 MB) — real model is ~2 GB. */
        private const val MIN_VALID_FILE_SIZE = 1_000_000L

        /** Connection timeout in milliseconds. */
        private const val CONNECT_TIMEOUT_MS = 30_000

        /** Read timeout in milliseconds. */
        private const val READ_TIMEOUT_MS = 60_000

        /** Download buffer size (8 KB). */
        private const val BUFFER_SIZE = 8 * 1024
    }
}
