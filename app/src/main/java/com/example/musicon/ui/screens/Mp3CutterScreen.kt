package com.example.musicon.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicon.data.local.TrackEntity
import com.example.musicon.ui.components.StellarBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Mp3CutterScreen(
    track: TrackEntity,
    onBack: () -> Unit
) {
    var startRange by remember { mutableFloatStateOf(0f) }
    var endRange by remember { mutableFloatStateOf(track.duration.toFloat()) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    BackHandler(onBack = onBack)

    StellarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    modifier = if (isLandscape) Modifier.height(IntrinsicSize.Min) else Modifier,
                    title = { Text("MP3 Cutter", color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 18.sp else 22.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = if (isLandscape) Modifier.size(36.dp) else Modifier) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = if (isLandscape) Modifier.size(20.dp) else Modifier)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(track.displayName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(track.displayArtist, color = Color.Gray, fontSize = 14.sp)
                
                Spacer(Modifier.height(48.dp))
                
                // Placeholder for Waveform
                Box(
                    Modifier.fillMaxWidth().height(120.dp).background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Waveform Visualization", color = Color.Gray)
                }
                
                Spacer(Modifier.height(32.dp))
                
                RangeSlider(
                    value = startRange..endRange,
                    onValueChange = { range ->
                        startRange = range.start
                        endRange = range.endInclusive
                    },
                    valueRange = 0f..track.duration.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
                )
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(startRange.toLong()), color = Color.White)
                    Text(formatTime(endRange.toLong()), color = Color.White)
                }
                
                Spacer(Modifier.weight(1f))
                
                Button(
                    onClick = { /* Logic to trim and save */ },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Cut & Save as Ringtone", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
