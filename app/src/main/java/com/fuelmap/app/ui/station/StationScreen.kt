package com.fuelmap.app.ui.station

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuelmap.app.domain.model.ConfirmationType
import com.fuelmap.app.domain.model.FuelType
import com.fuelmap.app.domain.model.MarkFreshness
import com.fuelmap.app.ui.AppViewModelProvider
import com.fuelmap.app.ui.common.PriceChart
import com.fuelmap.app.util.MarkerIcons
import com.fuelmap.app.util.NavigationLauncher
import com.fuelmap.app.util.TimeFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationScreen(
    stationId: Long,
    onBack: () -> Unit,
    onMark: (Long) -> Unit,
    vm: StationViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val stationFlow = remember(stationId) { vm.observeStation(stationId) }
    val station by stationFlow.collectAsStateWithLifecycle()
    val user by vm.currentUser.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val historyFlow = remember(stationId) { vm.stationHistory(stationId) }
    val history by historyFlow.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(station?.station?.name ?: "АЗС") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    val fav = favorites.contains(stationId)
                    IconButton(onClick = { vm.toggleFavorite(stationId) }) {
                        Icon(
                            if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "В избранное",
                            tint = if (fav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val s = station
        if (s == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Загрузка…")
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(s.station.brand, style = MaterialTheme.typography.titleMedium)
            Text(s.station.address, style = MaterialTheme.typography.bodyMedium)

            val mark = s.currentMark
            val freshness = MarkFreshness.of(mark?.mark?.createdAt, mark?.hasAvailableFuel ?: false)
            val accent = Color(MarkerIcons.colorFor(freshness))

            Card(
                colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.14f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = statusTitle(freshness),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (mark == null) {
                        Text("Данных по этой заправке пока нет. Станьте первым!")
                    } else {
                        Text("Обновлено: ${TimeFormat.ago(mark.mark.createdAt)} (${TimeFormat.dateTime(mark.mark.createdAt)})",
                            style = MaterialTheme.typography.labelMedium)
                        Text("Очередь: ${mark.mark.queue.title}", style = MaterialTheme.typography.labelMedium)
                        Divider()
                        mark.items.forEach { item ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.type.title)
                                Text(
                                    if (item.available) "есть · %.2f ₽".format(item.price) else "нет",
                                    color = if (item.available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Divider()
                        Text(
                            "Подтверждений: ${mark.mark.confirmCount} · «закончилось»: ${mark.mark.emptyCount}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (user != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { vm.confirm(mark.mark.id, ConfirmationType.CONFIRM) }) {
                                    Text("Подтверждаю")
                                }
                                OutlinedButton(onClick = { vm.confirm(mark.mark.id, ConfirmationType.EMPTY) }) {
                                    Text("Закончилось")
                                }
                            }
                        }
                    }
                }
            }

            val chartType = FuelType.entries.maxByOrNull { t ->
                history.count { m -> m.items.any { it.type == t && it.available } }
            }
            val series = chartType?.let { t ->
                history.mapNotNull { m -> m.items.firstOrNull { it.type == t && it.available }?.price?.toFloat() }
            }.orEmpty()
            if (series.size >= 2 && chartType != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Динамика цен · ${chartType.title}", style = MaterialTheme.typography.titleMedium)
                        PriceChart(
                            points = series,
                            lineColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        )
                        Text(
                            "от ${"%.2f".format(series.min())} ₽ до ${"%.2f".format(series.max())} ₽",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = { NavigationLauncher.route(context, s.station.lat, s.station.lng, s.station.name) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.NearMe, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Поехали")
            }

            Button(
                onClick = { onMark(s.station.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (user == null) "Войдите, чтобы отметить" else "Отметить наличие")
            }
        }
    }
}

private fun statusTitle(f: MarkFreshness): String = when (f) {
    MarkFreshness.FRESH -> "🟢 Топливо есть (свежие данные)"
    MarkFreshness.AGING -> "🟡 Топливо есть (данные устаревают)"
    MarkFreshness.STALE -> "⚪ Нет актуальных данных"
    MarkFreshness.NO_FUEL -> "🔴 Топлива нет"
}
