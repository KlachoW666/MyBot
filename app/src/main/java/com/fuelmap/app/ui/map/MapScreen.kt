package com.fuelmap.app.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fuelmap.app.util.LocationProvider
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuelmap.app.domain.model.FuelType
import com.fuelmap.app.domain.model.MarkFreshness
import com.fuelmap.app.ui.AppViewModelProvider
import com.fuelmap.app.ui.common.SupportFooter
import com.fuelmap.app.util.MarkerIcons
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    isLoggedIn: Boolean,
    isAdmin: Boolean,
    onStationClick: (Long) -> Unit,
    onLoginClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAdminClick: () -> Unit,
    vm: MapViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val markers by vm.markers.collectAsStateWithLifecycle()
    val filter by vm.fuelFilter.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var focusPoint by remember { mutableStateOf<Point?>(null) }

    fun locate() {
        scope.launch {
            LocationProvider.currentLocation(context)?.let {
                focusPoint = Point(it.latitude, it.longitude)
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

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    "Russia Oil",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            actions = {
                if (isAdmin) {
                    IconButton(onClick = onAdminClick) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Админка")
                    }
                }
                if (isLoggedIn) {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Filled.Person, contentDescription = "Профиль")
                    }
                } else {
                    IconButton(onClick = onLoginClick) {
                        Icon(Icons.Filled.Login, contentDescription = "Войти")
                    }
                }
            }
        )
    }) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            YandexMap(
                markers = markers,
                focusPoint = focusPoint,
                onStationTap = onStationClick,
                modifier = Modifier.fillMaxSize()
            )

            FloatingActionButton(
                onClick = { onLocateClick() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Моя геолокация")
            }

            // Фильтр по типу топлива
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
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
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(
                        "🟢 свежее · 🟡 устаревает · ⚪ нет данных · 🔴 нет топлива",
                        style = MaterialTheme.typography.labelSmall
                    )
                    SupportFooter(modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun YandexMap(
    markers: List<StationMarker>,
    focusPoint: Point?,
    onStationTap: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }

    // Иконки маркеров кэшируются (всего 4 варианта) — не создаём bitmap на каждую АЗС.
    val markerIcons = remember {
        MarkFreshness.entries.associateWith { ImageProvider.fromBitmap(MarkerIcons.bitmap(it)) }
    }

    // Держим сильные ссылки на тап-листенеры: MapKit хранит их как weak references.
    val tapListeners = remember { mutableListOf<MapObjectTapListener>() }

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

    // Начальное положение камеры — Московская область.
    LaunchedEffect(Unit) {
        mapView.mapWindow.map.move(
            CameraPosition(Point(55.75, 37.62), 9.0f, 0.0f, 0.0f)
        )
    }

    // Переход к местоположению пользователя по кнопке.
    LaunchedEffect(focusPoint) {
        focusPoint?.let {
            mapView.mapWindow.map.move(CameraPosition(it, 15.0f, 0.0f, 0.0f))
        }
    }

    // Перерисовка маркеров при изменении данных.
    LaunchedEffect(markers) {
        val map = mapView.mapWindow.map
        map.mapObjects.clear()
        tapListeners.clear()
        markers.forEach { marker ->
            val point = Point(marker.station.station.lat, marker.station.station.lng)
            val placemark = map.mapObjects.addPlacemark()
            placemark.geometry = point
            markerIcons[marker.freshness]?.let { placemark.setIcon(it) }
            val stationId = marker.station.station.id
            placemark.userData = stationId
            val listener = MapObjectTapListener { _, _ ->
                onStationTap(stationId)
                true
            }
            tapListeners.add(listener)
            placemark.addTapListener(listener)
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
