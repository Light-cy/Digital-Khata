package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EarningGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.LoanGivenAmber
import com.example.ui.theme.LoanTakenBlue
import com.example.util.CurrencyUtils

@Composable
fun DailySummaryCard(
    openingBalance: Double,
    totalEarning: Double,
    totalExpense: Double,
    dailyNetSavings: Double,
    runningBalance: Double,
    cashRunning: Double = 0.0,
    accountRunning: Double = 0.0,
    unsettledLoanGiven: Double = 0.0,
    unsettledLoanTaken: Double = 0.0,
    modifier: Modifier = Modifier
) {
    var showFormulaDialog by remember { mutableStateOf(false) }

    val isPositiveRunning = runningBalance >= 0
    val isPositiveTodayNet = dailyNetSavings >= 0

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val gradientColors = if (isDark) {
        if (isPositiveRunning) {
            listOf(
                Color(0xFF1B2B22),
                Color(0xFF161A1D)
            )
        } else {
            listOf(
                Color(0xFF2E1A1E),
                Color(0xFF161A1D)
            )
        }
    } else {
        if (isPositiveRunning) {
            listOf(
                Color(0xFFF0FDF4),
                Color(0xFFFFFFFF)
            )
        } else {
            listOf(
                Color(0xFFFFF1F2),
                Color(0xFFFFFFFF)
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.99f),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(gradientColors)
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header: Main Prominent Running Balance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (isPositiveRunning) EarningGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Daily Running Balance",
                                tint = if (isPositiveRunning) EarningGreen else ExpenseRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Day Running Balance (Carried + Today)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            AnimatedCurrencyText(
                                amount = runningBalance,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPositiveRunning) EarningGreen else ExpenseRed,
                                isSigned = false
                            )
                        }
                    }

                    IconButton(
                        onClick = { showFormulaDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick { showFormulaDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Calculation info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Opening Balance & Today's Net Indicator Strip
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Opening: ",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.format(openingBalance),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Today's Net: ",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = (if (dailyNetSavings > 0) "+ " else if (dailyNetSavings < 0) "- " else "") + CurrencyUtils.format(dailyNetSavings),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isPositiveTodayNet) EarningGreen else ExpenseRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sub-metrics Row (Today's Earnings, Expenses, Net Activity)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Today's Earning Pill
                    SummaryPill(
                        label = "Today's In",
                        amount = totalEarning,
                        color = EarningGreen,
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )

                    // Today's Expense Pill
                    SummaryPill(
                        label = "Today's Out",
                        amount = totalExpense,
                        color = ExpenseRed,
                        icon = Icons.Default.TrendingDown,
                        modifier = Modifier.weight(1f)
                    )

                    // Today's Net Savings Pill
                    SummaryPill(
                        label = "Daily Net",
                        amount = dailyNetSavings,
                        color = if (isPositiveTodayNet) EarningGreen else ExpenseRed,
                        icon = Icons.Default.Savings,
                        isSigned = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Optional: Unsettled Loans pending overview if active
                if (unsettledLoanGiven > 0 || unsettledLoanTaken > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (unsettledLoanGiven > 0) {
                            LoanBadge(
                                label = "Lent (Pending)",
                                amount = unsettledLoanGiven,
                                color = LoanGivenAmber,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (unsettledLoanTaken > 0) {
                            LoanBadge(
                                label = "Borrowed (Pending)",
                                amount = unsettledLoanTaken,
                                color = LoanTakenBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFormulaDialog) {
        AlertDialog(
            onDismissRequest = { showFormulaDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Daily Balance Breakdown")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1. Running Total Balance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "= Opening Balance (Carried from yesterday) + Today's Earnings − Today's Expenses.\n" +
                                "Split into Cash in Hand & Bank / Account in the Balance Overview card above.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "2. Daily Net Savings (Today Only)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "= Today's Earnings − Today's Expenses.\n" +
                                "Shows the net profit or cash flow generated on this date.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "3. Payment Modes (Cash vs Bank)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• Cash entries update your Cash balance.\n" +
                                "• Account / Bank entries update your Bank balance.\n" +
                                "• You can adjust initial base amounts in Settings anytime.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showFormulaDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SummaryPill(
    label: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSigned: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            AnimatedCurrencyText(
                amount = amount,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                fontWeight = FontWeight.Bold,
                color = color,
                isSigned = isSigned
            )
        }
    }
}

@Composable
private fun LoanBadge(
    label: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.09f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = color,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = CurrencyUtils.format(amount),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
