package com.tomady.nutrition.service.nutrition

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * [NutrientDataProvider] backed by the USDA FoodData Central public API
 * (https://fdc.nal.usda.gov/api-guide). Verified shape (not guessed):
 * `GET /fdc/v1/foods/search?query=<name>&api_key=<key>&pageSize=1` returns
 * `{ foods: [ { description, foodNutrients: [ {nutrientName, value, unitName}, ... ] } ] }`.
 *
 * Works with the shared `DEMO_KEY` (rate-limited: 30 req/hour, 50/day) for
 * development — swap in a real key via `nutrition.usdaApiKey` config for
 * real usage (self-serve, instant signup, no approval wait).
 */
class UsdaFdcNutrientProvider(private val apiKey: String) : NutrientDataProvider {
    override val id: String = "usda_fdc"

    override suspend fun getMacros(foodName: String): FoodMacros? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(foodName, "UTF-8")
            val url = URL("$BASE_URL/foods/search?query=$encodedQuery&api_key=$apiKey&pageSize=1")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")

            if (connection.responseCode !in 200..299) {
                Log.w(TAG, "USDA FDC search failed for '$foodName': HTTP ${connection.responseCode}")
                return@withContext null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JsonParser.parseString(body).asJsonObject
            val foods = root.getAsJsonArray("foods")
            if (foods == null || foods.size() == 0) return@withContext null

            val food = foods[0].asJsonObject
            val matchedName = food.get("description")?.asString ?: foodName
            val nutrients = food.getAsJsonArray("foodNutrients") ?: return@withContext null

            fun findValue(vararg nameContains: String): Double? {
                for (element in nutrients) {
                    val obj = element.asJsonObject
                    val name = obj.get("nutrientName")?.asString?.lowercase() ?: continue
                    if (nameContains.any { name.contains(it) }) {
                        return obj.get("value")?.takeIf { !it.isJsonNull }?.asDouble
                    }
                }
                return null
            }

            FoodMacros(
                source = id,
                matchedName = matchedName,
                calories = findValue("energy"),
                proteinG = findValue("protein"),
                carbsG = findValue("carbohydrate"),
                fatG = findValue("total lipid", "fat"),
                fiberG = findValue("fiber")
            )
        } catch (e: Exception) {
            Log.e(TAG, "USDA FDC lookup failed for '$foodName': ${e.message}", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val TAG = "UsdaFdcNutrientProvider"
        private const val BASE_URL = "https://api.nal.usda.gov/fdc/v1"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
    }
}
