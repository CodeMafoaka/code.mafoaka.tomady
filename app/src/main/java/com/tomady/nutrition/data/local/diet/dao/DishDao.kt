package com.tomady.nutrition.data.local.diet.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tomady.nutrition.data.local.diet.entity.Dish
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [Dish] entity CRUD operations.
 */
@Dao
interface DishDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dish: Dish): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dishes: List<Dish>): List<Long>

    @Update
    suspend fun update(dish: Dish)

    @Delete
    suspend fun delete(dish: Dish)

    @Query("SELECT * FROM dish WHERE id = :id")
    suspend fun getById(id: String): Dish?

    @Query("SELECT * FROM dish WHERE id = :id")
    fun observeById(id: String): Flow<Dish?>

    @Query("SELECT * FROM dish ORDER BY name ASC")
    fun observeAll(): Flow<List<Dish>>

    @Query("SELECT * FROM dish WHERE category = :category ORDER BY name ASC")
    fun observeByCategory(category: String): Flow<List<Dish>>

    @Query("SELECT * FROM dish WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<Dish>

    @Query("DELETE FROM dish WHERE id = :id")
    suspend fun deleteById(id: String)
}
