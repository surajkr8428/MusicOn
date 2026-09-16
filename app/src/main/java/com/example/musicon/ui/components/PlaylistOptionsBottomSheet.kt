package com.example.musicon.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicon.data.local.Playlist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistOptionsBottomSheet(
    playlist: Playlist,
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
            Text(
                text = playlist.name,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            PlaylistActionItem(Icons.Default.PlayArrow, "Play") { onAction("play") }
            PlaylistActionItem(Icons.Default.Edit, "Rename") { onAction("rename") }
            PlaylistActionItem(Icons.Default.Share, "Share") { onAction("share") }
            PlaylistActionItem(Icons.Default.RemoveCircleOutline, "Remove from list") { onAction("remove") }
            PlaylistActionItem(Icons.Default.Delete, "Delete Playlist", Color.Red) { onAction("delete") }
        }
    }
}

@Composable
fun PlaylistActionItem(icon: ImageVector, label: String, color: Color = Color.White, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
            Text(
                text = label,
                modifier = Modifier.padding(start = 20.dp),
                color = color,
                fontSize = 16.sp
            )
        }
    }
}
