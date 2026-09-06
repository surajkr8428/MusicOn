package com.example.musicon.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicon.ui.components.ColorWheel
import com.example.musicon.ui.theme.CategoryPurple
import com.example.musicon.ui.viewmodel.MainViewModel

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

    val bgPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.updateCustomBackground(uri.toString())
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SettingsHeader("General")
            val backgroundMode by viewModel.backgroundMode.collectAsState()
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Background Animation", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val modes = listOf(
                    "DYNAMIC", "SPACE", "NEBULA", "AURORA", "SHINOBI", 
                    "SUPERMAN", "SPIDERMAN", "BATMAN", "IRONMAN", 
                    "NARUTO_SASUKE", "TOM_JERRY", "THOR", "CAPTAIN_AMERICA", "BLACK_PANTHER"
                )
                
                // Vertical Grid for Animation Modes
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.chunked(3).forEach { rowModes ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowModes.forEach { mode ->
                                val isSelected = backgroundMode == mode
                                Button(
                                    onClick = { viewModel.updateBackgroundMode(mode) },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant, 
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
                            // Fill empty slots in last row
                            repeat(3 - rowModes.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            StellarSettingsItem(Icons.Default.Scanner, "Scan local music", "Search for local files") { onScanClick() }
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
            val themeMode by viewModel.themeMode.collectAsState()
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Theme Mode", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.example.musicon.ui.theme.ThemeMode.entries.forEach { mode ->
                        val isSelected = themeMode == mode
                        Button(
                            onClick = { viewModel.updateThemeMode(mode) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text(mode.name, fontSize = 10.sp) }
                    }
                }
            }
            StellarSettingsToggle(Icons.Default.ColorLens, "Auto Theme Color", "Extract theme color from song image", autoTheme) { viewModel.updateAutoTheme(it) }
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Manual Theme Color", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    IconButton(onClick = { showColorWheel = !showColorWheel }) { Icon(if (showColorWheel) Icons.Default.Close else Icons.Default.Palette, null, tint = primaryColor) }
                }
                AnimatedVisibility(visible = showColorWheel, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(Modifier.fillMaxWidth().padding(start = 16.dp), horizontalAlignment = Alignment.Start) {
                        Spacer(Modifier.height(16.dp))
                        ColorWheel(selectedColor = primaryColor, onColorSelected = { viewModel.updateAccentColor(it.toArgb()) }, modifier = Modifier.size(200.dp))
                        Spacer(Modifier.height(24.dp))
                    }
                }
                Text("Custom Hex Color", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                    OutlinedTextField(
                        value = hexInput, 
                        onValueChange = { if (it.length <= 7) hexInput = it }, 
                        placeholder = { Text("#FFFFFF", color = Color.Gray) }, 
                        modifier = Modifier.width(120.dp), 
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface, 
                            focusedContainerColor = Color.Transparent, 
                            unfocusedContainerColor = Color.Transparent, 
                            focusedBorderColor = primaryColor, 
                            unfocusedBorderColor = Color.Gray
                        ), 
                        singleLine = true, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = { try { val color = Color(android.graphics.Color.parseColor(if (hexInput.startsWith("#")) hexInput else "#$hexInput")); viewModel.updateAccentColor(color.toArgb()) } catch (e: Exception) {} }, colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black), shape = RoundedCornerShape(8.dp)) { Text("Apply", fontWeight = FontWeight.Bold) }
                }
            }
        }

        item {
            SettingsHeader("Playback")
            StellarSettingsToggle(Icons.Default.PauseCircle, "Pause on detach", "Pause playback when headphone is detached", pauseOnDetach) { viewModel.updatePauseOnDetach(it) }
            StellarSettingsToggle(Icons.Default.BlurOn, "Crossfade", "Previous song fades out, next song fades in", crossfade) { viewModel.updateCrossfade(it) }
        }

        item {
            SettingsHeader("Preference")
            StellarSettingsToggle(Icons.Default.WbSunny, "Keep screen on", "Stay on while on the player screen", keepScreenOn) { viewModel.updateKeepScreenOn(it) }
            StellarSettingsToggle(Icons.Default.Notifications, "Notifications", "Show playback controls in notification", showNotifications) { viewModel.updateShowNotifications(it) }
            val shakeState by viewModel.shakeToSkip.collectAsState()
            StellarSettingsToggle(Icons.Default.PhoneAndroid, "Shake to skip", "Shake device to play next song", shakeState) { viewModel.updateShakeToSkip(it) }
        }

        item {
            SettingsHeader("Help & Feedback")
            StellarSettingsItem(Icons.Default.Email, "Send Feedback", "Report bugs to suraj.y7428@gmail.com") {
                val intent = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:suraj.y7428@gmail.com"); putExtra(Intent.EXTRA_SUBJECT, "MusicOn Feedback") }
                context.startActivity(intent)
            }
        }
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(text = title, color = CategoryPurple, fontSize = 15.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp))
}

@Composable
fun StellarSettingsItem(icon: ImageVector, title: String, subtitle: String?, onClick: (() -> Unit)? = null) {
    val isBright = com.example.musicon.ui.components.LocalIsBackgroundBright.current
    val contentColor = if (isBright) Color.Black else Color.White
    
    Surface(onClick = { onClick?.invoke() }, color = Color.Transparent, enabled = onClick != null) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = contentColor.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 20.dp)) {
                Text(text = title, color = contentColor, fontSize = 17.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StellarSettingsToggle(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val isBright = com.example.musicon.ui.components.LocalIsBackgroundBright.current
    val contentColor = if (isBright) Color.Black else Color.White
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = contentColor.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 20.dp)) {
            Text(text = title, color = contentColor, fontSize = 17.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.3f), uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color.DarkGray))
    }
}
