package com.example.musicon.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BackgroundGridDialog(
    currentMode: String,
    onDismiss: () -> Unit,
    onModeSelected: (String) -> Unit
) {
    val modes = listOf(
        "DYNAMIC", "SPACE", "NEBULA", "AURORA", "SHINOBI", 
        "SUPERMAN", "SPIDERMAN", "BATMAN", "IRONMAN", 
        "NARUTO_SASUKE", "TOM_JERRY", "THOR", "CAPTAIN_AMERICA", "BLACK_PANTHER"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Background Animation", fontWeight = FontWeight.Bold) },
        text = {
            Box(Modifier.height(400.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(modes) { mode ->
                        val isSelected = currentMode == mode
                        Button(
                            onClick = { onModeSelected(mode); onDismiss() },
                            modifier = Modifier.height(45.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = mode.replace("_", " "),
                                fontSize = 8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
