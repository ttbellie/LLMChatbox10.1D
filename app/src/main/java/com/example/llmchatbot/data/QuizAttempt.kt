package com.example.llmchatbot.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single quiz question attempt stored in the local Room database.
 * Tracks question text, user's answer, correct answer, and whether it was correct.
 */
@Entity(tableName = "quiz_attempts")
data class QuizAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val question: String,
    val userAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)
