package com.example.musicon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.musicon.ui.theme.NebulaPurple
import com.example.musicon.ui.theme.DeepNebula
import kotlin.random.Random

@Composable
fun StellarBackground(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NebulaPurple,
                        DeepNebula,
                        Color.Black
                    )
                )
            )
    ) {
        // High-density Star Field
        val stars = remember {
            List(200) {
                Offset(Random.nextFloat(), Random.nextFloat()) to (Random.nextFloat() * 1.5f + 0.5f)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            stars.forEach { (pos, radius) ->
                drawCircle(
                    color = Color.White.copy(alpha = Random.nextFloat() * 0.4f + 0.2f),
                    radius = radius,
                    center = Offset(pos.x * canvasWidth, pos.y * canvasHeight)
                )
            }
            
            // Layered Nebula Glows for better depth
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7B68EE).copy(alpha = 0.1f), Color.Transparent),
                    center = Offset(canvasWidth * 0.3f, canvasHeight * 0.2f),
                    radius = canvasWidth * 1.2f
                ),
                radius = canvasWidth * 1.2f,
                center = Offset(canvasWidth * 0.3f, canvasHeight * 0.2f)
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF00FF).copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(canvasWidth * 0.8f, canvasHeight * 0.7f),
                    radius = canvasWidth * 1.5f
                ),
                radius = canvasWidth * 1.5f,
                center = Offset(canvasWidth * 0.8f, canvasHeight * 0.7f)
            )
        }

        content()
    }
}
