package com.tomady.nutrition.config

/**
 * Root configuration object for the Tomady backend.
 *
 * Persisted as JSON on-device and mutable at runtime via the
 * `GET/POST /api/v1/config` REST endpoints (see [ConfigManager]).
 */
data class TomadyConfig(
    var server: ServerConfig = ServerConfig(),
    var gemma: GemmaConfig = GemmaConfig(),
    var postgres: PostgresConfig = PostgresConfig(),
    var nutrition: NutritionConfig = NutritionConfig()
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

/**
 * Connection details for the remote PostgreSQL instance holding the full
 * FooDB dataset (same table shape as `init_ressources/foodb_generated_schema_only.sql`,
 * hosted on Postgres instead of embedded SQLite). Set via `POST /api/v1/config`,
 * then trigger `POST /api/v1/foodb/sync` to pull `food`/`content`/`nutrient`
 * rows into the local Room cache — see FooDBRemoteSyncService.
 */
data class PostgresConfig(
    var host: String? = null,
    var port: Int = 5432,
    var database: String? = null,
    var username: String? = null,
    var password: String? = null,
    /** One of "disable" | "prefer" | "require" (libpq sslmode values). */
    var sslMode: String = "prefer"
)

/**
 * Which [com.tomady.nutrition.service.nutrition.NutrientDataProvider] serves
 * macro-nutrient (calorie/protein/carb/fat) lookups, and its credentials.
 * FooDB (see [PostgresConfig]) turned out not to carry usable macro data —
 * it's a compound/phytochemical composition database — so this is a
 * separate, pluggable source. Currently only "usda_fdc" is implemented.
 */
data class NutritionConfig(
    var provider: String = "usda_fdc",
    /** USDA FoodData Central API key. "DEMO_KEY" works out of the box but is
     * rate-limited (30 req/hour, 50/day) — get a free instant key at
     * fdc.nal.usda.gov/api-key-signup.html for real usage. */
    var usdaApiKey: String = "DEMO_KEY"
)
