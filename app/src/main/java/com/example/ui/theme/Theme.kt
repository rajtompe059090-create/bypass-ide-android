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

private val BypassColorScheme =
  darkColorScheme(
    primary = CyanAccent,
    secondary = PurpleAccent,
    tertiary = GreenStatus,
    background = BgColor,
    surface = SurfaceColor,
    surfaceVariant = SurfaceVariantColor,
    onPrimary = BgColor,
    onSecondary = PrimaryTextColor,
    onTertiary = BgColor,
    onBackground = PrimaryTextColor,
    onSurface = PrimaryTextColor,
    onSurfaceVariant = MutedTextColor,
    error = ErrorColor
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = BypassColorScheme, typography = Typography, content = content)
}
