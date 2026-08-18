package com.example.ui.screens.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.KhataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoanTotals(
    val totalReceivable: Double = 0.0,
    val totalPayable: Double = 0.0,
    val unsettledGivenCount: Int = 0,
    val unsettledTakenCount: Int = 0
)

class LoansViewModel(private val repository: KhataRepository) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0) // 0: Loans Given (Receivable), 1: Loans Taken (Payable)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val allLoans: StateFlow<List<Transaction>> = repository.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loanTotals: StateFlow<LoanTotals> = allLoans
        .combine(_selectedTab) { loans, _ ->
            val unsettledGiven = loans.filter { it.type == TransactionType.LOAN_GIVEN && !it.isSettled }
            val unsettledTaken = loans.filter { it.type == TransactionType.LOAN_TAKEN && !it.isSettled }

            LoanTotals(
                totalReceivable = unsettledGiven.sumOf { it.amount },
                totalPayable = unsettledTaken.sumOf { it.amount },
                unsettledGivenCount = unsettledGiven.size,
                unsettledTakenCount = unsettledTaken.size
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LoanTotals())

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun toggleSettledStatus(transaction: Transaction) {
        viewModelScope.launch {
            repository.setLoanSettled(transaction.id, !transaction.isSettled)
        }
    }

    fun addLoan(
        type: TransactionType,
        amount: Double,
        title: String,
        personName: String,
        note: String?,
        dateMillis: Long,
        receiptImageUri: String? = null
    ) {
        viewModelScope.launch {
            val loan = Transaction(
                date = dateMillis,
                type = type,
                amount = amount,
                title = title,
                personName = personName,
                note = note,
                isSettled = false,
                receiptImageUri = receiptImageUri
            )
            repository.insertTransaction(loan)
        }
    }

    fun updateLoan(loan: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(loan)
        }
    }

    fun deleteLoan(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}
