package com.tomady.nutrition.data.local.diet;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "dishes")
public class Dish {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private double calories;
    private int recipeId;

    public Dish() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getCalories() { return calories; }
    public void setCalories(double calories) { this.calories = calories; }
    public int getRecipeId() { return recipeId; }
    public void setRecipeId(int recipeId) { this.recipeId = recipeId; }
}
