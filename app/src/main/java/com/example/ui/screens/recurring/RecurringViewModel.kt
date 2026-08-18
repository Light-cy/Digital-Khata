package com.example.ui.screens.recurring

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringTemplate
import com.example.data.model.TransactionType
import com.example.data.repository.KhataRepository
import com.example.util.RecurringSyncWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecurringViewModel(private val repository: KhataRepository) : ViewModel() {

    val recurringTemplates: StateFlow<List<RecurringTemplate>> = repository.allRecurringTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTemplateActive(template: RecurringTemplate) {
        viewModelScope.launch {
            repository.updateRecurringTemplate(template.copy(isActive = !template.isActive))
        }
    }

    fun addTemplate(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String?,
        frequency: RecurringFrequency,
        dayOfMonth: Int,
        dayOfWeek: Int,
        note: String?
    ) {
        viewModelScope.launch {
            val template = RecurringTemplate(
                title = title,
                amount = amount,
                type = type,
                category = category,
                frequency = frequency,
                dayOfMonth = dayOfMonth,
                dayOfWeek = dayOfWeek,
                note = note,
                lastGeneratedDate = 0L,
                isActive = true
            )
            repository.insertRecurringTemplate(template)
        }
    }

    fun deleteTemplate(template: RecurringTemplate) {
        viewModelScope.launch {
            repository.deleteRecurringTemplate(template)
        }
    }

    fun syncDueEntries(context: Context, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = RecurringSyncWorker.processRecurringEntries(repository)
            onComplete(count)
        }
    }
}
