package com.tomady.nutrition.config

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

/**
 * Reads/writes [TomadyConfig] as JSON at `context.filesDir/config/tomady_config.json`.
 *
 * Stateless by design: every [get] re-reads the file from disk, so any
 * `ConfigManager(context)` instance (REST server, Gemma service, RN bridge, ...)
 * always observes the latest persisted config without needing a shared singleton.
 */
class ConfigManager(private val context: Context) {

    private val gson = Gson()

    private val configFile: File
        get() {
            val dir = File(context.filesDir, CONFIG_DIR_NAME).also { it.mkdirs() }
            return File(dir, CONFIG_FILE_NAME)
        }

    /** Returns the current effective config, or defaults if none has been saved yet. */
    fun get(): TomadyConfig {
        return try {
            if (configFile.exists()) {
                gson.fromJson(configFile.readText(), TomadyConfig::class.java) ?: TomadyConfig()
            } else {
                TomadyConfig()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read config, falling back to defaults: ${e.message}", e)
            TomadyConfig()
        }
    }

    /**
     * Merges [partial] on top of the current config (only the fields present in
     * [partial] are overwritten, recursively) and persists the result.
     */
    fun update(partial: JsonObject): TomadyConfig = synchronized(LOCK) {
        val currentJson = gson.toJsonTree(get()).asJsonObject
        val merged = deepMerge(currentJson, partial)
        val newConfig = gson.fromJson(merged, TomadyConfig::class.java)
        configFile.writeText(gson.toJson(newConfig))
        newConfig
    }

    /** Loads a JSON config document from an on-device file path and merges it in via [update]. */
    fun loadFromFilePath(path: String): TomadyConfig {
        val file = File(path)
        require(file.exists()) { "Config file not found: $path" }
        val parsed = JsonParser.parseString(file.readText())
        require(parsed.isJsonObject) { "Config file must contain a JSON object: $path" }
        return update(parsed.asJsonObject)
    }

    private fun deepMerge(base: JsonObject, overlay: JsonObject): JsonObject {
        for ((key, value) in overlay.entrySet()) {
            val existing = base.get(key)
            if (value.isJsonObject && existing != null && existing.isJsonObject) {
                deepMerge(existing.asJsonObject, value.asJsonObject)
            } else {
                base.add(key, value)
            }
        }
        return base
    }

    companion object {
        private const val TAG = "ConfigManager"
        private const val CONFIG_DIR_NAME = "config"
        private const val CONFIG_FILE_NAME = "tomady_config.json"
        private val LOCK = Any()
    }
}
