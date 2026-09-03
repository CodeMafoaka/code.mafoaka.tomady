package com.tomady.nutrition.service.nutrition

import com.tomady.nutrition.config.ConfigManager

/**
 * Facade over pluggable [NutrientDataProvider]s. Resolves the active provider
 * from `nutrition.provider` config on every call (cheap — [ConfigManager] is
 * a stateless file read), so switching providers via `POST /api/v1/config`
 * takes effect immediately without restarting the app.
 */
class NutritionLookupService(private val configManager: ConfigManager) {

    suspend fun getMacros(foodName: String): FoodMacros? {
        return resolveProvider()?.getMacros(foodName)
    }

    private fun resolveProvider(): NutrientDataProvider? {
        val config = configManager.get().nutrition
        return when (config.provider) {
            "usda_fdc" -> UsdaFdcNutrientProvider(config.usdaApiKey)
            else -> null
        }
    }
}
