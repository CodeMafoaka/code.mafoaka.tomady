package com.tomady.nutrition.bridge

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap

/**
 * Shared utility functions for React Native bridge modules.
 *
 * Provides type conversion helpers to translate Kotlin data structures
 * (maps, lists, primitives, nulls) into React Native-friendly
 * [WritableMap] and [WritableArray] instances.
 */
object BridgeUtils {

    /**
     * Converts a Kotlin [Map<String, Any?>] to a React Native [WritableMap].
     *
     * Handles all common types:
     * - null → `putNull`
     * - Boolean → `putBoolean`
     * - Int/Long/Double/Float → `putDouble` (RN numeric type)
     * - String → `putString`
     * - List → `WritableArray` (with recursive conversion of nested Maps)
     * - Map → `WritableMap` (recursive)
     * - Any other type → `toString()` as String
     *
     * @param map The Kotlin map to convert.
     * @return A [WritableMap] suitable for passing to [Promise.resolve].
     */
    fun mapToWritable(map: Map<String, Any?>): WritableMap {
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
                is List<*> -> writable.putArray(key, listToWritable(value))
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    writable.putMap(key, mapToWritable(value as Map<String, Any?>))
                }
                else -> writable.putString(key, value.toString())
            }
        }
        return writable
    }

    /**
     * Converts a Kotlin [List<Any?>] to a React Native [WritableArray].
     *
     * @param list The Kotlin list to convert.
     * @return A [WritableArray] suitable for [WritableMap.putArray].
     */
    fun listToWritable(list: List<*>): WritableArray {
        val array = Arguments.createArray()
        for (item in list) {
            when (item) {
                null -> array.pushNull()
                is String -> array.pushString(item)
                is Boolean -> array.pushBoolean(item)
                is Int -> array.pushInt(item)
                is Long -> array.pushDouble(item.toDouble())
                is Double -> array.pushDouble(item)
                is Float -> array.pushDouble(item.toDouble())
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    array.pushMap(mapToWritable(item as Map<String, Any?>))
                }
                is List<*> -> array.pushArray(listToWritable(item))
                else -> array.pushString(item.toString())
            }
        }
        return array
    }
}
