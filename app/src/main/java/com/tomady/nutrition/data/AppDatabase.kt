package com.tomady.nutrition.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tomady.nutrition.data.local.foodb.dao.FoodItemDao
import com.tomady.nutrition.data.local.foodb.dao.NutrientPropertyDao
import com.tomady.nutrition.data.local.foodb.entity.FoodItem
import com.tomady.nutrition.data.local.foodb.entity.NutrientProperty

/**
 * Room database for the Tomady application.
 *
 * Manages two database contexts:
 * - **FooDB Database**: Read-only mirror of the FooDB food composition schema,
 *   containing [FoodItem] and [NutrientProperty] entities.
 * - **Diet Database**: User-centric domain tables for personal health tracking
 *   (to be added when diet entities are ready).
 *
 * This class currently exposes the FooDB DAOs. Diet DAOs will be added in a
 * subsequent feature commit.
 */
@Database(
    entities = [
        FoodItem::class,
        NutrientProperty::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

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
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
