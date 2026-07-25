package com.facebook.react.bridge;

public interface WritableArray extends ReadableArray {
    void pushNull();
    void pushBoolean(boolean value);
    void pushDouble(double value);
    void pushInt(int value);
    void pushString(String value);
    void pushMap(ReadableMap value);
    void pushArray(ReadableArray value);
}
