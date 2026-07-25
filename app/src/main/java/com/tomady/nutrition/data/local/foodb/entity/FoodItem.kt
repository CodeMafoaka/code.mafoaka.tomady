package com.tomady.nutrition.data.local.foodb.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a food entry from the FooDB catalog.
 *
 * Maps to the FooDB `food` table. Fields follow the schema defined in
 * `init_ressources/foodb_generated_schema_only.sql`.
 */
@Entity(
    tableName = "food_item",
    indices = [
        Index("food_group"),
        Index("name")
    ]
)
data class FoodItem(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "public_id")
    val publicId: String? = null,

    @ColumnInfo(name = "name")
    val name: String? = null,

    @ColumnInfo(name = "name_scientific")
    val nameScientific: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "food_group")
    val foodGroup: String? = null,

    @ColumnInfo(name = "food_subgroup")
    val foodSubgroup: String? = null,

    @ColumnInfo(name = "food_type")
    val foodType: String? = null,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "itis_id")
    val itisId: String? = null,

    @ColumnInfo(name = "wikipedia_id")
    val wikipediaId: String? = null,

    @ColumnInfo(name = "ncbi_taxonomy_id")
    val ncbiTaxonomyId: Long? = null,

    @ColumnInfo(name = "picture_file_name")
    val pictureFileName: String? = null
)
