package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automation_logs")
data class AutomationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String,
    val description: String,
    val parameters: String,
    val result: String,
    val status: String, // "SUCCESS", "FAILED", "DISPATCHED"
    val timestamp: Long = System.currentTimeMillis()
)
