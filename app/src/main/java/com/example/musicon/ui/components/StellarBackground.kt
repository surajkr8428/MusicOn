package com.example.musicon.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.time.LocalTime
import kotlin.random.Random
import com.example.musicon.ui.theme.ThemeMode

val LocalCustomBackground = staticCompositionLocalOf<String?> { null }
val LocalIsBackgroundBright = compositionLocalOf { false }

enum class DayPhase { SUNRISE, DAY, SUNSET, NIGHT }

@Composable
fun StellarBackground(
    showInternalBackground: Boolean = true,
    themeMode: ThemeMode = ThemeMode.SPOTIFY_DARK,
    backgroundMode: String = "DYNAMIC",
    content: @Composable () -> Unit
) {
    val customBgUri = LocalCustomBackground.current
    
    val time = LocalTime.now()
    val phase = remember(backgroundMode) {
        if (backgroundMode == "DYNAMIC") {
            when (time.hour) {
                in 5..7 -> DayPhase.SUNRISE
                in 8..16 -> DayPhase.DAY
                in 17..19 -> DayPhase.SUNSET
                else -> DayPhase.NIGHT
            }
        } else {
            when (backgroundMode) {
                "NEBULA" -> DayPhase.NIGHT
                "AURORA" -> DayPhase.SUNRISE
                "SPACE" -> DayPhase.NIGHT
                else -> DayPhase.DAY
            }
        }
    }

    val topColor by animateColorAsState(
        targetValue = when (phase) {
            DayPhase.SUNRISE -> if (themeMode == ThemeMode.LIGHT) Color(0xFFFFCCBC) else Color(0xFFFF9E80)
            DayPhase.DAY -> if (themeMode == ThemeMode.LIGHT) Color(0xFFE1F5FE) else Color(0xFF4FC3F7)
            DayPhase.SUNSET -> if (themeMode == ThemeMode.LIGHT) Color(0xFFFFAB91) else Color(0xFFFF7043)
            DayPhase.NIGHT -> if (themeMode == ThemeMode.LIGHT) Color(0xFFECEFF1) else Color(0xFF0F0B21)
        },
        animationSpec = tween(2000), label = "topColor"
    )
    
    val bottomColor by animateColorAsState(
        targetValue = when (phase) {
            DayPhase.SUNRISE -> if (themeMode == ThemeMode.LIGHT) Color(0xFFFFE0B2) else Color(0xFFFB8C00)
            DayPhase.DAY -> if (themeMode == ThemeMode.LIGHT) Color(0xFFFFFFFF) else Color(0xFF0288D1)
            DayPhase.SUNSET -> if (themeMode == ThemeMode.LIGHT) Color(0xFFFFCCBC) else Color(0xFFBF360C)
            DayPhase.NIGHT -> if (themeMode == ThemeMode.LIGHT) Color(0xFFCFD8DC) else Color(0xFF090716)
        },
        animationSpec = tween(2000), label = "bottomColor"
    )

    val isBright = phase == DayPhase.DAY || phase == DayPhase.SUNRISE || themeMode == ThemeMode.LIGHT

    CompositionLocalProvider(LocalIsBackgroundBright provides isBright) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showInternalBackground) {
                if (customBgUri != null) {
                    AsyncImage(model = customBgUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    val overlayAlpha = if (themeMode == ThemeMode.LIGHT) 0.3f else 0.65f
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = overlayAlpha)))
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(topColor, bottomColor))))

                    val stars = remember { List(120) { Offset(Random.nextFloat(), Random.nextFloat()) to (Random.nextFloat() * 1.6f + 0.2f) } }
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        
                        if (phase != DayPhase.NIGHT || themeMode == ThemeMode.LIGHT) {
                            val sunColor = if (themeMode == ThemeMode.LIGHT) Color(0xFFFFD54F) else Color(0xFFFFEE58)
                            drawCircle(
                                color = sunColor.copy(alpha = 0.3f),
                                radius = canvasWidth * 0.12f,
                                center = Offset(canvasWidth * 0.85f, canvasHeight * 0.15f)
                            )
                        }

                        if ((backgroundMode == "SPACE" || phase == DayPhase.NIGHT) && themeMode != ThemeMode.LIGHT) {
                            stars.forEach { (pos, radius) ->
                                drawCircle(color = Color.White.copy(alpha = 0.4f), radius = radius, center = Offset(pos.x * canvasWidth, pos.y * canvasHeight))
                            }
                        }
                        
                        if (backgroundMode == "NEBULA") {
                            val nebulaColor = if (themeMode == ThemeMode.LIGHT) Color(0xFFB39DDB) else Color(0xFF7C4DFF)
                            drawCircle(
                                brush = Brush.radialGradient(colors = listOf(nebulaColor.copy(0.15f), Color.Transparent), center = Offset(canvasWidth * 0.3f, canvasHeight * 0.4f), radius = canvasWidth * 0.8f),
                                radius = canvasWidth * 0.8f, center = Offset(canvasWidth * 0.3f, canvasHeight * 0.4f)
                            )
                        }
                        
                        if (backgroundMode == "AURORA") {
                            val auroraColor = if (themeMode == ThemeMode.LIGHT) Color(0xFFB2DFDB) else Color(0xFF1DE9B6)
                            drawCircle(
                                brush = Brush.verticalGradient(colors = listOf(Color.Transparent, auroraColor.copy(alpha = 0.1f), Color.Transparent)),
                                center = Offset(0f, canvasHeight * 0.7f),
                                radius = canvasHeight * 0.2f
                            )
                        }
                    }
                }
            }
            content()
        }
    }
}
