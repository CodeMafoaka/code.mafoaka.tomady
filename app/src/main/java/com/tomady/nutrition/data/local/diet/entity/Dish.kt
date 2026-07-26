package com.tomady.nutrition.data.local.diet.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a known dish or meal in the catalog.
 */
@Entity(tableName = "dish")
data class Dish(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "calories")
    val calories: Int? = null,

    @ColumnInfo(name = "protein_grams")
    val proteinGrams: Double? = null,

    @ColumnInfo(name = "carbs_grams")
    val carbsGrams: Double? = null,

    @ColumnInfo(name = "fat_grams")
    val fatGrams: Double? = null,

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
