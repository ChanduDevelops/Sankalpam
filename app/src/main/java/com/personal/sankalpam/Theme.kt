package com.personal.sankalpam

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun sankalpamTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    accentColor: AccentColor = AccentColor.BLUE,
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current

    val useDark = when (appTheme) {
        AppTheme.SYSTEM -> dark
        AppTheme.DARK, AppTheme.AMOLED -> true
        AppTheme.LIGHT -> false
    }

    val scheme = when {
        appTheme == AppTheme.AMOLED -> generateAmoledScheme(accentColor)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && appTheme == AppTheme.SYSTEM ->
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDark -> generateDarkScheme(accentColor)
        else -> generateLightScheme(accentColor)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private fun Color.mixWithBackground(bg: Color, fraction: Float): Color {
    return Color(
        red = this.red * fraction + bg.red * (1 - fraction),
        green = this.green * fraction + bg.green * (1 - fraction),
        blue = this.blue * fraction + bg.blue * (1 - fraction),
        alpha = 1f
    )
}

private fun generateLightScheme(accent: AccentColor): androidx.compose.material3.ColorScheme {
    val base = Color(accent.colorValue)
    val bg = Color(0xFFFBFDFD)
    return lightColorScheme(
        primary = base,
        onPrimary = Color.White,
        primaryContainer = base.mixWithBackground(Color.White, 0.18f),
        onPrimaryContainer = base.mixWithBackground(Color.Black, 0.85f),
        secondary = base.mixWithBackground(Color(0xFF4A5568), 0.5f),
        secondaryContainer = base.mixWithBackground(Color.White, 0.12f),
        onSecondaryContainer = base.mixWithBackground(Color.Black, 0.8f),
        tertiary = base.mixWithBackground(Color(0xFF8A5A00), 0.4f),
        tertiaryContainer = Color(0xFFFFDEA3),
        onTertiaryContainer = Color(0xFF2B1800),
        background = bg,
        onBackground = Color(0xFF191C1E),
        surface = bg,
        onSurface = Color(0xFF191C1E),
        surfaceVariant = Color(0xFFE0E3E8),
        onSurfaceVariant = Color(0xFF44474E),
        outline = Color(0xFF74777F),
        outlineVariant = Color(0xFFC4C7D0),
        surfaceContainerLow = Color(0xFFF5F6F9),
        surfaceContainer = Color(0xFFEFF1F4),
        surfaceContainerHigh = Color(0xFFE9EBF0),
        surfaceContainerHighest = Color(0xFFE2E4E9),
    )
}

private fun generateDarkScheme(accent: AccentColor): androidx.compose.material3.ColorScheme {
    val rawBase = Color(accent.colorValue)
    val base = rawBase.mixWithBackground(Color.White, 0.75f) // Vibrant tone for dark mode
    val bg = Color(0xFF111418)
    val onPrimaryColor = rawBase.mixWithBackground(Color.Black, 0.85f)
    return darkColorScheme(
        primary = base,
        onPrimary = onPrimaryColor,
        primaryContainer = rawBase.mixWithBackground(Color.Black, 0.35f),
        onPrimaryContainer = rawBase.mixWithBackground(Color.White, 0.9f),
        secondary = base.mixWithBackground(Color.Gray, 0.6f),
        secondaryContainer = rawBase.mixWithBackground(Color.Black, 0.25f),
        onSecondaryContainer = base.mixWithBackground(Color.White, 0.85f),
        tertiary = Color(0xFFFFB951),
        tertiaryContainer = Color(0xFF654100),
        onTertiaryContainer = Color(0xFFFFDDB3),
        background = bg,
        onBackground = Color(0xFFE1E2E8),
        surface = bg,
        onSurface = Color(0xFFE1E2E8),
        surfaceVariant = Color(0xFF43474E),
        onSurfaceVariant = Color(0xFFC4C7D0),
        outline = Color(0xFF8E9099),
        outlineVariant = Color(0xFF43474E),
        surfaceContainerLow = Color(0xFF191C20),
        surfaceContainer = Color(0xFF1D2024),
        surfaceContainerHigh = Color(0xFF282A2F),
        surfaceContainerHighest = Color(0xFF33353A),
    )
}

private fun generateAmoledScheme(accent: AccentColor): androidx.compose.material3.ColorScheme {
    val rawBase = Color(accent.colorValue)
    val base = rawBase.mixWithBackground(Color.White, 0.75f)
    val bg = Color.Black
    val onPrimaryColor = rawBase.mixWithBackground(Color.Black, 0.85f)
    return darkColorScheme(
        primary = base,
        onPrimary = onPrimaryColor,
        primaryContainer = rawBase.mixWithBackground(Color.Black, 0.35f),
        onPrimaryContainer = rawBase.mixWithBackground(Color.White, 0.9f),
        secondary = base.mixWithBackground(Color.Gray, 0.6f),
        secondaryContainer = rawBase.mixWithBackground(Color.Black, 0.25f),
        onSecondaryContainer = base.mixWithBackground(Color.White, 0.85f),
        tertiary = Color(0xFFFFB951),
        tertiaryContainer = Color(0xFF654100),
        onTertiaryContainer = Color(0xFFFFDDB3),
        background = bg,
        onBackground = Color(0xFFE1E2E8),
        surface = bg,
        onSurface = Color(0xFFE1E2E8),
        surfaceVariant = Color(0xFF1E2125),
        onSurfaceVariant = Color(0xFFC4C7D0),
        outline = Color(0xFF8E9099),
        outlineVariant = Color(0xFF33353A),
        surfaceContainerLow = Color(0xFF0D0E11),
        surfaceContainer = Color(0xFF131518),
        surfaceContainerHigh = Color(0xFF1B1D21),
        surfaceContainerHighest = Color(0xFF232529),
    )
}
