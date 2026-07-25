package com.tomady.nutrition.data.local.diet.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tomady.nutrition.data.local.diet.entity.Profile
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [Profile] entity CRUD operations.
 */
@Dao
interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: Profile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<Profile>): List<Long>

    @Update
    suspend fun update(profile: Profile)

    @Delete
    suspend fun delete(profile: Profile)

    @Query("SELECT * FROM profile WHERE id = :id")
    suspend fun getById(id: String): Profile?

    @Query("SELECT * FROM profile WHERE user_id = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): Profile?

    @Query("SELECT * FROM profile WHERE user_id = :userId LIMIT 1")
    fun observeByUserId(userId: String): Flow<Profile?>

    @Query("SELECT * FROM profile ORDER BY created_at DESC")
    fun observeAll(): Flow<List<Profile>>

    @Query("DELETE FROM profile WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM profile WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)
}
