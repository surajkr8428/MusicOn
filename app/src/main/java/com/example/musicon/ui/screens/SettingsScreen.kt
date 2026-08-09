package com.example.musicon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicon.ui.components.StellarBackground
import com.example.musicon.ui.theme.CategoryPurple
import com.example.musicon.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onSignInClick: () -> Unit,
    onScanClick: () -> Unit,
    onBack: () -> Unit
) {
    val isUserSignedIn by viewModel.isUserSignedIn
    val pauseOnDetach by viewModel.pauseOnDetach.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val showNotifications by viewModel.showNotifications.collectAsState()
    val crossfade by viewModel.crossfade.collectAsState()

    StellarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                item {
                    SettingsHeader("General")
                    StellarSettingsItem(Icons.Default.Scanner, "Scan local music", "Search for local files") {
                        onScanClick()
                    }
                    StellarSettingsItem(Icons.Default.CloudSync, "Sync GDrive", "Sync with Google Drive") {
                        viewModel.syncCloudTracks()
                    }
                    StellarSettingsItem(Icons.Default.Backup, "Backup & restore", "Backup your database to cloud (Coming soon)")
                }

                item {
                    SettingsHeader("Playback")
                    StellarSettingsToggle(
                        Icons.Default.PauseCircle, 
                        "Pause on detach", 
                        "Pause playback when headphone is detached", 
                        pauseOnDetach
                    ) { viewModel.updatePauseOnDetach(it) }
                    
                    StellarSettingsToggle(
                        Icons.Default.BlurOn, 
                        "Crossfade", 
                        "Previous song fades out, next song fades in", 
                        crossfade
                    ) { viewModel.updateCrossfade(it) }
                }

                item {
                    SettingsHeader("Preference")
                    StellarSettingsToggle(
                        Icons.Default.WbSunny, 
                        "Keep screen on", 
                        "Stay on while on the player screen", 
                        keepScreenOn
                    ) { viewModel.updateKeepScreenOn(it) }
                    
                    StellarSettingsToggle(
                        Icons.Default.Notifications, 
                        "Notifications", 
                        "Show playback controls in notification", 
                        showNotifications
                    ) { viewModel.updateShowNotifications(it) }
                }
                
                item {
                    SettingsHeader("Account")
                    if (!isUserSignedIn) {
                        Button(
                            onClick = onSignInClick,
                            modifier = Modifier.padding(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                        ) {
                            Text("Sign in with Google")
                        }
                    } else {
                        StellarSettingsItem(Icons.Default.AccountCircle, "Signed In", "Connected to Google Drive")
                    }
                }

                item {
                    SettingsHeader("Help")
                    StellarSettingsItem(Icons.AutoMirrored.Filled.Help, "FAQ", "Frequently asked questions")
                    StellarSettingsItem(Icons.Default.Feedback, "Feedback", "Report bugs or suggest features")
                    StellarSettingsItem(Icons.Default.PrivacyTip, "Privacy policy", "How we handle your data")
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        color = CategoryPurple,
        fontSize = 15.sp,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
fun StellarSettingsItem(
    icon: ImageVector, 
    title: String, 
    subtitle: String?, 
    hasDot: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        color = Color.Transparent,
        enabled = onClick != null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title, 
                        color = Color.White, 
                        fontSize = 17.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (hasDot) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                    }
                }
                if (subtitle != null) {
                    Text(subtitle, color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StellarSettingsToggle(
    icon: ImageVector, 
    title: String, 
    subtitle: String, 
    checked: Boolean, 
    hasDot: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title, 
                    color = Color.White, 
                    fontSize = 17.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                    fontWeight = FontWeight.SemiBold
                )
                if (hasDot) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                }
            }
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CategoryPurple,
                checkedTrackColor = CategoryPurple.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}
