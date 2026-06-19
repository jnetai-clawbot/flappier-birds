package com.jnetaol.flappierbirds.ui.screens.about

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnetaol.flappierbirds.logger.DebugLogger
import com.jnetaol.flappierbirds.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                "FLAPPIER BIRDS",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF58A6FF),
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Made by jnetai.com",
                fontSize = 14.sp,
                color = Color(0xFF8B949E)
            )

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Version", fontSize = 12.sp, color = Color(0xFF8B949E))
                    Text(versionName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Spacer(Modifier.height(12.dp))

                    if (latestVersion != null) {
                        if (updateAvailable) {
                            Text(
                                "Update available: v$latestVersion",
                                color = Color(0xFF3FB950),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                "Up to date",
                                color = Color(0xFF3FB950),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                checking = true
                                latestVersion = checkForUpdates(context)
                                checking = false
                                if (latestVersion != null) {
                                    updateAvailable = latestVersion != versionName
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !checking,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58A6FF))
                    ) {
                        if (checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Checking...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Check For Updates", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Flappier Birds")
                        putExtra(Intent.EXTRA_TEXT, "Check out Flappier Birds! A fun arcade game for Android.\nhttps://github.com/jnetai-clawbot/flappier-birds/releases")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share App"))
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FB950))
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share App", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Features", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    FeatureItem("Endless side-scrolling gameplay")
                    FeatureItem("Multiple game modes")
                    FeatureItem("Unlockable bird skins and backgrounds")
                    FeatureItem("Achievement system")
                    FeatureItem("Day/night cycle and weather effects")
                    FeatureItem("Local leaderboard")
                    FeatureItem("Daily rewards")
                    FeatureItem("Offline playable")
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "2024 jnetai.com. All rights reserved.",
                fontSize = 11.sp,
                color = Color(0xFF8B949E),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF3FB950),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = Color(0xFFE6EDF3))
    }
}

private suspend fun checkForUpdates(context: android.content.Context): String? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/jnetai-clawbot/flappier-birds/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val json = JSONObject(response)
            val tagName = json.optString("tag_name", "")
            if (tagName.startsWith("v")) tagName.substring(1) else tagName
        } catch (e: Exception) {
            DebugLogger.logException("FB-300", "Update check failed", e)
            null
        }
    }
}
