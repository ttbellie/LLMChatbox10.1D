package com.example.llmchatbot.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.llmchatbot.R
import com.example.llmchatbot.data.AppDatabase
import com.example.llmchatbot.databinding.ActivityProfileBinding
import com.example.llmchatbot.network.ChatRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Profile screen displaying user stats, upgrade option, AI summary,
 * and QR-code based profile sharing.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        username = intent.getStringExtra(LoginActivity.EXTRA_USERNAME) ?: "User"

        binding.tvUsername.text = username
        binding.tvEmail.text = "$username@example.com"

        binding.btnBack.setOnClickListener { finish() }

        binding.btnShare.setOnClickListener {
            showQrProfileDialog()
        }

        binding.btnSummarizeAi.setOnClickListener {
            summarizeWithAi()
        }

        binding.btnUpgrade.setOnClickListener {
            startActivity(Intent(this, UpgradeActivity::class.java).apply {
                putExtra(LoginActivity.EXTRA_USERNAME, username)
            })
        }

        binding.bottomNav.selectedItemId = R.id.nav_profile
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chat -> {
                    finish()
                    false
                }

                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java).apply {
                        putExtra(LoginActivity.EXTRA_USERNAME, username)
                    })
                    finish()
                    false
                }

                R.id.nav_profile -> true
                else -> false
            }
        }

        loadStats()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_profile
        loadStats()
    }

    private fun loadStats() {
        val db = AppDatabase.getInstance(this)

        lifecycleScope.launch {
            val quizDao = db.quizAttemptDao()

            val total = quizDao.getTotalCount(username)
            val correct = quizDao.getCorrectCount(username)
            val incorrect = quizDao.getIncorrectCount(username)

            binding.tvTotalQuestions.text = total.toString()
            binding.tvCorrectAnswers.text = correct.toString()
            binding.tvIncorrectAnswers.text = incorrect.toString()

            val profile = db.userProfileDao().getProfile(username)
            if (profile != null && profile.tier != "Free") {
                binding.tvNotification.text = "⭐ ${profile.tier} Plan Active"
            } else {
                binding.tvNotification.text = "🔔 Upgrade to unlock improved quiz generation"
            }
        }
    }

    /**
     * Shows the generated QR code first, then lets the user share it as an image.
     * This makes the QR code feature visible during the demo.
     */
    private fun showQrProfileDialog() {
        lifecycleScope.launch {
            val profileData = buildProfileShareData()
            val qrBitmap = generateQrCode(profileData)

            val container = LinearLayout(this@ProfileActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(40, 30, 40, 20)
            }

            val title = TextView(this@ProfileActivity).apply {
                text = "Scan this QR code to view profile data"
                textSize = 16f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 20)
            }

            val qrImage = ImageView(this@ProfileActivity).apply {
                setImageBitmap(qrBitmap)
                adjustViewBounds = true
                layoutParams = LinearLayout.LayoutParams(650, 650)
            }

            val details = TextView(this@ProfileActivity).apply {
                text = profileData
                textSize = 13f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.CENTER
                setPadding(0, 20, 0, 0)
            }

            container.addView(title)
            container.addView(qrImage)
            container.addView(details)

            AlertDialog.Builder(this@ProfileActivity)
                .setTitle("Share Profile QR")
                .setView(container)
                .setPositiveButton("Share QR") { _, _ ->
                    shareQrImage(qrBitmap)
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    private suspend fun buildProfileShareData(): String {
        val db = AppDatabase.getInstance(this)
        val quizDao = db.quizAttemptDao()

        val total = quizDao.getTotalCount(username)
        val correct = quizDao.getCorrectCount(username)
        val incorrect = quizDao.getIncorrectCount(username)

        val tier = db.userProfileDao().getProfile(username)?.tier ?: "Free"

        return """
            LLM ChatBot Profile
            Username: $username
            Plan: $tier
            Total Questions: $total
            Correct Answers: $correct
            Incorrect Answers: $incorrect
        """.trimIndent()
    }

    private fun shareQrImage(qrBitmap: Bitmap) {
        try {
            val qrFile = File(cacheDir, "profile_qr_$username.png")

            FileOutputStream(qrFile).use { out ->
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                qrFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, "LLM ChatBot Profile QR")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Profile QR"))
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to share QR code: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun generateQrCode(data: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 800, 800)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                )
            }
        }

        return bitmap
    }

    private fun summarizeWithAi() {
        Toast.makeText(
            this,
            "Querying AI for summary of incorrect answers...",
            Toast.LENGTH_SHORT
        ).show()

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@ProfileActivity)
                val attempts = db.quizAttemptDao().getAttemptsForUser(username)
                val incorrectAttempts = attempts.filter { !it.isCorrect }

                if (incorrectAttempts.isEmpty()) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "No incorrect answers to summarize!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val summaryPrompt = "Summarize these incorrect quiz answers and suggest areas to improve:\n" +
                    incorrectAttempts.joinToString("\n") {
                        "Q: ${it.question} | User answered: ${it.userAnswer} | Correct: ${it.correctAnswer}"
                    }

                val chatRepo = ChatRepository(db.messageDao(), db.quizAttemptDao())
                val summary = chatRepo.sendMessage(emptyList(), summaryPrompt)

                AlertDialog.Builder(this@ProfileActivity)
                    .setTitle("AI Summary")
                    .setMessage(summary)
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
