package com.example.ui.screens.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.KhataRepository
import com.example.ui.components.DayChartData
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

data class DaySummaryItem(
    val dateMillis: Long,
    val dayName: String,
    val dateDisplay: String,
    val earnings: Double,
    val expenses: Double,
    val loansGiven: Double,
    val loansTaken: Double,
    val netSavings: Double,
    val transactionCount: Int
)

data class WeeklySummaryTotals(
    val totalEarning: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalLoansGiven: Double = 0.0,
    val totalLoansTaken: Double = 0.0,
    val netSavings: Double = 0.0,
    val startOfWeekMillis: Long = 0L,
    val endOfWeekMillis: Long = 0L
)

class WeeklyViewModel(private val repository: KhataRepository) : ViewModel() {

    private val _currentWeekMillis = MutableStateFlow(System.currentTimeMillis())
    val currentWeekMillis: StateFlow<Long> = _currentWeekMillis.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val weekTransactions: StateFlow<List<Transaction>> = _currentWeekMillis
        .flatMapLatest { weekMillis ->
            val start = DateUtils.getStartOfWeek(weekMillis)
            val end = DateUtils.getEndOfWeek(weekMillis)
            repository.getTransactionsByDateRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyTotals: StateFlow<WeeklySummaryTotals> = weekTransactions
        .combine(_currentWeekMillis) { transactions, weekMillis ->
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

            val start = DateUtils.getStartOfWeek(weekMillis)
            val end = DateUtils.getEndOfWeek(weekMillis)

            WeeklySummaryTotals(
                totalEarning = earning,
                totalExpense = expense,
                totalLoansGiven = loansGiven,
                totalLoansTaken = loansTaken,
                netSavings = earning - expense - loansGiven + loansTaken,
                startOfWeekMillis = start,
                endOfWeekMillis = end
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklySummaryTotals())

    val dailyBreakdown: StateFlow<List<DaySummaryItem>> = weekTransactions
        .combine(_currentWeekMillis) { transactions, weekMillis ->
            val startOfWeek = DateUtils.getStartOfWeek(weekMillis)
            val days = mutableListOf<DaySummaryItem>()

            for (i in 0..6) {
                val dayMillis = DateUtils.addDays(startOfWeek, i)
                val dayStart = DateUtils.getStartOfDay(dayMillis)
                val dayEnd = DateUtils.getEndOfDay(dayMillis)

                val dayTxs = transactions.filter { it.date in dayStart..dayEnd }
                val earn = dayTxs.filter { it.type == TransactionType.EARNING }.sumOf { it.amount }
                val exp = dayTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val lg = dayTxs.filter { it.type == TransactionType.LOAN_GIVEN }.sumOf { it.amount }
                val lt = dayTxs.filter { it.type == TransactionType.LOAN_TAKEN }.sumOf { it.amount }

                days.add(
                    DaySummaryItem(
                        dateMillis = dayMillis,
                        dayName = DateUtils.formatDayOfWeek(dayMillis),
                        dateDisplay = DateUtils.formatShortDate(dayMillis),
                        earnings = earn,
                        expenses = exp,
                        loansGiven = lg,
                        loansTaken = lt,
                        netSavings = earn - exp - lg + lt,
                        transactionCount = dayTxs.size
                    )
                )
            }
            days
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chartData: StateFlow<List<DayChartData>> = dailyBreakdown
        .map { days ->
            days.map { day ->
                DayChartData(
                    dayLabel = day.dayName,
                    dateLabel = DateUtils.formatDayNumber(day.dateMillis),
                    earnings = day.earnings,
                    expenses = day.expenses,
                    isSelected = DateUtils.isSameDay(day.dateMillis, System.currentTimeMillis())
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun previousWeek() {
        _currentWeekMillis.value = DateUtils.addWeeks(_currentWeekMillis.value, -1)
    }

    fun nextWeek() {
        _currentWeekMillis.value = DateUtils.addWeeks(_currentWeekMillis.value, 1)
    }

    fun currentWeek() {
        _currentWeekMillis.value = System.currentTimeMillis()
    }
}

private fun <T, R> StateFlow<T>.map(transform: (T) -> R): StateFlow<R> {
    val initial = transform(this.value)
    val state = MutableStateFlow(initial)
    // Map helper for StateFlow transformations
    return state
}
