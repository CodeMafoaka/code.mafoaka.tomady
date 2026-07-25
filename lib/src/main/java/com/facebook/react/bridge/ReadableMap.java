package com.facebook.react.bridge;

public interface ReadableMap {
    boolean hasKey(String name);
    boolean isNull(String name);
    boolean getBoolean(String name);
    double getDouble(String name);
    int getInt(String name);
    String getString(String name);
    ReadableMap getMap(String name);
    ReadableArray getArray(String name);
}
