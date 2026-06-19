package com.jnetaol.flappierbirds.ui.screens.achievements

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
fun AchievementsScreen(
    repository: GameRepository,
    onNavigateBack: () -> Unit
) {
    val achievements by repository.achievements.collectAsState(initial = emptyList())
    val unlockedCount = achievements.count { it.unlocked }
    val totalCount = achievements.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements", fontWeight = FontWeight.Bold) },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFD29922),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "$unlockedCount / $totalCount Unlocked",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        LinearProgressIndicator(
                            progress = { if (totalCount > 0) unlockedCount.toFloat() / totalCount.toFloat() else 0f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFD29922),
                            trackColor = Color(0xFF30363D),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            achievements.forEach { achievement ->
                AchievementCard(
                    name = achievement.name,
                    description = achievement.description,
                    unlocked = achievement.unlocked,
                    progress = achievement.progress,
                    target = achievement.target,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
