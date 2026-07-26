package com.tomady.nutrition.data.local.diet.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a full recipe with preparation instructions.
 *
 * Linked to a [Dish] via [dishId] so that nutrition computation can
 * resolve ingredients without fragile name-based searches.
 */
@Entity(
    tableName = "recipe",
    foreignKeys = [
        ForeignKey(
            entity = Dish::class,
            parentColumns = ["id"],
            childColumns = ["dish_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("dish_id")]
)
data class Recipe(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "dish_id")
    val dishId: String? = null,

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
