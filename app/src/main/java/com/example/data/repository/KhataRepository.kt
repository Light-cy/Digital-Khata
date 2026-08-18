package com.example.data.repository

import android.content.Context
import com.example.data.local.BudgetDao
import com.example.data.local.RecurringTemplateDao
import com.example.data.local.TransactionDao
import com.example.data.model.Budget
import com.example.data.model.PaymentMode
import com.example.data.model.RecurringTemplate
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.widget.KhataWidgetProvider
import kotlinx.coroutines.flow.Flow

class KhataRepository(
    private val transactionDao: TransactionDao,
    private val recurringTemplateDao: RecurringTemplateDao,
    private val budgetDao: BudgetDao,
    private val context: Context? = null
) {
    // Transactions
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allLoans: Flow<List<Transaction>> = transactionDao.getAllLoans()

    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startTime, endTime)
    }

    fun getLoansByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.getLoansByType(type)
    }

    fun getUnsettledLoansByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.getUnsettledLoansByType(type)
    }

    fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(query)
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        val id = transactionDao.insert(transaction)
        notifyWidgetUpdate()
        return id
    }

    suspend fun insertTransactions(transactions: List<Transaction>): List<Long> {
        val ids = transactionDao.insertAll(transactions)
        notifyWidgetUpdate()
        return ids
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction)
        notifyWidgetUpdate()
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        // Delete linked settlement transaction if present
        transaction.linkedTransactionId?.let { linkedId ->
            transactionDao.deleteById(linkedId)
        }
        // If this transaction is a settlement transaction, reopen the parent loan
        transactionDao.findLoanByLinkedTransactionId(transaction.id)?.let { parentLoan ->
            transactionDao.update(parentLoan.copy(isSettled = false, linkedTransactionId = null))
        }
        transactionDao.delete(transaction)
        notifyWidgetUpdate()
    }

    suspend fun deleteTransactionById(id: Long) {
        val tx = transactionDao.getTransactionById(id)
        if (tx != null) {
            deleteTransaction(tx)
        } else {
            transactionDao.deleteById(id)
            notifyWidgetUpdate()
        }
    }

    private fun notifyWidgetUpdate() {
        context?.let { ctx ->
            try {
                KhataWidgetProvider.updateAllWidgets(ctx)
            } catch (e: Exception) {
                // Ignore in background tasks
            }
        }
    }

    suspend fun settleLoan(
        loan: Transaction,
        settlementDateMillis: Long = System.currentTimeMillis(),
        settlementPaymentMode: PaymentMode = loan.paymentMode
    ): Long {
        if (loan.isSettled && loan.linkedTransactionId != null) {
            return loan.linkedTransactionId
        }

        val isLoanGiven = loan.type == TransactionType.LOAN_GIVEN
        val person = loan.personName?.takeIf { it.isNotBlank() } ?: loan.title
        val settlementTitle = if (isLoanGiven) "Loan Recovered — $person" else "Loan Repaid — $person"
        val settlementType = if (isLoanGiven) TransactionType.EARNING else TransactionType.EXPENSE
        val settlementCategory = if (isLoanGiven) "Loan Recovery" else "Loan Repayment"

        val settlementTx = Transaction(
            date = settlementDateMillis,
            type = settlementType,
            amount = loan.amount,
            title = settlementTitle,
            personName = loan.personName,
            category = settlementCategory,
            paymentMode = settlementPaymentMode,
            isSystemGenerated = true,
            linkedTransactionId = loan.id,
            note = "Settlement for loan: ${loan.title}",
            createdAt = System.currentTimeMillis()
        )
        val settlementId = transactionDao.insert(settlementTx)
        val updatedLoan = loan.copy(isSettled = true, linkedTransactionId = settlementId)
        transactionDao.update(updatedLoan)
        notifyWidgetUpdate()
        return settlementId
    }

    suspend fun unsettleLoan(loan: Transaction) {
        if (!loan.isSettled) return
        loan.linkedTransactionId?.let { linkedId ->
            transactionDao.deleteById(linkedId)
        }
        val updatedLoan = loan.copy(isSettled = false, linkedTransactionId = null)
        transactionDao.update(updatedLoan)
        notifyWidgetUpdate()
    }

    suspend fun toggleLoanSettled(
        loan: Transaction,
        settlementDateMillis: Long = System.currentTimeMillis(),
        settlementPaymentMode: PaymentMode = loan.paymentMode
    ): Boolean {
        return if (loan.isSettled) {
            unsettleLoan(loan)
            false
        } else {
            settleLoan(loan, settlementDateMillis, settlementPaymentMode)
            true
        }
    }

    suspend fun setLoanSettled(id: Long, isSettled: Boolean) {
        val loan = transactionDao.getTransactionById(id) ?: return
        if (isSettled && !loan.isSettled) {
            settleLoan(loan)
        } else if (!isSettled && loan.isSettled) {
            unsettleLoan(loan)
        }
    }

    suspend fun getAllTransactionsSnapshot(): List<Transaction> {
        return transactionDao.getAllTransactionsSnapshot()
    }

    // Recurring Templates
    val allRecurringTemplates: Flow<List<RecurringTemplate>> = recurringTemplateDao.getAllTemplates()
    val activeRecurringTemplates: Flow<List<RecurringTemplate>> = recurringTemplateDao.getActiveTemplates()

    suspend fun getActiveTemplatesSnapshot(): List<RecurringTemplate> {
        return recurringTemplateDao.getActiveTemplatesSnapshot()
    }

    suspend fun insertRecurringTemplate(template: RecurringTemplate): Long {
        return recurringTemplateDao.insert(template)
    }

    suspend fun updateRecurringTemplate(template: RecurringTemplate) {
        recurringTemplateDao.update(template)
    }

    suspend fun deleteRecurringTemplate(template: RecurringTemplate) {
        recurringTemplateDao.delete(template)
    }

    suspend fun deleteRecurringTemplateById(id: Long) {
        recurringTemplateDao.deleteById(id)
    }

    suspend fun updateRecurringLastGenerated(id: Long, date: Long) {
        recurringTemplateDao.updateLastGenerated(id, date)
    }

    suspend fun setRecurringActive(id: Long, isActive: Boolean) {
        recurringTemplateDao.updateActiveStatus(id, isActive)
    }

    // Budgets
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()

    fun getBudgetForCategory(category: String): Flow<Budget?> {
        return budgetDao.getBudgetForCategory(category)
    }

    suspend fun getAllBudgetsSnapshot(): List<Budget> {
        return budgetDao.getAllBudgetsSnapshot()
    }

    suspend fun insertBudget(budget: Budget): Long {
        return budgetDao.insert(budget)
    }

    suspend fun insertOrUpdateBudget(budget: Budget): Long {
        return budgetDao.insert(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.delete(budget)
    }

    suspend fun deleteBudgetByCategory(category: String) {
        budgetDao.deleteByCategory(category)
    }

    suspend fun deleteBudgetById(id: Long) {
        val budgets = budgetDao.getAllBudgetsSnapshot()
        val target = budgets.find { it.id == id }
        if (target != null) {
            budgetDao.delete(target)
        }
    }

    // Database Reset / Clear
    suspend fun clearAllData() {
        transactionDao.deleteAll()
        recurringTemplateDao.deleteAll()
        budgetDao.deleteAll()
    }
}
