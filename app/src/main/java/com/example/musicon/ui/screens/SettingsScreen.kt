package com.example.musicon.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicon.ui.components.ColorWheel
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
    val context = LocalContext.current
    val pauseOnDetach by viewModel.pauseOnDetach.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val showNotifications by viewModel.showNotifications.collectAsState()
    val crossfade by viewModel.crossfade.collectAsState()
    val autoTheme by viewModel.autoTheme.collectAsState()
    val customBgUri by viewModel.customBgUri.collectAsState()
    val accentColorInt by viewModel.accentColor.collectAsState()
    
    val primaryColor = Color(accentColorInt)
    var hexInput by remember { mutableStateOf("") }
    var showColorWheel by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    val remainingTime by viewModel.sleepTimerRemaining.collectAsState()

    val bgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.updateCustomBackground(uri.toString())
        }
    }

    BackHandler(onBack = onBack)

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    StellarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                TopAppBar(
                    modifier = if (isLandscape) Modifier.height(IntrinsicSize.Min) else Modifier,
                    windowInsets = WindowInsets.statusBars,
                    title = { 
                        Text(
                            "Settings", 
                            color = Color.White, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = if (isLandscape) 18.sp else 22.sp,
                            modifier = Modifier.padding(bottom = if (isLandscape) 0.dp else 4.dp)
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = if (isLandscape) Modifier.size(36.dp).padding(start = 4.dp) else Modifier) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = if (isLandscape) Modifier.size(20.dp) else Modifier)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                item {
                    SettingsHeader("General")
                    StellarSettingsItem(Icons.Default.Scanner, "Scan local music", "Search for local files") {
                        onScanClick()
                    }
                    StellarSettingsItem(Icons.Default.CloudSync, "Sync GDrive", "Manual cloud synchronization") {
                        viewModel.syncCloudTracks()
                    }
                    StellarSettingsItem(Icons.Default.Image, "App Background", if (customBgUri != null) "Custom photo set" else "Default nebula") {
                        bgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    if (customBgUri != null) {
                        TextButton(onClick = { viewModel.updateCustomBackground(null) }, modifier = Modifier.padding(start = 64.dp)) {
                            Text("Reset to default", color = primaryColor, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    SettingsHeader("Theming")
                    StellarSettingsToggle(Icons.Default.ColorLens, "Auto Theme Color", "Extract theme color from song image", autoTheme) { 
                        viewModel.updateAutoTheme(it) 
                    }
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Manual Theme Color", color = Color.White, fontSize = 14.sp)
                            IconButton(onClick = { showColorWheel = !showColorWheel }) {
                                Icon(
                                    if (showColorWheel) Icons.Default.Close else Icons.Default.Palette,
                                    null,
                                    tint = primaryColor
                                )
                            }
                        }
                        
                        AnimatedVisibility(
                            visible = showColorWheel,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Spacer(Modifier.height(16.dp))
                                ColorWheel(
                                    selectedColor = primaryColor,
                                    onColorSelected = { viewModel.updateAccentColor(it.toArgb()) },
                                    modifier = Modifier.size(200.dp)
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                        
                        Text("Custom Hex Color", color = Color.White, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                            OutlinedTextField(
                                value = hexInput,
                                onValueChange = { if (it.length <= 7) hexInput = it },
                                placeholder = { Text("#FFFFFF", color = Color.Gray) },
                                modifier = Modifier.width(120.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(0.05f),
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = primaryColor,
                                    unfocusedIndicatorColor = Color.Gray
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                            )
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    try {
                                        val color = Color(android.graphics.Color.parseColor(if (hexInput.startsWith("#")) hexInput else "#$hexInput"))
                                        viewModel.updateAccentColor(color.toArgb())
                                    } catch (e: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Apply", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    SettingsHeader("Playback")
                    StellarSettingsItem(
                        Icons.Default.Timer, 
                        "Sleep Timer", 
                        if (remainingTime != null) "Remaining: ${(remainingTime!! / 60000)}m" else "Set auto-stop timer"
                    ) {
                        showTimerDialog = true
                    }
                    StellarSettingsToggle(Icons.Default.PauseCircle, "Pause on detach", "Pause playback when headphone is detached", pauseOnDetach) { viewModel.updatePauseOnDetach(it) }
                    StellarSettingsToggle(Icons.Default.BlurOn, "Crossfade", "Previous song fades out, next song fades in", crossfade) { viewModel.updateCrossfade(it) }
                }

                item {
                    SettingsHeader("Preference")
                    StellarSettingsItem(Icons.Default.HighQuality, "Audio Quality", "High (320kbps)") { }
                    StellarSettingsToggle(Icons.Default.WbSunny, "Keep screen on", "Stay on while on the player screen", keepScreenOn) { viewModel.updateKeepScreenOn(it) }
                    StellarSettingsToggle(Icons.Default.Notifications, "Notifications", "Show playback controls in notification", showNotifications) { viewModel.updateShowNotifications(it) }
                    val shakeState by viewModel.shakeToSkip.collectAsState()
                    StellarSettingsToggle(Icons.Default.PhoneAndroid, "Shake to skip", "Shake device to play next song", shakeState) { viewModel.updateShakeToSkip(it) }
                }

                item {
                    SettingsHeader("Help & Feedback")
                    StellarSettingsItem(Icons.Default.Email, "Send Feedback", "Report bugs to suraj.y7428@gmail.com") {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:suraj.y7428@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "MusicOn Feedback")
                        }
                        context.startActivity(intent)
                    }
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    if (showTimerDialog) {
        SleepTimerDialog(
            onDismiss = { showTimerDialog = false },
            onSet = { viewModel.setSleepTimer(it); showTimerDialog = false }
        )
    }
}

@Composable
fun SleepTimerDialog(onDismiss: () -> Unit, onSet: (Int) -> Unit) {
    var customMinutes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer", color = Color.White, fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFF1E1B36),
        text = {
            Column {
                val times = listOf(0 to "Off", 15 to "15 minutes", 30 to "30 minutes", 60 to "60 minutes")
                times.forEach { (mins, label) ->
                    TextButton(
                        onClick = { onSet(mins) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, color = Color.White)
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
                
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { if (it.all { char -> char.isDigit() }) customMinutes = it },
                    label = { Text("Custom Minutes", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val mins = customMinutes.toIntOrNull() ?: 0
                    if (mins > 0) onSet(mins)
                },
                enabled = customMinutes.isNotEmpty()
            ) {
                Text("Set Custom")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
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
fun StellarSettingsItem(icon: ImageVector, title: String, subtitle: String?, onClick: (() -> Unit)? = null) {
    Surface(onClick = { onClick?.invoke() }, color = Color.Transparent, enabled = onClick != null) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 20.dp)) {
                Text(text = title, color = Color.White, fontSize = 17.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StellarSettingsToggle(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 20.dp)) {
            Text(text = title, color = Color.White, fontSize = 17.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = primaryColor, 
                checkedTrackColor = primaryColor.copy(alpha = 0.3f), 
                uncheckedThumbColor = Color.Gray, 
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}
