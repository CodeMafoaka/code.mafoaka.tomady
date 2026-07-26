package com.tomady.nutrition.data.local.diet.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tomady.nutrition.data.local.diet.entity.Recipe
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [Recipe] entity CRUD operations.
 */
@Dao
interface RecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: Recipe): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<Recipe>): List<Long>

    @Update
    suspend fun update(recipe: Recipe)

    @Delete
    suspend fun delete(recipe: Recipe)

    @Query("SELECT * FROM recipe WHERE id = :id")
    suspend fun getById(id: String): Recipe?

    @Query("SELECT * FROM recipe WHERE dish_id = :dishId")
    suspend fun getByDishId(dishId: String): List<Recipe>

    @Query("SELECT * FROM recipe WHERE id = :id")
    fun observeById(id: String): Flow<Recipe?>

    @Query("SELECT * FROM recipe ORDER BY name ASC")
    fun observeAll(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipe WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<Recipe>

    @Query("DELETE FROM recipe WHERE id = :id")
    suspend fun deleteById(id: String)
}
