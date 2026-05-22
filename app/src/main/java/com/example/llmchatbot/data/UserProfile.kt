package com.example.llmchatbot.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local user profile stored in Room.
 * Tracks the user's subscription tier and email.
 */
@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val username: String,
    val email: String = "",
    val tier: String = "Free",       // Free, Starter, Intermediate, Advanced
    val createdAt: Long = System.currentTimeMillis()
)
