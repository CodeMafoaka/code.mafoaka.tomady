package com.tomady.nutrition.data.local.diet.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tomady.nutrition.data.local.diet.entity.RecipeIngredient
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [RecipeIngredient] entity CRUD operations.
 */
@Dao
interface RecipeIngredientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ingredient: RecipeIngredient): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<RecipeIngredient>): List<Long>

    @Update
    suspend fun update(ingredient: RecipeIngredient)

    @Delete
    suspend fun delete(ingredient: RecipeIngredient)

    @Query("SELECT * FROM recipe_ingredient WHERE id = :id")
    suspend fun getById(id: String): RecipeIngredient?

    @Query("SELECT * FROM recipe_ingredient WHERE recipe_id = :recipeId")
    fun observeByRecipe(recipeId: String): Flow<List<RecipeIngredient>>

    @Query("SELECT * FROM recipe_ingredient WHERE recipe_id = :recipeId")
    suspend fun getByRecipe(recipeId: String): List<RecipeIngredient>

    @Query("DELETE FROM recipe_ingredient WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recipe_ingredient WHERE recipe_id = :recipeId")
    suspend fun deleteByRecipe(recipeId: String)
}
