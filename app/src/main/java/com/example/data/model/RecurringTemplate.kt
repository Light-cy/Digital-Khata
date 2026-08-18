package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    MONTHLY
}

@Entity(tableName = "recurring_templates")
data class RecurringTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType, // EARNING or EXPENSE
    val category: String? = null,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val dayOfMonth: Int = 1, // 1-31
    val dayOfWeek: Int = 1,  // 1 (Monday) to 7 (Sunday)
    val isActive: Boolean = true,
    val lastGeneratedDate: Long? = null, // epoch millis
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
