package com.example.musicon.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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

val LocalCustomBackground = staticCompositionLocalOf<String?> { null }

enum class DayPhase { SUNRISE, DAY, SUNSET, NIGHT }

@Composable
fun StellarBackground(
    showInternalBackground: Boolean = true,
    content: @Composable () -> Unit
) {
    val customBgUri = LocalCustomBackground.current
    
    // Determine Day Phase based on Real Time
    val time = LocalTime.now()
    val phase = when (time.hour) {
        in 5..7 -> DayPhase.SUNRISE
        in 8..16 -> DayPhase.DAY
        in 17..19 -> DayPhase.SUNSET
        else -> DayPhase.NIGHT
    }

    // Dynamic Colors based on Phase
    val topColor by animateColorAsState(
        targetValue = when (phase) {
            DayPhase.SUNRISE -> Color(0xFFFF9E80)
            DayPhase.DAY -> Color(0xFF4FC3F7)
            DayPhase.SUNSET -> Color(0xFFFF7043)
            DayPhase.NIGHT -> Color(0xFF0F0B21)
        },
        animationSpec = tween(2000), label = "topColor"
    )
    
    val bottomColor by animateColorAsState(
        targetValue = when (phase) {
            DayPhase.SUNRISE -> Color(0xFFFB8C00)
            DayPhase.DAY -> Color(0xFF0288D1)
            DayPhase.SUNSET -> Color(0xFFBF360C)
            DayPhase.NIGHT -> Color(0xFF090716)
        },
        animationSpec = tween(2000), label = "bottomColor"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (showInternalBackground) {
            if (customBgUri != null) {
                AsyncImage(
                    model = customBgUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)))
            } else {
                // Animated Dynamic Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors = listOf(topColor, bottomColor)))
                )

                val stars = remember {
                    List(150) {
                        Offset(Random.nextFloat(), Random.nextFloat()) to (Random.nextFloat() * 1.2f + 0.3f)
                    }
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Sun or Moon based on Phase
                    if (phase != DayPhase.NIGHT) {
                        drawCircle(
                            color = Color(0xFFFFEE58).copy(alpha = 0.4f),
                            radius = canvasWidth * 0.15f,
                            center = Offset(canvasWidth * 0.8f, canvasHeight * 0.2f)
                        )
                    }
                    
                    // Nebula Clouds (More visible at night/sunset)
                    val cloudAlpha = if (phase == DayPhase.NIGHT || phase == DayPhase.SUNSET) 0.12f else 0.04f
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF6200EE).copy(alpha = cloudAlpha), Color.Transparent),
                            center = Offset(canvasWidth * 0.2f, canvasHeight * 0.3f),
                            radius = canvasWidth * 1.5f
                        ),
                        radius = canvasWidth * 1.5f,
                        center = Offset(canvasWidth * 0.2f, canvasHeight * 0.3f)
                    )
                    
                    // Distant Stars (Only visible at Night/Sunrise/Sunset)
                    if (phase != DayPhase.DAY) {
                        stars.forEach { (pos, radius) ->
                            drawCircle(
                                color = Color.White.copy(alpha = Random.nextFloat() * 0.3f + 0.1f),
                                radius = radius,
                                center = Offset(pos.x * canvasWidth, pos.y * canvasHeight)
                            )
                        }
                    }
                }
            }
        }

        content()
    }
}
