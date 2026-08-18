package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Budget
import com.example.data.model.PaymentMode
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringTemplate
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.KhataRepository
import com.example.util.DateUtils
import com.example.util.StartingBalanceManager
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
    val openingBalance: Double = 0.0,
    val totalEarning: Double = 0.0,
    val totalExpense: Double = 0.0,
    val dailyNetSavings: Double = 0.0,
    val runningBalance: Double = 0.0,
    // Cash vs Account breakdown for the day
    val cashOpening: Double = 0.0,
    val cashRunning: Double = 0.0,
    val cashDayEarning: Double = 0.0,
    val cashDayExpense: Double = 0.0,
    val cashDayNet: Double = 0.0,
    val accountOpening: Double = 0.0,
    val accountRunning: Double = 0.0,
    val accountDayEarning: Double = 0.0,
    val accountDayExpense: Double = 0.0,
    val accountDayNet: Double = 0.0,
    // Loans
    val totalLoanGiven: Double = 0.0,
    val totalLoanTaken: Double = 0.0,
    val unsettledLoanGiven: Double = 0.0,
    val unsettledLoanTaken: Double = 0.0,
    val allTimeReceivables: Double = 0.0,
    val allTimePayables: Double = 0.0
)

data class BalanceOverview(
    val cashBalance: Double = 0.0,
    val accountBalance: Double = 0.0,
    val totalNetBalance: Double = 0.0,
    val cashStarting: Double = 0.0,
    val accountStarting: Double = 0.0,
    val totalStarting: Double = 0.0,
    val cashTotalEarning: Double = 0.0,
    val cashTotalExpense: Double = 0.0,
    val accountTotalEarning: Double = 0.0,
    val accountTotalExpense: Double = 0.0
)

data class CategoryBudgetProgress(
    val category: String,
    val spentThisMonth: Double,
    val budgetLimit: Double,
    val percentage: Float
)

