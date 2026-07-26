package com.tomady.nutrition.bridge

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

/**
 * React Native [ReactPackage] that registers all Tomady backend bridge
 * modules for the React Native host application.
 *
 * ## Integration
 *
 * In your React Native host app's `MainApplication.java` or equivalent,
 * add this package to `getPackages()`:
 *
 * ```kotlin
 * // MainApplication.kt
 * override fun getPackages(): List<ReactPackage> = PackageList(this).apply {
 *     add(TomadyBridgePackage())
 * }
 * ```
 *
 * Or in `MainApplication.java`:
 * ```java
 * @Override
 * protected List<ReactPackage> getPackages() {
 *     return new PackageList(this).add(new TomadyBridgePackage());
 * }
 * ```
 *
 * ## Registered Modules
 * | JS module name | Wraps | Description |
 * |---|---|---|
 * | `FooDBModule` | [FooDBDataAPIService] | Food catalogue search, nutrient lookup |
 * | `DietModule` | [DietAPIService] | User/profile/meal CRUD, nutrition, validation |
 * | `GemmaModule` | [GemmaAndroidService] | On-device LLM inference, recipe, Q&A, streaming |
 *
 * ## JS Usage
 * ```js
 * import { NativeModules } from 'react-native';
 *
 * const { FooDBModule, DietModule, GemmaModule } = NativeModules;
 *
 * // Search foods
 * const foods = await FooDBModule.searchFood('banane');
 *
 * // Log a meal
 * await DietModule.logMeal('user-1', 'dish-1', '2026-07-26', 'dejeuner');
 *
 * // Ask the AI assistant
 * const answer = await GemmaModule.askQuestion('Puis-je manger du riz?', 'user-1');
 * ```
 */
class TomadyBridgePackage : ReactPackage {

    override fun createNativeModules(
        reactContext: ReactApplicationContext
    ): List<NativeModule> {
        return listOf(
            // FooDBModule — Food catalogue with cache-first search
            FooDBModule(reactContext),

            // DietModule — User, profile, bio, meal logging, nutrition
            DietModule(reactContext),

            // GemmaModule — On-device LLM (MediaPipe Gemma) with recipe/Q&A
            GemmaModule(reactContext)
        )
    }

    override fun createViewManagers(
        reactContext: ReactApplicationContext
    ): List<ViewManager<*, *>> {
        // Tomady is a headless service — no custom UI views
        return emptyList()
    }
}
