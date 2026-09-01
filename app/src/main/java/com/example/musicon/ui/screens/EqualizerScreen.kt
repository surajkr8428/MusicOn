package com.example.musicon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicon.ui.components.StellarBackground
import com.example.musicon.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val eqEnabled by viewModel.eqEnabled.collectAsState()
    val eqBandsStr by viewModel.eqBands.collectAsState()
    val bassBoost by viewModel.bassBoost.collectAsState()
    val virtualizer by viewModel.virtualizer.collectAsState()

    val bands = remember(eqBandsStr) { eqBandsStr.split(",").map { it.toInt() } }
    val primaryColor = MaterialTheme.colorScheme.primary

    StellarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Equalizer", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    actions = {
                        Switch(
                            checked = eqEnabled,
                            onCheckedChange = { viewModel.updateEqEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = primaryColor,
                                checkedTrackColor = primaryColor.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Text("Presets", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    val presets = listOf("Flat", "Rock", "Pop", "Jazz", "Classical", "Bass Boost")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets) { preset ->
                            FilterChip(
                                selected = false,
                                onClick = { if (eqEnabled) viewModel.setPreset(preset) },
                                label = { Text(preset) },
                                enabled = eqEnabled,
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = Color.White,
                                    disabledLabelColor = Color.Gray
                                )
                            )
                        }
                    }
                }

                item {
                    Text("Bands", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        bands.forEachIndexed { index, level ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Slider(
                                    value = level.toFloat(),
                                    onValueChange = { newLevel ->
                                        val newBands = bands.toMutableList()
                                        newBands[index] = newLevel.toInt()
                                        viewModel.updateEqBands(newBands.joinToString(","))
                                    },
                                    valueRange = -1500f..1500f,
                                    modifier = Modifier.weight(1f).width(40.dp),
                                    enabled = eqEnabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = primaryColor,
                                        activeTrackColor = primaryColor,
                                        inactiveTrackColor = primaryColor.copy(alpha = 0.2f)
                                    )
                                )
                                Text("${index + 1}", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    Text("Audio Effects", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    
                    EffectSlider("Bass Boost", bassBoost.toFloat(), 0f..1000f, eqEnabled, primaryColor) {
                        viewModel.updateBassBoost(it.toInt())
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    EffectSlider("Virtualizer", virtualizer.toFloat(), 0f..1000f, eqEnabled, primaryColor) {
                        viewModel.updateVirtualizer(it.toInt())
                    }
                }
            }
        }
    }
}

@Composable
fun EffectSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 16.sp)
            Text("${(value / 10).toInt()}%", color = Color.Gray, fontSize = 14.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor
            )
        )
    }
}
