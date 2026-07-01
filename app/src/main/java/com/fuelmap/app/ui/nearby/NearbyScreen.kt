package com.fuelmap.app.ui.nearby

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuelmap.app.ui.AppViewModelProvider
import com.fuelmap.app.ui.common.EmptyState
import com.fuelmap.app.util.LocationProvider
import com.fuelmap.app.util.NavigationLauncher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(
    onStationDetails: (Long) -> Unit,
    vm: NearbyViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val items by vm.items.collectAsStateWithLifecycle()
    val brands by vm.brands.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val brand by vm.brand.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val onlyFav by vm.onlyFavorites.collectAsStateWithLifecycle()
    val noQueue by vm.onlyNoQueue.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var brandExpanded by remember { mutableStateOf(false) }

    fun loadLocation() {
        scope.launch {
            LocationProvider.currentLocation(context)?.let { vm.setLocation(it.latitude, it.longitude) }
        }
    }
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) loadLocation() }

    LaunchedEffect(Unit) {
        if (LocationProvider.hasPermission(context)) loadLocation()
        else permission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.clearMessage() }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Рядом") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                label = { Text("Поиск по названию или адресу") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = brandExpanded,
                    onExpandedChange = { brandExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = brand ?: "Все бренды",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Бренд") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = brandExpanded, onDismissRequest = { brandExpanded = false }) {
                        DropdownMenuItem(text = { Text("Все бренды") }, onClick = { vm.setBrand(null); brandExpanded = false })
                        brands.forEach { b ->
                            DropdownMenuItem(text = { Text(b) }, onClick = { vm.setBrand(b); brandExpanded = false })
                        }
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                SegmentedButton(
                    selected = sort == NearbySort.DISTANCE,
                    onClick = { vm.setSort(NearbySort.DISTANCE) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("По близости") }
                SegmentedButton(
                    selected = sort == NearbySort.PRICE,
                    onClick = { vm.setSort(NearbySort.PRICE) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("По цене") }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = onlyFav, onClick = { vm.toggleOnlyFavorites() }, label = { Text("Избранное") })
                FilterChip(selected = noQueue, onClick = { vm.toggleNoQueue() }, label = { Text("Без очереди") })
            }

            if (items.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.NearMe,
                    title = "Ничего не найдено",
                    subtitle = "Измените фильтры или разрешите геолокацию, чтобы видеть ближайшие АЗС."
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.station.station.id }) { item ->
                        NearbyCard(
                            item = item,
                            onClick = { onStationDetails(item.station.station.id) },
                            onFavorite = { vm.toggleFavorite(item.station.station.id) },
                            onRoute = {
                                NavigationLauncher.route(
                                    context,
                                    item.station.station.lat,
                                    item.station.station.lng,
                                    item.station.station.name
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyCard(
    item: NearbyItem,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onRoute: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.station.station.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "В избранное",
                        tint = if (item.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val meta = buildString {
                if (item.station.station.brand.isNotBlank()) append(item.station.station.brand)
                if (item.distanceMeters != null) {
                    if (isNotEmpty()) append(" · ")
                    append(formatDistance(item.distanceMeters))
                }
            }
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            val available = item.station.currentMark?.items?.filter { it.available }.orEmpty()
            if (available.isEmpty()) {
                Text("Нет актуальных данных по топливу", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    available.joinToString("   ") { "${it.type.title} ${formatPrice(it.price)}₽" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            FilledTonalButton(onClick = onRoute, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.NearMe, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Поехали")
            }
        }
    }
}

private fun formatDistance(m: Double): String =
    if (m < 1000) "${m.toInt()} м" else "%.1f км".format(m / 1000.0)

private fun formatPrice(p: Double): String =
    if (p % 1.0 == 0.0) p.toInt().toString() else "%.1f".format(p)
