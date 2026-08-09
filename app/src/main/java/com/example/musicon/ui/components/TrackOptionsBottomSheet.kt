package com.example.musicon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicon.R
import com.example.musicon.data.local.TrackEntity
import com.example.musicon.ui.theme.LavenderTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsBottomSheet(
    track: TrackEntity,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF13112B),
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.customCoverPath ?: track.localPath ?: R.drawable.ic_launcher_foreground,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = track.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Cursive,
                            color = LavenderTitle,
                            fontSize = 20.sp
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "${track.displayArtist} | 321kbps",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }
                IconButton(onClick = { onAction("info") }) {
                    Icon(Icons.Default.Info, null, tint = Color.White.copy(alpha = 0.7f))
                }
                IconButton(onClick = { onAction("share") }) {
                    Icon(Icons.Default.Share, null, tint = Color.White.copy(alpha = 0.7f))
                }
            }

            // High-fidelity Action Grid
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                val gridActions = listOf(
                    Triple(Icons.Default.NotificationsActive, "Set as ringtone", "ringtone"),
                    Triple(Icons.Default.Image, "Change cover", "cover"),
                    Triple(Icons.Default.Label, "Edit tags", "tags"),
                    Triple(Icons.Default.Album, "Go to album", "album"),
                    Triple(Icons.Default.VisibilityOff, "Hide song", "hide"),
                    Triple(Icons.Default.DeleteOutline, "Delete from device", "delete")
                )

                for (i in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionBlock(gridActions[i*2], Modifier.weight(1f)) { onAction(it) }
                        ActionBlock(gridActions[i*2+1], Modifier.weight(1f)) { onAction(it) }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback Options
            ListItemAction(Icons.Default.PlayArrow, "Play now") { onAction("play") }
            ListItemAction(Icons.AutoMirrored.Filled.PlaylistPlay, "Play next") { onAction("play_next") }
            ListItemAction(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue") { onAction("add_to_queue") }
            ListItemAction(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist") { onAction("add_to_playlist") }
            ListItemAction(Icons.Default.RemoveCircleOutline, "Remove from library") { onAction("remove") }
        }
    }
}

@Composable
fun ActionBlock(
    action: Triple<ImageVector, String, String>,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    Surface(
        onClick = { onClick(action.third) },
        modifier = modifier.height(64.dp),
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.first,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = action.second,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ListItemAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(26.dp))
            Text(
                text = label,
                modifier = Modifier.padding(start = 20.dp),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
