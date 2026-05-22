package com.example.llmchatbot.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.llmchatbot.R
import com.example.llmchatbot.data.AppDatabase
import com.example.llmchatbot.data.QuizAttempt
import com.example.llmchatbot.databinding.ActivityHistoryBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * History screen showing saved quiz attempts from Room.
 * The list reloads in onResume so new answers appear immediately after returning to this screen.
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        username = intent.getStringExtra(LoginActivity.EXTRA_USERNAME) ?: "User"

        binding.btnBack.setOnClickListener { finish() }
        binding.rvHistory.layoutManager = LinearLayoutManager(this)

        binding.bottomNav.selectedItemId = R.id.nav_history
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chat -> {
                    finish()
                    false
                }
                R.id.nav_history -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        putExtra(LoginActivity.EXTRA_USERNAME, username)
                    })
                    finish()
                    false
                }
                else -> false
            }
        }

        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_history
        loadHistory()
    }

    private fun loadHistory() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val attempts = db.quizAttemptDao().getAttemptsForUser(username)

            if (attempts.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvHistory.visibility = View.GONE
                binding.rvHistory.adapter = null
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvHistory.visibility = View.VISIBLE
                binding.rvHistory.adapter = HistoryAdapter(attempts)
            }
        }
    }
}

/**
 * Adapter for displaying expandable quiz history items.
 */
class HistoryAdapter(
    private val attempts: List<QuizAttempt>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumber: TextView = view.findViewById(R.id.tvNumber)
        val tvQuestion: TextView = view.findViewById(R.id.tvQuestion)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val ivExpand: ImageView = view.findViewById(R.id.ivExpand)
        val expandableLayout: LinearLayout = view.findViewById(R.id.expandableLayout)
        val tvUserAnswer: TextView = view.findViewById(R.id.tvUserAnswer)
        val tvUserAnswerLabel: TextView = view.findViewById(R.id.tvUserAnswerLabel)
        val tvCorrectAnswer: TextView = view.findViewById(R.id.tvCorrectAnswer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attempt = attempts[position]
        val num = position + 1
        val status = if (attempt.isCorrect) "Correct" else "Incorrect"
        val dateText = dateFormat.format(Date(attempt.timestamp))

        holder.tvNumber.text = "$num."
        holder.tvQuestion.text = attempt.question
        holder.tvCategory.text = "${attempt.category} • $status • $dateText"

        holder.tvUserAnswer.text = attempt.userAnswer
        if (attempt.isCorrect) {
            holder.tvUserAnswerLabel.text = "✅ Your Answer"
            holder.tvUserAnswerLabel.setTextColor(0xFF4CAF50.toInt())
        } else {
            holder.tvUserAnswerLabel.text = "❌ Your Answer"
            holder.tvUserAnswerLabel.setTextColor(0xFFF44336.toInt())
        }

        holder.tvCorrectAnswer.text = attempt.correctAnswer

        val isExpanded = expandedPositions.contains(position)
        holder.expandableLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.ivExpand.rotation = if (isExpanded) 180f else 0f

        holder.itemView.setOnClickListener {
            if (expandedPositions.contains(position)) {
                expandedPositions.remove(position)
            } else {
                expandedPositions.add(position)
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = attempts.size
}
