package com.example.data.repository

import android.content.Context
import com.example.data.local.BudgetDao
import com.example.data.local.RecurringTemplateDao
import com.example.data.local.TransactionDao
import com.example.data.model.Budget
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
        transactionDao.delete(transaction)
        notifyWidgetUpdate()
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteById(id)
        notifyWidgetUpdate()
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

    suspend fun setLoanSettled(id: Long, isSettled: Boolean) {
        transactionDao.updateSettledStatus(id, isSettled)
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
