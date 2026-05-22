package com.example.llmchatbot.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.llmchatbot.data.Message
import com.example.llmchatbot.databinding.ItemMessageBotBinding
import com.example.llmchatbot.databinding.ItemMessageUserBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter that renders two bubble types:
 *   - VIEW_TYPE_USER  -> right-aligned grey bubble, with a circular "U" avatar.
 *   - VIEW_TYPE_BOT   -> left-aligned grey bubble, with a bot avatar.
 *
 * Each bubble shows a HH:mm timestamp underneath, satisfying the "timestamps on
 * each message bubble" requirement from the task brief.
 */
class MessageAdapter(
    private val messages: MutableList<Message>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun getItemViewType(position: Int): Int =
        if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_BOT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            UserHolder(ItemMessageUserBinding.inflate(inflater, parent, false))
        } else {
            BotHolder(ItemMessageBotBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        val timeStr = timeFormat.format(Date(msg.timestamp))
        when (holder) {
            is UserHolder -> {
                holder.binding.tvContent.text = msg.content
                holder.binding.tvTime.text = timeStr
            }
            is BotHolder -> {
                holder.binding.tvContent.text = msg.content
                holder.binding.tvTime.text = timeStr
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    /** Append a new message to the list and notify the adapter. */
    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    /** Replace the entire list (used when loading history on activity start). */
    fun setMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    class UserHolder(val binding: ItemMessageUserBinding) : RecyclerView.ViewHolder(binding.root)
    class BotHolder(val binding: ItemMessageBotBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
    }
}
