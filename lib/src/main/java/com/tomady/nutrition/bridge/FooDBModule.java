package com.tomady.nutrition.bridge;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.tomady.nutrition.service.foodb.FooDBDataAPIService;
import com.tomady.nutrition.data.local.diet.FoodItem;
import com.tomady.nutrition.data.local.diet.NutrientProperty;

/**
 * React Native Native Module for querying the local/full FooDB database.
 */
public class FooDBModule extends ReactContextBaseJavaModule {
    private final FooDBDataAPIService service;

    public FooDBModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.service = new FooDBDataAPIService();
    }

    public FooDBModule(ReactApplicationContext reactContext, FooDBDataAPIService service) {
        super(reactContext);
        this.service = service;
    }

    @Override
    public String getName() {
        return "FooDBModule";
    }

    /**
     * React Native method to search for food items in the local cache.
     * Resolves with a WritableArray of food item maps.
     *
     * @param query Search keywords.
     * @param promise React Native Promise.
     */
    @ReactMethod
    public void searchFood(String query, Promise promise) {
        try {
            WritableArray array = Arguments.createArray();
            for (FoodItem item : service.searchFood(query)) {
                WritableMap map = Arguments.createMap();
                map.putInt("id", item.getId());
                map.putString("name", item.getName());
                map.putString("groupName", item.getGroupName());
                array.pushMap(map);
            }
            promise.resolve(array);
        } catch (Exception e) {
            promise.reject("SEARCH_FAILED", e.getMessage());
        }
    }

    /**
     * React Native method to fetch food details with cache-first strategy.
     * If data is missing locally, triggers simulated remote API fetch.
     * Resolves with a WritableMap containing metadata and nutrient properties.
     * Rejects with error code on simulated network failures.
     *
     * @param foodIdDouble Food item ID passed as Double from Javascript.
     * @param promise React Native Promise.
     */
    @ReactMethod
    public void getFoodDetails(double foodIdDouble, Promise promise) {
        long foodId = (long) foodIdDouble;
        try {
            FooDBDataAPIService.FoodDetails details = service.getFoodDetails(foodId);
            if (details == null) {
                promise.reject("NOT_FOUND", "Food item not found: " + foodId);
                return;
            }

            WritableMap result = Arguments.createMap();

            WritableMap foodMap = Arguments.createMap();
            foodMap.putInt("id", details.getFoodItem().getId());
            foodMap.putString("name", details.getFoodItem().getName());
            foodMap.putString("groupName", details.getFoodItem().getGroupName());
            result.putMap("foodItem", foodMap);

            WritableArray nutrientsArray = Arguments.createArray();
            for (NutrientProperty prop : details.getNutrientProperties()) {
                WritableMap nutrientMap = Arguments.createMap();
                nutrientMap.putInt("id", prop.getId());
                nutrientMap.putInt("foodItemId", prop.getFoodItemId());
                nutrientMap.putString("propertyName", prop.getPropertyName());
                nutrientMap.putDouble("propertyValue", prop.getPropertyValue());
                nutrientsArray.pushMap(nutrientMap);
            }
            result.putArray("nutrientProperties", nutrientsArray);

            promise.resolve(result);
        } catch (Exception e) {
            promise.reject("NETWORK_FAILURE", e.getMessage());
        }
    }

    /**
     * React Native method stub to fetch compounds in a food item.
     */
    @ReactMethod
    public void getCompoundsByFoodId(int foodId, Promise promise) {
        promise.resolve("[]");
    }

    /**
     * React Native method stub to fetch specific nutrient attributes.
     */
    @ReactMethod
    public void getNutrientDetails(String publicId, Promise promise) {
        promise.resolve("");
    }
}
