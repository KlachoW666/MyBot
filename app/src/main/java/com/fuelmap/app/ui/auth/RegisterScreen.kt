package com.fuelmap.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuelmap.app.domain.model.SupportedRegions
import com.fuelmap.app.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    vm: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var plate by rememberSaveable { mutableStateOf("") }
    var region by rememberSaveable { mutableStateOf(SupportedRegions.names.first()) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Регистрация") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        )
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = login, onValueChange = { login = it },
                label = { Text("Логин") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Пароль (мин. 8 символов)") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Почта") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = plate, onValueChange = { plate = it },
                label = { Text("Госномер (пример: А123ВС 750)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = region,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Регион") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SupportedRegions.names.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = { region = name; expanded = false }
                        )
                    }
                }
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = { vm.register(login, password, email, plate, region, onRegistered) },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.padding(end = 8.dp))
                Text("Создать аккаунт")
            }
        }
    }
}
