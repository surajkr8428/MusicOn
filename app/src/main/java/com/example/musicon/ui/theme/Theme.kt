package com.example.musicon.ui.theme

import android.app.Activity
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
    SPOTIFY_DARK,
    LIGHT,
    SYSTEM,
    DYNAMIC
}

private val SpotifyDarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = Color.Black,
    primaryContainer = SpotifyGreen,
    onPrimaryContainer = Color.Black,
    secondary = SpotifyGray,
    onSecondary = SpotifyWhite,
    background = SpotifyBlack,
    onBackground = SpotifyWhite,
    surface = SpotifyDarkGray,
    onSurface = SpotifyWhite,
    surfaceVariant = SpotifyGray,
    onSurfaceVariant = SpotifyLightGray
)

private val LightColorScheme = lightColorScheme(
    primary = SpotifyGreen,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = SpotifyBlack,
    surface = Color.White,
    onSurface = SpotifyBlack
)

@Composable
fun MusicOnTheme(
    themeMode: ThemeMode = ThemeMode.SPOTIFY_DARK,
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
        themeMode == ThemeMode.SPOTIFY_DARK -> SpotifyDarkColorScheme
        darkTheme -> darkColorScheme(primary = SpotifyGreen) // Fallback dark
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
