package com.tomady.nutrition.data.local.foodb.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tomady.nutrition.data.local.foodb.entity.FoodItem
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [FoodItem] entity CRUD operations against the local FooDB database.
 */
@Dao
interface FoodItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(foodItem: FoodItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(foodItems: List<FoodItem>): List<Long>

    @Update
    suspend fun update(foodItem: FoodItem)

    @Delete
    suspend fun delete(foodItem: FoodItem)

    @Query("SELECT * FROM food_item WHERE id = :id")
    suspend fun getById(id: Long): FoodItem?

    @Query("SELECT * FROM food_item WHERE id = :id")
    fun observeById(id: Long): Flow<FoodItem?>

    @Query("SELECT * FROM food_item ORDER BY name ASC")
    fun observeAll(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_item WHERE food_group = :group ORDER BY name ASC")
    suspend fun getByFoodGroup(group: String): List<FoodItem>

    @Query("SELECT * FROM food_item WHERE name LIKE '%' || :query || '%' OR name_scientific LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<FoodItem>

    @Query("SELECT DISTINCT food_group FROM food_item WHERE food_group IS NOT NULL ORDER BY food_group ASC")
    fun observeFoodGroups(): Flow<List<String>>

    @Query("SELECT * FROM food_item WHERE food_group = :group AND food_subgroup = :subgroup ORDER BY name ASC")
    suspend fun getByGroupAndSubgroup(group: String, subgroup: String): List<FoodItem>

    @Query("DELETE FROM food_item WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM food_item")
    suspend fun deleteAll()
}
