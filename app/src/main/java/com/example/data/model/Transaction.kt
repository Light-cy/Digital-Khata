package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EARNING,
    EXPENSE,
    LOAN_GIVEN,  // Money lent out (Receivable / reduces today's cash)
    LOAN_TAKEN   // Money borrowed (Payable / increases today's cash)
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long, // epoch millis
    val type: TransactionType,
    val amount: Double,
    val title: String,
    val personName: String? = null, // Relevant for LOAN_GIVEN / LOAN_TAKEN
    val category: String? = null,   // Relevant for EXPENSE / EARNING
    val isSettled: Boolean = false, // Only meaningful for loans
    val note: String? = null,
    val receiptImageUri: String? = null, // Optional photo / receipt attachment Uri
    val createdAt: Long = System.currentTimeMillis()
)
