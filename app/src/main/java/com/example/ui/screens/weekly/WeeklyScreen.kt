package com.example.ui.screens.weekly

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import androidx.compose.material.icons.filled.CalendarViewWeek
import com.example.ui.components.AppTopHeader
import com.example.ui.components.DayChartData
import com.example.ui.components.WeeklyBarChart
import com.example.ui.theme.EarningGreen
import com.example.ui.theme.EarningGreenLight
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.LoanGivenAmber
import com.example.ui.theme.LoanTakenBlue
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.NotificationHelper

@Composable
fun WeeklyScreen(
    viewModel: WeeklyViewModel,
    onDayClick: (Long) -> Unit,
    notificationHelper: NotificationHelper? = null,
    unreadNotificationsCount: Int = 0,
    onNavigateToSettings: (() -> Unit)? = null,
    onNotificationClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentWeekMillis by viewModel.currentWeekMillis.collectAsStateWithLifecycle()
    val weeklyTotals by viewModel.weeklyTotals.collectAsStateWithLifecycle()
    val dailyBreakdown by viewModel.dailyBreakdown.collectAsStateWithLifecycle()
    val weekTransactions by viewModel.weekTransactions.collectAsStateWithLifecycle()
    val isReminderActive = notificationHelper?.isDailyReminderEnabled() ?: false

    val chartData = dailyBreakdown.map { day ->
        DayChartData(
            dayLabel = day.dayName,
            dateLabel = DateUtils.formatDayNumber(day.dateMillis),
            earnings = day.earnings,
            expenses = day.expenses,
            isSelected = DateUtils.isSameDay(day.dateMillis, System.currentTimeMillis())
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Screen Top Header
        AppTopHeader(
            title = "Weekly Review",
            subtitle = "7-Day Cash Flow Breakdown",
            screenIcon = Icons.Default.CalendarViewWeek,
            isReminderActive = isReminderActive,
            unreadNotificationsCount = unreadNotificationsCount,
            onNavigateToSettings = onNavigateToSettings,
            onNotificationClick = onNotificationClick ?: onNavigateToSettings
        )

        // Week Navigation Header
        WeekNavigationBar(
            startOfWeekMillis = weeklyTotals.startOfWeekMillis,
            endOfWeekMillis = weeklyTotals.endOfWeekMillis,
            onPreviousClick = { viewModel.previousWeek() },
            onNextClick = { viewModel.nextWeek() },
            onCurrentWeekClick = { viewModel.currentWeek() }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Interactive 7-Day Chart
            item {
                Text(
                    text = "Weekly Cash Flow",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                WeeklyBarChart(days = chartData)
            }

            // Weekly Summary Breakdown Card
            item {
                WeeklySummaryTotalsCard(totals = weeklyTotals)
            }

            // Day-by-Day Breakdown Header
            item {
                Text(
                    text = "Day-by-Day Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            // 7 Days List
            items(dailyBreakdown, key = { it.dateMillis }) { day ->
                val dayTransactions = weekTransactions.filter {
                    val start = DateUtils.getStartOfDay(day.dateMillis)
                    val end = DateUtils.getEndOfDay(day.dateMillis)
                    it.date in start..end
                }

                WeeklyDayCard(
                    daySummary = day,
                    transactions = dayTransactions,
                    onOpenDayInDaily = { onDayClick(day.dateMillis) }
                )
            }
        }
    }
}

@Composable
private fun WeekNavigationBar(
    startOfWeekMillis: Long,
    endOfWeekMillis: Long,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onCurrentWeekClick: () -> Unit
) {
    val dateRangeText = if (startOfWeekMillis > 0 && endOfWeekMillis > 0) {
        "${DateUtils.formatShortDate(startOfWeekMillis)} – ${DateUtils.formatShortDate(endOfWeekMillis)}"
    } else {
        "This Week"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousClick) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous week"
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = dateRangeText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNextClick) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next week"
                    )
                }

                IconButton(onClick = onCurrentWeekClick) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = "Current Week",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklySummaryTotalsCard(totals: WeeklySummaryTotals) {
    val isPositive = totals.netSavings >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Net Savings Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isPositive) EarningGreenLight else ExpenseRedLight,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = if (isPositive) EarningGreen else ExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Net Weekly Savings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyUtils.format(totals.netSavings),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) EarningGreen else ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(14.dp))

            // Breakdown Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Total Earning",
                    amount = totals.totalEarning,
                    color = EarningGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Total Expense",
                    amount = totals.totalExpense,
                    color = ExpenseRed,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Receivables (Out)",
                    amount = totals.totalLoansGiven,
                    color = LoanGivenAmber,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Payables (In)",
                    amount = totals.totalLoansTaken,
                    color = LoanTakenBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = CurrencyUtils.format(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun WeeklyDayCard(
    daySummary: DaySummaryItem,
    transactions: List<Transaction>,
    onOpenDayInDaily: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isToday = DateUtils.isSameDay(daySummary.dateMillis, System.currentTimeMillis())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Day Circle Badge
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = daySummary.dayName.take(3),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = DateUtils.formatDayNumber(daySummary.dateMillis),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = daySummary.dateDisplay,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isToday) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "TODAY",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${daySummary.transactionCount} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right Net Savings & Expand Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Net: ${CurrencyUtils.format(daySummary.netSavings)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (daySummary.netSavings >= 0) EarningGreen else ExpenseRed
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (daySummary.earnings > 0) {
                                Text(
                                    text = "+${CurrencyUtils.formatWithoutSymbol(daySummary.earnings)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = EarningGreen
                                )
                            }
                            if (daySummary.expenses > 0) {
                                Text(
                                    text = "-${CurrencyUtils.formatWithoutSymbol(daySummary.expenses)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = ExpenseRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expanded Transactions List
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (transactions.isEmpty()) {
                        Text(
                            text = "No transactions logged on this day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        transactions.forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${tx.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = CurrencyUtils.format(tx.amount),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (tx.type) {
                                        com.example.data.model.TransactionType.EARNING -> EarningGreen
                                        com.example.data.model.TransactionType.EXPENSE -> ExpenseRed
                                        com.example.data.model.TransactionType.LOAN_GIVEN -> LoanGivenAmber
                                        com.example.data.model.TransactionType.LOAN_TAKEN -> LoanTakenBlue
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenDayInDaily() }
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(vertical = 6.dp),
                        color = Color.Transparent
                    ) {
                        Text(
                            text = "Open in Daily View →",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
