package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Budget
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringTemplate
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.KhataRepository
import com.example.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailySummary(
    val totalEarning: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalLoanGiven: Double = 0.0,
    val totalLoanTaken: Double = 0.0,
    val netSavings: Double = 0.0
)

data class CategoryBudgetProgress(
    val category: String,
    val spentThisMonth: Double,
    val budgetLimit: Double,
    val percentage: Float
)

class HomeViewModel(private val repository: KhataRepository) : ViewModel() {

    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactionsForDay: StateFlow<List<Transaction>> = _selectedDateMillis
        .flatMapLatest { date ->
            val start = DateUtils.getStartOfDay(date)
            val end = DateUtils.getEndOfDay(date)
            repository.getTransactionsByDateRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySummary: StateFlow<DailySummary> = transactionsForDay
        .combine(_selectedDateMillis) { transactions, _ ->
            var earning = 0.0
            var expense = 0.0
            var loanGiven = 0.0
            var loanTaken = 0.0

            for (tx in transactions) {
                when (tx.type) {
                    TransactionType.EARNING -> earning += tx.amount
                    TransactionType.EXPENSE -> expense += tx.amount
                    TransactionType.LOAN_GIVEN -> loanGiven += tx.amount
                    TransactionType.LOAN_TAKEN -> loanTaken += tx.amount
                }
            }

            DailySummary(
                totalEarning = earning,
                totalExpense = expense,
                totalLoanGiven = loanGiven,
                totalLoanTaken = loanTaken,
                netSavings = earning - expense - loanGiven + loanTaken
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailySummary())

    // Month's budgets and spending
    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetProgressList: StateFlow<List<CategoryBudgetProgress>> = _selectedDateMillis
        .flatMapLatest { date ->
            val startOfMonth = DateUtils.getStartOfMonth(date)
            val endOfMonth = DateUtils.getEndOfMonth(date)
            combine(
                repository.getTransactionsByDateRange(startOfMonth, endOfMonth),
                repository.allBudgets
            ) { monthTransactions, budgets ->
                val spentMap = mutableMapOf<String, Double>()
                for (tx in monthTransactions) {
                    if (tx.type == TransactionType.EXPENSE && tx.category != null) {
                        spentMap[tx.category] = (spentMap[tx.category] ?: 0.0) + tx.amount
                    }
                }

                budgets.map { budget ->
                    val spent = spentMap[budget.category] ?: 0.0
                    val percentage = if (budget.monthlyLimit > 0) ((spent / budget.monthlyLimit) * 100).toFloat() else 0f
                    CategoryBudgetProgress(
                        category = budget.category,
                        spentThisMonth = spent,
                        budgetLimit = budget.monthlyLimit,
                        percentage = percentage
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedDate(timeMillis: Long) {
        _selectedDateMillis.value = timeMillis
    }

    fun previousDay() {
        _selectedDateMillis.value = DateUtils.addDays(_selectedDateMillis.value, -1)
    }

    fun nextDay() {
        _selectedDateMillis.value = DateUtils.addDays(_selectedDateMillis.value, 1)
    }

    fun goToToday() {
        _selectedDateMillis.value = System.currentTimeMillis()
    }

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        title: String,
        personName: String?,
        category: String?,
        note: String?,
        dateMillis: Long,
        makeRecurring: Boolean,
        recurringFrequency: RecurringFrequency,
        receiptImageUri: String? = null
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                date = dateMillis,
                type = type,
                amount = amount,
                title = title,
                personName = personName,
                category = category,
                note = note,
                receiptImageUri = receiptImageUri
            )
            repository.insertTransaction(transaction)

            if (makeRecurring && (type == TransactionType.EARNING || type == TransactionType.EXPENSE)) {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = dateMillis }
                val dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
                val template = RecurringTemplate(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    frequency = recurringFrequency,
                    dayOfMonth = dayOfMonth,
                    dayOfWeek = dayOfWeek,
                    lastGeneratedDate = dateMillis,
                    note = note
                )
                repository.insertRecurringTemplate(template)
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}
