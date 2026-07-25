package com.tomady.nutrition.data.local.foodb;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.tomady.nutrition.data.local.diet.FoodItem;
import com.tomady.nutrition.data.local.diet.NutrientProperty;
import java.util.List;

@Dao
public interface FooDBLocalDao {

    @Insert
    void insertFoodItem(FoodItem foodItem);

    @Insert
    void insertNutrientProperties(List<NutrientProperty> properties);

    @Query("SELECT * FROM food_items WHERE id = :id")
    FoodItem getFoodItemById(int id);

    @Query("SELECT * FROM nutrient_properties WHERE foodItemId = :foodItemId")
    List<NutrientProperty> getNutrientPropertiesByFoodId(int foodItemId);

    @Query("SELECT * FROM food_items WHERE name LIKE :query")
    List<FoodItem> searchLocalFoods(String query);
}
