package com.owl.minerva.stocklab.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = StockLabPrimaryLight,
    onPrimary = StockLabPrimaryDark,
    primaryContainer = StockLabPrimary,
    onPrimaryContainer = StockLabPrimaryLight,
    secondary = StockLabSecondaryLight,
    onSecondary = StockLabSecondaryDark,
    secondaryContainer = StockLabSecondary,
    onSecondaryContainer = StockLabSecondaryLight,
    tertiary = StockLabAccent,
    onTertiary = StockLabPrimaryLight,
    background = StockLabBackgroundDark,
    onBackground = StockLabPrimaryLight,
    surface = StockLabSurfaceDark,
    onSurface = StockLabPrimaryLight,
    error = StockLabDanger
)

private val LightColorScheme = lightColorScheme(
    primary = StockLabPrimary,
    onPrimary = StockLabSurface,
    primaryContainer = StockLabPrimaryLight,
    onPrimaryContainer = StockLabPrimaryDark,
    secondary = StockLabSecondary,
    onSecondary = StockLabSurface,
    secondaryContainer = StockLabSecondaryLight,
    onSecondaryContainer = StockLabSecondaryDark,
    tertiary = StockLabAccent,
    onTertiary = StockLabSurface,
    background = StockLabBackground,
    onBackground = StockLabPrimaryDark,
    surface = StockLabSurface,
    onSurface = StockLabPrimaryDark,
    error = StockLabDanger

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun StockLabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
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
        content = content
    )
}
