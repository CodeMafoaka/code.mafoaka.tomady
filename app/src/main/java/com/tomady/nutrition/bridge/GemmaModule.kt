package com.tomady.nutrition.bridge

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

/**
 * React Native bridge module exposing Gemma AI/ML capabilities to the RN host.
 *
 * Provides methods for meal analysis and meal suggestion via on-device Gemma inference.
 */
class GemmaModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "TomadyGemma"

    @ReactMethod
    fun analyzeMeal(mealDescription: String, promise: Promise) {
        // TODO: Implement Gemma meal analysis
        promise.resolve(null)
    }

    @ReactMethod
    fun suggestMeal(userProfileJson: String, promise: Promise) {
        // TODO: Implement Gemma meal suggestion
        promise.resolve(null)
    }

    @ReactMethod
    fun isModelLoaded(promise: Promise) {
        // TODO: Check model loading status
        promise.resolve(false)
    }
}
