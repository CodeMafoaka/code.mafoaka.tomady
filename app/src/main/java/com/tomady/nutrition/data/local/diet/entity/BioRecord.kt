package com.tomady.nutrition.data.local.diet.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a biometric time-series measurement
 * (e.g. daily weight, body fat percentage, blood pressure).
 */
@Entity(
    tableName = "bio_record",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("user_id"), Index("date")]
)
data class BioRecord(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "weight_kg")
    val weightKg: Double? = null,

    @ColumnInfo(name = "body_fat_percentage")
    val bodyFatPercentage: Double? = null,

    @ColumnInfo(name = "systolic_bp")
    val systolicBp: Int? = null,

    @ColumnInfo(name = "diastolic_bp")
    val diastolicBp: Int? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
