package com.facebook.react.bridge;

public interface WritableMap extends ReadableMap {
    void putNull(String key);
    void putBoolean(String key, boolean value);
    void putDouble(String key, double value);
    void putInt(String key, int value);
    void putString(String key, String value);
    void putMap(String key, ReadableMap value);
    void putArray(String key, ReadableArray value);
}
