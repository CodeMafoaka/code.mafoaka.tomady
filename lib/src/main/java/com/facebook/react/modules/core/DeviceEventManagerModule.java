package com.facebook.react.modules.core;

public class DeviceEventManagerModule {
    public interface RCTDeviceEventEmitter {
        void emit(String eventName, Object data);
    }
}
