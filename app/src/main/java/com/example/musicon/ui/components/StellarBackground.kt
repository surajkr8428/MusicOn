package com.example.musicon.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin
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
                "SHINOBI", "NARUTO_SASUKE", "IRONMAN", "BATMAN", "BLACK_PANTHER", "THOR" -> DayPhase.NIGHT
                "SUPERMAN", "SPIDERMAN", "TOM_JERRY", "CAPTAIN_AMERICA" -> DayPhase.DAY
                else -> DayPhase.DAY
            }
        }
    }

    val topColor by animateColorAsState(
        targetValue = when (backgroundMode) {
            "SUPERMAN" -> Color(0xFF1565C0)
            "BATMAN" -> Color(0xFF121212)
            "IRONMAN" -> Color(0xFFB71C1C)
            "NARUTO_SASUKE" -> Color(0xFF4A148C)
            "BLACK_PANTHER" -> Color(0xFF000000)
            "SPIDERMAN" -> Color(0xFFD32F2F)
            else -> when (phase) {
                DayPhase.SUNRISE -> if (themeMode == ThemeMode.LIGHT) Color(0xFFFFCCBC) else Color(0xFFFF9E80)
                DayPhase.DAY -> if (themeMode == ThemeMode.LIGHT) Color(0xFFE1F5FE) else Color(0xFF4FC3F7)
                DayPhase.SUNSET -> if (themeMode == ThemeMode.LIGHT) Color(0xFFFFAB91) else Color(0xFFFF7043)
                DayPhase.NIGHT -> if (themeMode == ThemeMode.LIGHT) Color(0xFFECEFF1) else Color(0xFF0F0B21)
            }
        },
        animationSpec = tween(2000), label = "topColor"
    )
    
    val bottomColor by animateColorAsState(
        targetValue = when (backgroundMode) {
            "SUPERMAN" -> Color(0xFFC62828)
            "BATMAN" -> Color(0xFF37474F)
            "IRONMAN" -> Color(0xFFFF8F00)
            "NARUTO_SASUKE" -> Color(0xFF1A237E)
            "BLACK_PANTHER" -> Color(0xFF311B92)
            "SPIDERMAN" -> Color(0xFF1976D2)
            else -> when (phase) {
                DayPhase.SUNRISE -> if (themeMode == ThemeMode.LIGHT) Color(0xFFFFE0B2) else Color(0xFFFB8C00)
                DayPhase.DAY -> if (themeMode == ThemeMode.LIGHT) Color(0xFFFFFFFF) else Color(0xFF0288D1)
                DayPhase.SUNSET -> if (themeMode == ThemeMode.LIGHT) Color(0xFFFFCCBC) else Color(0xFFBF360C)
                DayPhase.NIGHT -> if (themeMode == ThemeMode.LIGHT) Color(0xFFCFD8DC) else Color(0xFF090716)
            }
        },
        animationSpec = tween(2000), label = "bottomColor"
    )

    val backgroundTransition = rememberInfiniteTransition(label = "background")
    val pulseScale by backgroundTransition.animateFloat(
        initialValue = 1f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(animation = tween(10000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulseScale"
    )
    val driftOffset by backgroundTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(40000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "driftOffset"
    )
    val swirlRotation by backgroundTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(60000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "swirlRotation"
    )

    val cycleColor by backgroundTransition.animateColor(
        initialValue = Color(0xFF7C4DFF),
        targetValue = Color(0xFF00B0FF),
        animationSpec = infiniteRepeatable(animation = tween(15000), repeatMode = RepeatMode.Reverse),
        label = "cycleColor"
    )

    val characterModes = listOf("SHINOBI", "SUPERMAN", "SPIDERMAN", "BATMAN", "IRONMAN", "NARUTO_SASUKE", "TOM_JERRY", "THOR", "CAPTAIN_AMERICA", "BLACK_PANTHER")
    val isBright = (phase == DayPhase.DAY || phase == DayPhase.SUNRISE || themeMode == ThemeMode.LIGHT) && backgroundMode !in characterModes && backgroundMode != "AURORA"

    val leaves = remember { List(25) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 10f + 5f) } }

    CompositionLocalProvider(LocalIsBackgroundBright provides isBright) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showInternalBackground) {
                if (customBgUri != null) {
                    AsyncImage(model = customBgUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    val overlayAlpha = if (themeMode == ThemeMode.LIGHT) 0.3f else 0.65f
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = overlayAlpha)))
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(topColor, bottomColor))))

                    val stars = remember { 
                        List(120) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 1.6f + 0.2f) } 
                    }
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val center = Offset(canvasWidth / 2, canvasHeight / 2)
                        
                        if ((backgroundMode == "SPACE" || phase == DayPhase.NIGHT) && themeMode != ThemeMode.LIGHT) {
                            stars.forEach { (x, y, radius) ->
                                val alpha = 0.2f + (0.5f * sin((driftOffset * 4 * Math.PI + x * 20).toFloat()).coerceIn(0f, 1f))
                                drawCircle(color = Color.White.copy(alpha = alpha), radius = radius, center = Offset(x * canvasWidth, y * canvasHeight))
                            }
                        }
                        
                        if (backgroundMode == "NEBULA") {
                            rotate(swirlRotation) {
                                drawCircle(
                                    brush = Brush.radialGradient(colors = listOf(cycleColor.copy(0.2f), Color.Transparent), center = center, radius = canvasWidth * 0.8f * pulseScale),
                                    radius = canvasWidth * 0.8f * pulseScale, center = center
                                )
                            }
                        }
                        
                        if (backgroundMode == "AURORA") {
                            val auroraColor = if (themeMode == ThemeMode.LIGHT) Color(0xFF00796B) else Color(0xFF1DE9B6)
                            for (i in 0..5) {
                                val yPos = canvasHeight * (0.4f + (i * 0.08f))
                                val alpha = (0.5f - (i * 0.08f)) * pulseScale
                                drawCircle(
                                    brush = Brush.verticalGradient(colors = listOf(Color.Transparent, auroraColor.copy(alpha = alpha), Color.Transparent)),
                                    center = Offset(canvasWidth * ((driftOffset + i * 0.2f) % 1f), yPos),
                                    radius = canvasHeight * 0.25f
                                )
                            }
                        }

                        // THEMED LOGO ANIMATIONS
                        when (backgroundMode) {
                            "SUPERMAN" -> {
                                for (i in 0..5) {
                                    val x = (0.2f + i * 0.15f + driftOffset * 0.1f) % 1f
                                    val y = (0.8f - i * 0.1f - driftOffset * 0.3f) % 1f
                                    val s = 40.dp.toPx()
                                    rotate(driftOffset * 60f + i * 45, Offset(x * canvasWidth, y * canvasHeight)) {
                                        val p = Path().apply {
                                            moveTo(x * canvasWidth, y * canvasHeight - s)
                                            lineTo(x * canvasWidth + s, y * canvasHeight - s*0.2f)
                                            lineTo(x * canvasWidth + s*0.6f, y * canvasHeight + s)
                                            lineTo(x * canvasWidth - s*0.6f, y * canvasHeight + s)
                                            lineTo(x * canvasWidth - s, y * canvasHeight - s*0.2f)
                                            close()
                                        }
                                        drawPath(p, Color(0xFFFBC02D).copy(0.3f)) // Yellow Shield
                                    }
                                }
                            }
                            "SPIDERMAN" -> {
                                for (i in 1..4) {
                                    val r = (canvasWidth * 0.3f * i * pulseScale) % (canvasWidth * 1.5f)
                                    drawCircle(Color.White.copy(0.1f), r, center, style = Stroke(1.dp.toPx()))
                                    for (j in 0..7) {
                                        val ang = (j * 45f) * (Math.PI / 180f).toFloat()
                                        drawLine(Color.White.copy(0.1f), center, Offset(center.x + cos(ang)*r, center.y + sin(ang)*r))
                                    }
                                }
                            }
                            "BATMAN" -> {
                                val bx = center.x + cos(driftOffset * 2 * Math.PI.toFloat()) * canvasWidth * 0.2f
                                val by = center.y * 0.6f + sin(driftOffset * 2 * Math.PI.toFloat()) * canvasHeight * 0.1f
                                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFEA00).copy(0.2f), Color.Transparent), Offset(bx, by), canvasWidth * 0.4f), canvasWidth * 0.4f, Offset(bx, by))
                                rotate(driftOffset * 30f, Offset(bx, by)) {
                                    drawOval(Color.Black.copy(0.4f), Offset(bx - 60.dp.toPx(), by - 25.dp.toPx()), androidx.compose.ui.geometry.Size(120.dp.toPx(), 50.dp.toPx()))
                                }
                            }
                            "IRONMAN" -> {
                                drawCircle(Color(0xFF00E5FF).copy(0.15f * pulseScale), 80.dp.toPx() * pulseScale, center, style = Stroke(10.dp.toPx()))
                                drawCircle(Color.White.copy(0.3f), 30.dp.toPx(), center)
                            }
                            "CAPTAIN_AMERICA" -> {
                                rotate(swirlRotation) {
                                    drawCircle(Color.Red.copy(0.2f), canvasWidth * 0.45f, center)
                                    drawCircle(Color.White.copy(0.2f), canvasWidth * 0.35f, center)
                                    drawCircle(Color.Red.copy(0.2f), canvasWidth * 0.25f, center)
                                    drawCircle(Color.Blue.copy(0.3f), canvasWidth * 0.15f, center)
                                }
                            }
                            "NARUTO_SASUKE", "SHINOBI" -> {
                                leaves.forEachIndexed { i, (x, y, s) ->
                                    val ly = (y + driftOffset) % 1f
                                    rotate(driftOffset * 360f + i * 20, Offset(x * canvasWidth, ly * canvasHeight)) {
                                        drawRect(Color(0xFFFF7043).copy(0.5f), Offset(x * canvasWidth, ly * canvasHeight), androidx.compose.ui.geometry.Size(s, s))
                                    }
                                }
                            }
                            "BLACK_PANTHER" -> {
                                for (i in 0..3) {
                                    val r = (canvasWidth * 0.3f + i * 60.dp.toPx() + driftOffset * 100.dp.toPx()) % canvasWidth
                                    drawCircle(Color(0xFF7B1FA2).copy(0.2f), r, center, style = Stroke(4.dp.toPx()))
                                }
                            }
                        }
                    }
                }
            }
            content()
        }
    }
}
