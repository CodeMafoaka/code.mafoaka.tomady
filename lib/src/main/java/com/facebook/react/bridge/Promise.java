package com.facebook.react.bridge;

public interface Promise {
    void resolve(Object value);
    void reject(String code, String message);
    void reject(Throwable throwable);
}
