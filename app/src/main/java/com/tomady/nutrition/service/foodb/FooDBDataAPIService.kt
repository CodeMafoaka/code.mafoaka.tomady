package com.tomady.nutrition.service.foodb

import com.tomady.nutrition.data.local.foodb.entity.FoodItem
import com.tomady.nutrition.data.local.foodb.entity.NutrientProperty

/**
 * Service stub for accessing the local FooDB food composition database.
 *
 * Provides search, lookup, and aggregate nutrition queries over the
 * read-only FooDB catalog. See `init_ressources/foodb_generated_schema_only.sql`
 * for the full schema reference.
 *
 * Business logic to be implemented in a subsequent feature commit.
 */
class FooDBDataAPIService {

    /**
     * Searches for food items by name or scientific name.
     *
     * @param query Search string
     * @return List of matching [FoodItem] entries
     */
    suspend fun searchFood(query: String): List<FoodItem> {
        // TODO: Implement search
        return emptyList()
    }

    /**
     * Retrieves all nutrient/compound properties for a given food item.
     *
     * @param foodItemId FooDB food item ID
     * @return List of [NutrientProperty] entries
     */
    suspend fun getNutrients(foodItemId: Long): List<NutrientProperty> {
        // TODO: Implement nutrient lookup
        return emptyList()
    }

    /**
     * Gets all food items belonging to a specific food group.
     *
     * @param group Food group name (e.g. "Vegetables", "Fruits")
     * @return List of matching [FoodItem] entries
     */
    suspend fun getFoodsByGroup(group: String): List<FoodItem> {
        // TODO: Implement group filter
        return emptyList()
    }

    /**
     * Returns the distinct list of available food groups.
     *
     * @return List of food group names
     */
    suspend fun getFoodGroups(): List<String> {
        // TODO: Implement groups query
        return emptyList()
    }

    /**
     * Gets all available nutrient/compound names in the database.
     *
     * @return List of distinct nutrient names
     */
    suspend fun getNutrientNames(): List<String> {
        // TODO: Implement nutrient names query
        return emptyList()
    }
}
