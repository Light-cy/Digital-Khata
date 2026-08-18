package com.example.ui.screens.monthly

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Budget
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.KhataRepository
import com.example.ui.components.CategorySlice
import com.example.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class MonthlyTotals(
    val totalEarning: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalLoansGiven: Double = 0.0,
    val totalLoansTaken: Double = 0.0,
    val netSavings: Double = 0.0,
    val monthMillis: Long = 0L
)

data class CategoryBreakdownItem(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val budgetLimit: Double? = null,
    val color: Color
)

data class MonthWeekItem(
    val weekNumber: Int,
    val weekLabel: String,
    val earnings: Double,
    val expenses: Double,
    val netSavings: Double
)

class MonthlyViewModel(private val repository: KhataRepository) : ViewModel() {

    private val _currentMonthMillis = MutableStateFlow(System.currentTimeMillis())
    val currentMonthMillis: StateFlow<Long> = _currentMonthMillis.asStateFlow()

    private val categoryColors = listOf(
        Color(0xFFE11D48), // Rose
        Color(0xFFEA580C), // Orange
        Color(0xFFD97706), // Amber
        Color(0xFF059669), // Emerald
        Color(0xFF0284C7), // Light Blue
        Color(0xFF6366F1), // Indigo
        Color(0xFF9333EA), // Purple
        Color(0xFFDB2777), // Pink
        Color(0xFF475569)  // Slate
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthTransactions: StateFlow<List<Transaction>> = _currentMonthMillis
        .flatMapLatest { monthMillis ->
            val start = DateUtils.getStartOfMonth(monthMillis)
            val end = DateUtils.getEndOfMonth(monthMillis)
            repository.getTransactionsByDateRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyTotals: StateFlow<MonthlyTotals> = monthTransactions
        .combine(_currentMonthMillis) { transactions, monthMillis ->
            var earning = 0.0
            var expense = 0.0
            var loansGiven = 0.0
            var loansTaken = 0.0

            for (tx in transactions) {
                when (tx.type) {
                    TransactionType.EARNING -> earning += tx.amount
                    TransactionType.EXPENSE -> expense += tx.amount
                    TransactionType.LOAN_GIVEN -> loansGiven += tx.amount
                    TransactionType.LOAN_TAKEN -> loansTaken += tx.amount
                }
            }

            MonthlyTotals(
                totalEarning = earning,
                totalExpense = expense,
                totalLoansGiven = loansGiven,
                totalLoansTaken = loansTaken,
                netSavings = earning - expense - loansGiven + loansTaken,
                monthMillis = monthMillis
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyTotals())

    val categoryBreakdown: StateFlow<List<CategoryBreakdownItem>> = combine(
        monthTransactions,
        repository.allBudgets
    ) { transactions, budgets ->
        val budgetMap = budgets.associate { it.category to it.monthlyLimit }
        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpense = expenseTransactions.sumOf { it.amount }

        val grouped = expenseTransactions
            .groupBy { it.category ?: "Other" }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        grouped.mapIndexed { index, pair ->
            val category = pair.first
            val amount = pair.second
            val percentage = if (totalExpense > 0) ((amount / totalExpense) * 100).toFloat() else 0f
            val color = categoryColors[index % categoryColors.size]

            CategoryBreakdownItem(
                category = category,
                amount = amount,
                percentage = percentage,
                budgetLimit = budgetMap[category],
                color = color
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorySlices: StateFlow<List<CategorySlice>> = categoryBreakdown
        .map { items ->
            items.map {
                CategorySlice(
                    category = it.category,
                    amount = it.amount,
                    percentage = it.percentage,
                    color = it.color
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weekByWeekBreakdown: StateFlow<List<MonthWeekItem>> = monthTransactions
        .combine(_currentMonthMillis) { transactions, monthMillis ->
            val startOfMonth = DateUtils.getStartOfMonth(monthMillis)
            val endOfMonth = DateUtils.getEndOfMonth(monthMillis)
            val weeks = mutableListOf<MonthWeekItem>()

            var currentWeekStart = startOfMonth
            var weekIndex = 1

            while (currentWeekStart <= endOfMonth) {
                val currentWeekEnd = (DateUtils.getEndOfWeek(currentWeekStart)).coerceAtMost(endOfMonth)
                val weekTxs = transactions.filter { it.date in currentWeekStart..currentWeekEnd }

                val earn = weekTxs.filter { it.type == TransactionType.EARNING }.sumOf { it.amount }
                val exp = weekTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val lg = weekTxs.filter { it.type == TransactionType.LOAN_GIVEN }.sumOf { it.amount }
                val lt = weekTxs.filter { it.type == TransactionType.LOAN_TAKEN }.sumOf { it.amount }

                val startLabel = DateUtils.formatShortDate(currentWeekStart)
                val endLabel = DateUtils.formatShortDate(currentWeekEnd)

                weeks.add(
                    MonthWeekItem(
                        weekNumber = weekIndex,
                        weekLabel = "Week $weekIndex ($startLabel - $endLabel)",
                        earnings = earn,
                        expenses = exp,
                        netSavings = earn - exp - lg + lt
                    )
                )

                currentWeekStart = DateUtils.addDays(currentWeekEnd, 1)
                weekIndex++
            }
            weeks
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun previousMonth() {
        _currentMonthMillis.value = DateUtils.addMonths(_currentMonthMillis.value, -1)
    }

    fun nextMonth() {
        _currentMonthMillis.value = DateUtils.addMonths(_currentMonthMillis.value, 1)
    }

    fun currentMonth() {
        _currentMonthMillis.value = System.currentTimeMillis()
    }
}

private fun <T, R> StateFlow<T>.map(transform: (T) -> R): StateFlow<R> {
    val initial = transform(this.value)
    return MutableStateFlow(initial)
}
