package com.fuelmap.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fuelmap.app.domain.model.FuelType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, DARK, LIGHT }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val markRadiusMeters: Int = 750,
    val preferredFuel: FuelType? = null,
    val geoNotifyEnabled: Boolean = false
)

class SettingsManager(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val radiusKey = intPreferencesKey("mark_radius")
    private val fuelKey = stringPreferencesKey("preferred_fuel")
    private val geoKey = booleanPreferencesKey("geo_notify")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            themeMode = p[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            markRadiusMeters = p[radiusKey] ?: 750,
            preferredFuel = p[fuelKey]?.let { FuelType.fromName(it) },
            geoNotifyEnabled = p[geoKey] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setMarkRadius(meters: Int) {
        context.settingsDataStore.edit { it[radiusKey] = meters }
    }

    suspend fun setPreferredFuel(fuel: FuelType?) {
        context.settingsDataStore.edit { prefs ->
            if (fuel == null) prefs.remove(fuelKey) else prefs[fuelKey] = fuel.name
        }
    }

    suspend fun setGeoNotify(enabled: Boolean) {
        context.settingsDataStore.edit { it[geoKey] = enabled }
    }
}
