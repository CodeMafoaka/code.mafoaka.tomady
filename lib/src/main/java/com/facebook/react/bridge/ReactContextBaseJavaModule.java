package com.facebook.react.bridge;

public abstract class ReactContextBaseJavaModule implements NativeModule {
    private final ReactApplicationContext reactContext;

    public ReactContextBaseJavaModule(ReactApplicationContext reactContext) {
        this.reactContext = reactContext;
    }

    public ReactApplicationContext getReactApplicationContext() {
        return reactContext;
    }

    @Override
    public abstract String getName();
}
