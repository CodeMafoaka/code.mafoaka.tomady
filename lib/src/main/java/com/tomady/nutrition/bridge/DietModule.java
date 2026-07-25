package com.tomady.nutrition.bridge;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

/**
 * React Native Native Module for managing user diet patterns and targets.
 */
public class DietModule extends ReactContextBaseJavaModule {

    public DietModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "DietModule";
    }

    /**
     * React Native method stub to synchronize user diet records.
     */
    @ReactMethod
    public void syncDietData(int userId, Promise promise) {
        promise.resolve(false);
    }

    /**
     * React Native method stub to retrieve suggestion Targets.
     */
    @ReactMethod
    public void calculateDailyTargets(int profileId, Promise promise) {
        promise.resolve("{}");
    }
}
