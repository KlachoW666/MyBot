package com.fuelmap.app.ui.station

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuelmap.app.data.repository.FuelEntry
import com.fuelmap.app.domain.model.FuelType
import com.fuelmap.app.domain.model.Queue
import com.fuelmap.app.ui.AppViewModelProvider

private data class FuelInput(var available: Boolean = true, var price: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkScreen(
    stationId: Long,
    onBack: () -> Unit,
    vm: StationViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val selected = remember { mutableStateMapOf<FuelType, FuelInput>() }
    var queue by remember { mutableStateOf(Queue.NONE) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Отметить наличие") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Выберите типы топлива и укажите наличие и цену:", style = MaterialTheme.typography.bodyMedium)

            FuelType.entries.forEach { type ->
                val input = selected[type]
                val isOn = input != null
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isOn,
                                onCheckedChange = { checked ->
                                    if (checked) selected[type] = FuelInput()
                                    else selected.remove(type)
                                }
                            )
                            Text(type.title, style = MaterialTheme.typography.titleMedium)
                        }
                        if (input != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (input.available) "Есть" else "Нет", Modifier.width(48.dp))
                                Switch(
                                    checked = input.available,
                                    onCheckedChange = { selected[type] = input.copy(available = it) }
                                )
                            }
                            if (input.available) {
                                OutlinedTextField(
                                    value = input.price,
                                    onValueChange = { selected[type] = input.copy(price = it) },
                                    label = { Text("Цена за литр, ₽") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Text("Очередь:", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Queue.entries.forEach { q ->
                    FilterChip(
                        selected = queue == q,
                        onClick = { queue = q },
                        label = { Text(q.title) }
                    )
                }
            }

            Button(
                onClick = {
                    val entries = selected.map { (type, input) ->
                        FuelEntry(
                            type = type,
                            available = input.available,
                            price = input.price.replace(',', '.').toDoubleOrNull() ?: 0.0
                        )
                    }
                    vm.submitMark(stationId, entries, queue, onDone = onBack)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить отметку")
            }
        }
    }
}
