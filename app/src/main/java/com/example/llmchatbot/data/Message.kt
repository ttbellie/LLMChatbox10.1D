package com.example.llmchatbot.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single chat message persisted in the local Room database.
 *
 * @param isUser true if the message was sent by the user, false if it came from the chatbot.
 * @param timestamp epoch millis for when the message was created (used for the per-bubble time label).
 */
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