class HomeViewModel(
    private val repository: KhataRepository,
    private val startingBalanceManager: StartingBalanceManager? = null
) : ViewModel() {

    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    val lastUsedPaymentMode: StateFlow<PaymentMode> = startingBalanceManager?.lastUsedPaymentMode
        ?: MutableStateFlow(PaymentMode.CASH).asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactionsForDay: StateFlow<List<Transaction>> = _selectedDateMillis
        .flatMapLatest { date ->
            val start = DateUtils.getStartOfDay(date)
            val end = DateUtils.getEndOfDay(date)
            repository.getTransactionsByDateRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All-time balance overview (Cash vs Account vs Net Total)
    val balanceOverview: StateFlow<BalanceOverview> = combine(
        repository.allTransactions,
        startingBalanceManager?.cashStartingBalance ?: MutableStateFlow(0.0),
        startingBalanceManager?.accountStartingBalance ?: MutableStateFlow(0.0)
    ) { allTransactions, cashStarting, accountStarting ->
        var cashEarn = 0.0
        var cashExp = 0.0
        var cashLoanGiven = 0.0
        var cashLoanTaken = 0.0

        var acctEarn = 0.0
        var acctExp = 0.0
        var acctLoanGiven = 0.0
        var acctLoanTaken = 0.0

        for (tx in allTransactions) {
            when (tx.paymentMode) {
                PaymentMode.CASH -> {
                    when (tx.type) {
                        TransactionType.EARNING -> cashEarn += tx.amount
                        TransactionType.EXPENSE -> cashExp += tx.amount
                        TransactionType.LOAN_GIVEN -> cashLoanGiven += tx.amount
                        TransactionType.LOAN_TAKEN -> cashLoanTaken += tx.amount
                    }
                }
                PaymentMode.ACCOUNT -> {
                    when (tx.type) {
                        TransactionType.EARNING -> acctEarn += tx.amount
                        TransactionType.EXPENSE -> acctExp += tx.amount
                        TransactionType.LOAN_GIVEN -> acctLoanGiven += tx.amount
                        TransactionType.LOAN_TAKEN -> acctLoanTaken += tx.amount
                    }
                }
            }
        }

        val finalCash = cashStarting + cashEarn - cashExp - cashLoanGiven + cashLoanTaken
        val finalAccount = accountStarting + acctEarn - acctExp - acctLoanGiven + acctLoanTaken
        val totalNet = finalCash + finalAccount

        BalanceOverview(
            cashBalance = finalCash,
            accountBalance = finalAccount,
            totalNetBalance = totalNet,
            cashStarting = cashStarting,
            accountStarting = accountStarting,
            totalStarting = cashStarting + accountStarting,
            cashTotalEarning = cashEarn,
            cashTotalExpense = cashExp,
            accountTotalEarning = acctEarn,
            accountTotalExpense = acctExp
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BalanceOverview())

    val dailySummary: StateFlow<DailySummary> = combine(
        repository.allTransactions,
        _selectedDateMillis,
        startingBalanceManager?.cashStartingBalance ?: MutableStateFlow(0.0),
        startingBalanceManager?.accountStartingBalance ?: MutableStateFlow(0.0)
    ) { allTransactions, selectedDate, cashStarting, accountStarting ->
        val startOfDay = DateUtils.getStartOfDay(selectedDate)
        val endOfDay = DateUtils.getEndOfDay(selectedDate)

        // Historical prior transactions
        val priorTxs = allTransactions.filter { it.date < startOfDay }

        var priorCashEarn = 0.0
        var priorCashExp = 0.0
        var priorCashLg = 0.0
        var priorCashLt = 0.0

        var priorAcctEarn = 0.0
        var priorAcctExp = 0.0
        var priorAcctLg = 0.0
        var priorAcctLt = 0.0

        for (tx in priorTxs) {
            when (tx.paymentMode) {
                PaymentMode.CASH -> {
                    when (tx.type) {
                        TransactionType.EARNING -> priorCashEarn += tx.amount
                        TransactionType.EXPENSE -> priorCashExp += tx.amount
                        TransactionType.LOAN_GIVEN -> priorCashLg += tx.amount
                        TransactionType.LOAN_TAKEN -> priorCashLt += tx.amount
                    }
                }
                PaymentMode.ACCOUNT -> {
                    when (tx.type) {
                        TransactionType.EARNING -> priorAcctEarn += tx.amount
                        TransactionType.EXPENSE -> priorAcctExp += tx.amount
                        TransactionType.LOAN_GIVEN -> priorAcctLg += tx.amount
                        TransactionType.LOAN_TAKEN -> priorAcctLt += tx.amount
                    }
                }
            }
        }

        val cashOpening = cashStarting + priorCashEarn - priorCashExp - priorCashLg + priorCashLt
        val accountOpening = accountStarting + priorAcctEarn - priorAcctExp - priorAcctLg + priorAcctLt
        val totalOpening = cashOpening + accountOpening

        // Today's Transactions
        val dayTxs = allTransactions.filter { it.date in startOfDay..endOfDay }

        var cashDayEarn = 0.0
        var cashDayExp = 0.0
        var cashDayLg = 0.0
        var cashDayLt = 0.0

        var acctDayEarn = 0.0
        var acctDayExp = 0.0
        var acctDayLg = 0.0
        var acctDayLt = 0.0

        for (tx in dayTxs) {
            when (tx.paymentMode) {
                PaymentMode.CASH -> {
                    when (tx.type) {
                        TransactionType.EARNING -> cashDayEarn += tx.amount
                        TransactionType.EXPENSE -> cashDayExp += tx.amount
                        TransactionType.LOAN_GIVEN -> cashDayLg += tx.amount
                        TransactionType.LOAN_TAKEN -> cashDayLt += tx.amount
                    }
                }
                PaymentMode.ACCOUNT -> {
                    when (tx.type) {
                        TransactionType.EARNING -> acctDayEarn += tx.amount
                        TransactionType.EXPENSE -> acctDayExp += tx.amount
                        TransactionType.LOAN_GIVEN -> acctDayLg += tx.amount
                        TransactionType.LOAN_TAKEN -> acctDayLt += tx.amount
                    }
                }
            }
        }

        val cashDayNet = cashDayEarn - cashDayExp - cashDayLg + cashDayLt
        val accountDayNet = acctDayEarn - acctDayExp - acctDayLg + acctDayLt

        val totalDayEarn = cashDayEarn + acctDayEarn
        val totalDayExp = cashDayExp + acctDayExp
        val totalDayLg = cashDayLg + acctDayLg
        val totalDayLt = cashDayLt + acctDayLt

        val totalDayNet = totalDayEarn - totalDayExp - totalDayLg + totalDayLt
        val running = totalOpening + totalDayNet

        val cashRunning = cashOpening + cashDayNet
        val accountRunning = accountOpening + accountDayNet

        val unsettledGiven = dayTxs.filter { it.type == TransactionType.LOAN_GIVEN && !it.isSettled }.sumOf { it.amount }
        val unsettledTaken = dayTxs.filter { it.type == TransactionType.LOAN_TAKEN && !it.isSettled }.sumOf { it.amount }

        val allTimeReceivables = allTransactions.filter { it.type == TransactionType.LOAN_GIVEN && !it.isSettled }.sumOf { it.amount }
        val allTimePayables = allTransactions.filter { it.type == TransactionType.LOAN_TAKEN && !it.isSettled }.sumOf { it.amount }

        DailySummary(
            openingBalance = totalOpening,
            totalEarning = totalDayEarn,
            totalExpense = totalDayExp,
            dailyNetSavings = totalDayNet,
            runningBalance = running,
            cashOpening = cashOpening,
            cashRunning = cashRunning,
            cashDayEarning = cashDayEarn,
            cashDayExpense = cashDayExp,
            cashDayNet = cashDayNet,
            accountOpening = accountOpening,
            accountRunning = accountRunning,
            accountDayEarning = acctDayEarn,
            accountDayExpense = acctDayExp,
            accountDayNet = accountDayNet,
            totalLoanGiven = totalDayLg,
            totalLoanTaken = totalDayLt,
            unsettledLoanGiven = unsettledGiven,
            unsettledLoanTaken = unsettledTaken,
            allTimeReceivables = allTimeReceivables,
            allTimePayables = allTimePayables
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailySummary())

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
        paymentMode: PaymentMode = PaymentMode.CASH,
        makeRecurring: Boolean = false,
        recurringFrequency: RecurringFrequency = RecurringFrequency.MONTHLY,
        receiptImageUri: String? = null
    ) {
        viewModelScope.launch {
            startingBalanceManager?.setLastUsedPaymentMode(paymentMode)
            val transaction = Transaction(
                date = dateMillis,
                type = type,
                amount = amount,
                title = title,
                personName = personName,
                category = category,
                paymentMode = paymentMode,
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
            startingBalanceManager?.setLastUsedPaymentMode(transaction.paymentMode)
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun restoreTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    fun toggleLoanSettled(
        loan: Transaction,
        settlementPaymentMode: PaymentMode = loan.paymentMode,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val isNowSettled = repository.toggleLoanSettled(
                loan = loan,
                settlementDateMillis = System.currentTimeMillis(),
                settlementPaymentMode = settlementPaymentMode
            )
            onComplete?.invoke(isNowSettled)
        }
    }

    fun setStartingBalances(cash: Double, account: Double) {
        startingBalanceManager?.setStartingBalances(cash, account)
    }
}
