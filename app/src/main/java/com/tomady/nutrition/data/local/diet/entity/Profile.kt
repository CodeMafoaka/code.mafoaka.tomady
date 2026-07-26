package com.tomady.nutrition.data.local.diet.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an extended user profile with health goals and metrics.
 *
 * Champs ajoutés v2 : allergies, intolerances, conditions, restrictedFoods,
 * forbiddenByDoctor, age, activityLevel — correspondant au modèle de données
 * de l'application mobile Tomady (voir database.js).
 */
@Entity(
    tableName = "profile",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("user_id")]
)
data class Profile(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "display_name")
    val displayName: String? = null,

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String? = null,

    @ColumnInfo(name = "date_of_birth")
    val dateOfBirth: String? = null,

    @ColumnInfo(name = "age")
    val age: Int? = null,

    @ColumnInfo(name = "height_cm")
    val heightCm: Double? = null,

    @ColumnInfo(name = "weight_kg")
    val weightKg: Double? = null,

    @ColumnInfo(name = "daily_calorie_target")
    val dailyCalorieTarget: Int? = null,

    @ColumnInfo(name = "protein_grams_target")
    val proteinGramsTarget: Int? = null,

    @ColumnInfo(name = "carbs_grams_target")
    val carbsGramsTarget: Int? = null,

    @ColumnInfo(name = "fat_grams_target")
    val fatGramsTarget: Int? = null,

    @ColumnInfo(name = "goal")
    val goal: String? = null,

    @ColumnInfo(name = "activity_level")
    val activityLevel: String? = null,

    @ColumnInfo(name = "allergies")
    val allergies: String? = null,

    @ColumnInfo(name = "intolerances")
    val intolerances: String? = null,

    @ColumnInfo(name = "conditions")
    val conditions: String? = null,

    @ColumnInfo(name = "restricted_foods")
    val restrictedFoods: String? = null,

    @ColumnInfo(name = "forbidden_by_doctor")
    val forbiddenByDoctor: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
