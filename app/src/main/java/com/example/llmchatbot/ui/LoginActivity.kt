package com.example.llmchatbot.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.llmchatbot.databinding.ActivityLoginBinding

/**
 * First screen of the app. The user enters a username and taps "Go" to enter the chat.
 * The username is forwarded to ChatActivity via Intent extra and is also used as the
 * partition key for chat history rows in Room.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGo.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra(EXTRA_USERNAME, username)
            }
            startActivity(intent)
        }
    }

    companion object {
        const val EXTRA_USERNAME = "extra_username"
    }
}
