package com.facebook.react.bridge;

public abstract class ReactContextBaseJavaModule implements NativeModule {
    public ReactContextBaseJavaModule(ReactApplicationContext reactContext) {
    }

    @Override
    public abstract String getName();
}
