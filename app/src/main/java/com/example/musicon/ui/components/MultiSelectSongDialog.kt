package com.example.musicon.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicon.data.local.TrackEntity

@Composable
fun MultiSelectSongDialog(
    allTracks: List<TrackEntity>,
    existingTrackIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var selectedSongs by remember { mutableStateOf(setOf<String>()) }
    val filteredTracks = remember(allTracks, existingTrackIds) {
        allTracks.filter { it.id !in existingTrackIds }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Songs to Playlist", fontWeight = FontWeight.Bold) },
        text = {
            Box(modifier = Modifier.sizeIn(maxHeight = 400.dp)) {
                if (filteredTracks.isEmpty()) {
                    Text("All songs are already in this playlist", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn {
                        items(filteredTracks) { track ->
                            val isChecked = track.id in selectedSongs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSongs = if (isChecked) selectedSongs - track.id else selectedSongs + track.id
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        selectedSongs = if (isChecked) selectedSongs - track.id else selectedSongs + track.id
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(track.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                    Text(track.displayArtist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSongs.isNotEmpty()) {
                        onConfirm(selectedSongs.toList())
                    }
                    onDismiss()
                },
                enabled = selectedSongs.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
