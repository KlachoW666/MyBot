package com.fuelmap.app.ui.admin

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuelmap.app.data.local.GasStationEntity
import com.fuelmap.app.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddStationScreen(
    onBack: () -> Unit,
    vm: AdminViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val regions by vm.regions.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var name by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var lat by rememberSaveable { mutableStateOf("") }
    var lng by rememberSaveable { mutableStateOf("") }
    var regionId by rememberSaveable { mutableStateOf(0L) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(regions) {
        if (regionId == 0L && regions.isNotEmpty()) regionId = regions.first().id
    }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.clearMessage() }
    }

    val regionName = regions.firstOrNull { it.id == regionId }?.name ?: "Выберите регион"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить АЗС") },
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
            OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(brand, { brand = it }, label = { Text("Бренд") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(address, { address = it }, label = { Text("Адрес") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                lat, { lat = it }, label = { Text("Широта (lat)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                lng, { lng = it }, label = { Text("Долгота (lng)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = regionName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Регион") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    regions.forEach { r ->
                        DropdownMenuItem(text = { Text(r.name) }, onClick = { regionId = r.id; expanded = false })
                    }
                }
            }

            Button(
                onClick = {
                    val latD = lat.replace(',', '.').toDoubleOrNull()
                    val lngD = lng.replace(',', '.').toDoubleOrNull()
                    if (name.isBlank() || latD == null || lngD == null || regionId == 0L) {
                        return@Button
                    }
                    vm.addStation(
                        GasStationEntity(
                            name = name, brand = brand.ifBlank { name },
                            lat = latD, lng = lngD, address = address, regionId = regionId
                        ),
                        onDone = onBack
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить АЗС")
            }
        }
    }
}
