package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PaymentMode
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.AddEditTransactionSheet
import com.example.ui.components.AnimatedCurrencyText
import com.example.ui.components.BalanceOverviewCard
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.DailySummaryCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StartingBalanceDialog
import com.example.ui.components.SwipeToDeleteContainer
import com.example.ui.components.TransactionSkeletonItem
import com.example.ui.components.AppTopHeader
import com.example.ui.components.bounceClick
import com.example.ui.theme.EarningGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.LoanGivenAmber
import com.example.ui.theme.LoanTakenBlue
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.AutoMode
import com.example.data.model.isSettlementEntry
import com.example.ui.components.ReceiptViewerDialog
import com.example.ui.components.SystemSettlementDetailDialog
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import com.example.util.UserManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userManager: UserManager? = null,
    notificationHelper: NotificationHelper? = null,
    unreadNotificationsCount: Int = 0,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToLoans: (() -> Unit)? = null,
    onNotificationClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDateMillis.collectAsStateWithLifecycle()
    val transactions by viewModel.transactionsForDay.collectAsStateWithLifecycle()
    val dailySummary by viewModel.dailySummary.collectAsStateWithLifecycle()
    val balanceOverview by viewModel.balanceOverview.collectAsStateWithLifecycle()
    val lastUsedPaymentMode by viewModel.lastUsedPaymentMode.collectAsStateWithLifecycle()
    val budgetProgress by viewModel.budgetProgressList.collectAsStateWithLifecycle()
    val userName by (userManager?.userName?.collectAsStateWithLifecycle() ?: remember { mutableStateOf("") })
    val isReminderActive = notificationHelper?.isDailyReminderEnabled() ?: false

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    var isRefreshing by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var sheetInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var viewingSettlementTransaction by remember { mutableStateOf<Transaction?>(null) }
    var viewingReceiptTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartingBalanceDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Header: App Branding / Greeting + Settings & Notification Bell
            AppTopHeader(
                userName = userName,
                isHomeScreen = true,
                isReminderActive = isReminderActive,
                unreadNotificationsCount = unreadNotificationsCount,
                onNavigateToSettings = onNavigateToSettings,
                onNotificationClick = onNotificationClick ?: onNavigateToSettings
            )

            // Date Navigation Bar
            DateNavigationBar(
                selectedDateMillis = selectedDate,
                onPreviousClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.previousDay()
                },
                onNextClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.nextDay()
                },
                onDatePickClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showDatePicker = true
                },
                onTodayClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.goToToday()
                }
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        delay(400)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Balance Overview Card (Cash vs Account vs Net Total)
                    item(key = "balance_overview_card") {
                        BalanceOverviewCard(
                            overview = balanceOverview,
                            onAdjustStartingBalance = { showStartingBalanceDialog = true }
                        )
                    }

                    // Pinned Daily Summary Card
                    item(key = "daily_summary_card") {
                        DailySummaryCard(
                            openingBalance = dailySummary.openingBalance,
                            totalEarning = dailySummary.totalEarning,
                            totalExpense = dailySummary.totalExpense,
                            dailyNetSavings = dailySummary.dailyNetSavings,
                            runningBalance = dailySummary.runningBalance,
                            cashRunning = dailySummary.cashRunning,
                            accountRunning = dailySummary.accountRunning,
                            unsettledLoanGiven = dailySummary.unsettledLoanGiven,
                            unsettledLoanTaken = dailySummary.unsettledLoanTaken
                        )
                    }

                    // Quick Add Action Buttons (4 distinct colored cards)
                    item(key = "quick_add_row") {
                        Text(
                            text = "Quick Add Entry",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickAddCard(
                                title = "Earning",
                                icon = Icons.Default.TrendingUp,
                                color = EarningGreen,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    sheetInitialType = TransactionType.EARNING
                                    editingTransaction = null
                                    showAddSheet = true
                                }
                            )

                            QuickAddCard(
                                title = "Expense",
                                icon = Icons.Default.TrendingDown,
                                color = ExpenseRed,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    sheetInitialType = TransactionType.EXPENSE
                                    editingTransaction = null
                                    showAddSheet = true
                                }
                            )

                            QuickAddCard(
                                title = "Lent",
                                icon = Icons.Default.Handshake,
                                color = LoanGivenAmber,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    sheetInitialType = TransactionType.LOAN_GIVEN
                                    editingTransaction = null
                                    showAddSheet = true
                                }
                            )

                            QuickAddCard(
                                title = "Borrowed",
                                icon = Icons.Default.Receipt,
                                color = LoanTakenBlue,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    sheetInitialType = TransactionType.LOAN_TAKEN
                                    editingTransaction = null
                                    showAddSheet = true
                                }
                            )
                        }
                    }

                    // Budget Alerts (if any category is high)
                    val urgentBudgets = budgetProgress.filter { it.percentage >= 90f }
                    if (urgentBudgets.isNotEmpty()) {
                        item(key = "urgent_budget_alerts") {
                            urgentBudgets.forEach { budget ->
                                val isOver = budget.percentage >= 100f
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bounceClick(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isOver) ExpenseRed.copy(alpha = 0.12f) else LoanGivenAmber.copy(alpha = 0.12f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (isOver) ExpenseRed else LoanGivenAmber,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isOver) "Budget Exceeded: ${budget.category}" else "Budget Alert: ${budget.category}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOver) ExpenseRed else LoanGivenAmber
                                            )
                                            Text(
                                                text = "${CurrencyUtils.format(budget.spentThisMonth)} of ${CurrencyUtils.format(budget.budgetLimit)} limit (${budget.percentage.toInt()}%)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Today's Entries Header
                    item(key = "entries_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Entries (${transactions.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (transactions.isNotEmpty()) {
                                Text(
                                    text = "Swipe to delete",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Entries List or Empty State or Shimmer loading
                    if (isRefreshing) {
                        items(3) {
                            TransactionSkeletonItem()
                        }
                    } else if (transactions.isEmpty()) {
                        item(key = "empty_state") {
                            EmptyStateView(
                                icon = Icons.Default.Savings,
                                title = "No Entries for Today",
                                subtitle = "No expenses or earnings logged yet for this date. Tap quick add to log your cash flow.",
                                actionButtonText = "+ Add First Entry",
                                onActionClick = {
                                    sheetInitialType = TransactionType.EXPENSE
                                    editingTransaction = null
                                    showAddSheet = true
                                }
                            )
                        }
                    } else {
                        items(transactions, key = { it.id }) { transaction ->
                            val isSettlement = transaction.isSettlementEntry
                            Box(modifier = Modifier.animateItem()) {
                                SwipeToDeleteContainer(
                                    enableSwipe = !isSettlement,
                                    itemKey = Pair(transaction.id, transaction.isSettled),
                                    onDelete = {
                                        if (isSettlement) return@SwipeToDeleteContainer
                                        val deletedTx = transaction
                                        viewModel.deleteTransaction(deletedTx)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Deleted ${deletedTx.title}",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.addTransaction(
                                                    type = deletedTx.type,
                                                    amount = deletedTx.amount,
                                                    title = deletedTx.title,
                                                    personName = deletedTx.personName,
                                                    category = deletedTx.category,
                                                    note = deletedTx.note,
                                                    dateMillis = deletedTx.date,
                                                    paymentMode = deletedTx.paymentMode,
                                                    makeRecurring = false,
                                                    recurringFrequency = com.example.data.model.RecurringFrequency.MONTHLY,
                                                    receiptImageUri = deletedTx.receiptImageUri
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    TransactionItemRow(
                                        transaction = transaction,
                                        onClick = {
                                            if (isSettlement) {
                                                viewingSettlementTransaction = transaction
                                            } else {
                                                editingTransaction = transaction
                                                sheetInitialType = transaction.type
                                                showAddSheet = true
                                            }
                                        },
                                        onDelete = {
                                            if (!isSettlement) {
                                                val deletedTx = transaction
                                                viewModel.deleteTransaction(deletedTx)
                                                scope.launch {
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = "Deleted ${deletedTx.title}",
                                                        actionLabel = "Undo",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        viewModel.addTransaction(
                                                            type = deletedTx.type,
                                                            amount = deletedTx.amount,
                                                            title = deletedTx.title,
                                                            personName = deletedTx.personName,
                                                            category = deletedTx.category,
                                                            note = deletedTx.note,
                                                            dateMillis = deletedTx.date,
                                                            paymentMode = deletedTx.paymentMode,
                                                            makeRecurring = false,
                                                            recurringFrequency = com.example.data.model.RecurringFrequency.MONTHLY,
                                                            receiptImageUri = deletedTx.receiptImageUri
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onToggleSettled = if (isSettlement) null else { tx ->
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val wasSettled = tx.isSettled
                                            val amountFormatted = CurrencyUtils.format(tx.amount)
                                            viewModel.toggleLoanSettled(tx, tx.paymentMode) { isNowSettled ->
                                                scope.launch {
                                                    val message = if (isNowSettled) {
                                                        if (tx.type == TransactionType.LOAN_GIVEN) {
                                                            "Marked as recovered (${if (tx.paymentMode == PaymentMode.CASH) "Cash" else "Bank"}) — $amountFormatted added"
                                                        } else {
                                                            "Marked as paid back (${if (tx.paymentMode == PaymentMode.CASH) "Cash" else "Bank"}) — $amountFormatted added"
                                                        }
                                                    } else {
                                                        "Loan reopened — settlement transaction removed"
                                                    }
                                                    snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                                                }
                                            }
                                        },
                                        onViewReceipt = { viewingReceiptTransaction = transaction }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp, start = 8.dp, end = 8.dp)
        )
    }

    if (showAddSheet) {
        AddEditTransactionSheet(
            initialType = sheetInitialType,
            initialTransaction = editingTransaction,
            initialPaymentMode = lastUsedPaymentMode,
            defaultDateMillis = selectedDate,
            onDismiss = {
                showAddSheet = false
                editingTransaction = null
            },
            onSaveTransaction = { type, amount, title, personName, category, note, dateMillis, paymentMode, makeRecurring, freq, receiptUri ->
                if (editingTransaction != null) {
                    viewModel.updateTransaction(
                        editingTransaction!!.copy(
                            type = type,
                            amount = amount,
                            title = title,
                            personName = personName,
                            category = category,
                            paymentMode = paymentMode,
                            note = note,
                            date = dateMillis,
                            receiptImageUri = receiptUri
                        )
                    )
                } else {
                    viewModel.addTransaction(
                        type = type,
                        amount = amount,
                        title = title,
                        personName = personName,
                        category = category,
                        note = note,
                        dateMillis = dateMillis,
                        paymentMode = paymentMode,
                        makeRecurring = makeRecurring,
                        recurringFrequency = freq,
                        receiptImageUri = receiptUri
                    )
                }
            },
            onDeleteTransaction = { tx ->
                viewModel.deleteTransaction(tx)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Entry deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreTransaction(tx)
                    }
                }
            },
            onUnsettleTransaction = { tx ->
                viewModel.toggleLoanSettled(tx, tx.paymentMode)
            }
        )
    }

    if (showStartingBalanceDialog) {
        StartingBalanceDialog(
            initialCash = balanceOverview.cashStarting,
            initialAccount = balanceOverview.accountStarting,
            onDismiss = { showStartingBalanceDialog = false },
            onSave = { cash, account ->
                viewModel.setStartingBalances(cash, account)
                scope.launch {
                    snackbarHostState.showSnackbar("Starting balances updated successfully!", duration = SnackbarDuration.Short)
                }
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

    if (viewingSettlementTransaction != null) {
        SystemSettlementDetailDialog(
            transaction = viewingSettlementTransaction!!,
            onDismiss = { viewingSettlementTransaction = null },
            onNavigateToLoans = onNavigateToLoans
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.setSelectedDate(it)
                    }
                    showDatePicker = false
                }) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DateNavigationBar(
    selectedDateMillis: Long,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onDatePickClick: () -> Unit,
    onTodayClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier.bounceClick { onPreviousClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous day"
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDatePickClick() }
                    .bounceClick { onDatePickClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Calendar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = DateUtils.formatDisplayDate(selectedDateMillis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.bounceClick { onNextClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next day"
                    )
                }

                if (!DateUtils.isSameDay(selectedDateMillis, System.currentTimeMillis())) {
                    IconButton(
                        onClick = onTodayClick,
                        modifier = Modifier.bounceClick { onTodayClick() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Jump to Today",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .bounceClick { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.09f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "+ $title",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: Transaction,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleSettled: ((Transaction) -> Unit)? = null,
    onViewReceipt: (() -> Unit)? = null
) {
    val isLoan = transaction.type == TransactionType.LOAN_GIVEN || transaction.type == TransactionType.LOAN_TAKEN
    val color = CategoryIconHelper.getTypeColor(transaction.type)
    val icon = if (isLoan) {
        Icons.Default.Handshake
    } else {
        CategoryIconHelper.getCategoryIcon(transaction.category)
    }

    val isSettled = transaction.isSettled
    val isCash = transaction.paymentMode == PaymentMode.CASH
    val modeColor = if (isCash) Color(0xFF10B981) else Color(0xFF6366F1)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val settledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f).compositeOver(surfaceColor)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .bounceClick(scaleDown = 0.98f) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSettled) settledContainerColor else surfaceColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSettled) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // If it's a loan, show the quick-settle checkbox button on the left
            if (isLoan && onToggleSettled != null) {
                IconButton(
                    onClick = { onToggleSettled(transaction) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isSettled) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (isSettled) "Settled (Tap to reopen)" else "Tap to mark settled",
                        tint = if (isSettled) EarningGreen else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                // Color-coded Category/Type Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (isSettled) color.copy(alpha = 0.07f) else color.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSettled) color.copy(alpha = 0.6f) else color,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Title & Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSettled) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (isSettled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (transaction.receiptImageUri != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { onViewReceipt?.invoke() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "View Receipt",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Bill",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Type Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSettled) color.copy(alpha = 0.08f) else color.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = CategoryIconHelper.getTypeLabel(transaction.type),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isSettled) color.copy(alpha = 0.75f) else color,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Payment Mode Badge (Cash vs Bank)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = modeColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCash) Icons.Default.Payments else Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = modeColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isCash) "Cash" else "Bank",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = modeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (transaction.isSettlementEntry) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Auto Settlement",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(9.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Auto",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (transaction.personName != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• ${transaction.personName}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (transaction.category != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• ${transaction.category}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isSettled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "[Settled]",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = EarningGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!transaction.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount
            val amountPrefix = when (transaction.type) {
                TransactionType.EARNING -> "+ "
                TransactionType.EXPENSE -> "- "
                TransactionType.LOAN_GIVEN -> "- "
                TransactionType.LOAN_TAKEN -> "+ "
            }

            Text(
                text = "$amountPrefix${CurrencyUtils.format(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSettled) color.copy(alpha = 0.6f) else color
            )
        }
    }
}
