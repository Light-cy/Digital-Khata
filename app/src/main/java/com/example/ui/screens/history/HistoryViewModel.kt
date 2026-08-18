package com.example.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PaymentMode
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

class HistoryViewModel(private val repository: KhataRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedTypeFilter: StateFlow<TransactionType?> = _selectedTypeFilter.asStateFlow()

    private val _selectedPaymentModeFilter = MutableStateFlow<PaymentMode?>(null)
    val selectedPaymentModeFilter: StateFlow<PaymentMode?> = _selectedPaymentModeFilter.asStateFlow()

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        repository.allTransactions,
        _searchQuery,
        _selectedTypeFilter,
        _selectedPaymentModeFilter
    ) { transactions, query, typeFilter, modeFilter ->
        transactions.filter { tx ->
            val matchesType = typeFilter == null || tx.type == typeFilter
            val matchesMode = modeFilter == null || tx.paymentMode == modeFilter
            val matchesQuery = query.isBlank() ||
                    tx.title.contains(query, ignoreCase = true) ||
                    (tx.personName?.contains(query, ignoreCase = true) == true) ||
                    (tx.category?.contains(query, ignoreCase = true) == true) ||
                    (tx.note?.contains(query, ignoreCase = true) == true)
            matchesType && matchesMode && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun setPaymentModeFilter(mode: PaymentMode?) {
        _selectedPaymentModeFilter.value = mode
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
