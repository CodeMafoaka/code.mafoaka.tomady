package com.tomady.nutrition.bridge;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.tomady.nutrition.service.gemma.GemmaAndroidService;
import com.tomady.nutrition.service.gemma.GemmaAndroidService.RecipeResponse;

/**
 * React Native Native Module for interacting with local Gemma AI features.
 */
public class GemmaModule extends ReactContextBaseJavaModule {
    private final GemmaAndroidService service;

    public GemmaModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.service = new GemmaAndroidService();
    }

    public GemmaModule(ReactApplicationContext reactContext, GemmaAndroidService service) {
        super(reactContext);
        this.service = service;
    }

    @Override
    public String getName() {
        return "GemmaModule";
    }

    /**
     * Initializes the local Gemma model.
     * @param promise Promise resolved with true if model loaded successfully.
     */
    @ReactMethod
    public void initializeModel(Promise promise) {
        try {
            boolean status = service.initializeModel();
            promise.resolve(status);
        } catch (Exception e) {
            promise.reject("INIT_FAILED", e.getMessage());
        }
    }

    /**
     * Unloads local model resources.
     * @param promise Promise resolved when complete.
     */
    @ReactMethod
    public void unloadModel(Promise promise) {
        try {
            service.unloadModel();
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("UNLOAD_FAILED", e.getMessage());
        }
    }

    /**
     * React Native method to ask a question with a single text response.
     *
     * @param question The prompt or question.
     * @param promise React Native Promise.
     */
    @ReactMethod
    public void askQuestion(String question, Promise promise) {
        try {
            String answer = service.askQuestion(question);
            promise.resolve(answer);
        } catch (Exception e) {
            promise.reject("ASK_FAILED", e.getMessage());
        }
    }

    /**
     * React Native method to stream response tokens for a question.
     * Emits token streams via DeviceEventEmitter onTokenStream events.
     *
     * @param question The question to ask.
     */
    @ReactMethod
    public void askQuestionStreaming(final String question) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                service.askQuestionStreaming(question, new GemmaAndroidService.TokenStreamListener() {
                    @Override
                    public void onToken(String token) {
                        ReactApplicationContext context = getReactApplicationContext();
                        if (context != null) {
                            DeviceEventManagerModule.RCTDeviceEventEmitter emitter =
                                context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
                            if (emitter != null) {
                                emitter.emit("onTokenStream", token);
                            }
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * React Native method to trigger the Recipe Computation and Validation pipeline.
     * Checks generated dish profiles against active profile restrictions.
     *
     * @param userRequest Prompt context describing user request.
     * @param profileIdDouble Profile ID as Double from JS.
     * @param promise React Native Promise.
     */
    @ReactMethod
    public void computeRecipe(String userRequest, double profileIdDouble, Promise promise) {
        try {
            RecipeResponse resp = service.computeRecipe(userRequest, (int) profileIdDouble);
            WritableMap map = Arguments.createMap();
            map.putString("recipeTitle", resp.getRecipeTitle());
            map.putBoolean("isSafe", resp.isSafe());

            WritableArray warningsArray = Arguments.createArray();
            for (String warning : resp.getWarnings()) {
                warningsArray.pushString(warning);
            }
            map.putArray("warnings", warningsArray);

            promise.resolve(map);
        } catch (Exception e) {
            promise.reject("COMPUTATION_FAILED", e.getMessage());
        }
    }

    /**
     * React Native method stub to request prompt completion (Backward Compatibility).
     */
    @ReactMethod
    public void generateSuggestion(String prompt, Promise promise) {
        try {
            String suggestion = service.generateSuggestion(prompt);
            promise.resolve(suggestion);
        } catch (Exception e) {
            promise.reject("SUGGESTION_FAILED", e.getMessage());
        }
    }
}
