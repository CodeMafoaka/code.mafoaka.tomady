package com.tomady.nutrition.bridge;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

/**
 * React Native Native Module for querying the local/full FooDB database.
 */
public class FooDBModule extends ReactContextBaseJavaModule {

    public FooDBModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "FooDBModule";
    }

    /**
     * React Native method stub to fetch compounds in a food item.
     */
    @ReactMethod
    public void getCompoundsByFoodId(int foodId, Promise promise) {
        promise.resolve("[]");
    }

    /**
     * React Native method stub to fetch specific nutrient attributes.
     */
    @ReactMethod
    public void getNutrientDetails(String publicId, Promise promise) {
        promise.resolve("");
    }
}
