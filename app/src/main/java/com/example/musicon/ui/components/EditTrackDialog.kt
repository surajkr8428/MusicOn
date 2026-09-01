package com.example.musicon.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musicon.R
import com.example.musicon.data.local.TrackEntity
import java.io.File

@Composable
fun EditTrackDialog(
    track: TrackEntity,
    onDismiss: () -> Unit,
    onConfirm: (title: String, artist: String, album: String, coverPath: String?, lyrics: String?) -> Unit
) {
    var title by remember { mutableStateOf(track.displayName) }
    var artist by remember { mutableStateOf(track.displayArtist) }
    var album by remember { mutableStateOf(track.displayAlbum) }
    var lyrics by remember { mutableStateOf(track.lyrics ?: "") }
    var coverPath by remember { mutableStateOf(track.customCoverPath) }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coverPath = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Track Info") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cover Picker
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center
                ) {
                    val model = remember(coverPath) {
                        if (coverPath != null) {
                            if (coverPath!!.startsWith("content://")) Uri.parse(coverPath) else File(coverPath!!)
                        } else {
                            track.localPath?.let { if (it.startsWith("content://")) Uri.parse(it) else File(it) } ?: R.drawable.ic_launcher_foreground
                        }
                    }
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                    Icon(Icons.Default.Image, null, tint = Color.White)
                }

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("Album") }, modifier = Modifier.fillMaxWidth())
                
                OutlinedTextField(
                    value = lyrics,
                    onValueChange = { lyrics = it },
                    label = { Text("Lyrics (Plain or .LRC format)") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text("[00:10.00]Line 1\n[00:15.00]Line 2") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, artist, album, coverPath, lyrics.ifBlank { null }) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
