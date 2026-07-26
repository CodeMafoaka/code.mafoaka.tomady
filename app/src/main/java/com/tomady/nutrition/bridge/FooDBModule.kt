package com.tomady.nutrition.bridge

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tomady.nutrition.data.AppDatabase
import com.tomady.nutrition.data.local.foodb.FooDBLocalDatabase
import com.tomady.nutrition.service.foodb.FooDBDataAPIService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * React Native bridge module exposing [FooDBDataAPIService] to JavaScript.
 *
 * ## JS Usage
 * ```js
 * import { NativeModules } from 'react-native';
 * const { FooDBModule } = NativeModules;
 *
 * // Search foods
 * const results = await FooDBModule.searchFood('apple');
 * // results = [{ id: 1, name: 'Apple', foodGroup: 'Fruits', ... }, ...]
 *
 * // Get food details
 * const detail = await FooDBModule.getFoodDetails(42);
 * // detail = { food: {...}, nutrients: [...] }
 *
 * // Get food groups
 * const groups = await FooDBModule.getFoodGroups();
 * // groups = ['Fruits', 'Vegetables', 'Grains', ...]
 * ```
 */
class FooDBModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = MODULE_NAME

    // ── Service initialisation ──────────────────────────────────────────

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val gson = Gson()

    @Suppress("UNCHECKED_CAST")
    private fun <T> toMap(obj: T): Map<String, Any?> =
        gson.fromJson(gson.toJson(obj), Map::class.java) as Map<String, Any?>

    private val foodbService: FooDBDataAPIService by lazy {
        val db = AppDatabase.getInstance(reactApplicationContext)
        val localDb = FooDBLocalDatabase(
            foodItemDao = db.foodItemDao(),
            nutrientPropertyDao = db.nutrientPropertyDao()
        )
        FooDBDataAPIService(localDatabase = localDb)
    }

    // ════════════════════════════════════════════════════════════════════
    // Public API — exposed to React Native via @ReactMethod
    // ════════════════════════════════════════════════════════════════════

    /**
     * Searches the local/remote food catalog by name.
     *
     * @param query   Free-text search string (e.g., "apple", "rice").
     * @param promise Resolves with a JSON array of matching food items.
     */
    @ReactMethod
    fun searchFood(query: String, promise: Promise) {
        scope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    foodbService.searchFood(query)
                }
                val json = gson.toJson(results)
                val array = Arguments.createArray()
                // Parse JSON string into WritableArray for RN consumption
                val listType = object : TypeToken<List<Map<String, Any?>>>() {}.type
                val maps: List<Map<String, Any?>> = gson.fromJson(json, listType)
                for (item in maps) {
                    array.pushMap(mapToWritable(item))
                }
                promise.resolve(array)
            } catch (e: Exception) {
                promise.reject("FOODB_SEARCH_ERROR", e.message, e)
            }
        }
    }

    /**
     * Retrieves full food details with nutrient properties (cache-first).
     *
     * @param foodId  The FooDB food identifier.
     * @param promise Resolves with { food: FoodItem, nutrients: [...] }.
     */
    @ReactMethod
    fun getFoodDetails(foodId: Double, promise: Promise) {
        scope.launch {
            try {
                val id = foodId.toLong()
                val detail = withContext(Dispatchers.IO) {
                    foodbService.getFoodDetails(id)
                }
                if (detail != null) {
                    val result = Arguments.createMap()
                    result.putMap("food", mapToWritable(toMap(detail.food)))
                    val nutrientsArray = Arguments.createArray()
                    val listType = object : TypeToken<List<Map<String, Any?>>>() {}.type
                    val nutrientMaps: List<Map<String, Any?>> = gson.fromJson(
                        gson.toJson(detail.nutrients), listType
                    )
                    for (np in nutrientMaps) {
                        nutrientsArray.pushMap(mapToWritable(np))
                    }
                    result.putArray("nutrients", nutrientsArray)
                    promise.resolve(result)
                } else {
                    promise.reject("FOODB_NOT_FOUND", "Food item not found: $foodId")
                }
            } catch (e: Exception) {
                promise.reject("FOODB_DETAIL_ERROR", e.message, e)
            }
        }
    }

    /**
     * Returns all distinct food groups available in the local catalog.
     */
    @ReactMethod
    fun getFoodGroups(promise: Promise) {
        scope.launch {
            try {
                val groups = withContext(Dispatchers.IO) {
                    foodbService.getFoodGroups()
                }
                val array = Arguments.createArray()
                for (group in groups) {
                    array.pushString(group)
                }
                promise.resolve(array)
            } catch (e: Exception) {
                promise.reject("FOODB_GROUPS_ERROR", e.message, e)
            }
        }
    }

    /**
     * Returns all distinct nutrient names available in the local database.
     */
    @ReactMethod
    fun getNutrientNames(promise: Promise) {
        scope.launch {
            try {
                val names = withContext(Dispatchers.IO) {
                    foodbService.getNutrientNames()
                }
                val array = Arguments.createArray()
                for (name in names) {
                    array.pushString(name)
                }
                promise.resolve(array)
            } catch (e: Exception) {
                promise.reject("FOODB_NUTRIENT_NAMES_ERROR", e.message, e)
            }
        }
    }

    // ── Helper ──────────────────────────────────────────────────────────

    /**
     * Converts a Kotlin Map<String, Any?> to a React Native WritableMap.
     */
    private fun mapToWritable(map: Map<String, Any?>): WritableMap {
        val writable = Arguments.createMap()
        for ((key, value) in map) {
            when (value) {
                null -> writable.putNull(key)
                is Boolean -> writable.putBoolean(key, value)
                is Int -> writable.putInt(key, value)
                is Long -> writable.putDouble(key, value.toDouble())
                is Double -> writable.putDouble(key, value)
                is Float -> writable.putDouble(key, value.toDouble())
                is String -> writable.putString(key, value)
                is List<*> -> {
                    val array = Arguments.createArray()
                    for (item in value) {
                        when (item) {
                            is String -> array.pushString(item)
                            is Number -> array.pushDouble(item.toDouble())
                            is Boolean -> array.pushBoolean(item)
                            is Map<*, *> -> {
                                @Suppress("UNCHECKED_CAST")
                                array.pushMap(mapToWritable(item as Map<String, Any?>))
                            }
                            else -> array.pushString(item?.toString())
                        }
                    }
                    writable.putArray(key, array)
                }
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    writable.putMap(key, mapToWritable(value as Map<String, Any?>))
                }
                else -> writable.putString(key, value.toString())
            }
        }
        return writable
    }

    companion object {
        const val MODULE_NAME = "FooDBModule"
    }
}
