package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EARNING,
    EXPENSE,
    LOAN_GIVEN,  // Money lent out (Receivable / reduces today's cash or account)
    LOAN_TAKEN   // Money borrowed (Payable / increases today's cash or account)
}

enum class PaymentMode {
    CASH,
    ACCOUNT
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
    val paymentMode: PaymentMode = PaymentMode.CASH, // Cash vs Account (Bank)
    val isSettled: Boolean = false, // Only meaningful for loans
    val linkedTransactionId: Long? = null, // ID of auto-generated settlement transaction or linked original loan ID
    val isSystemGenerated: Boolean = false, // True for auto-generated settlement transactions
    val note: String? = null,
    val receiptImageUri: String? = null, // Optional photo / receipt attachment Uri
    val createdAt: Long = System.currentTimeMillis()
)

val Transaction.isSettlementEntry: Boolean
    get() = isSystemGenerated ||
            (category in listOf("Loan Recovery", "Loan Repayment") && (linkedTransactionId != null || title.startsWith("Loan Recovered") || title.startsWith("Loan Repaid")))

