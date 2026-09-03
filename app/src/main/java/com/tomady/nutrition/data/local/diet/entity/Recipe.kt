package com.tomady.nutrition.data.local.diet.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a full recipe with preparation instructions.
 */
@Entity(tableName = "recipe", indices = [Index("dish_id")])
data class Recipe(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /** The [Dish] this recipe composes — its aggregated ingredient macros
     * (see `DietAPIService.computeDishNutrition`) are that dish's nutrition. */
    @ColumnInfo(name = "dish_id")
    val dishId: String? = null,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "instructions")
    val instructions: String? = null,

    @ColumnInfo(name = "prep_time_minutes")
    val prepTimeMinutes: Int? = null,

    @ColumnInfo(name = "cook_time_minutes")
    val cookTimeMinutes: Int? = null,

    @ColumnInfo(name = "servings")
    val servings: Int? = null,

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
