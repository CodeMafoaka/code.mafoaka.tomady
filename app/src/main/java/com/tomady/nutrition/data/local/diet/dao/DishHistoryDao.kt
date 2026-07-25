package com.tomady.nutrition.data.local.diet.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tomady.nutrition.data.local.diet.entity.DishHistory
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [DishHistory] entity CRUD operations.
 */
@Dao
interface DishHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dishHistory: DishHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dishHistories: List<DishHistory>): List<Long>

    @Update
    suspend fun update(dishHistory: DishHistory)

    @Delete
    suspend fun delete(dishHistory: DishHistory)

    @Query("SELECT * FROM dish_history WHERE id = :id")
    suspend fun getById(id: String): DishHistory?

    @Query("SELECT * FROM dish_history WHERE user_id = :userId ORDER BY date DESC, created_at DESC")
    fun observeByUser(userId: String): Flow<List<DishHistory>>

    @Query("SELECT * FROM dish_history WHERE user_id = :userId AND date = :date ORDER BY created_at ASC")
    suspend fun getByUserAndDate(userId: String, date: String): List<DishHistory>

    @Query("SELECT * FROM dish_history WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getByUserInDateRange(userId: String, startDate: String, endDate: String): List<DishHistory>

    @Query("SELECT * FROM dish_history WHERE user_id = :userId AND meal_type = :mealType AND date = :date ORDER BY created_at ASC")
    suspend fun getByUserDateAndMealType(userId: String, date: String, mealType: String): List<DishHistory>

    @Query("DELETE FROM dish_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM dish_history WHERE user_id = :userId")
    suspend fun deleteByUser(userId: String)
}
