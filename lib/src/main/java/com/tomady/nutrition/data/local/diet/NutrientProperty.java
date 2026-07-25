package com.tomady.nutrition.data.local.diet;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "nutrient_properties")
public class NutrientProperty {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int foodItemId;
    private String propertyName;
    private double propertyValue;

    public NutrientProperty() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getFoodItemId() { return foodItemId; }
    public void setFoodItemId(int foodItemId) { this.foodItemId = foodItemId; }
    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }
    public double getPropertyValue() { return propertyValue; }
    public void setPropertyValue(double propertyValue) { this.propertyValue = propertyValue; }
}
