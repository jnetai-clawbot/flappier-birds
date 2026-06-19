package com.jnetaol.flappierbirds.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnetaol.flappierbirds.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    musicVolume: Float,
    soundVolume: Float,
    hapticEnabled: Boolean,
    graphicsQuality: String,
    showFps: Boolean,
    debugMode: Boolean,
    onMusicVolumeChange: (Float) -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onHapticToggle: (Boolean) -> Unit,
    onGraphicsQualityChange: (String) -> Unit,
    onShowFpsToggle: (Boolean) -> Unit,
    onDebugModeToggle: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showGraphicsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1117),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0D1117)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionHeader("Audio")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF58A6FF), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Music Volume", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Medium)
                        Text("${(musicVolume * 100).toInt()}%", color = Color(0xFF8B949E), fontSize = 14.sp)
                    }
                    Slider(
                        value = musicVolume,
                        onValueChange = onMusicVolumeChange,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF58A6FF),
                            activeTrackColor = Color(0xFF58A6FF),
                            inactiveTrackColor = Color(0xFF30363D)
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF3FB950), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Sound Volume", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Medium)
                        Text("${(soundVolume * 100).toInt()}%", color = Color(0xFF8B949E), fontSize = 14.sp)
                    }
                    Slider(
                        value = soundVolume,
                        onValueChange = onSoundVolumeChange,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF3FB950),
                            activeTrackColor = Color(0xFF3FB950),
                            inactiveTrackColor = Color(0xFF30363D)
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionHeader("Gameplay")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Vibration, contentDescription = null, tint = Color(0xFFA371F7), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Haptic Feedback", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = onHapticToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFA371F7),
                            checkedTrackColor = Color(0xFFA371F7).copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.HighQuality, contentDescription = null, tint = Color(0xFFDB6D28), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Graphics Quality", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Medium)
                    Text(
                        graphicsQuality.replaceFirstChar { it.uppercase() },
                        color = Color(0xFF8B949E),
                        fontSize = 14.sp
                    )
                    IconButton(onClick = { showGraphicsDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Change", tint = Color(0xFF58A6FF), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionHeader("Display")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF39D2C0), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Show FPS", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = showFps,
                        onCheckedChange = onShowFpsToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF39D2C0),
                            checkedTrackColor = Color(0xFF39D2C0).copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = Color(0xFFF85149), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Debug Mode", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = debugMode,
                        onCheckedChange = onDebugModeToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFF85149),
                            checkedTrackColor = Color(0xFFF85149).copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }

    if (showGraphicsDialog) {
        GraphicsQualityDialog(
            current = graphicsQuality,
            onSelect = { quality ->
                showGraphicsDialog = false
                onGraphicsQualityChange(quality)
            },
            onDismiss = { showGraphicsDialog = false }
        )
    }
}

@Composable
private fun GraphicsQualityDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF161B22),
        title = {
            Text("Graphics Quality", fontWeight = FontWeight.Bold, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        },
        text = {
            Column {
                listOf("low", "medium", "high").forEach { quality ->
                    val isSelected = quality == current
                    Button(
                        onClick = { onSelect(quality) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF58A6FF) else Color(0xFF21262D)
                        )
                    ) {
                        Text(
                            quality.replaceFirstChar { it.uppercase() },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF8B949E)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8B949E))
            }
        }
    )
}
