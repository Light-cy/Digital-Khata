package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EarningGreen
import com.example.ui.theme.ExpenseRed
import com.example.util.CurrencyUtils
import kotlin.math.max

data class DayChartData(
    val dayLabel: String,      // Mon, Tue, etc.
    val dateLabel: String,     // 17, 18, etc.
    val earnings: Double,
    val expenses: Double,
    val isSelected: Boolean = false
)

@Composable
fun WeeklyBarChart(
    days: List<DayChartData>,
    modifier: Modifier = Modifier
) {
    val maxVal = max(100.0, days.maxOfOrNull { max(it.earnings, it.expenses) } ?: 100.0)
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(days) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Chart Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(EarningGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Earning",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(ExpenseRed, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Expense",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas Chart
            val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val chartHeight = size.height - 30.dp.toPx()
                val chartWidth = size.width
                val dayCount = if (days.isEmpty()) 7 else days.size
                val slotWidth = chartWidth / dayCount
                val barWidth = (slotWidth * 0.28f).coerceAtMost(16.dp.toPx())

                // Draw background grid lines (3 lines)
                for (i in 1..3) {
                    val y = chartHeight * (i / 3f)
                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw Bars for each day
                days.forEachIndexed { index, day ->
                    val centerX = slotWidth * index + slotWidth / 2

                    val earnHeight = ((day.earnings / maxVal) * chartHeight * animationProgress.value).toFloat()
                    val expHeight = ((day.expenses / maxVal) * chartHeight * animationProgress.value).toFloat()

                    // Earning bar (left)
                    val earnLeft = centerX - barWidth - 2.dp.toPx()
                    val earnTop = chartHeight - earnHeight
                    if (earnHeight > 0) {
                        drawRoundRect(
                            color = EarningGreen,
                            topLeft = Offset(earnLeft, earnTop),
                            size = Size(barWidth, earnHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    } else {
                        // Tiny baseline dot
                        drawCircle(
                            color = EarningGreen.copy(alpha = 0.4f),
                            radius = 2.dp.toPx(),
                            center = Offset(earnLeft + barWidth / 2, chartHeight - 2.dp.toPx())
                        )
                    }

                    // Expense bar (right)
                    val expLeft = centerX + 2.dp.toPx()
                    val expTop = chartHeight - expHeight
                    if (expHeight > 0) {
                        drawRoundRect(
                            color = ExpenseRed,
                            topLeft = Offset(expLeft, expTop),
                            size = Size(barWidth, expHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    } else {
                        // Tiny baseline dot
                        drawCircle(
                            color = ExpenseRed.copy(alpha = 0.4f),
                            radius = 2.dp.toPx(),
                            center = Offset(expLeft + barWidth / 2, chartHeight - 2.dp.toPx())
                        )
                    }
                }
            }

            // Bottom Labels (Days & Dates)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                days.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp)
                    ) {
                        Text(
                            text = day.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (day.isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = day.dateLabel,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            fontWeight = if (day.isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

data class CategorySlice(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

@Composable
fun MonthlyDonutChart(
    slices: List<CategorySlice>,
    totalExpense: Double,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Expense Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (slices.isEmpty() || totalExpense <= 0) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Expenses\nThis Month",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        val strokeWidth = 32.dp.toPx()
                        var currentAngle = -90f

                        slices.forEach { slice ->
                            val sweepAngle = (slice.percentage / 100f) * 360f * animationProgress.value
                            drawArc(
                                color = slice.color,
                                startAngle = currentAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            currentAngle += sweepAngle
                        }
                    }

                    // Center Summary Text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyUtils.format(totalExpense),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }
        }
    }
}
