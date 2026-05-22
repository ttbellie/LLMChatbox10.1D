package com.example.llmchatbot.network

import com.example.llmchatbot.BuildConfig
import com.example.llmchatbot.data.Message
import com.example.llmchatbot.data.MessageDao
import com.example.llmchatbot.data.QuizAttempt
import com.example.llmchatbot.data.QuizAttemptDao

/**
 * Single source of truth for chat data.
 *
 * - Reads/writes message history from Room.
 * - Sends conversation context to Gemini and returns the model reply.
 * - Saves quiz attempts for History and Profile statistics.
 */
class ChatRepository(
    private val dao: MessageDao,
    private val quizDao: QuizAttemptDao
) {

    private val api = GeminiClient.api
    private val model = "gemini-2.0-flash"

    suspend fun loadHistory(username: String): List<Message> =
        dao.getMessagesForUser(username)

    suspend fun saveMessage(message: Message): Long = dao.insert(message)

    suspend fun clearHistory(username: String) = dao.clearForUser(username)

    suspend fun saveQuizAttempt(attempt: QuizAttempt): Long = quizDao.insert(attempt)

    suspend fun sendMessage(
        history: List<Message>,
        userPrompt: String,
        systemPrompt: String? = null
    ): String {
        validateApiKey()

        val contents = mutableListOf<Content>()

        if (!systemPrompt.isNullOrBlank()) {
            contents.add(Content(role = "user", parts = listOf(Part(systemPrompt))))
            contents.add(
                Content(
                    role = "model",
                    parts = listOf(Part("Understood. I will follow this format."))
                )
            )
        }

        history.forEach { msg ->
            contents.add(
                Content(
                    role = if (msg.isUser) "user" else "model",
                    parts = listOf(Part(msg.content))
                )
            )
        }

        contents.add(Content(role = "user", parts = listOf(Part(userPrompt))))

        val response = api.generateContent(
            model = model,
            apiKey = BuildConfig.GEMINI_API_KEY,
            request = GeminiRequest(contents = contents)
        )

        response.error?.let { error ->
            throw RuntimeException("Gemini error ${error.code ?: ""}: ${error.message ?: "Unknown error"}")
        }

        return response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?.trim()
            ?: "Sorry, I couldn't generate a response."
    }

    private fun validateApiKey() {
        val key = BuildConfig.GEMINI_API_KEY.trim()
        if (key.isBlank() || key == "YOUR_API_KEY") {
            throw IllegalStateException(
                "Gemini API key is missing. Add GEMINI_API_KEY to local.properties."
            )
        }
    }
}
