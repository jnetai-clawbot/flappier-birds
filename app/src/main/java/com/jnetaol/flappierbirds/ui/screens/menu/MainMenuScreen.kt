package com.jnetaol.flappierbirds.ui.screens.menu

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnetaol.flappierbirds.data.repository.GameRepository
import com.jnetaol.flappierbirds.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun MainMenuScreen(
    repository: GameRepository,
    onPlay: (mode: String) -> Unit,
    onStatistics: () -> Unit,
    onAchievements: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onExit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showModeSelector by remember { mutableStateOf(false) }
    var showDailyReward by remember { mutableStateOf(false) }
    var dailyRewardAmount by remember { mutableIntStateOf(0) }
    var canClaimDaily by remember { mutableStateOf(false) }
    var dailyStreak by remember { mutableIntStateOf(0) }
    val stats by repository.stats.collectAsState(initial = null)
    val coins = stats?.totalCoins ?: 0
    val bestScore = stats?.highestScore ?: 0

    LaunchedEffect(Unit) {
        canClaimDaily = repository.canClaimDailyReward()
        dailyStreak = repository.getDailyStreak()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                "FLAPPIER",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF58A6FF),
                letterSpacing = 4.sp
            )
            Text(
                "BIRDS",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3FB950),
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CoinDisplay(coins)
                Spacer(Modifier.width(16.dp))
                Text(
                    "Best: $bestScore",
                    fontSize = 14.sp,
                    color = Color(0xFFD29922),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(32.dp))

            GameButton(
                text = "Play",
                onClick = { showModeSelector = true },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.PlayArrow,
                color = Color(0xFF3FB950)
            )

            Spacer(Modifier.height(12.dp))

            if (canClaimDaily) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            dailyRewardAmount = repository.claimDailyReward()
                            showDailyReward = true
                            canClaimDaily = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD700)),
                    border = BorderStroke(1.dp, Color(0xFFFFD700))
                ) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Daily Reward", fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                }
                Spacer(Modifier.height(12.dp))
            }

            MenuButton("Statistics", onClick = onStatistics, icon = Icons.Default.BarChart, subtitle = "View your stats and progress")
            Spacer(Modifier.height(8.dp))
            MenuButton("Achievements", onClick = onAchievements, icon = Icons.Default.EmojiEvents, subtitle = "Track your accomplishments")
            Spacer(Modifier.height(8.dp))
            MenuButton("Settings", onClick = onSettings, icon = Icons.Default.Settings, subtitle = "Customize your experience")
            Spacer(Modifier.height(8.dp))
            MenuButton("About", onClick = onAbout, icon = Icons.Default.Info, subtitle = "App info and updates")

            Spacer(Modifier.height(24.dp))

            TextButton(onClick = onExit) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFF8B949E), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Exit", color = Color(0xFF8B949E))
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showModeSelector) {
        ModeSelectorDialog(
            onDismiss = { showModeSelector = false },
            onSelectMode = { mode ->
                showModeSelector = false
                onPlay(mode)
            }
        )
    }

    if (showDailyReward) {
        DailyRewardDialog(
            amount = dailyRewardAmount,
            streak = dailyStreak,
            onDismiss = { showDailyReward = false }
        )
    }
}

@Composable
private fun ModeSelectorDialog(
    onDismiss: () -> Unit,
    onSelectMode: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF161B22),
        title = {
            Text("Select Mode", fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        },
        text = {
            Column {
                Button(
                    onClick = { onSelectMode("endless") },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FB950))
                ) {
                    Icon(Icons.Default.AllInclusive, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Classic", fontWeight = FontWeight.Bold)
                        Text("Flappy Birds style — tap to flap through pipes", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onSelectMode("challenge") },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB6D28))
                ) {
                    Icon(Icons.Default.Whatshot, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Challenge", fontWeight = FontWeight.Bold)
                        Text("Faster speed, tighter gaps, harder timing", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onSelectMode("practice") },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58A6FF))
                ) {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Practice", fontWeight = FontWeight.Bold)
                        Text("Slower speed, wider gaps — learn to fly", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
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

@Composable
private fun DailyRewardDialog(
    amount: Int,
    streak: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF161B22),
        title = {
            Text("Daily Reward!", fontWeight = FontWeight.Bold, color = Color(0xFFFFD700), textAlign = TextAlign.Center)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "+$amount coins",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Day $streak streak!",
                    fontSize = 14.sp,
                    color = Color(0xFF8B949E)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FB950))
            ) {
                Text("Claim!", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {}
    )
}
