package com.fuelmap.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuelmap.app.domain.model.Role
import com.fuelmap.app.ui.AppViewModelProvider
import com.fuelmap.app.ui.common.MarkHistoryRow
import com.fuelmap.app.util.TimeFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserScreen(
    userId: Long,
    onBack: () -> Unit,
    vm: AdminViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val userFlow = remember(userId) { vm.observeUser(userId) }
    val user by userFlow.collectAsStateWithLifecycle()
    val historyFlow = remember(userId) { vm.userHistory(userId) }
    val history by historyFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val actor by vm.currentUser.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.clearMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.login ?: "Пользователь") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val u = user
        if (u == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { Text("Загрузка…") }
            return@Scaffold
        }
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(u.login, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Почта: ${u.email}")
                        Text("Госномер: ${u.carPlate}")
                        Text("Регион: ${u.region}")
                        Text("Карма: ${u.karma}")
                        Text("Роль: ${roleLabelFull(u.role)}", color = MaterialTheme.colorScheme.primary)
                        Text("Регистрация: ${TimeFormat.dateTime(u.createdAt)}", style = MaterialTheme.typography.labelMedium)
                        if (u.isBanned) {
                            Text("Статус: ЗАБАНЕН", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Действия модерации недоступны над супер-администратором
            if (u.role != Role.SUPER_ADMIN) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (u.isBanned) {
                            OutlinedButton(onClick = { vm.setBanned(u, false) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Разбанить")
                            }
                        } else {
                            Button(
                                onClick = { vm.setBanned(u, true) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Забанить") }
                        }

                        // Назначение роли — только супер-админ
                        if (actor?.role == Role.SUPER_ADMIN) {
                            if (u.role == Role.ADMIN) {
                                OutlinedButton(onClick = { vm.setAdmin(actor!!, u, false) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Снять роль администратора")
                                }
                            } else {
                                OutlinedButton(onClick = { vm.setAdmin(actor!!, u, true) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Назначить администратором")
                                }
                            }
                        }
                    }
                }
            }

            item { Divider() }
            item { Text("История отметок (${history.size})", style = MaterialTheme.typography.titleMedium) }
            if (history.isEmpty()) {
                item { Text("Нет отметок.") }
            } else {
                items(history) { mark ->
                    MarkHistoryRow(mark) {
                        TextButton(onClick = { vm.deleteMark(mark.mark.id) }) {
                            Text("Удалить отметку", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

private fun roleLabelFull(role: Role): String = when (role) {
    Role.SUPER_ADMIN -> "супер-администратор"
    Role.ADMIN -> "администратор"
    Role.USER -> "пользователь"
}
