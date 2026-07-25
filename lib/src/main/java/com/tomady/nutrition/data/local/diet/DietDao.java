package com.tomady.nutrition.data.local.diet;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface DietDao {
    @Insert
    void insertUser(User user);

    @Update
    void updateUser(User user);

    @Delete
    void deleteUser(User user);

    @Query("SELECT * FROM users WHERE id = :id")
    User getUserById(int id);

    @Query("SELECT * FROM users")
    List<User> getAllUsers();

    @Insert
    void insertProfile(Profile profile);

    @Update
    void updateProfile(Profile profile);

    @Delete
    void deleteProfile(Profile profile);

    @Query("SELECT * FROM profiles WHERE id = :id")
    Profile getProfileById(int id);

    @Query("SELECT * FROM profiles")
    List<Profile> getAllProfiles();

    @Insert
    void insertBioRecord(BioRecord bioRecord);

    @Update
    void updateBioRecord(BioRecord bioRecord);

    @Delete
    void deleteBioRecord(BioRecord bioRecord);

    @Query("SELECT * FROM bio_records WHERE id = :id")
    BioRecord getBioRecordById(int id);

    @Query("SELECT * FROM bio_records")
    List<BioRecord> getAllBioRecords();

    @Insert
    void insertDish(Dish dish);

    @Update
    void updateDish(Dish dish);

    @Delete
    void deleteDish(Dish dish);

    @Query("SELECT * FROM dishes WHERE id = :id")
    Dish getDishById(int id);

    @Query("SELECT * FROM dishes")
    List<Dish> getAllDishes();

    @Insert
    void insertRecipe(Recipe recipe);

    @Update
    void updateRecipe(Recipe recipe);

    @Delete
    void deleteRecipe(Recipe recipe);

    @Query("SELECT * FROM recipes WHERE id = :id")
    Recipe getRecipeById(int id);

    @Query("SELECT * FROM recipes")
    List<Recipe> getAllRecipes();

    @Insert
    void insertRecipeIngredient(RecipeIngredient recipeIngredient);

    @Update
    void updateRecipeIngredient(RecipeIngredient recipeIngredient);

    @Delete
    void deleteRecipeIngredient(RecipeIngredient recipeIngredient);

    @Query("SELECT * FROM recipe_ingredients WHERE id = :id")
    RecipeIngredient getRecipeIngredientById(int id);

    @Query("SELECT * FROM recipe_ingredients")
    List<RecipeIngredient> getAllRecipeIngredients();

    @Insert
    void insertDishHistory(DishHistory dishHistory);

    @Update
    void updateDishHistory(DishHistory dishHistory);

    @Delete
    void deleteDishHistory(DishHistory dishHistory);

    @Query("SELECT * FROM dish_history WHERE id = :id")
    DishHistory getDishHistoryById(int id);

    @Query("SELECT * FROM dish_history")
    List<DishHistory> getAllDishHistory();

    @Insert
    void insertFoodItem(FoodItem foodItem);

    @Update
    void updateFoodItem(FoodItem foodItem);

    @Delete
    void deleteFoodItem(FoodItem foodItem);

    @Query("SELECT * FROM food_items WHERE id = :id")
    FoodItem getFoodItemById(int id);

    @Query("SELECT * FROM food_items")
    List<FoodItem> getAllFoodItems();

    @Insert
    void insertNutrientProperty(NutrientProperty nutrientProperty);

    @Update
    void updateNutrientProperty(NutrientProperty nutrientProperty);

    @Delete
    void deleteNutrientProperty(NutrientProperty nutrientProperty);

    @Query("SELECT * FROM nutrient_properties WHERE id = :id")
    NutrientProperty getNutrientPropertyById(int id);

    @Query("SELECT * FROM nutrient_properties")
    List<NutrientProperty> getAllNutrientProperties();
}
