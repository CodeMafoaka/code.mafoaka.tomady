package com.tomady.nutrition.data.local.diet;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {
    User.class, Profile.class, BioRecord.class, Dish.class,
    Recipe.class, RecipeIngredient.class, DishHistory.class,
    FoodItem.class, NutrientProperty.class
}, version = 1, exportSchema = false)
public abstract class DietDatabase extends RoomDatabase {
    public abstract DietDao dietDao();
}
