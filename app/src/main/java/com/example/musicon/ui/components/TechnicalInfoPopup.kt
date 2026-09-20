package com.example.musicon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicon.data.local.TrackEntity
import com.example.musicon.logic.formatDuration
import java.io.File

@Composable
fun TechnicalInfoPopup(track: TrackEntity, onDismiss: () -> Unit) {
    val imagePath = track.customCoverPath ?: track.localPath
    val hasImage = imagePath != null && !imagePath.startsWith("http") && (if (imagePath.startsWith("content://")) true else File(imagePath).exists())

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text("Done") } },
        title = { Text("Song Specification", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary) },
        containerColor = Color(0xFF1E1B36),
        shape = RoundedCornerShape(16.dp),
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (hasImage || track.customCoverPath?.startsWith("http") == true) {
                    AsyncImage(
                        model = track.customCoverPath ?: track.localPath,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Column(Modifier.fillMaxWidth()) {
                    InfoLabelValue("Artist", track.displayArtist)
                    InfoLabelValue("Album", track.displayAlbum)
                    InfoLabelValue("Quality", track.bitrate ?: "320 kbps")
                    InfoLabelValue("Duration", formatDuration(track.duration))
                    InfoLabelValue("Source", if (track.gDriveId != null) "Cloud Synced" else "Local Storage")
                    Spacer(Modifier.height(8.dp))
                    Text("File Path:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(track.localPath ?: "Remote Google Drive", style = MaterialTheme.typography.bodySmall, color = Color.LightGray, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    )
}

@Composable
fun InfoLabelValue(label: String, value: String) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
