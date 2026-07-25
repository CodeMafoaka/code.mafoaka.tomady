package com.tomady.nutrition.data.local.foodb

import com.tomady.nutrition.data.local.foodb.dao.FoodItemDao
import com.tomady.nutrition.data.local.foodb.dao.NutrientPropertyDao
import com.tomady.nutrition.data.local.foodb.entity.FoodItem
import com.tomady.nutrition.data.local.foodb.entity.NutrientProperty
import kotlinx.coroutines.flow.first

/**
 * High-level wrapper over the local FooDB Room database.
 *
 * Provides convenient suspend functions for searching and retrieving cached food
 * and nutrient data from the local database without exposing DAO internals
 * to the service layer.
 *
 * @param foodItemDao The DAO for [FoodItem] CRUD operations.
 * @param nutrientPropertyDao The DAO for [NutrientProperty] CRUD operations.
 */
class FooDBLocalDatabase(
    private val foodItemDao: FoodItemDao,
    private val nutrientPropertyDao: NutrientPropertyDao
) {

    // ── FoodItem queries ────────────────────────────────────────────────

    /**
     * Retrieves a [FoodItem] by its primary key, or null if not cached locally.
     */
    suspend fun getFoodById(id: Long): FoodItem? {
        return foodItemDao.getById(id)
    }

    /**
     * Searches the local food catalog by name or scientific name.
     *
     * @param query Free-text search string.
     * @return List of matching [FoodItem] entries.
     */
    suspend fun searchFood(query: String): List<FoodItem> {
        return foodItemDao.search(query)
    }

    /**
     * Returns all food items belonging to a given food group.
     */
    suspend fun getFoodByGroup(group: String): List<FoodItem> {
        return foodItemDao.getByFoodGroup(group)
    }

    /**
     * Returns food items filtered by both group and subgroup.
     */
    suspend fun getFoodByGroupAndSubgroup(group: String, subgroup: String): List<FoodItem> {
        return foodItemDao.getByGroupAndSubgroup(group, subgroup)
    }

    /**
     * Returns all distinct food groups available in the local catalog.
     */
    suspend fun getFoodGroups(): List<String> {
        var result = emptyList<String>()
        foodItemDao.observeFoodGroups().first { emitted ->
            result = emitted
            true
        }
        return result
    }

    /**
     * Inserts a [FoodItem] into the local database.
     *
     * @return The row ID of the inserted item, or -1 if a conflict occurs.
     */
    suspend fun insertFood(foodItem: FoodItem): Long {
        return foodItemDao.insert(foodItem)
    }

    /**
     * Inserts a batch of [FoodItem] entries.
     */
    suspend fun insertFoods(foodItems: List<FoodItem>): List<Long> {
        return foodItemDao.insertAll(foodItems)
    }

    /**
     * Deletes all food items from the local database.
     */
    suspend fun deleteAllFoods() {
        foodItemDao.deleteAll()
    }

    // ── NutrientProperty queries ────────────────────────────────────────

    /**
     * Retrieves all [NutrientProperty] entries associated with a specific food item.
     */
    suspend fun getNutrientsByFoodId(foodItemId: Long): List<NutrientProperty> {
        return nutrientPropertyDao.getByFoodItem(foodItemId)
    }

    /**
     * Retrieves a specific nutrient property for a food item by nutrient name.
     */
    suspend fun getNutrientByFoodAndName(
        foodItemId: Long,
        nutrientName: String
    ): NutrientProperty? {
        return nutrientPropertyDao.getByFoodItemAndNutrient(foodItemId, nutrientName)
    }

    /**
     * Returns all distinct nutrient names available in the local database.
     */
    suspend fun getNutrientNames(): List<String> {
        var result = emptyList<String>()
        nutrientPropertyDao.observeNutrientNames().first { emitted ->
            result = emitted
            true
        }
        return result
    }

    /**
     * Inserts a [NutrientProperty] into the local database.
     */
    suspend fun insertNutrientProperty(property: NutrientProperty): Long {
        return nutrientPropertyDao.insert(property)
    }

    /**
     * Inserts a batch of [NutrientProperty] entries.
     */
    suspend fun insertNutrientProperties(properties: List<NutrientProperty>): List<Long> {
        return nutrientPropertyDao.insertAll(properties)
    }

    /**
     * Deletes all nutrient properties for a given food item.
     */
    suspend fun deleteNutrientsByFoodId(foodItemId: Long) {
        nutrientPropertyDao.deleteByFoodItem(foodItemId)
    }

    /**
     * Deletes all nutrient properties from the local database.
     */
    suspend fun deleteAllNutrients() {
        nutrientPropertyDao.deleteAll()
    }
}
