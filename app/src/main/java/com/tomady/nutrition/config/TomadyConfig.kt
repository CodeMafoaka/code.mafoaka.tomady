package com.tomady.nutrition.config

/**
 * Root configuration object for the Tomady backend.
 *
 * Persisted as JSON on-device and mutable at runtime via the
 * `GET/POST /api/v1/config` REST endpoints (see [ConfigManager]).
 */
data class TomadyConfig(
    var server: ServerConfig = ServerConfig(),
    var gemma: GemmaConfig = GemmaConfig()
)

/**
 * @param port Port the embedded REST API binds to. Changing this only takes
 * effect after the app process is restarted (NanoHTTPD binds at construction).
 */
data class ServerConfig(
    var port: Int = 7777
)

/**
 * Everything needed to locate, authenticate for, and load the on-device
 * Gemma model. `modelDownloadUrl` / `kaggleModelDownloadUrl` point at a
 * MediaPipe LLM Inference-compatible `.task` bundle (NOT a llama.cpp GGUF —
 * `com.google.mediapipe.tasks.genai.llminference.LlmInference` cannot load GGUF files).
 *
 * Defaults point at the litert-community mirror of Gemma 3 1B-IT, which is
 * gated on Hugging Face — set [huggingfaceToken] to a valid HF access token
 * with the Gemma license accepted, or set Kaggle credentials to use the
 * Kaggle mirror instead.
 */
data class GemmaConfig(
    var modelFileName: String = "gemma3-1b-it-int4.task",
    var modelDownloadUrl: String =
        "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task?download=true",
    var kaggleModelDownloadUrl: String =
        "https://www.kaggle.com/api/v1/models/google/gemma-3/tfLite/gemma3-1b-it-int4/1/download",
    var kaggleUsername: String? = null,
    var kaggleApiKey: String? = null,
    var huggingfaceToken: String? = null,
    var maxTokens: Int = 1024
)
