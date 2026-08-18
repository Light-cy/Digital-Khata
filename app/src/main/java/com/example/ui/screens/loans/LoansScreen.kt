package com.example.ui.screens.loans

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ReceiptViewerDialog
import com.example.ui.components.bounceClick
import com.example.ui.theme.EarningGreen
import com.example.ui.theme.EarningGreenLight
import com.example.ui.components.AppTopHeader
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.LoanGivenAmber
import com.example.ui.theme.LoanGivenAmberLight
import com.example.ui.theme.LoanTakenBlue
import com.example.ui.theme.LoanTakenBlueLight
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import kotlinx.coroutines.launch

@Composable
fun LoansScreen(
    viewModel: LoansViewModel,
    notificationHelper: NotificationHelper? = null,
    unreadNotificationsCount: Int = 0,
    onNavigateToSettings: (() -> Unit)? = null,
    onNotificationClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val allLoans by viewModel.allLoans.collectAsStateWithLifecycle()
    val totals by viewModel.loanTotals.collectAsStateWithLifecycle()
    val isReminderActive = notificationHelper?.isDailyReminderEnabled() ?: false

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var viewingReceiptTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showSettledHistory by remember { mutableStateOf(false) }

    val currentType = if (selectedTab == 0) TransactionType.LOAN_GIVEN else TransactionType.LOAN_TAKEN
    val currentLoans = allLoans.filter { it.type == currentType }
    val unsettledLoans = currentLoans.filter { !it.isSettled }
    val settledLoans = currentLoans.filter { it.isSettled }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    editingTransaction = null
                    showAddSheet = true
                },
                containerColor = if (selectedTab == 0) LoanGivenAmber else LoanTakenBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.bounceClick {
                    editingTransaction = null
                    showAddSheet = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Loan")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Screen Top Header
            AppTopHeader(
                title = "Credit & Loans",
                subtitle = "Receivables & Payables Ledger",
                screenIcon = Icons.Default.Handshake,
                isReminderActive = isReminderActive,
                unreadNotificationsCount = unreadNotificationsCount,
                onNavigateToSettings = onNavigateToSettings,
                onNotificationClick = onNotificationClick ?: onNavigateToSettings
            )

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setSelectedTab(0)
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Receivables", fontWeight = FontWeight.Bold)
                            if (totals.unsettledGivenCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = LoanGivenAmber
                                ) {
                                    Text(
                                        text = "${totals.unsettledGivenCount}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setSelectedTab(1)
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Payables", fontWeight = FontWeight.Bold)
                            if (totals.unsettledTakenCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = LoanTakenBlue
                                ) {
                                    Text(
                                        text = "${totals.unsettledTakenCount}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Running Total Card
                item(key = "running_total_card") {
                    val isReceivable = selectedTab == 0
                    val amount = if (isReceivable) totals.totalReceivable else totals.totalPayable
                    val label = if (isReceivable) "Total Receivable (Owed to me)" else "Total Payable (I Owe)"
                    val count = if (isReceivable) totals.unsettledGivenCount else totals.unsettledTakenCount
                    val themeColor = if (isReceivable) LoanGivenAmber else LoanTakenBlue

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleDown = 0.99f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(themeColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Handshake,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    AnimatedCurrencyText(
                                        amount = amount,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = themeColor
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = themeColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "$count Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Section: Active / Unsettled Loans
                item(key = "unsettled_header") {
                    Text(
                        text = if (selectedTab == 0) "Pending Recovery (${unsettledLoans.size})" else "Pending Repayment (${unsettledLoans.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                if (unsettledLoans.isEmpty()) {
                    item(key = "empty_unsettled") {
                        EmptyStateView(
                            icon = Icons.Default.Handshake,
                            title = if (selectedTab == 0) "No Pending Receivables" else "No Pending Payables",
                            subtitle = if (selectedTab == 0) "All your receivables have been settled cleanly." else "You don't have any outstanding payables.",
                            actionButtonText = if (selectedTab == 0) "+ Add Receivable" else "+ Add Payable",
                            onActionClick = {
                                editingTransaction = null
                                showAddSheet = true
                            }
                        )
                    }
                } else {
                    items(unsettledLoans, key = { it.id }) { loan ->
                        Box(modifier = Modifier.animateItem()) {
                            LoanItemCard(
                                loan = loan,
                                isGiven = selectedTab == 0,
                                onToggleSettled = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleSettledStatus(loan) { isNowSettled ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = if (isNowSettled) {
                                                    if (selectedTab == 0) "Marked as recovered & added to income" else "Marked as paid back & recorded in expenses"
                                                } else {
                                                    "Loan reopened (settlement record removed)"
                                                },
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    editingTransaction = loan
                                    showAddSheet = true
                                },
                                onViewReceipt = { viewingReceiptTransaction = loan }
                            )
                        }
                    }
                }

                // Collapsible Section: Settled History
                if (settledLoans.isNotEmpty()) {
                    item(key = "settled_header") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showSettledHistory = !showSettledHistory
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Settled Loans History (${settledLoans.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = if (showSettledHistory) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle settled",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (showSettledHistory) {
                        items(settledLoans, key = { it.id }) { loan ->
                            Box(modifier = Modifier.animateItem()) {
                                LoanItemCard(
                                    loan = loan,
                                    isGiven = selectedTab == 0,
                                    onToggleSettled = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.toggleSettledStatus(loan) { isNowSettled ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = if (isNowSettled) "Marked as settled" else "Loan reopened (settlement record removed)",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    },
                                     onClick = {
                                         editingTransaction = loan
                                         showAddSheet = true
                                     },
                                     onViewReceipt = { viewingReceiptTransaction = loan }
                                 )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddEditTransactionSheet(
            initialType = currentType,
            initialTransaction = editingTransaction,
            onDismiss = {
                showAddSheet = false
                editingTransaction = null
            },
            onSaveTransaction = { type, amount, title, personName, category, note, dateMillis, paymentMode, _, _, receiptUri ->
                if (editingTransaction != null) {
                    viewModel.updateLoan(
                        editingTransaction!!.copy(
                            type = type,
                            amount = amount,
                            title = title,
                            personName = personName ?: "Unknown",
                            paymentMode = paymentMode,
                            note = note,
                            date = dateMillis,
                            receiptImageUri = receiptUri
                        )
                    )
                } else {
                    viewModel.addLoan(
                        type = type,
                        amount = amount,
                        title = title,
                        personName = personName ?: "Unknown",
                        note = note,
                        dateMillis = dateMillis,
                        paymentMode = paymentMode,
                        receiptImageUri = receiptUri
                    )
                }
            },
            onDeleteTransaction = { tx ->
                viewModel.deleteLoan(tx)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Loan entry deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreLoan(tx)
                    }
                }
            },
            onUnsettleTransaction = { tx ->
                viewModel.toggleSettledStatus(tx)
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

@Composable
private fun LoanItemCard(
    loan: Transaction,
    isGiven: Boolean,
    onToggleSettled: () -> Unit,
    onClick: () -> Unit,
    onViewReceipt: (() -> Unit)? = null
) {
    val themeColor = if (isGiven) LoanGivenAmber else LoanTakenBlue
    val daysAgo = ((System.currentTimeMillis() - loan.date) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val settledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f).compositeOver(surfaceColor)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .bounceClick(scaleDown = 0.98f) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (loan.isSettled) settledContainerColor else surfaceColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (loan.isSettled) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Person Info
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (loan.isSettled) MaterialTheme.colorScheme.surfaceVariant else themeColor.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (loan.personName?.take(1) ?: "P").uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = if (loan.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else themeColor,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = loan.personName ?: loan.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (loan.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (loan.receiptImageUri != null) {
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${DateUtils.formatShortDate(loan.date)} • $daysAgo days ago",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (loan.paymentMode == PaymentMode.CASH) {
                                    EarningGreen.copy(alpha = 0.12f)
                                } else {
                                    LoanTakenBlue.copy(alpha = 0.12f)
                                }
                            ) {
                                Text(
                                    text = if (loan.paymentMode == PaymentMode.CASH) "Cash" else "Bank",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (loan.paymentMode == PaymentMode.CASH) EarningGreen else LoanTakenBlue,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Amount
                AnimatedCurrencyText(
                    amount = loan.amount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (loan.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else themeColor
                )
            }

            if (!loan.note.isNullOrBlank() || loan.title != loan.personName) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (loan.note.isNullOrBlank()) loan.title else "${loan.title} — ${loan.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Button: Mark Settled Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loan.isSettled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EarningGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGiven) "Recovered / Settled" else "Paid Back / Settled",
                            style = MaterialTheme.typography.bodySmall,
                            color = EarningGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = onToggleSettled,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.bounceClick { onToggleSettled() }
                    ) {
                        Text(
                            text = "Reopen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = if (isGiven) "Awaiting recovery" else "Pending repayment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onToggleSettled,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EarningGreen
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.bounceClick { onToggleSettled() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGiven) "Mark Recovered" else "Mark Paid Back",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
