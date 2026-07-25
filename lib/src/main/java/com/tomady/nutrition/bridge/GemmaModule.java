package com.tomady.nutrition.bridge;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

/**
 * React Native Native Module for interacting with local Gemma AI features.
 */
public class GemmaModule extends ReactContextBaseJavaModule {

    public GemmaModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "GemmaModule";
    }

    /**
     * React Native method stub to initialize local model.
     */
    @ReactMethod
    public void initializeModel(Promise promise) {
        promise.resolve(false);
    }

    /**
     * React Native method stub to request prompt completion.
     */
    @ReactMethod
    public void generateSuggestion(String prompt, Promise promise) {
        promise.resolve("");
    }
}
