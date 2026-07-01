package com.fuelmap.app.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuelmap.app.domain.model.FuelType
import com.fuelmap.app.domain.model.MarkFreshness
import com.fuelmap.app.ui.AppViewModelProvider
import com.fuelmap.app.util.GeoUtils
import com.fuelmap.app.util.LocationProvider
import com.fuelmap.app.util.MarkerIcons
import com.fuelmap.app.util.NavigationLauncher
import com.fuelmap.app.util.Notifications
import com.fuelmap.app.util.TimeFormat
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import android.graphics.PointF
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.ClusterListener
import com.yandex.mapkit.map.ClusterTapListener
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map as YMap
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onStationDetails: (Long) -> Unit,
    onMark: (Long) -> Unit,
    vm: MapViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val markers by vm.markers.collectAsStateWithLifecycle()
    val filter by vm.fuelFilter.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val settings by vm.appSettings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var focusPoint by remember { mutableStateOf<Point?>(null) }
    var userPoint by remember { mutableStateOf<Point?>(null) }
    var selectedStationId by remember { mutableStateOf<Long?>(null) }
    var addPoint by remember { mutableStateOf<Point?>(null) }
    // Станции, о которых уже уведомили в этой сессии (чтобы не спамить).
    val notifiedStations = remember { mutableSetOf<Long>() }

    /**
     * Проверяет избранные АЗС с нужным топливом в радиусе и шлёт локальное уведомление.
     * Полностью on-device: используется текущая геопозиция и локальные метки.
     */
    fun checkGeoNotifications(lat: Double, lng: Double) {
        val s = settings
        val fuel = s.preferredFuel
        if (!s.geoNotifyEnabled || fuel == null) return
        markers.forEach { marker ->
            val st = marker.station.station
            if (!favorites.contains(st.id)) return@forEach
            val item = marker.station.currentMark?.items
                ?.firstOrNull { it.type == fuel && it.available } ?: return@forEach
            val dist = GeoUtils.distanceMeters(lat, lng, st.lat, st.lng)
            if (dist <= s.markRadiusMeters && notifiedStations.add(st.id)) {
                Notifications.notifyNearby(
                    context, st.id, st.name,
                    "${fuel.title} есть · %.2f ₽ · %d м".format(item.price, dist.toInt())
                )
            }
        }
    }

    fun locate() {
        scope.launch {
            val loc = LocationProvider.currentLocation(context)
            if (loc != null) {
                val p = Point(loc.latitude, loc.longitude)
                userPoint = p
                focusPoint = p
                checkGeoNotifications(loc.latitude, loc.longitude)
            } else {
                snackbar.showSnackbar("Не удалось определить геолокацию")
            }
        }
    }
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) locate() }

    fun onLocateClick() {
        if (LocationProvider.hasPermission(context)) locate()
        else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.clearMessage() }
    }

    val selected = markers.firstOrNull { it.station.station.id == selectedStationId }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Russia Oil",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            YandexMap(
                markers = markers,
                userPoint = userPoint,
                focusPoint = focusPoint,
                initialCamera = vm.lastCamera,
                onCameraIdle = { vm.lastCamera = it },
                onStationTap = { selectedStationId = it },
                onLongTap = { addPoint = it },
                modifier = Modifier.fillMaxSize()
            )

            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
            ) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(6.dp)
                ) {
                    FilterChip(
                        selected = filter == null,
                        onClick = { vm.setFilter(null) },
                        label = { Text("Все") },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    FuelType.entries.forEach { type ->
                        FilterChip(
                            selected = filter == type,
                            onClick = { vm.setFilter(if (filter == type) null else type) },
                            label = { Text(type.title) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }

            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        "🟢 свежее · 🟡 устаревает · ⚪ нет данных · 🔴 нет топлива",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "Удерживайте точку на карте, чтобы добавить АЗС",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            FloatingActionButton(
                onClick = { onLocateClick() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Моя геолокация")
            }
        }
    }

    if (selected != null) {
        StationInfoSheet(
            marker = selected,
            isFavorite = favorites.contains(selected.station.station.id),
            onFavorite = { vm.toggleFavorite(selected.station.station.id) },
            onRoute = {
                NavigationLauncher.route(
                    context,
                    selected.station.station.lat,
                    selected.station.station.lng,
                    selected.station.station.name
                )
            },
            onDismiss = { selectedStationId = null },
            onMark = { selectedStationId = null; onMark(it) },
            onDetails = { selectedStationId = null; onStationDetails(it) }
        )
    }

    addPoint?.let { p ->
        AddStationDialog(
            onDismiss = { addPoint = null },
            onConfirm = { name, brand ->
                vm.addStationAt(name, brand, p.latitude, p.longitude)
                addPoint = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationInfoSheet(
    marker: StationMarker,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onRoute: () -> Unit,
    onDismiss: () -> Unit,
    onMark: (Long) -> Unit,
    onDetails: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val station = marker.station.station
    val mark = marker.station.currentMark

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(station.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (station.brand.isNotBlank()) Text(station.brand, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "В избранное",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (station.address.isNotBlank()) {
                Text(station.address, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(statusTitle(marker.freshness), style = MaterialTheme.typography.titleMedium)

            if (mark == null) {
                Text("Данных по топливу пока нет. Будьте первым!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                HorizontalDivider()
                mark.items.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.type.title)
                        Text(
                            if (item.available) "есть · %.2f ₽".format(item.price) else "нет",
                            color = if (item.available) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    "Обновлено: ${TimeFormat.ago(mark.mark.createdAt)} · очередь: ${mark.mark.queue.title}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(onClick = onRoute, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Filled.NearMe, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Поехали")
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { onDetails(station.id) }, modifier = Modifier.weight(1f)) {
                    Text("Подробнее")
                }
                Button(onClick = { onMark(station.id) }, modifier = Modifier.weight(1f)) {
                    Text("Отметить наличие")
                }
            }
        }
    }
}

@Composable
private fun AddStationDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, brand: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая АЗС") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Метка будет привязана к выбранной точке. Заявки обычных пользователей отправляются на модерацию.",
                    style = MaterialTheme.typography.bodySmall
                )
                androidx.compose.material3.OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Название") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.OutlinedTextField(
                    value = brand, onValueChange = { brand = it },
                    label = { Text("Бренд (необязательно)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(name, brand) },
                enabled = name.isNotBlank()
            ) { Text("Добавить") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private fun statusTitle(f: MarkFreshness): String = when (f) {
    MarkFreshness.FRESH -> "🟢 Топливо есть (свежие данные)"
    MarkFreshness.AGING -> "🟡 Топливо есть (данные устаревают)"
    MarkFreshness.STALE -> "⚪ Нет актуальных данных"
    MarkFreshness.NO_FUEL -> "🔴 Топлива нет"
}

@Composable
private fun YandexMap(
    markers: List<StationMarker>,
    userPoint: Point?,
    focusPoint: Point?,
    initialCamera: CameraState?,
    onCameraIdle: (CameraState) -> Unit,
    onStationTap: (Long) -> Unit,
    onLongTap: (Point) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    val map = remember { mapView.mapWindow.map }
    val clusterTapListeners = remember { mutableListOf<ClusterTapListener>() }
    val clusterListener = remember {
        ClusterListener { cluster ->
            cluster.appearance.setIcon(ImageProvider.fromBitmap(MarkerIcons.clusterBitmap(cluster.size)))
            val tap = ClusterTapListener { c ->
                map.move(CameraPosition(c.appearance.geometry, map.cameraPosition.zoom + 2.0f, 0.0f, 0.0f))
                true
            }
            clusterTapListeners.add(tap)
            cluster.addClusterTapListener(tap)
        }
    }
    val stationsCollection = remember { map.mapObjects.addClusterizedPlacemarkCollection(clusterListener) }
    val userCollection = remember { map.mapObjects.addCollection() }

    val markerIcons = remember {
        MarkFreshness.entries.associateWith { ImageProvider.fromBitmap(MarkerIcons.bitmap(it)) }
    }
    val userIcon = remember { ImageProvider.fromBitmap(MarkerIcons.userBitmap()) }
    val tapListeners = remember { mutableListOf<MapObjectTapListener>() }
    val showLabels = remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    MapKitFactory.getInstance().onStart()
                    mapView.onStart()
                }
                Lifecycle.Event.ON_STOP -> {
                    mapView.onStop()
                    MapKitFactory.getInstance().onStop()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val inputListener = remember {
        object : InputListener {
            override fun onMapTap(m: YMap, p: Point) {}
            override fun onMapLongTap(m: YMap, p: Point) { onLongTap(p) }
        }
    }
    val cameraListener = remember {
        CameraListener { _, pos, _, finished ->
            if (finished) {
                onCameraIdle(CameraState(pos.target.latitude, pos.target.longitude, pos.zoom))
                val zoomedIn = pos.zoom >= LABEL_ZOOM
                if (zoomedIn != showLabels.value) showLabels.value = zoomedIn
            }
        }
    }
    DisposableEffect(Unit) {
        map.addInputListener(inputListener)
        map.addCameraListener(cameraListener)
        onDispose {
            map.removeInputListener(inputListener)
            map.removeCameraListener(cameraListener)
        }
    }

    LaunchedEffect(Unit) {
        val target = initialCamera?.let { Point(it.lat, it.lng) } ?: Point(55.75, 37.62)
        val zoom = initialCamera?.zoom ?: 9.0f
        map.move(CameraPosition(target, zoom, 0.0f, 0.0f))
    }

    LaunchedEffect(focusPoint) {
        focusPoint?.let { map.move(CameraPosition(it, 15.0f, 0.0f, 0.0f)) }
    }

    LaunchedEffect(userPoint) {
        userCollection.clear()
        userPoint?.let {
            userCollection.addPlacemark().apply {
                geometry = it
                setIcon(userIcon)
            }
        }
    }

    LaunchedEffect(markers, showLabels.value) {
        stationsCollection.clear()
        tapListeners.clear()
        clusterTapListeners.clear()
        markers.forEach { marker ->
            val placemark = stationsCollection.addPlacemark()
            placemark.geometry = Point(marker.station.station.lat, marker.station.station.lng)

            val currentMark = marker.station.currentMark
            if (showLabels.value && currentMark != null) {
                val available = currentMark.items.filter { it.available }
                val lines = if (available.isEmpty()) listOf("Нет топлива")
                else available.take(3).map { "${it.type.title}  ${formatPrice(it.price)} ₽" }
                placemark.setIcon(
                    ImageProvider.fromBitmap(
                        MarkerIcons.labelBitmap(lines, MarkerIcons.colorFor(marker.freshness))
                    )
                )
                placemark.setIconStyle(IconStyle().setAnchor(PointF(0.5f, 1.0f)).setScale(0.5f))
            } else {
                markerIcons[marker.freshness]?.let { placemark.setIcon(it) }
            }

            val stationId = marker.station.station.id
            val listener = MapObjectTapListener { _, _ ->
                onStationTap(stationId)
                true
            }
            tapListeners.add(listener)
            placemark.addTapListener(listener)
        }
        stationsCollection.clusterPlacemarks(CLUSTER_RADIUS, CLUSTER_MIN_ZOOM)
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private const val LABEL_ZOOM = 14.5f
private const val CLUSTER_RADIUS = 60.0
private const val CLUSTER_MIN_ZOOM = 14

private fun formatPrice(p: Double): String =
    if (p % 1.0 == 0.0) p.toInt().toString() else "%.1f".format(p)
