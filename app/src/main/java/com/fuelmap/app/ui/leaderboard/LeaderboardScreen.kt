package com.fuelmap.app.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuelmap.app.ui.AppViewModelProvider
import com.fuelmap.app.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onBack: (() -> Unit)? = null,
    vm: LeaderboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val region by vm.region.collectAsStateWithLifecycle()
    val leaders by vm.leaders.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Лидеры: $region") },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            }
        )
    }) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leaders.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.EmojiEvents,
                        title = "Пока нет лидеров",
                        subtitle = "Подтверждайте отметки и зарабатывайте карму — лучшие появятся здесь."
                    )
                }
            }
            itemsIndexed(leaders) { index, user ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (index < 3) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RankBadge(index + 1)
                            Text(
                                user.login,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                        Text(
                            "${user.karma} ★",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val color = when (rank) {
        1 -> Color(0xFFFFC107) // золото
        2 -> Color(0xFFB0BEC5) // серебро
        3 -> Color(0xFFCD7F32) // бронза
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    Box(
        Modifier.size(36.dp).background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$rank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (rank <= 3) Color(0xFF1A1C1E) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
