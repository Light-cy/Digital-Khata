package com.example.ui.screens.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Budget
import com.example.data.model.TransactionType
import com.example.data.repository.KhataRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryBudgetItem(
    val id: Long,
    val category: String,
    val monthlyLimit: Double,
    val spentThisMonth: Double,
    val percentage: Float
)

class BudgetsViewModel(private val repository: KhataRepository) : ViewModel() {

    private val currentMonthStart = DateUtils.getStartOfMonth(System.currentTimeMillis())
    private val currentMonthEnd = DateUtils.getEndOfMonth(System.currentTimeMillis())

    val budgetItems: StateFlow<List<CategoryBudgetItem>> = combine(
        repository.allBudgets,
        repository.getTransactionsByDateRange(currentMonthStart, currentMonthEnd)
    ) { budgets, monthTransactions ->
        val spentMap = mutableMapOf<String, Double>()
        for (tx in monthTransactions) {
            if (tx.type == TransactionType.EXPENSE && tx.category != null) {
                spentMap[tx.category] = (spentMap[tx.category] ?: 0.0) + tx.amount
            }
        }

        budgets.map { budget ->
            val spent = spentMap[budget.category] ?: 0.0
            val percentage = if (budget.monthlyLimit > 0) ((spent / budget.monthlyLimit) * 100).toFloat() else 0f
            CategoryBudgetItem(
                id = budget.id,
                category = budget.category,
                monthlyLimit = budget.monthlyLimit,
                spentThisMonth = spent,
                percentage = percentage
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveBudget(category: String, monthlyLimit: Double) {
        viewModelScope.launch {
            val budget = Budget(
                category = category,
                monthlyLimit = monthlyLimit
            )
            repository.insertBudget(budget)
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudgetById(id)
        }
    }
}
