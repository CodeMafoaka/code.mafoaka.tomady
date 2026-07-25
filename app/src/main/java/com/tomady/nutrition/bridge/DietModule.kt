package com.tomady.nutrition.bridge

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

/**
 * React Native bridge module exposing diet-planning capabilities to the RN host.
 *
 * Provides methods for daily targets, meal plans, meal logging, and history.
 */
class DietModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "TomadyDiet"

    @ReactMethod
    fun getDailyPlan(userId: String, promise: Promise) {
        // TODO: Implement daily plan retrieval
        promise.resolve(null)
    }

    @ReactMethod
    fun logMeal(userId: String, dishId: String, date: String, mealType: String, servings: Double, promise: Promise) {
        // TODO: Implement meal logging
        promise.resolve(null)
    }

    @ReactMethod
    fun getTodaySummary(userId: String, promise: Promise) {
        // TODO: Implement today's summary
        promise.resolve(null)
    }

    @ReactMethod
    fun getHistory(userId: String, startDate: String, endDate: String, promise: Promise) {
        // TODO: Implement history retrieval
        promise.resolve(null)
    }
}
