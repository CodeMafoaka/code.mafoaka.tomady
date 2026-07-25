package com.facebook.react.bridge;

public class ReactApplicationContext {
    private Object mockEmitter = null;

    @SuppressWarnings("unchecked")
    public <T> T getJSModule(Class<T> jsInterfaceClass) {
        if (mockEmitter == null) {
            try {
                if (jsInterfaceClass.getSimpleName().equals("RCTDeviceEventEmitter")) {
                    mockEmitter = java.lang.reflect.Proxy.newProxyInstance(
                        jsInterfaceClass.getClassLoader(),
                        new Class<?>[]{jsInterfaceClass},
                        new java.lang.reflect.InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                                // No-op for mock logging
                                return null;
                            }
                        }
                    );
                }
            } catch (Exception e) {
                // Ignored
            }
        }
        return (T) mockEmitter;
    }
}
