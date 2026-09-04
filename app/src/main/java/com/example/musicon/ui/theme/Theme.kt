package com.example.musicon.ui.theme

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

enum class ThemeMode {
    SPOTIFY_DARK, LIGHT, SYSTEM, DYNAMIC
}

@Composable
fun MusicOnTheme(
    themeMode: ThemeMode = ThemeMode.SPOTIFY_DARK,
    accentColor: Color = Color(0xFFBB86FC),
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SPOTIFY_DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DYNAMIC -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        themeMode == ThemeMode.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color.Black,
            secondary = accentColor.copy(alpha = 0.8f),
            onSecondary = Color.Black,
            tertiary = Color(0xFF03DAC5),
            background = Color(0xFF0D0B1F),
            surface = Color(0xFF1E1B36),
            onSurface = Color.White,
            onBackground = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.3f),
            onPrimaryContainer = Color.White,
            outline = accentColor.copy(alpha = 0.5f)
        )
        else -> lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            secondary = accentColor.copy(alpha = 0.8f),
            onSecondary = Color.White,
            background = Color(0xFFF5F5F7), // Apple-style soft grey/white
            surface = Color.White,
            onSurface = Color.Black,
            onBackground = Color.Black,
            primaryContainer = accentColor.copy(alpha = 0.1f),
            onPrimaryContainer = accentColor,
            outline = Color.LightGray
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
