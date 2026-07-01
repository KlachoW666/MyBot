package com.fuelmap.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuelmap.app.data.settings.AppSettings
import com.fuelmap.app.data.settings.SettingsManager
import com.fuelmap.app.data.settings.ThemeMode
import com.fuelmap.app.domain.model.FuelType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settings: SettingsManager) : ViewModel() {

    val state: StateFlow<AppSettings> =
        settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setTheme(mode: ThemeMode) { viewModelScope.launch { settings.setThemeMode(mode) } }
    fun setRadius(meters: Int) { viewModelScope.launch { settings.setMarkRadius(meters) } }
    fun setPreferredFuel(fuel: FuelType?) { viewModelScope.launch { settings.setPreferredFuel(fuel) } }
    fun setGeoNotify(enabled: Boolean) { viewModelScope.launch { settings.setGeoNotify(enabled) } }
}
