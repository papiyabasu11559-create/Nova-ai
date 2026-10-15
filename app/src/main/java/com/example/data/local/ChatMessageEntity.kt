package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String = "default_session",
    val role: String, // "user", "model", "system"
    val content: String,
    val toolCallName: String? = null,
    val toolCallArgs: String? = null,
    val toolResult: String? = null,
    val toolStatus: String? = null, // "SUCCESS", "FAILED", "DISPATCHED", "PENDING"
    val timestamp: Long = System.currentTimeMillis()
)
