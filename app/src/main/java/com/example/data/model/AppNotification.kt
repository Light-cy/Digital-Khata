package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NotificationType {
    DAILY_REMINDER,
    BUDGET_ALERT,
    LOAN_REMINDER,
    BACKUP_REMINDER,
    RECURRING_ADDED
}

@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val relatedScreen: String? = null,
    val relatedId: Long? = null
)
