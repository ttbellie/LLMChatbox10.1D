package com.example.llmchatbot.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface QuizAttemptDao {

    @Insert
    suspend fun insert(attempt: QuizAttempt): Long

    @Query("SELECT * FROM quiz_attempts WHERE username = :username ORDER BY timestamp DESC")
    suspend fun getAttemptsForUser(username: String): List<QuizAttempt>

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE username = :username")
    suspend fun getTotalCount(username: String): Int

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE username = :username AND isCorrect = 1")
    suspend fun getCorrectCount(username: String): Int

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE username = :username AND isCorrect = 0")
    suspend fun getIncorrectCount(username: String): Int

    @Query("DELETE FROM quiz_attempts WHERE username = :username")
    suspend fun clearForUser(username: String)
}
