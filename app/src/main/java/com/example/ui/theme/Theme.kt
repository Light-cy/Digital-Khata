package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnPrimaryDark,
    primaryContainer = TealPrimaryContainerDark,
    onPrimaryContainer = TealOnPrimaryContainerDark,
    secondary = SlateTertiary,
    onSecondary = TextPrimaryDark,
    secondaryContainer = ElevatedSurfaceDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = SlateTertiary,
    tertiaryContainer = ElevatedSurfaceDark,
    onTertiaryContainer = TextPrimaryDark,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = CardBackgroundDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = ElevatedSurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = CardBackgroundDark,
    surfaceContainerHigh = ElevatedSurfaceDark,
    surfaceContainerHighest = ElevatedSurfaceDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark.copy(alpha = 0.5f)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    secondary = TealSecondary,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealOnSecondaryContainer,
    tertiary = SlateTertiary,
    tertiaryContainer = SlateTertiaryContainer,
    onTertiaryContainer = SlateOnTertiaryContainer,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = CardBackgroundLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = ElevatedSurfaceLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = CardBackgroundLight,
    surfaceContainerHigh = ElevatedSurfaceLight,
    surfaceContainerHighest = ElevatedSurfaceLight,
    outline = OutlineLight,
    outlineVariant = OutlineLight.copy(alpha = 0.6f)
  )

@Composable
fun DigitalKhataTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = AppShapes,
    content = content
  )
}


