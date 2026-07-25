package com.tomady.nutrition.data.local.diet;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recipe_ingredients")
public class RecipeIngredient {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int recipeId;
    private int foodItemId;
    private double quantity;

    public RecipeIngredient() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRecipeId() { return recipeId; }
    public void setRecipeId(int recipeId) { this.recipeId = recipeId; }
    public int getFoodItemId() { return foodItemId; }
    public void setFoodItemId(int foodItemId) { this.foodItemId = foodItemId; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
}
