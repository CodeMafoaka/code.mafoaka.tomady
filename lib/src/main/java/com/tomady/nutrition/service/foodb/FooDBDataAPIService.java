package com.tomady.nutrition.service.foodb;

import com.tomady.nutrition.data.local.foodb.FooDBLocalDatabase;
import com.tomady.nutrition.data.local.diet.FoodItem;
import com.tomady.nutrition.data.local.diet.NutrientProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service interacting with local and full FooDB datasets.
 * Features a cache-first lookup strategy utilizing FooDBLocalDatabase.
 */
public class FooDBDataAPIService {
    private final FooDBLocalDatabase database;

    /**
     * Public static inner class representing combined food details (metadata + nutrients).
     */
    public static class FoodDetails {
        private final FoodItem foodItem;
        private final List<NutrientProperty> nutrientProperties;

        public FoodDetails(FoodItem foodItem, List<NutrientProperty> nutrientProperties) {
            this.foodItem = foodItem;
            this.nutrientProperties = nutrientProperties;
        }

        public FoodItem getFoodItem() {
            return foodItem;
        }

        public List<NutrientProperty> getNutrientProperties() {
            return nutrientProperties;
        }
    }

    public FooDBDataAPIService() {
        this.database = null;
    }

    public FooDBDataAPIService(FooDBLocalDatabase database) {
        this.database = database;
    }

    /**
     * Cache-first logic to get food details.
     * Checks FooDBLocalDatabase first. If present, returns it immediately.
     * If missing, fetches from the remote FooDB API, caches into local DB, and returns the details.
     *
     * @param foodId The ID of the food item.
     * @return FoodDetails representing the food and its nutrient properties.
     * @throws IOException If a simulated remote API network failure occurs.
     */
    public FoodDetails getFoodDetails(long foodId) throws IOException {
        if (database != null) {
            FoodItem cachedFood = database.fooDBLocalDao().getFoodItemById((int) foodId);
            if (cachedFood != null) {
                List<NutrientProperty> cachedNutrients = database.fooDBLocalDao().getNutrientPropertiesByFoodId((int) foodId);
                return new FoodDetails(cachedFood, cachedNutrients);
            }
        }

        // Cache miss: Fetch from remote FooDB API
        FoodDetails remoteDetails = fetchFromRemoteAPI(foodId);

        // Cache the fetched results in the local database
        if (database != null && remoteDetails != null) {
            database.fooDBLocalDao().insertFoodItem(remoteDetails.getFoodItem());
            database.fooDBLocalDao().insertNutrientProperties(remoteDetails.getNutrientProperties());
        }

        return remoteDetails;
    }

    /**
     * Simulates fetching a food item and its nutrients from a remote FooDB API.
     *
     * @param foodId The ID of the food item to fetch.
     * @return Simulated FoodDetails.
     * @throws IOException If foodId is 999 (to simulate a network failure).
     */
    public FoodDetails fetchFromRemoteAPI(long foodId) throws IOException {
        if (foodId == 999L) {
            throw new IOException("Remote connection timed out (Simulated Network Failure)");
        }

        FoodItem remoteFood = new FoodItem();
        remoteFood.setId((int) foodId);
        remoteFood.setName("Remote Food #" + foodId);
        remoteFood.setGroupName("Remote Group");

        List<NutrientProperty> properties = new ArrayList<>();

        NutrientProperty p1 = new NutrientProperty();
        p1.setFoodItemId((int) foodId);
        p1.setPropertyName("Water");
        p1.setPropertyValue(92.1);
        properties.add(p1);

        NutrientProperty p2 = new NutrientProperty();
        p2.setFoodItemId((int) foodId);
        p2.setPropertyName("Protein");
        p2.setPropertyValue(1.2);
        properties.add(p2);

        return new FoodDetails(remoteFood, properties);
    }

    /**
     * Searches for food items matching a query in the local database.
     *
     * @param query Search query.
     * @return List of FoodItem objects matching the query.
     */
    public List<FoodItem> searchFood(String query) {
        if (database == null) {
            return Collections.emptyList();
        }
        return database.fooDBLocalDao().searchLocalFoods("%" + query + "%");
    }

    /**
     * Looks up compounds in a food item.
     * @param foodId The ID of the food item.
     * @return List of compound public IDs.
     */
    public List<String> getCompoundsByFoodId(int foodId) {
        return Collections.emptyList();
    }

    /**
     * Fetches detailed information for a specific nutrient.
     * @param publicId Nutrient public identifier.
     * @return Description or JSON properties of the nutrient.
     */
    public String getNutrientDetails(String publicId) {
        return "";
    }
}
