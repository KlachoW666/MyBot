package com.fuelmap.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuelmap.app.domain.model.Role
import com.fuelmap.app.ui.AppViewModelProvider
import com.fuelmap.app.ui.common.MarkHistoryRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onLeaderboard: () -> Unit,
    vm: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val user by vm.currentUser.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Профиль") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        )
    }) { padding ->
        val u = user
        if (u == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { Text("Вы не авторизованы") }
            return@Scaffold
        }
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(u.login, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Почта: ${u.email}")
                        Text("Госномер: ${u.carPlate}")
                        Text("Регион: ${u.region}")
                        Text("Карма: ${u.karma}")
                        if (u.role != Role.USER) {
                            Text("Роль: ${if (u.role == Role.SUPER_ADMIN) "супер-администратор" else "администратор"}",
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Divider()
                        TextButton(onClick = onLeaderboard) { Text("Таблица лидеров региона") }
                        OutlinedButton(onClick = { vm.logout(onLoggedOut) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Выйти")
                        }
                    }
                }
            }
            item {
                Text("Мои отметки", style = MaterialTheme.typography.titleMedium)
            }
            if (history.isEmpty()) {
                item { Text("Пока нет отметок.") }
            } else {
                items(history) { mark -> MarkHistoryRow(mark) }
            }
        }
    }
}
