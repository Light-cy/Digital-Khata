package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.util.CurrencyUtils
import kotlin.math.abs

@Composable
fun AnimatedCurrencyText(
    amount: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    isSigned: Boolean = false,
    durationMillis: Int = 600
) {
    val animatedAmount by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
        label = "AmountCounterAnimation"
    )

    val formatted = if (isSigned) {
        val sign = if (animatedAmount > 0) "+" else if (animatedAmount < 0) "-" else ""
        "$sign${CurrencyUtils.currencySymbol} ${String.format("%,.0f", abs(animatedAmount.toDouble()))}"
    } else {
        "${CurrencyUtils.currencySymbol} ${String.format("%,.0f", abs(animatedAmount.toDouble()))}"
    }

    Text(
        text = formatted,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight
    )
}
