package com.example.llmchatbot.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(profile: UserProfile)

    @Query("SELECT * FROM user_profiles WHERE username = :username LIMIT 1")
    suspend fun getProfile(username: String): UserProfile?

    @Query("UPDATE user_profiles SET tier = :tier WHERE username = :username")
    suspend fun updateTier(username: String, tier: String)

    @Query("UPDATE user_profiles SET email = :email WHERE username = :username")
    suspend fun updateEmail(username: String, email: String)
}
