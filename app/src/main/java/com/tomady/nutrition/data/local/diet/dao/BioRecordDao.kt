package com.tomady.nutrition.data.local.diet.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tomady.nutrition.data.local.diet.entity.BioRecord
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [BioRecord] entity CRUD operations.
 */
@Dao
interface BioRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BioRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<BioRecord>): List<Long>

    @Update
    suspend fun update(record: BioRecord)

    @Delete
    suspend fun delete(record: BioRecord)

    @Query("SELECT * FROM bio_record WHERE id = :id")
    suspend fun getById(id: String): BioRecord?

    @Query("SELECT * FROM bio_record WHERE user_id = :userId ORDER BY date DESC")
    fun observeByUser(userId: String): Flow<List<BioRecord>>

    @Query("SELECT * FROM bio_record WHERE user_id = :userId AND date = :date LIMIT 1")
    suspend fun getByUserAndDate(userId: String, date: String): BioRecord?

    @Query("SELECT * FROM bio_record WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getByUserInDateRange(userId: String, startDate: String, endDate: String): List<BioRecord>

    @Query("DELETE FROM bio_record WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM bio_record WHERE user_id = :userId")
    suspend fun deleteByUser(userId: String)
}
