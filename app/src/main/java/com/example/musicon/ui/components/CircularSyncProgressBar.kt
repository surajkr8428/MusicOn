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

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = Color.White.copy(alpha = 0.05f)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) { // Giant 240dp (slightly smaller for padding)
            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                // Background Track
                drawCircle(
                    color = trackColor,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )

                // Progress Arc
                val sweep = if (isIndeterminate) 90f else (progress.coerceIn(0f, 1f) * 360f)
                val startAngle = if (isIndeterminate) rotation else -90f
                
                drawArc(
                    color = primaryColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Percentage Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!isIndeterminate && syncStatus !is SyncStatus.Idle) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 42.sp
                        ),
                        color = Color.White
                    )
                } else if (syncStatus is SyncStatus.Idle) {
                     Text(
                        text = "Ready",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        text = "...",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Song Count Text - Hero Status
        val statusText = when (syncStatus) {
            is SyncStatus.Loading -> {
                if (syncStatus.total > 0) "Uploading: ${syncStatus.current} / ${syncStatus.total}" else "Connecting..."
            }
            is SyncStatus.Success -> "${syncStatus.uploaded} Songs Uploaded"
            is SyncStatus.Error -> "Sync Paused"
            is SyncStatus.Idle -> "All Songs Up to Date"
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = if (syncStatus is SyncStatus.Error) Color.Red else Color.Gray
        )
    }
}
