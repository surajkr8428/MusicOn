package com.example.musicon.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.musicon.data.local.TrackEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOptionsBottomSheet(
    track: TrackEntity,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF13112B)
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            ListItemAction(Icons.Default.Info, "Song Details") { onAction("info") }
            ListItemAction(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to Playlist") { onAction("playlist") }
            ListItemAction(Icons.Default.Share, "Share Song") { onAction("share") }
            ListItemAction(Icons.Default.Timer, "Sleep Timer") { onAction("timer") }
            ListItemAction(Icons.Default.Edit, "Edit Tags") { onAction("edit") }
        }
    }
}
