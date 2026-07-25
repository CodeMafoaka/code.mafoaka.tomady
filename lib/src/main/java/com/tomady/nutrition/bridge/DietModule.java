package com.tomady.nutrition.bridge;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.tomady.nutrition.service.diet.DietAPIService;
import com.tomady.nutrition.data.local.diet.Profile;
import com.tomady.nutrition.data.local.diet.DishHistory;
import java.util.List;

/**
 * React Native Native Module for managing user diet patterns and targets.
 */
public class DietModule extends ReactContextBaseJavaModule {
    private final DietAPIService service;

    public DietModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.service = new DietAPIService();
    }

    public DietModule(ReactApplicationContext reactContext, DietAPIService service) {
        super(reactContext);
        this.service = service;
    }

    @Override
    public String getName() {
        return "DietModule";
    }

    /**
     * React Native method to retrieve a user profile.
     * Resolves with a WritableMap representing the profile.
     *
     * @param userIdDouble User identifier as Double from JS.
     * @param promise React Native Promise.
     */
    @ReactMethod
    public void getProfile(double userIdDouble, Promise promise) {
        try {
            Profile profile = service.getProfileByUserId((int) userIdDouble);
            if (profile == null) {
                promise.reject("PROFILE_NOT_FOUND", "No profile found for userId: " + (int) userIdDouble);
                return;
            }

            WritableMap map = Arguments.createMap();
            map.putInt("id", profile.getId());
            map.putInt("userId", profile.getUserId());
            map.putInt("age", profile.getAge());
            map.putDouble("height", profile.getHeight());
            map.putDouble("weight", profile.getWeight());
            map.putString("allergies", profile.getAllergies());
            map.putString("diseases", profile.getDiseases());
            promise.resolve(map);
        } catch (Exception e) {
            promise.reject("FETCH_PROFILE_FAILED", e.getMessage());
        }
    }

    /**
     * React Native method to create or update a user profile.
     * Resolves with a WritableMap representing the saved profile.
     *
     * @param profileMap ReadableMap passed from JS.
     * @param promise React Native Promise.
     */
    @ReactMethod
    public void updateProfile(ReadableMap profileMap, Promise promise) {
        try {
            int id = profileMap.hasKey("id") ? profileMap.getInt("id") : 0;
            int userId = profileMap.hasKey("userId") ? profileMap.getInt("userId") : 0;
            int age = profileMap.hasKey("age") ? profileMap.getInt("age") : 0;
            double height = profileMap.hasKey("height") ? profileMap.getDouble("height") : 0.0;
            double weight = profileMap.hasKey("weight") ? profileMap.getDouble("weight") : 0.0;
            String allergies = profileMap.hasKey("allergies") ? profileMap.getString("allergies") : "";
            String diseases = profileMap.hasKey("diseases") ? profileMap.getString("diseases") : "";

            Profile p = null;
            if (id > 0) {
                p = service.getProfileById(id);
            }
            if (p == null) {
                p = new Profile();
                if (id > 0) p.setId(id);
            }
            p.setUserId(userId);
            p.setAge(age);
            p.setHeight(height);
            p.setWeight(weight);
            p.setAllergies(allergies);
            p.setDiseases(diseases);

            if (p.getId() > 0 && service.getProfileById(p.getId()) != null) {
                service.updateProfile(p);
            } else {
                service.insertProfile(p);
            }

            WritableMap map = Arguments.createMap();
            map.putInt("id", p.getId());
            map.putInt("userId", p.getUserId());
            map.putInt("age", p.getAge());
            map.putDouble("height", p.getHeight());
            map.putDouble("weight", p.getWeight());
            map.putString("allergies", p.getAllergies());
            map.putString("diseases", p.getDiseases());
            promise.resolve(map);
        } catch (Exception e) {
            promise.reject("UPDATE_PROFILE_FAILED", e.getMessage());
        }
    }

    /**
     * React Native method to retrieve a user's logged dish history.
     * Resolves with a WritableArray of history maps.
     *
     * @param userIdDouble User identifier as Double from JS.
     * @param promise React Native Promise.
     */
    @ReactMethod
    public void getDishHistory(double userIdDouble, Promise promise) {
        try {
            List<DishHistory> historyList = service.getDishHistoryByUserId((int) userIdDouble);
            WritableArray array = Arguments.createArray();
            for (DishHistory h : historyList) {
                WritableMap hMap = Arguments.createMap();
                hMap.putInt("id", h.getId());
                hMap.putInt("userId", h.getUserId());
                hMap.putInt("dishId", h.getDishId());
                hMap.putString("consumedAt", h.getConsumedAt());
                array.pushMap(hMap);
            }
            promise.resolve(array);
        } catch (Exception e) {
            promise.reject("FETCH_HISTORY_FAILED", e.getMessage());
        }
    }

    /**
     * React Native method to log consumption of a dish.
     * Resolves with a WritableMap representing the logged entry.
     *
     * @param userIdDouble User identifier.
     * @param dishIdDouble Dish identifier.
     * @param consumedAt ISO Timestamp of consumption.
     * @param promise React Native Promise.
     */
    @ReactMethod
    public void logDishConsumption(double userIdDouble, double dishIdDouble, String consumedAt, Promise promise) {
        try {
            DishHistory h = new DishHistory();
            h.setUserId((int) userIdDouble);
            h.setDishId((int) dishIdDouble);
            h.setConsumedAt(consumedAt);
            service.logDishConsumption(h);

            WritableMap map = Arguments.createMap();
            map.putInt("id", h.getId());
            map.putInt("userId", h.getUserId());
            map.putInt("dishId", h.getDishId());
            map.putString("consumedAt", h.getConsumedAt());
            promise.resolve(map);
        } catch (Exception e) {
            promise.reject("LOG_DISH_FAILED", e.getMessage());
        }
    }

    /**
     * React Native method stub to synchronize user diet records.
     */
    @ReactMethod
    public void syncDietData(int userId, Promise promise) {
        promise.resolve(service.syncDietData(userId));
    }

    /**
     * React Native method stub to retrieve suggestion Targets.
     */
    @ReactMethod
    public void calculateDailyTargets(int profileId, Promise promise) {
        promise.resolve(service.calculateDailyTargets(profileId));
    }
}
