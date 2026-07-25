package com.tomady.nutrition.data.local.foodb;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.tomady.nutrition.data.local.diet.FoodItem;
import com.tomady.nutrition.data.local.diet.NutrientProperty;

@Database(entities = {FoodItem.class, NutrientProperty.class}, version = 1, exportSchema = false)
public abstract class FooDBLocalDatabase extends RoomDatabase {
    public abstract FooDBLocalDao fooDBLocalDao();
}
