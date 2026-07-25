package com.tomady.nutrition.data.local.foodb.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a nutrient or compound content value for a food item.
 *
 * Maps to the FooDB `content` and `nutrient` tables. Each row records the amount
 * of a specific nutrient or compound found in a given food.
 */
@Entity(
    tableName = "nutrient_property",
    indices = [
        Index("food_item_id"),
        Index("nutrient_name")
    ]
)
data class NutrientProperty(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "food_item_id")
    val foodItemId: Long,

    @ColumnInfo(name = "nutrient_name")
    val nutrientName: String? = null,

    @ColumnInfo(name = "amount")
    val amount: Double? = null,

    @ColumnInfo(name = "unit")
    val unit: String? = null,

    @ColumnInfo(name = "standard_content")
    val standardContent: Double? = null,

    @ColumnInfo(name = "preparation_type")
    val preparationType: String? = null,

    @ColumnInfo(name = "citation")
    val citation: String? = null,

    @ColumnInfo(name = "orig_content")
    val origContent: String? = null
)
