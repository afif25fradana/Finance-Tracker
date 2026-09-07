package com.financetracker.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TerminalColorScheme = darkColorScheme(
  primary = SignalPositive,
  onPrimary = TermBg,
  primaryContainer = Color(0xFF0F2622),
  onPrimaryContainer = SignalPositive,
  secondary = TermMuted,
  onSecondary = TermBg,
  secondaryContainer = TermPanelAlt,
  onSecondaryContainer = TermText,
  tertiary = SignalNegative,
  onTertiary = TermBg,
  error = SignalNegative,
  onError = TermBg,
  background = TermBg,
  onBackground = TermText,
  surface = TermPanel,
  onSurface = TermText,
  surfaceVariant = TermPanelAlt,
  onSurfaceVariant = TermMuted,
  outline = TermBorder,
  outlineVariant = Color(0xFF1E1E1E)
)

@Composable
fun FinanceTrackerTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = TerminalColorScheme,
    typography = Typography,
    content = content
  )
}

