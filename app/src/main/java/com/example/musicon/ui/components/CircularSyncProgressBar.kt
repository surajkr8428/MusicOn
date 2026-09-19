package com.example.musicon.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicon.data.remote.SyncStatus

@Composable
fun CircularSyncProgressBar(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier
) {
    val progress = when (syncStatus) {
        is SyncStatus.Loading -> syncStatus.progress
        is SyncStatus.Success -> 1f
        else -> 0f
    }

    val isIndeterminate = progress < 0f && syncStatus is SyncStatus.Loading
    
    val infiniteTransition = rememberInfiniteTransition(label = "indeterminate_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val accentColor = MaterialTheme.colorScheme.primary
    val trackColor = Color.White.copy(alpha = 0.05f)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Double the circle size: 240dp
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Background Track
                drawCircle(
                    color = trackColor,
                    style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                )

                // Progress Arc
                val sweep = if (isIndeterminate) 90f else (progress.coerceIn(0f, 1f) * 360f)
                val startAngle = if (isIndeterminate) rotation else -90f
                
                drawArc(
                    color = accentColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Percentage Text - Dynamic & Big
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!isIndeterminate && syncStatus !is SyncStatus.Idle) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 48.sp
                        ),
                        color = accentColor
                    )
                } else if (syncStatus is SyncStatus.Idle) {
                     Text(
                        text = "READY",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = accentColor.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        text = "...",
                        style = MaterialTheme.typography.displayLarge,
                        color = accentColor
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Song Count Text - Always visible status
        val statusText = when (syncStatus) {
            is SyncStatus.Loading -> {
                if (syncStatus.total > 0) "Uploading: ${syncStatus.current} / ${syncStatus.total}" else "Connecting..."
            }
            is SyncStatus.Success -> "Success: ${syncStatus.uploaded} Uploaded"
            is SyncStatus.Error -> "Sync Paused"
            is SyncStatus.Idle -> "Library is Synchronized"
        }

        Text(
            text = statusText.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            ),
            color = if (syncStatus is SyncStatus.Error) Color.Red else Color.Gray
        )
    }
}
