package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.TransactionType
import com.example.ui.theme.EarningGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.LoanGivenAmber
import com.example.ui.theme.LoanTakenBlue

object CategoryIconHelper {
    val expenseCategories = listOf(
        "Food",
        "Transport",
        "Bills",
        "Rent",
        "Shopping",
        "Healthcare",
        "Entertainment",
        "Education",
        "Other"
    )

    val earningCategories = listOf(
        "Salary",
        "Freelance",
        "Business",
        "Investment",
        "Gift",
        "Other"
    )

    fun getCategoryIcon(category: String?): ImageVector {
        return when (category?.lowercase()?.trim()) {
            "food", "groceries", "dining" -> Icons.Default.Fastfood
            "transport", "travel", "fuel" -> Icons.Default.DirectionsCar
            "bills", "utilities", "electricity" -> Icons.Default.ReceiptLong
            "rent", "housing" -> Icons.Default.Home
            "shopping", "clothes" -> Icons.Default.ShoppingBag
            "healthcare", "medical", "doctor" -> Icons.Default.LocalHospital
            "entertainment", "movies", "games" -> Icons.Default.Movie
            "education", "books", "tuition" -> Icons.Default.School
            "salary" -> Icons.Default.AttachMoney
            "freelance" -> Icons.Default.Work
            "business" -> Icons.Default.Business
            "investment" -> Icons.Default.TrendingUp
            "gift" -> Icons.Default.CardGiftcard
            else -> Icons.Default.Category
        }
    }

    fun getTypeColor(type: TransactionType): Color {
        return when (type) {
            TransactionType.EARNING -> EarningGreen
            TransactionType.EXPENSE -> ExpenseRed
            TransactionType.LOAN_GIVEN -> LoanGivenAmber
            TransactionType.LOAN_TAKEN -> LoanTakenBlue
        }
    }

    fun getTypeLabel(type: TransactionType): String {
        return when (type) {
            TransactionType.EARNING -> "Earning"
            TransactionType.EXPENSE -> "Expense"
            TransactionType.LOAN_GIVEN -> "Receivable"
            TransactionType.LOAN_TAKEN -> "Payable"
        }
    }
}
