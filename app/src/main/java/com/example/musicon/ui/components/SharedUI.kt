package com.example.musicon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicon.data.remote.SyncStatus

@Composable
fun HeaderStatusPill(isOnline: Boolean, isWifi: Boolean = false) {
    Surface(
        color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.height(30.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isOnline && isWifi) {
                Icon(Icons.Default.Wifi, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text = if (isOnline) "ONLINE" else "OFFLINE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = Color.White)
        }
    }
}

@Composable
fun SyncProgressBar(syncStatus: SyncStatus, modifier: Modifier = Modifier) {
    if (syncStatus is SyncStatus.Idle) return
    Box(modifier = modifier.height(4.dp).fillMaxWidth().background(Color.Gray.copy(0.2f))) {
        LinearProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxSize())
    }
}
