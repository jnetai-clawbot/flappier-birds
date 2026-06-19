package com.jnetaol.flappierbirds.ui.screens.statistics

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
import com.jnetaol.flappierbirds.data.repository.GameRepository
import com.jnetaol.flappierbirds.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    repository: GameRepository,
    onNavigateBack: () -> Unit
) {
    val stats by repository.stats.collectAsState(initial = null)
    val highScores by repository.highScores.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics", fontWeight = FontWeight.Bold) },
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
            SectionHeader("Game Stats")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    "Games Played",
                    "${stats?.gamesPlayed ?: 0}",
                    Modifier.weight(1f),
                    Icons.Default.VideogameAsset
                )
                StatCard(
                    "Highest Score",
                    "${stats?.highestScore ?: 0}",
                    Modifier.weight(1f),
                    Icons.Default.EmojiEvents
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    "Total Score",
                    "${stats?.totalScore ?: 0}",
                    Modifier.weight(1f),
                    Icons.Default.TrendingUp
                )
                StatCard(
                    "Total Coins",
                    "${stats?.totalCoins ?: 0}",
                    Modifier.weight(1f),
                    Icons.Default.MonetizationOn
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    "Total Flaps",
                    "${stats?.totalFlaps ?: 0}",
                    Modifier.weight(1f),
                    Icons.Default.Air
                )
                StatCard(
                    "Obstacles Passed",
                    "${stats?.obstaclesPassed ?: 0}",
                    Modifier.weight(1f),
                    Icons.Default.CheckCircle
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    "Avg Score",
                    if ((stats?.gamesPlayed ?: 0) > 0) "${(stats?.totalScore ?: 0) / (stats?.gamesPlayed ?: 1)}" else "0",
                    Modifier.weight(1f),
                    Icons.Default.Functions
                )
                StatCard(
                    "Longest Session",
                    formatDuration(stats?.longestSessionMs ?: 0),
                    Modifier.weight(1f),
                    Icons.Default.Timer
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    "Endless",
                    "${stats?.endlessSessions ?: 0}",
                    Modifier.weight(1f),
                    Icons.Default.AllInclusive
                )
                StatCard(
                    "Challenge",
                    "${stats?.challengeSessions ?: 0}",
                    Modifier.weight(1f),
                    Icons.Default.Whatshot
                )
                StatCard(
                    "Practice",
                    "${stats?.practiceSessions ?: 0}",
                    Modifier.weight(1f),
                    Icons.Default.School
                )
            }

            Spacer(Modifier.height(24.dp))

            SectionHeader("Leaderboard")

            if (highScores.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF8B949E), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No scores yet", color = Color(0xFF8B949E))
                        Text("Play a game to get on the leaderboard!", fontSize = 12.sp, color = Color(0xFF8B949E))
                    }
                }
            } else {
                highScores.forEachIndexed { index, score ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (index == 0) Color(0xFF3D2E0A) else Color(0xFF21262D)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "#${index + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (index == 0) Color(0xFFFFD700) else if (index == 1) Color(0xFFC0C0C0) else if (index == 2) Color(0xFFCD7F32) else Color(0xFF8B949E),
                                modifier = Modifier.width(40.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    score.mode.replaceFirstChar { it.uppercase() },
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    "${score.coins} coins",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8B949E)
                                )
                            }
                            Text(
                                "${score.score}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) "${minutes}m ${remainingSeconds}s" else "${remainingSeconds}s"
}
