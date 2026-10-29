package com.gallery.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Custom dark color palette (deep slate + violet accent)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9C77F5),          // Soft violet
    onPrimary = Color(0xFF1A0F33),
    primaryContainer = Color(0xFF3D2C7A),
    onPrimaryContainer = Color(0xFFD9CAFF),
    secondary = Color(0xFF03DAC5),        // Teal accent
    onSecondary = Color(0xFF003734),
    secondaryContainer = Color(0xFF005049),
    onSecondaryContainer = Color(0xFF70EFDE),
    background = Color(0xFF0E0E12),       // Near-black
    onBackground = Color(0xFFE8E4F0),
    surface = Color(0xFF181820),
    onSurface = Color(0xFFE8E4F0),
    surfaceVariant = Color(0xFF252535),
    onSurfaceVariant = Color(0xFFBCB3CF),
    outline = Color(0xFF6B6680),
    error = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6A3DE8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8DEFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF018786),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF00504F),
    background = Color(0xFFF5F2FF),
    onBackground = Color(0xFF1C1B23),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B23),
    surfaceVariant = Color(0xFFEAE4F4),
    onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFF7B7585)
)

@Composable
fun GalleryAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
