package com.tomady.nutrition.data.local.foodb.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tomady.nutrition.data.local.foodb.entity.NutrientProperty
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [NutrientProperty] entity CRUD operations against the local FooDB database.
 */
@Dao
interface NutrientPropertyDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(property: NutrientProperty): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(properties: List<NutrientProperty>): List<Long>

    @Update
    suspend fun update(property: NutrientProperty)

    @Delete
    suspend fun delete(property: NutrientProperty)

    @Query("SELECT * FROM nutrient_property WHERE id = :id")
    suspend fun getById(id: Long): NutrientProperty?

    @Query("SELECT * FROM nutrient_property WHERE food_item_id = :foodItemId")
    suspend fun getByFoodItem(foodItemId: Long): List<NutrientProperty>

    @Query("SELECT * FROM nutrient_property WHERE food_item_id = :foodItemId")
    fun observeByFoodItem(foodItemId: Long): Flow<List<NutrientProperty>>

    @Query("SELECT * FROM nutrient_property WHERE nutrient_name = :name")
    suspend fun getByNutrientName(name: String): List<NutrientProperty>

    @Query("SELECT * FROM nutrient_property WHERE food_item_id = :foodItemId AND nutrient_name = :name LIMIT 1")
    suspend fun getByFoodItemAndNutrient(foodItemId: Long, name: String): NutrientProperty?

    @Query("SELECT DISTINCT nutrient_name FROM nutrient_property WHERE nutrient_name IS NOT NULL ORDER BY nutrient_name ASC")
    fun observeNutrientNames(): Flow<List<String>>

    @Query("DELETE FROM nutrient_property WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM nutrient_property WHERE food_item_id = :foodItemId")
    suspend fun deleteByFoodItem(foodItemId: Long)

    @Query("DELETE FROM nutrient_property")
    suspend fun deleteAll()
}
