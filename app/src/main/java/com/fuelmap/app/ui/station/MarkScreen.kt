package com.fuelmap.app.ui.station

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuelmap.app.data.repository.FuelEntry
import com.fuelmap.app.domain.model.FuelType
import com.fuelmap.app.domain.model.Queue
import com.fuelmap.app.ui.AppViewModelProvider
import com.fuelmap.app.ui.common.GradientButton
import com.fuelmap.app.ui.common.OptionPill
import com.fuelmap.app.ui.common.SupportFooter
import com.fuelmap.app.util.LocationProvider
import com.fuelmap.app.util.PhotoStorage
import kotlinx.coroutines.launch

private data class FuelInput(val available: Boolean = true, val price: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkScreen(
    stationId: Long,
    onBack: () -> Unit,
    vm: StationViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val message by vm.message.collectAsStateWithLifecycle()
    val user by vm.currentUser.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val selected = remember { mutableStateMapOf<FuelType, FuelInput>() }
    var queue by remember { mutableStateOf(Queue.NONE) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locating by remember { mutableStateOf(false) }
    var photoPath by remember { mutableStateOf<String?>(null) }
    val isAdmin = user?.role?.isAdmin == true

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photoPath = PhotoStorage.copyToInternal(context, uri)
    }

    fun buildEntries(): List<FuelEntry> = selected.map { (type, input) ->
        FuelEntry(type, input.available, input.price.replace(',', '.').toDoubleOrNull() ?: 0.0)
    }

    suspend fun submitWithLocation() {
        locating = true
        val loc = try {
            LocationProvider.currentLocation(context)
        } finally {
            locating = false
        }
        vm.submitMark(stationId, buildEntries(), queue, loc?.latitude, loc?.longitude, photoPath, onDone = onBack)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scope.launch { submitWithLocation() }
        else vm.showMessage("Для отметки нужен доступ к геолокации — это защищает карту от ложных меток.")
    }

    fun attemptSubmit() {
        // Админ/супер-админ — без геолокации и проверки расстояния.
        if (isAdmin) {
            scope.launch { vm.submitMark(stationId, buildEntries(), queue, null, null, photoPath, onDone = onBack) }
            return
        }
        if (LocationProvider.hasPermission(context)) scope.launch { submitWithLocation() }
        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ситуация на АЗС") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 12.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (isAdmin) "Режим администратора: метку можно ставить на любую АЗС без проверки расстояния."
                        else "При сохранении проверяется геолокация: отметку можно ставить только рядом с этой АЗС.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GradientButton(
                        text = if (locating) "Определяем геолокацию…" else "Подтверждаю",
                        onClick = { attemptSubmit() },
                        enabled = !locating && selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    SupportFooter()
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Какое топливо есть на АЗС?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            items(FuelType.entries) { type ->
                FuelTypeCard(
                    type = type,
                    input = selected[type],
                    onToggle = { checked ->
                        if (checked) selected[type] = FuelInput() else selected.remove(type)
                    },
                    onAvailableChange = { selected[type] = (selected[type] ?: FuelInput()).copy(available = it) },
                    onPriceChange = { selected[type] = (selected[type] ?: FuelInput()).copy(price = it) }
                )
            }

            item {
                Text(
                    "Оцените очередь",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Queue.entries.forEach { q ->
                        OptionPill(
                            label = q.title,
                            selected = queue == q,
                            onClick = { queue = q },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Text(
                    "Фото (необязательно)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (photoPath == null) "Прикрепить фото табло/очереди" else "Фото прикреплено ✓ — заменить")
                }
            }
        }
    }
}

@Composable
private fun FuelTypeCard(
    type: FuelType,
    input: FuelInput?,
    onToggle: (Boolean) -> Unit,
    onAvailableChange: (Boolean) -> Unit,
    onPriceChange: (String) -> Unit
) {
    val selected = input != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(!selected) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.LocalGasStation,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    type.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Checkbox(checked = selected, onCheckedChange = onToggle)
            }

            AnimatedVisibility(visible = input != null) {
                if (input != null) {
                    Column(
                        Modifier.padding(top = 4.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                if (input.available) "В наличии" else "Нет",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(checked = input.available, onCheckedChange = onAvailableChange)
                        }
                        if (input.available) {
                            OutlinedTextField(
                                value = input.price,
                                onValueChange = onPriceChange,
                                label = { Text("Цена за литр") },
                                suffix = { Text("₽") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
