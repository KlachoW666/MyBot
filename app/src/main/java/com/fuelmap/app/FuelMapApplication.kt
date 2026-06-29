package com.fuelmap.app

import android.app.Application
import com.fuelmap.app.data.local.DatabaseSeeder
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FuelMapApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Yandex MapKit: ключ задаётся до initialize().
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
        MapKitFactory.initialize(this)

        container = AppContainer(this)

        // Идемпотентный сидинг: супер-админ, регионы, базовые АЗС.
        appScope.launch {
            DatabaseSeeder(container.database).seed()
        }
    }
}
