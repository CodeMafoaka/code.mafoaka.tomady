package com.tomady.nutrition.bridge

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

/**
 * React Native bridge module exposing FooDB food composition data to the RN host.
 *
 * Provides methods for food search and nutrient lookup against the local FooDB database.
 */
class FooDBModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "TomadyFooDB"

    @ReactMethod
    fun searchFood(query: String, promise: Promise) {
        // TODO: Implement food search
        promise.resolve(null)
    }

    @ReactMethod
    fun getNutrients(foodId: Double, promise: Promise) {
        // TODO: Implement nutrient lookup
        promise.resolve(null)
    }

    @ReactMethod
    fun getFoodGroups(promise: Promise) {
        // TODO: Implement food groups retrieval
        promise.resolve(null)
    }

    @ReactMethod
    fun getNutrientNames(promise: Promise) {
        // TODO: Implement nutrient names retrieval
        promise.resolve(null)
    }
}
