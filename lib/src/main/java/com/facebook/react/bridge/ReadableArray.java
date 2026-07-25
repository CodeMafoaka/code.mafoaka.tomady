package com.facebook.react.bridge;

public interface ReadableArray {
    int size();
    boolean isNull(int index);
    boolean getBoolean(int index);
    double getDouble(int index);
    int getInt(int index);
    String getString(int index);
    ReadableMap getMap(int index);
    ReadableArray getArray(int index);
}
