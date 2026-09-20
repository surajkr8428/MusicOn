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
import androidx.compose.ui.text.font.FontFamily
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold) } },
        title = { 
            Text(
                "Technical Specification", 
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Cursive
                ),
                color = MaterialTheme.colorScheme.primary
            ) 
        },
        containerColor = Color(0xFF1E1B36), // Nirvaana Deep Blue
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(16.dp),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Compact Compact Display
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (hasImage || track.customCoverPath?.startsWith("http") == true) {
                        AsyncImage(
                            model = track.customCoverPath ?: track.localPath,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                        }
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column(Modifier.weight(1f)) {
                        Text(track.displayName, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
                        Text(track.displayArtist, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Gray)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Detailed Grid
                Column(Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(Modifier.height(8.dp))
                    
                    Row(Modifier.fillMaxWidth()) {
                        InfoItem("Album", track.displayAlbum, Modifier.weight(1f))
                        InfoItem("Bitrate", track.bitrate ?: "320 kbps", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        InfoItem("Duration", formatDuration(track.duration), Modifier.weight(1f))
                        InfoItem("Source", if (track.gDriveId != null) "Cloud" else "Local", Modifier.weight(1f))
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    Text("File Path:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.Gray)
                    Text(
                        track.localPath ?: "Remote Google Drive", 
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), 
                        color = Color.LightGray, 
                        maxLines = 2, 
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    )
}

@Composable
private fun InfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
