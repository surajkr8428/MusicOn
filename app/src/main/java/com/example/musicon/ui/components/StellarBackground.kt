package com.example.musicon.ui.components

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
import kotlin.random.Random

val LocalCustomBackground = staticCompositionLocalOf<String?> { null }

@Composable
fun StellarBackground(
    showInternalBackground: Boolean = true,
    content: @Composable () -> Unit
) {
    val customBgUri = LocalCustomBackground.current
    
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
                // High-fidelity background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0F0B21),
                                    Color(0xFF161132),
                                    Color(0xFF090716)
                                )
                            )
                        )
                )

                val stars = remember {
                    List(150) {
                        Offset(Random.nextFloat(), Random.nextFloat()) to (Random.nextFloat() * 1.2f + 0.3f)
                    }
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Nebula Clouds
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF6200EE).copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(canvasWidth * 0.2f, canvasHeight * 0.3f),
                            radius = canvasWidth * 1.5f
                        ),
                        radius = canvasWidth * 1.5f,
                        center = Offset(canvasWidth * 0.2f, canvasHeight * 0.3f)
                    )
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF03DAC5).copy(alpha = 0.08f), Color.Transparent),
                            center = Offset(canvasWidth * 0.8f, canvasHeight * 0.8f),
                            radius = canvasWidth * 1.2f
                        ),
                        radius = canvasWidth * 1.2f,
                        center = Offset(canvasWidth * 0.8f, canvasHeight * 0.8f)
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFBB86FC).copy(alpha = 0.07f), Color.Transparent),
                            center = Offset(canvasWidth * 0.5f, canvasHeight * 0.5f),
                            radius = canvasWidth * 1.0f
                        ),
                        radius = canvasWidth * 1.0f,
                        center = Offset(canvasWidth * 0.5f, canvasHeight * 0.5f)
                    )
                    
                    // Distant Stars
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

        content()
    }
}
