package com.fuelmap.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuelmap.app.domain.model.Role
import com.fuelmap.app.ui.AppViewModelProvider
import com.fuelmap.app.ui.common.BrandHeader
import com.fuelmap.app.ui.common.EmptyState
import com.fuelmap.app.ui.common.MarkHistoryRow
import com.fuelmap.app.ui.common.SupportFooter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onBack: (() -> Unit)? = null,
    vm: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val user by vm.currentUser.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Профиль") },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            }
        )
    }) { padding ->
        val u = user
        if (u == null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrandHeader(subtitle = "Войдите, чтобы отмечать топливо и добавлять АЗС")
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Войти") }
                OutlinedButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
                    Text("Зарегистрироваться")
                }
                SupportFooter()
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ProfileHeader(
                    login = u.login,
                    roleLabel = when (u.role) {
                        Role.SUPER_ADMIN -> "Супер-админ"
                        Role.ADMIN -> "Администратор"
                        Role.USER -> "Водитель"
                    },
                    karma = u.karma,
                    marks = history.size,
                    region = u.region
                )
            }
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoRow("Почта", u.email)
                        InfoRow("Госномер", u.carPlate)
                        InfoRow("Регион", u.region)
                    }
                }
            }
            item {
                OutlinedButton(onClick = { vm.logout {} }, modifier = Modifier.fillMaxWidth()) {
                    Text("Выйти")
                }
            }
            item { Text("Мои отметки", style = MaterialTheme.typography.titleMedium) }
            if (history.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.LocalGasStation,
                        title = "Пока нет отметок",
                        subtitle = "Отметьте наличие топлива на ближайшей АЗС — история появится здесь."
                    )
                }
            } else {
                items(history) { mark -> MarkHistoryRow(mark) }
            }
            item { SupportFooter() }
        }
    }
}

@Composable
private fun ProfileHeader(
    login: String,
    roleLabel: String,
    karma: Int,
    marks: Int,
    region: String
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ),
                MaterialTheme.shapes.large
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        login.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        login,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) {
                        Text(
                            roleLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("$karma", "Карма", Modifier.weight(1f))
                StatTile("$marks", "Отметки", Modifier.weight(1f))
                StatTile(region.take(14), "Регион", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(
            Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
