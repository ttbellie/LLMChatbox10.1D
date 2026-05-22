package com.example.llmchatbot.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.llmchatbot.R
import com.example.llmchatbot.data.AppDatabase
import com.example.llmchatbot.data.Message
import com.example.llmchatbot.data.QuizAttempt
import com.example.llmchatbot.data.UserProfile
import com.example.llmchatbot.databinding.ActivityChatBinding
import com.example.llmchatbot.network.ChatRepository
import kotlinx.coroutines.launch
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var repository: ChatRepository
    private lateinit var username: String

    private val messages = mutableListOf<Message>()
    private var pendingQuizQuestion: String? = null
    private var pendingCorrectAnswer: String? = null

    private val pendingQuizPrefs by lazy {
        getSharedPreferences("pending_quiz_state", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        username = intent.getStringExtra(LoginActivity.EXTRA_USERNAME) ?: "User"
        binding.tvWelcome.text = "Welcome $username!"

        val db = AppDatabase.getInstance(this)
        repository = ChatRepository(db.messageDao(), db.quizAttemptDao())

        lifecycleScope.launch {
            val profileDao = db.userProfileDao()
            if (profileDao.getProfile(username) == null) {
                profileDao.insert(UserProfile(username = username))
            }
        }

        adapter = MessageAdapter(messages)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        binding.btnSend.setOnClickListener { sendMessage() }

        binding.tvWelcome.setOnLongClickListener {
            confirmClearHistory()
            true
        }

        binding.bottomNav.selectedItemId = R.id.nav_chat
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chat -> true
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java).apply {
                        putExtra(LoginActivity.EXTRA_USERNAME, username)
                    })
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        putExtra(LoginActivity.EXTRA_USERNAME, username)
                    })
                    false
                }
                else -> false
            }
        }

        restorePendingQuizState()
        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_chat
        restorePendingQuizState()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val history = repository.loadHistory(username)
            adapter.setMessages(history)
            scrollToBottom()
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return

        binding.etMessage.text?.clear()

        val userMsg = Message(username = username, content = text, isUser = true)

        lifecycleScope.launch {
            val userId = repository.saveMessage(userMsg)
            val savedUserMsg = userMsg.copy(id = userId)
            messages.add(savedUserMsg)
            adapter.notifyItemInserted(messages.size - 1)
            scrollToBottom()

            binding.tvTyping.visibility = View.VISIBLE

            try {
                val selectedLetter = extractUserAnswerLetter(text)
                val questionBeingAnswered = pendingQuizQuestion
                val correctLetter = pendingCorrectAnswer

                if (questionBeingAnswered != null && correctLetter != null && selectedLetter != null) {
                    handleQuizAnswer(
                        question = questionBeingAnswered,
                        userLetter = selectedLetter,
                        correctLetter = correctLetter
                    )
                } else {
                    requestGeminiReply(text)
                }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()

                if (msg.contains("429", ignoreCase = true) || msg.contains("quota", ignoreCase = true)) {
                    val fallbackQuiz = """
            Gemini limit reached, so this is a local demo quiz:

            What is Android Studio mainly used for?

            A) Editing videos
            B) Developing Android apps
            C) Playing online games
            D) Managing bank accounts
        """.trimIndent()

                    savePendingQuizState(
                        question = """
                What is Android Studio mainly used for?

                A) Editing videos
                B) Developing Android apps
                C) Playing online games
                D) Managing bank accounts
            """.trimIndent(),
                        correctLetter = "B"
                    )

                    saveAndDisplayBotMessage(fallbackQuiz)
                } else {
                    val errorReply = buildFriendlyErrorMessage(e)
                    saveAndDisplayBotMessage(errorReply)
                    Toast.makeText(this@ChatActivity, errorReply, Toast.LENGTH_LONG).show()
                }
            } finally {
                binding.tvTyping.visibility = View.GONE
            }
        }
    }

    private suspend fun requestGeminiReply(userText: String) {
        val historyBeforeNow = messages.dropLast(1)

        val systemPrompt = """
You are a quiz learning assistant for an Android learning app.

For normal conversation, answer normally and keep the response helpful.

When the user asks for a quiz, test, question, or practice question, generate ONE multiple-choice question using this exact format:

QUIZ_QUESTION: [question text]
A) [option A]
B) [option B]
C) [option C]
D) [option D]
CORRECT: [A/B/C/D]

Important rules:
- Always include exactly four options: A, B, C, and D.
- Always include the CORRECT line because the Android app uses it internally.
- Do not explain the correct answer until the user answers.
- Keep the quiz suitable for a student learning mobile app development, programming, AI, or general study topics.
""".trimIndent()

        val rawReply = repository.sendMessage(historyBeforeNow, userText, systemPrompt)
        val displayReply = cleanReplyForUser(rawReply)

        if (rawReply.contains("QUIZ_QUESTION:", ignoreCase = true)) {
            val correctLetter = extractCorrectLetter(rawReply)
            val quizText = extractQuizTextForStorage(displayReply)

            if (correctLetter != null && quizText.isNotBlank()) {
                savePendingQuizState(quizText, correctLetter)
            }
        }

        saveAndDisplayBotMessage(displayReply)
    }

    private suspend fun handleQuizAnswer(
        question: String,
        userLetter: String,
        correctLetter: String
    ) {
        val isCorrect = userLetter.equals(correctLetter, ignoreCase = true)
        val userAnswerText = findOptionText(question, userLetter) ?: userLetter.uppercase()
        val correctAnswerText = findOptionText(question, correctLetter) ?: correctLetter.uppercase()

        repository.saveQuizAttempt(
            QuizAttempt(
                username = username,
                question = question,
                userAnswer = userAnswerText,
                correctAnswer = correctAnswerText,
                isCorrect = isCorrect,
                category = "Quiz"
            )
        )

        clearPendingQuizState()

        val resultText = if (isCorrect) {
            "RESULT: CORRECT ✅\nEXPLANATION: Your answer ($userAnswerText) is correct. Good job!"
        } else {
            "RESULT: INCORRECT ❌\nEXPLANATION: Your answer was $userAnswerText. The correct answer is $correctAnswerText. Review this concept and try another question."
        }

        saveAndDisplayBotMessage(resultText)
    }

    private suspend fun saveAndDisplayBotMessage(reply: String) {
        val botMsg = Message(username = username, content = reply, isUser = false)
        val botId = repository.saveMessage(botMsg)
        messages.add(botMsg.copy(id = botId))
        adapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()
    }

    private fun cleanReplyForUser(reply: String): String {
        return reply.lines()
            .mapNotNull { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("CORRECT:", ignoreCase = true) -> null
                    trimmed.startsWith("QUIZ_QUESTION:", ignoreCase = true) -> {
                        trimmed.substringAfter(":").trim()
                    }
                    else -> line
                }
            }
            .joinToString("\n")
            .trim()
    }

    private fun extractQuizTextForStorage(reply: String): String {
        val lines = reply.lines()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }

        return lines.joinToString("\n").trim()
    }

    private fun extractCorrectLetter(reply: String): String? {
        val regex = Regex("(?im)^\\s*CORRECT\\s*:\\s*([A-D])\\b")
        return regex.find(reply)?.groupValues?.get(1)?.uppercase()
    }

    private fun extractUserAnswerLetter(text: String): String? {
        val regex = Regex("(?i)\\b([A-D])\\b")
        return regex.find(text)?.groupValues?.get(1)?.uppercase()
    }

    private fun findOptionText(question: String, letter: String): String? {
        val regex = Regex("(?im)^\\s*${Regex.escape(letter.uppercase())}\\)\\s*(.+)$")
        val option = regex.find(question)?.groupValues?.get(1)?.trim()
        return if (option.isNullOrBlank()) null else "${letter.uppercase()}) $option"
    }

    private fun savePendingQuizState(question: String, correctLetter: String) {
        pendingQuizQuestion = question
        pendingCorrectAnswer = correctLetter.uppercase()

        pendingQuizPrefs.edit()
            .putString("${username}_question", question)
            .putString("${username}_correct", correctLetter.uppercase())
            .apply()
    }

    private fun restorePendingQuizState() {
        pendingQuizQuestion = pendingQuizPrefs.getString("${username}_question", null)
        pendingCorrectAnswer = pendingQuizPrefs.getString("${username}_correct", null)
    }

    private fun clearPendingQuizState() {
        pendingQuizQuestion = null
        pendingCorrectAnswer = null

        pendingQuizPrefs.edit()
            .remove("${username}_question")
            .remove("${username}_correct")
            .apply()
    }

    private fun buildFriendlyErrorMessage(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("API key", ignoreCase = true) ->
                "Gemini API key is missing. Add GEMINI_API_KEY in local.properties and rebuild the app."
            msg.contains("404", ignoreCase = true) || msg.contains("not found", ignoreCase = true) ->
                "Gemini model was not found. Check the model name in ChatRepository.kt."
            msg.contains("429", ignoreCase = true) || msg.contains("quota", ignoreCase = true) ->
                "Gemini limit reached. Please wait and try again later."
            msg.contains("Unable to resolve host", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) ->
                "Network error. Check your internet connection and try again."
            else -> "Error: ${e.message}"
        }
    }

    private fun confirmClearHistory() {
        AlertDialog.Builder(this)
            .setTitle("Clear chat history?")
            .setMessage("This will permanently delete all chat messages for $username. Your quiz result history will stay saved.")
            .setPositiveButton("Clear") { _, _ ->
                lifecycleScope.launch {
                    repository.clearHistory(username)
                    messages.clear()
                    adapter.notifyDataSetChanged()
                    clearPendingQuizState()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            binding.rvMessages.scrollToPosition(messages.size - 1)
        }
    }
}
