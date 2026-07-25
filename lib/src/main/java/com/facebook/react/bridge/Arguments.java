package com.facebook.react.bridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Arguments {

    public static WritableMap createMap() {
        return new MockWritableMap();
    }

    public static WritableArray createArray() {
        return new MockWritableArray();
    }

    private static class MockWritableMap implements WritableMap {
        private final Map<String, Object> map = new HashMap<>();

        @Override
        public boolean hasKey(String name) {
            return map.containsKey(name);
        }

        @Override
        public boolean isNull(String name) {
            return map.get(name) == null;
        }

        @Override
        public boolean getBoolean(String name) {
            Object val = map.get(name);
            return val instanceof Boolean ? (Boolean) val : false;
        }

        @Override
        public double getDouble(String name) {
            Object val = map.get(name);
            return val instanceof Number ? ((Number) val).doubleValue() : 0.0;
        }

        @Override
        public int getInt(String name) {
            Object val = map.get(name);
            return val instanceof Number ? ((Number) val).intValue() : 0;
        }

        @Override
        public String getString(String name) {
            Object val = map.get(name);
            return val != null ? val.toString() : null;
        }

        @Override
        public ReadableMap getMap(String name) {
            Object val = map.get(name);
            return val instanceof ReadableMap ? (ReadableMap) val : null;
        }

        @Override
        public ReadableArray getArray(String name) {
            Object val = map.get(name);
            return val instanceof ReadableArray ? (ReadableArray) val : null;
        }

        @Override
        public void putNull(String key) {
            map.put(key, null);
        }

        @Override
        public void putBoolean(String key, boolean value) {
            map.put(key, value);
        }

        @Override
        public void putDouble(String key, double value) {
            map.put(key, value);
        }

        @Override
        public void putInt(String key, int value) {
            map.put(key, value);
        }

        @Override
        public void putString(String key, String value) {
            map.put(key, value);
        }

        @Override
        public void putMap(String key, ReadableMap value) {
            map.put(key, value);
        }

        @Override
        public void putArray(String key, ReadableArray value) {
            map.put(key, value);
        }

        @Override
        public String toString() {
            return map.toString();
        }
    }

    private static class MockWritableArray implements WritableArray {
        private final List<Object> list = new ArrayList<>();

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public boolean isNull(int index) {
            return list.get(index) == null;
        }

        @Override
        public boolean getBoolean(int index) {
            Object val = list.get(index);
            return val instanceof Boolean ? (Boolean) val : false;
        }

        @Override
        public double getDouble(int index) {
            Object val = list.get(index);
            return val instanceof Number ? ((Number) val).doubleValue() : 0.0;
        }

        @Override
        public int getInt(int index) {
            Object val = list.get(index);
            return val instanceof Number ? ((Number) val).intValue() : 0;
        }

        @Override
        public String getString(int index) {
            Object val = list.get(index);
            return val != null ? val.toString() : null;
        }

        @Override
        public ReadableMap getMap(int index) {
            Object val = list.get(index);
            return val instanceof ReadableMap ? (ReadableMap) val : null;
        }

        @Override
        public ReadableArray getArray(int index) {
            Object val = list.get(index);
            return val instanceof ReadableArray ? (ReadableArray) val : null;
        }

        @Override
        public void pushNull() {
            list.add(null);
        }

        @Override
        public void pushBoolean(boolean value) {
            list.add(value);
        }

        @Override
        public void pushDouble(double value) {
            list.add(value);
        }

        @Override
        public void pushInt(int value) {
            list.add(value);
        }

        @Override
        public void pushString(String value) {
            list.add(value);
        }

        @Override
        public void pushMap(ReadableMap value) {
            list.add(value);
        }

        @Override
        public void pushArray(ReadableArray value) {
            list.add(value);
        }

        @Override
        public String toString() {
            return list.toString();
        }
    }
}
