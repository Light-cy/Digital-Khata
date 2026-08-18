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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
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
import com.example.ui.theme.EarningGreenLight
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.LoanGivenAmber
import com.example.ui.theme.LoanTakenBlue
import com.example.util.CurrencyUtils

@Composable
fun DailySummaryCard(
    totalEarning: Double,
    totalExpense: Double,
    totalLoanGiven: Double,
    totalLoanTaken: Double,
    modifier: Modifier = Modifier
) {
    var showFormulaDialog by remember { mutableStateOf(false) }

    // Net Savings formula: Earning - Expense - Loan Given + Loan Taken
    val netSavings = totalEarning - totalExpense - totalLoanGiven + totalLoanTaken
    val netLoanImpact = totalLoanTaken - totalLoanGiven

    val isPositiveSavings = netSavings >= 0

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val gradientColors = if (isDark) {
        if (isPositiveSavings) {
            listOf(
                Color(0xFF1E2824), // Subtle dark green tint on top
                Color(0xFF1A1D23)  // Refined card surface base
            )
        } else {
            listOf(
                Color(0xFF281E22), // Subtle dark red tint on top
                Color(0xFF1A1D23)  // Refined card surface base
            )
        }
    } else {
        if (isPositiveSavings) {
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
        shape = RoundedCornerShape(20.dp),
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
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Row: Net Savings Title & Info button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isPositiveSavings) EarningGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = "Savings",
                                tint = if (isPositiveSavings) EarningGreen else ExpenseRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Daily Net Savings",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            AnimatedCurrencyText(
                                amount = netSavings,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPositiveSavings) EarningGreen else ExpenseRed,
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

                Spacer(modifier = Modifier.height(18.dp))

                // Sub-metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Earning Pill
                    SummaryPill(
                        label = "Earnings",
                        amount = totalEarning,
                        color = EarningGreen,
                        modifier = Modifier.weight(1f)
                    )

                    // Expense Pill
                    SummaryPill(
                        label = "Expenses",
                        amount = totalExpense,
                        color = ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )

                    // Loan Impact Pill
                    SummaryPill(
                        label = "Net Loans",
                        amount = netLoanImpact,
                        color = if (netLoanImpact >= 0) LoanTakenBlue else LoanGivenAmber,
                        isSigned = true,
                        modifier = Modifier.weight(1.1f)
                    )
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
                    Text("Daily Savings Formula")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Net Savings = (Earnings − Expenses) − Receivables + Payables",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Receivables reduce cash-in-hand today (money lent out).\n" +
                                "• Payables increase cash-in-hand today (money borrowed).\n" +
                                "• Both are tracked separately from ordinary expenses so you can accurately balance your daily wallet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    isSigned: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            AnimatedCurrencyText(
                amount = amount,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                fontWeight = FontWeight.Bold,
                color = color,
                isSigned = isSigned
            )
        }
    }
}

