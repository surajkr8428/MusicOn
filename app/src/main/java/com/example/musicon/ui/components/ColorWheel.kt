package com.example.musicon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun ColorWheel(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val hues = listOf(
        0f, 15f, 30f, 45f, 60f, 75f, 90f, 105f, 120f, 135f, 150f, 165f, 
        180f, 195f, 210f, 225f, 240f, 255f, 270f, 285f, 300f, 315f, 330f, 345f
    )

    BoxWithConstraints(modifier = modifier.aspectRatio(1f)) {
        val radius = constraints.maxWidth / 2f
        val center = Offset(radius, radius)

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    val distance = sqrt(dx * dx + dy * dy)
                    
                    if (distance <= radius) {
                        val angle = atan2(dy, dx) * (180 / PI).toFloat()
                        val normalizedAngle = (angle + 360) % 360
                        val hueIndex = ((normalizedAngle / (360f / hues.size)).toInt()) % hues.size
                        val hue = hues[hueIndex]
                        
                        // Determine shade/tint based on distance from center
                        val color = when {
                            distance < radius * 0.35f -> {
                                // Inner: Shade (Darker)
                                Color.hsv(hue, 1f, 0.6f)
                            }
                            distance < radius * 0.7f -> {
                                // Middle: Tint (Lighter/Desaturated)
                                Color.hsv(hue, 0.5f, 1f)
                            }
                            else -> {
                                // Outer: Pure Hue
                                Color.hsv(hue, 1f, 1f)
                            }
                        }
                        onColorSelected(color)
                    }
                }
            }
        ) {
            val sweepAngle = 360f / hues.size
            
            // Draw 3 layers for Pure, Tint, and Shade
            hues.forEachIndexed { index, hue ->
                val startAngle = index * sweepAngle
                
                // Outer ring: Pure
                drawArc(
                    color = Color.hsv(hue, 1f, 1f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = size
                )
                
                // Middle ring: Tint
                drawArc(
                    color = Color.hsv(hue, 0.5f, 1f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = size * 0.7f,
                    topLeft = Offset(size.width * 0.15f, size.height * 0.15f)
                )
                
                // Inner ring: Shade
                drawArc(
                    color = Color.hsv(hue, 1f, 0.6f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = size * 0.35f,
                    topLeft = Offset(size.width * 0.325f, size.height * 0.325f)
                )
            }
            
            // Center Selection Indicator
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = selectedColor,
                radius = 10.dp.toPx(),
                center = center
            )
        }
    }
}
