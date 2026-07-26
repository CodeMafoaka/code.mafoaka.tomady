package com.tomady.nutrition.data.local.diet.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tomady.nutrition.data.local.diet.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [User] entity CRUD operations.
 */
@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<User>): List<Long>

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("SELECT * FROM `user` WHERE id = :id")
    suspend fun getById(id: String): User?

    @Query("SELECT * FROM `user` WHERE id = :id")
    fun observeById(id: String): Flow<User?>

    @Query("SELECT * FROM `user` ORDER BY created_at DESC")
    fun observeAll(): Flow<List<User>>

    @Query("SELECT * FROM `user` WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): User?

    @Query("DELETE FROM `user` WHERE id = :id")
    suspend fun deleteById(id: String)
}
