package com.tomady.nutrition.service.foodb;

import java.util.List;

/**
 * Service stub for interacting with local and full FooDB datasets.
 * Allows querying foods, compounds, and nutritional properties from SQLite datasets.
 */
public class FooDBDataAPIService {
    public FooDBDataAPIService() {}

    /**
     * Looks up compounds in a food item.
     * @param foodId The ID of the food item.
     * @return List of compound public IDs.
     */
    public List<String> getCompoundsByFoodId(int foodId) {
        return java.util.Collections.emptyList();
    }

    /**
     * Fetches detailed information for a specific nutrient.
     * @param publicId Nutrient public identifier.
     * @return Description or JSON properties of the nutrient.
     */
    public String getNutrientDetails(String publicId) {
        return "";
    }

    /**
     * Searches for food items matching a name prefix or category.
     * @param query Search keywords.
     * @return List of food names.
     */
    public List<String> searchFood(String query) {
        return java.util.Collections.emptyList();
    }
}
