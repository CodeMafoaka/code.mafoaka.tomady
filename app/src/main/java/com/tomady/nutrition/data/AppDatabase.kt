package com.tomady.nutrition.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
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
 * Room database for the Tomady application.
 *
 * Manages two database contexts:
 * - **Diet Database**: User-centric domain tables for personal health tracking.
 * - **FooDB Database**: Read-only mirror of the FooDB food composition schema.
 */
@Database(
    entities = [
        // Diet entities
        User::class,
        Profile::class,
        BioRecord::class,
        Dish::class,
        Recipe::class,
        RecipeIngredient::class,
        DishHistory::class,
        // FooDB entities
        FoodItem::class,
        NutrientProperty::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // ── Diet DAOs ──
    abstract fun userDao(): UserDao
    abstract fun profileDao(): ProfileDao
    abstract fun bioRecordDao(): BioRecordDao
    abstract fun dishDao(): DishDao
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao
    abstract fun dishHistoryDao(): DishHistoryDao

    // ── FooDB DAOs ──
    abstract fun foodItemDao(): FoodItemDao
    abstract fun nutrientPropertyDao(): NutrientPropertyDao

    companion object {
        private const val DATABASE_NAME = "tomady_nutrition.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton [AppDatabase], creating it if necessary.
         *
         * @param context Application context for database construction.
         * @return The shared [AppDatabase] instance.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val seedCallback = SeedDataCallback()

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed data will be inserted after the database instance is fully created
                        // We use a coroutine launched on the IO dispatcher because DAO methods are suspend.
                        // AppDatabase.getInstance() is safe here because onCreate runs before any query.
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val instance = getInstance(context)
                            seedCallback.seedDatabase(instance)
                        }
                    }
                })
                .build()
        }
    }
}
