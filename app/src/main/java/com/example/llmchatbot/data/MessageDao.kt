package com.example.llmchatbot.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {

    /** Insert a single message and return its generated id. */
    @Insert
    suspend fun insert(message: Message): Long

    /** Load the full chat history for a given user, ordered chronologically. */
    @Query("SELECT * FROM messages WHERE username = :username ORDER BY timestamp ASC")
    suspend fun getMessagesForUser(username: String): List<Message>

    /** Wipe all messages for a given user (used by the "clear history" action). */
    @Query("DELETE FROM messages WHERE username = :username")
    suspend fun clearForUser(username: String)
}
