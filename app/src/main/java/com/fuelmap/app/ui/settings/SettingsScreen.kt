package com.fuelmap.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuelmap.app.data.settings.ThemeMode
import com.fuelmap.app.domain.model.FuelType
import com.fuelmap.app.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val settings by vm.state.collectAsStateWithLifecycle()
    var fuelExpanded by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Настройки") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingCard("Тема оформления") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val modes = listOf(ThemeMode.DARK to "Тёмная", ThemeMode.LIGHT to "Светлая", ThemeMode.SYSTEM to "Системная")
                    modes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { vm.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size)
                        ) { Text(label) }
                    }
                }
            }

            SettingCard("Предпочитаемое топливо") {
                Text(
                    "Карта будет по умолчанию показывать АЗС с этим топливом.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExposedDropdownMenuBox(expanded = fuelExpanded, onExpandedChange = { fuelExpanded = it }) {
                    OutlinedTextField(
                        value = settings.preferredFuel?.title ?: "Не выбрано",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = fuelExpanded, onDismissRequest = { fuelExpanded = false }) {
                        DropdownMenuItem(text = { Text("Не выбрано") }, onClick = { vm.setPreferredFuel(null); fuelExpanded = false })
                        FuelType.entries.forEach { f ->
                            DropdownMenuItem(text = { Text(f.title) }, onClick = { vm.setPreferredFuel(f); fuelExpanded = false })
                        }
                    }
                }
            }

            SettingCard("Радиус отметки: ${settings.markRadiusMeters} м") {
                Text(
                    "Насколько близко к АЗС нужно быть, чтобы поставить отметку (для обычных пользователей).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = settings.markRadiusMeters.toFloat(),
                    onValueChange = { vm.setRadius(it.toInt()) },
                    valueRange = 200f..3000f,
                    steps = 27
                )
            }

            SettingCard("Гео-уведомления") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Уведомлять о топливе рядом", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = settings.geoNotifyEnabled, onCheckedChange = { vm.setGeoNotify(it) })
                }
                Text(
                    "Пуш о наличии нужного топлива на избранных АЗС поблизости.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
