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
        animationSpec = infiniteRepeatable(animation = tween(35000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "driftOffset"
    )
    val swirlRotation by backgroundTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(50000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "swirlRotation"
    )

    val cycleColor by backgroundTransition.animateColor(
        initialValue = Color(0xFF7C4DFF),
        targetValue = Color(0xFF00B0FF),
        animationSpec = infiniteRepeatable(animation = tween(15000), repeatMode = RepeatMode.Reverse),
        label = "cycleColor"
    )

    val characterModes = listOf("SHINOBI", "SUPERMAN", "SPIDERMAN", "BATMAN", "IRONMAN", "NARUTO_SASUKE", "TOM_JERRY", "THOR", "CAPTAIN_AMERICA", "BLACK_PANTHER", "GOKU")
    val isBright = (phase == DayPhase.DAY || phase == DayPhase.SUNRISE || themeMode == ThemeMode.LIGHT) && backgroundMode !in characterModes && backgroundMode != "AURORA"

    val leaves = remember { List(25) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 10f + 5f) } }
    val heroFigures = remember { List(8) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 0.5f + 0.5f) } }

    val shapes = remember {
        List(12) { 
            val x = Random.nextFloat()
            val y = Random.nextFloat()
            val size = Random.nextFloat() * 50f + 25f
            val type = Random.nextInt(3) // 0: Square, 1: Circle ring, 2: Triangle
            Triple(Offset(x, y), size, type)
        }
    }

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
                            shapes.forEachIndexed { index, (pos, size, type) ->
                                val shapeRotation = swirlRotation * (if (index % 2 == 0) 1.2f else -1.8f) + (index * 30f)
                                val shapeAlpha = 0.1f + (0.3f * sin((driftOffset * 3 * Math.PI + index).toFloat()).coerceIn(0f, 1f))
                                rotate(shapeRotation, Offset(pos.x * canvasWidth, pos.y * canvasHeight)) {
                                    when (type) {
                                        0 -> drawRect(color = cycleColor.copy(alpha = shapeAlpha), topLeft = Offset(pos.x * canvasWidth - size/2, pos.y * canvasHeight - size/2), size = androidx.compose.ui.geometry.Size(size, size), style = Stroke(width = 2.dp.toPx()))
                                        1 -> drawCircle(color = Color.White.copy(alpha = shapeAlpha), radius = (size / 2) * pulseScale, center = Offset(pos.x * canvasWidth, pos.y * canvasHeight), style = Stroke(width = 1.5.dp.toPx()))
                                        else -> {
                                            val p = Path().apply { moveTo(pos.x * canvasWidth, pos.y * canvasHeight - size/2); lineTo(pos.x * canvasWidth - size/2, pos.y * canvasHeight + size/2); lineTo(pos.x * canvasWidth + size/2, pos.y * canvasHeight + size/2); close() }
                                            drawPath(p, color = Color(0xFF00E5FF).copy(alpha = shapeAlpha), style = Stroke(width = 2.dp.toPx()))
                                        }
                                    }
                                }
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

                        // THEMED LOGO & CHARACTER ANIMATIONS
                        when (backgroundMode) {
                            "SUPERMAN" -> {
                                // Background Symbol
                                val sSize = canvasWidth * 0.6f
                                drawCircle(Color.Red.copy(0.05f), sSize/2, center)
                                val shieldPath = Path().apply {
                                    moveTo(center.x, center.y - sSize/2); lineTo(center.x + sSize/2, center.y - sSize/4)
                                    lineTo(center.x + sSize/3, center.y + sSize/2); lineTo(center.x - sSize/3, center.y + sSize/2)
                                    lineTo(center.x - sSize/2, center.y - sSize/4); close()
                                }
                                drawPath(shieldPath, Color(0xFFFBC02D).copy(0.1f))

                                // Flying Silhouettes
                                heroFigures.forEachIndexed { i, (x, y, scale) ->
                                    val fx = (x + driftOffset * (0.3f + i*0.05f)) % 1.2f - 0.1f
                                    val fy = (y + sin(driftOffset * 5f + i).toFloat() * 0.1f)
                                    rotate(-15f, Offset(fx * canvasWidth, fy * canvasHeight)) {
                                        drawRect(Color.Red.copy(0.3f), Offset(fx * canvasWidth - 10.dp.toPx(), fy * canvasHeight), androidx.compose.ui.geometry.Size(30.dp.toPx() * scale, 12.dp.toPx() * scale))
                                        drawCircle(Color.Blue.copy(0.4f), 8.dp.toPx() * scale, Offset(fx * canvasWidth, fy * canvasHeight))
                                    }
                                }
                            }
                            "SPIDERMAN" -> {
                                // Background Logo
                                val r = canvasWidth * 0.3f
                                drawCircle(Color.Red.copy(0.1f), r, center, style = Stroke(2.dp.toPx()))
                                for (j in 0..11) {
                                    val ang = (j * 30f) * (Math.PI / 180f).toFloat()
                                    drawLine(Color.Black.copy(0.1f), center, Offset(center.x + cos(ang)*r*2f, center.y + sin(ang)*r*2f))
                                }
                                // Crawling Figures
                                heroFigures.forEachIndexed { i, (x, y, scale) ->
                                    val move = (driftOffset + i * 0.2f) % 1f
                                    val sx = if (i % 2 == 0) move * canvasWidth else (1-move) * canvasWidth
                                    val sy = if (i % 3 == 0) y * canvasHeight else (0.1f + i*0.1f) * canvasHeight
                                    drawCircle(Color.Black.copy(0.4f), 6.dp.toPx() * scale, Offset(sx, sy))
                                    for(l in 0..7) {
                                        val la = (l * 45f + sin(driftOffset * 20f + i) * 20f) * (Math.PI/180f).toFloat()
                                        drawLine(Color.Black.copy(0.4f), Offset(sx, sy), Offset(sx + cos(la)*10.dp.toPx(), sy + sin(la)*10.dp.toPx()))
                                    }
                                }
                            }
                            "BATMAN" -> {
                                // Black Symbol Rain
                                for (i in 0..15) {
                                    val x = (0.1f + i * 0.1f) % 1f
                                    val y = (driftOffset * (1.5f + (i%3)*0.2f) + i*0.1f) % 1.1f - 0.1f
                                    val s = (15 + (i%5)*5).dp.toPx()
                                    rotate(180f, Offset(x * canvasWidth, y * canvasHeight)) {
                                        drawOval(Color.Black.copy(0.6f), Offset(x * canvasWidth - s, y * canvasHeight - s/2), androidx.compose.ui.geometry.Size(s*2, s))
                                        drawCircle(Color.Black.copy(0.6f), s/3, Offset(x * canvasWidth, y * canvasHeight - s/2))
                                    }
                                }
                                val bx = center.x + cos(driftOffset * 2 * Math.PI.toFloat()) * canvasWidth * 0.2f
                                val by = center.y * 0.4f + sin(driftOffset * 2 * Math.PI.toFloat()) * canvasHeight * 0.1f
                                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFEA00).copy(0.2f), Color.Transparent), Offset(bx, by), canvasWidth * 0.4f), canvasWidth * 0.4f, Offset(bx, by))
                            }
                            "IRONMAN" -> {
                                // Multi Reactors
                                heroFigures.forEachIndexed { i, (x, y, scale) ->
                                    val rx = x * canvasWidth; val ry = y * canvasHeight
                                    val br = (20 + (i % 3) * 10).dp.toPx() * (0.9f + pulseScale * 0.1f) * scale
                                    val gc = if (i % 2 == 0) Color(0xFF00E5FF) else Color(0xFFFFD740)
                                    drawCircle(gc.copy(0.2f), br * 1.5f, Offset(rx, ry))
                                    drawCircle(gc.copy(0.5f), br, Offset(rx, ry), style = Stroke(3.dp.toPx()))
                                }
                            }
                            "CAPTAIN_AMERICA" -> {
                                rotate(swirlRotation) {
                                    drawCircle(Color(0xFFC62828).copy(0.3f), canvasWidth * 0.45f, center)
                                    drawCircle(Color.White.copy(0.3f), canvasWidth * 0.35f, center)
                                    drawCircle(Color(0xFFC62828).copy(0.3f), canvasWidth * 0.25f, center)
                                    drawCircle(Color(0xFF1565C0).copy(0.4f), canvasWidth * 0.15f, center)
                                }
                                // Running Silhouette
                                val rx = (driftOffset * 1.5f % 1.2f - 0.1f) * canvasWidth
                                drawCircle(Color.DarkGray.copy(0.5f), 20.dp.toPx(), Offset(rx, canvasHeight * 0.85f))
                                drawLine(Color.DarkGray.copy(0.5f), Offset(rx, canvasHeight * 0.85f), Offset(rx - 10.dp.toPx(), canvasHeight * 0.85f + 30.dp.toPx()), 4.dp.toPx())
                            }
                            "BLACK_PANTHER" -> {
                                heroFigures.take(4).forEachIndexed { i, (x, y, s) ->
                                    val alpha = (0.2f + 0.3f * sin((driftOffset * 5 * Math.PI + i).toFloat()).coerceIn(0f, 1f))
                                    val px = x * canvasWidth; val py = y * canvasHeight
                                    rotate(sin(driftOffset.toDouble() + i).toFloat() * 20f, Offset(px, py)) {
                                        drawRect(Color.Black.copy(alpha = alpha + 0.2f), Offset(px - 20.dp.toPx(), py - 40.dp.toPx()), androidx.compose.ui.geometry.Size(40.dp.toPx() * s, 80.dp.toPx() * s))
                                    }
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
                        }
                    }
                }
            }

            val characterResId = when (backgroundMode) {
                "SHINOBI" -> "ic_shinobi"
                "SUPERMAN" -> "ic_superman"
                "SPIDERMAN" -> "ic_spiderman"
                "BATMAN" -> "ic_batman"
                "IRONMAN" -> "ic_ironman"
                "NARUTO_SASUKE" -> "ic_sasuke_naruto"
                "TOM_JERRY" -> "ic_tom_jerry"
                "THOR" -> "ic_thor"
                "CAPTAIN_AMERICA" -> "ic_captain_america"
                "BLACK_PANTHER" -> "ic_black_panther"
                "GOKU" -> "ic_goku"
                else -> null
            }
            
            if (characterResId != null && showInternalBackground) {
                val context = LocalContext.current
                val resId = context.resources.getIdentifier(characterResId, "drawable", context.packageName)
                if (resId != 0) {
                    Box(Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.BottomCenter) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(resId),
                            contentDescription = null,
                            modifier = Modifier.size(180.dp).offset(y = (sin(driftOffset * 2 * Math.PI) * 10).dp)
                        )
                    }
                }
            }
            content()
        }
    }
}
