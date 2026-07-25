package com.tomady.nutrition.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tomady.nutrition.data.local.diet.dao.BioRecordDao
import com.tomady.nutrition.data.local.diet.dao.DishDao
import com.tomady.nutrition.data.local.diet.dao.DishHistoryDao
import com.tomady.nutrition.data.local.diet.dao.ProfileDao
import com.tomady.nutrition.data.local.diet.dao.RecipeDao
import com.tomady.nutrition.data.local.diet.dao.RecipeIngredientDao
import com.tomady.nutrition.data.local.diet.dao.UserDao
import com.tomady.nutrition.data.local.diet.entity.BioRecord
import com.tomady.nutrition.data.local.diet.entity.Dish
import com.tomady.nutrition.data.local.diet.entity.DishHistory
import com.tomady.nutrition.data.local.diet.entity.Profile
import com.tomady.nutrition.data.local.diet.entity.Recipe
import com.tomady.nutrition.data.local.diet.entity.RecipeIngredient
import com.tomady.nutrition.data.local.diet.entity.User
import com.tomady.nutrition.data.local.foodb.dao.FoodItemDao
import com.tomady.nutrition.data.local.foodb.dao.NutrientPropertyDao
import com.tomady.nutrition.data.local.foodb.entity.FoodItem
import com.tomady.nutrition.data.local.foodb.entity.NutrientProperty

/**
 * Room database abstract class for the Diet domain database.
 *
 * Stores user profiles, biometric records, dishes, recipes, and meal history.
 */
@Database(
    entities = [
        User::class,
        Profile::class,
        BioRecord::class,
        Dish::class,
        Recipe::class,
        RecipeIngredient::class,
        DishHistory::class
    ],
    version = 1,
    exportSchema = true
)
abstract class DietDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun profileDao(): ProfileDao
    abstract fun bioRecordDao(): BioRecordDao
    abstract fun dishDao(): DishDao
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao
    abstract fun dishHistoryDao(): DishHistoryDao
}

/**
 * Room database abstract class for the read-only FooDB reference database.
 *
 * Contains food composition data imported from the FooDB schema.
 * See `init_ressources/foodb_generated_schema_only.sql` for the schema reference.
 */
@Database(
    entities = [
        FoodItem::class,
        NutrientProperty::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FooDBDatabase : RoomDatabase() {

    abstract fun foodItemDao(): FoodItemDao
    abstract fun nutrientPropertyDao(): NutrientPropertyDao
}
