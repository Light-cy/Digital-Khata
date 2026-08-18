package com.example.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.AddEditTransactionSheet
import com.example.ui.components.AppTopHeader
import com.example.ui.components.CategoryIconHelper
import com.example.ui.screens.home.TransactionItemRow
import com.example.ui.theme.EarningGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.LoanGivenAmber
import com.example.ui.theme.LoanTakenBlue
import com.example.ui.components.ReceiptViewerDialog
import com.example.util.CurrencyUtils
import com.example.util.NotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    notificationHelper: NotificationHelper? = null,
    unreadNotificationsCount: Int = 0,
    onNavigateToSettings: (() -> Unit)? = null,
    onNotificationClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val isReminderActive = notificationHelper?.isDailyReminderEnabled() ?: false

    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    var viewingReceiptTransaction by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // App Top Header
            AppTopHeader(
                title = "All Transactions",
                subtitle = "${transactions.size} entries found",
                onBack = onBack,
                isReminderActive = isReminderActive,
                unreadNotificationsCount = unreadNotificationsCount,
                onNavigateToSettings = onNavigateToSettings,
                onNotificationClick = onNotificationClick ?: onNavigateToSettings
            )

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by title, person, note, category...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // Type Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTypeFilter == null,
                    onClick = { viewModel.setTypeFilter(null) },
                    label = { Text("All Types") }
                )

                FilterChip(
                    selected = selectedTypeFilter == TransactionType.EARNING,
                    onClick = { viewModel.setTypeFilter(if (selectedTypeFilter == TransactionType.EARNING) null else TransactionType.EARNING) },
                    label = { Text("Earnings") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EarningGreen.copy(alpha = 0.15f),
                        selectedLabelColor = EarningGreen
                    )
                )

                FilterChip(
                    selected = selectedTypeFilter == TransactionType.EXPENSE,
                    onClick = { viewModel.setTypeFilter(if (selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE) },
                    label = { Text("Expenses") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ExpenseRed.copy(alpha = 0.15f),
                        selectedLabelColor = ExpenseRed
                    )
                )

                FilterChip(
                    selected = selectedTypeFilter == TransactionType.LOAN_GIVEN,
                    onClick = { viewModel.setTypeFilter(if (selectedTypeFilter == TransactionType.LOAN_GIVEN) null else TransactionType.LOAN_GIVEN) },
                    label = { Text("Receivables") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LoanGivenAmber.copy(alpha = 0.15f),
                        selectedLabelColor = LoanGivenAmber
                    )
                )

                FilterChip(
                    selected = selectedTypeFilter == TransactionType.LOAN_TAKEN,
                    onClick = { viewModel.setTypeFilter(if (selectedTypeFilter == TransactionType.LOAN_TAKEN) null else TransactionType.LOAN_TAKEN) },
                    label = { Text("Payables") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LoanTakenBlue.copy(alpha = 0.15f),
                        selectedLabelColor = LoanTakenBlue
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Transactions List
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Transactions Found",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionItemRow(
                            transaction = transaction,
                            onClick = {
                                editingTransaction = transaction
                                showEditSheet = true
                            },
                            onDelete = { viewModel.deleteTransaction(transaction) },
                            onViewReceipt = { viewingReceiptTransaction = transaction }
                        )
                    }
                }
            }
        }
    }

    if (showEditSheet && editingTransaction != null) {
        AddEditTransactionSheet(
            initialType = editingTransaction!!.type,
            initialTransaction = editingTransaction,
            onDismiss = {
                showEditSheet = false
                editingTransaction = null
            },
            onSaveTransaction = { type, amount, title, personName, category, note, dateMillis, _, _, receiptUri ->
                viewModel.updateTransaction(
                    editingTransaction!!.copy(
                        type = type,
                        amount = amount,
                        title = title,
                        personName = personName,
                        category = category,
                        note = note,
                        date = dateMillis,
                        receiptImageUri = receiptUri
                    )
                )
            },
            onDeleteTransaction = { tx ->
                viewModel.deleteTransaction(tx)
            }
        )
    }

    if (viewingReceiptTransaction?.receiptImageUri != null) {
        ReceiptViewerDialog(
            imageUri = viewingReceiptTransaction!!.receiptImageUri!!,
            title = viewingReceiptTransaction!!.title,
            onDismiss = { viewingReceiptTransaction = null }
        )
    }
}
