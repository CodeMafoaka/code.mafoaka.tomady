package com.tomady.nutrition.bridge

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.tomady.nutrition.data.AppDatabase
import com.tomady.nutrition.data.local.foodb.FooDBLocalDatabase
import com.tomady.nutrition.service.foodb.FooDBDataAPIService
import com.tomady.nutrition.service.foodb.FooDbApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * React Native bridge module exposing FooDB food composition data to the RN host.
 *
 * Provides methods for food search and nutrient lookup against the local FooDB database
 * with cache-first semantics and remote API fallback.
 *
 * All public methods are annotated with [ReactMethod] and follow the RN Promise pattern:
 * - On success: [promise.resolve] with a [WritableMap] or [WritableArray]
 * - On error: [promise.reject] with an error code and message
 */
class FooDBModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val moduleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val service: FooDBDataAPIService by lazy {
        val db = AppDatabase.getInstance(reactContext.applicationContext)
        val localDb = FooDBLocalDatabase(
            foodItemDao = db.foodItemDao(),
            nutrientPropertyDao = db.nutrientPropertyDao()
        )
        FooDBDataAPIService(localDatabase = localDb)
    }

    override fun getName(): String = "TomadyFooDB"

    // ── Public bridge methods ──────────────────────────────────────────

    /**
     * Searches for foods matching the given query in the local database.
     *
     * @param query  Free-text search string.
     * @param promise Resolves with a WritableArray of food item maps, or rejects on error.
     */
    @ReactMethod
    fun searchFood(query: String, promise: Promise) {
        moduleScope.launch {
            try {
                val foods = withContext(Dispatchers.IO) {
                    service.searchFood(query)
                }
                val resultArray = Arguments.createArray()
                for (food in foods) {
                    resultArray.pushMap(foodItemToWritableMap(food))
                }
                promise.resolve(resultArray)
            } catch (e: Exception) {
                handleError("FOOD_SEARCH_ERROR", e, promise)
            }
        }
    }

    /**
     * Retrieves detailed nutrient information for a specific food item.
     *
     * Implements cache-first logic: checks local DB first, fetches from remote API
     * on cache miss, persists locally, and returns the result.
     *
     * @param foodId  The FooDB food identifier (received as Double from RN, converted to Long).
     * @param promise Resolves with a WritableMap containing "food" and "nutrients" keys,
     *                or rejects on error.
     */
    @ReactMethod
    fun getNutrients(foodId: Double, promise: Promise) {
        moduleScope.launch {
            try {
                val id = foodId.toLong()
                val detail = withContext(Dispatchers.IO) {
                    service.getFoodDetails(id)
                }

                if (detail == null) {
                    promise.resolve(null)
                    return@launch
                }

                val resultMap = Arguments.createMap()
                resultMap.putMap("food", foodItemToWritableMap(detail.food))
                resultMap.putArray(
                    "nutrients",
                    nutrientPropertiesToWritableArray(detail.nutrients)
                )
                promise.resolve(resultMap)
            } catch (e: Exception) {
                handleError("FOOD_NUTRIENTS_ERROR", e, promise)
            }
        }
    }

    /**
     * Retrieves all distinct food groups available in the local catalog.
     *
     * @param promise Resolves with a WritableArray of group strings, or rejects on error.
     */
    @ReactMethod
    fun getFoodGroups(promise: Promise) {
        moduleScope.launch {
            try {
                val groups = withContext(Dispatchers.IO) {
                    service.getFoodGroups()
                }
                val resultArray = Arguments.createArray()
                for (group in groups) {
                    resultArray.pushString(group)
                }
                promise.resolve(resultArray)
            } catch (e: Exception) {
                handleError("FOOD_GROUPS_ERROR", e, promise)
            }
        }
    }

    /**
     * Retrieves all distinct nutrient names available in the local database.
     *
     * @param promise Resolves with a WritableArray of nutrient name strings, or rejects on error.
     */
    @ReactMethod
    fun getNutrientNames(promise: Promise) {
        moduleScope.launch {
            try {
                val names = withContext(Dispatchers.IO) {
                    service.getNutrientNames()
                }
                val resultArray = Arguments.createArray()
                for (name in names) {
                    resultArray.pushString(name)
                }
                promise.resolve(resultArray)
            } catch (e: Exception) {
                handleError("NUTRIENT_NAMES_ERROR", e, promise)
            }
        }
    }

    // ── Mapping helpers ────────────────────────────────────────────────

    /**
     * Converts a [FoodItem] entity to a [WritableMap] for React Native.
     */
    private fun foodItemToWritableMap(food: com.tomady.nutrition.data.local.foodb.entity.FoodItem): WritableMap {
        val map = Arguments.createMap()
        map.putDouble("id", food.id.toDouble())
        food.publicId?.let { map.putString("publicId", it) }
        food.name?.let { map.putString("name", it) }
        food.nameScientific?.let { map.putString("nameScientific", it) }
        food.description?.let { map.putString("description", it) }
        food.foodGroup?.let { map.putString("foodGroup", it) }
        food.foodSubgroup?.let { map.putString("foodSubgroup", it) }
        food.foodType?.let { map.putString("foodType", it) }
        food.category?.let { map.putString("category", it) }
        food.itisId?.let { map.putString("itisId", it) }
        food.wikipediaId?.let { map.putString("wikipediaId", it) }
        food.ncbiTaxonomyId?.let { map.putDouble("ncbiTaxonomyId", it.toDouble()) }
        food.pictureFileName?.let { map.putString("pictureFileName", it) }
        return map
    }

    /**
     * Converts a list of [NutrientProperty] entities to a [WritableArray] for React Native.
     */
    private fun nutrientPropertiesToWritableArray(
        nutrients: List<com.tomady.nutrition.data.local.foodb.entity.NutrientProperty>
    ): WritableArray {
        val array = Arguments.createArray()
        for (np in nutrients) {
            val map = Arguments.createMap()
            map.putDouble("id", np.id.toDouble())
            map.putDouble("foodItemId", np.foodItemId.toDouble())
            np.nutrientName?.let { map.putString("nutrientName", it) }
            np.amount?.let { map.putDouble("amount", it) }
            np.unit?.let { map.putString("unit", it) }
            np.standardContent?.let { map.putDouble("standardContent", it) }
            np.preparationType?.let { map.putString("preparationType", it) }
            np.citation?.let { map.putString("citation", it) }
            np.origContent?.let { map.putString("origContent", it) }
            array.pushMap(map)
        }
        return array
    }

    // ── Error handling ─────────────────────────────────────────────────

    /**
     * Translates exceptions into appropriate React Native Promise rejections.
     *
     * - [FooDbApiException] → "FOODB_API_ERROR" with the HTTP status code
     * - [IOException] → "NETWORK_ERROR"
     * - All others → "INTERNAL_ERROR"
     */
    private fun handleError(code: String, error: Exception, promise: Promise) {
        when (error) {
            is FooDbApiException -> {
                val errorMap = Arguments.createMap()
                errorMap.putInt("statusCode", error.statusCode)
                errorMap.putString("message", error.message)
                promise.reject("FOODB_API_ERROR", error.message, error, errorMap)
            }
            is IOException -> {
                promise.reject(
                    "NETWORK_ERROR",
                    "Network request failed: ${error.message}",
                    error
                )
            }
            else -> {
                promise.reject(
                    "INTERNAL_ERROR",
                    "An unexpected error occurred: ${error.message}",
                    error
                )
            }
        }
    }
}
