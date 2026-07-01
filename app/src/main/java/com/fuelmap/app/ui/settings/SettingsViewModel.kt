package com.fuelmap.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuelmap.app.data.repository.AuthRepository
import com.fuelmap.app.data.settings.AppSettings
import com.fuelmap.app.data.settings.SettingsManager
import com.fuelmap.app.data.settings.ThemeMode
import com.fuelmap.app.domain.model.FuelType
import com.fuelmap.app.domain.model.hasPremium
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsManager,
    private val authRepo: AuthRepository
) : ViewModel() {

    val state: StateFlow<AppSettings> =
        settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val isPremium: StateFlow<Boolean> =
        authRepo.currentUser.map { it?.hasPremium() ?: false }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun clearMessage() { _message.value = null }

    fun setTheme(mode: ThemeMode) { viewModelScope.launch { settings.setThemeMode(mode) } }
    fun setRadius(meters: Int) { viewModelScope.launch { settings.setMarkRadius(meters) } }
    fun setPreferredFuel(fuel: FuelType?) { viewModelScope.launch { settings.setPreferredFuel(fuel) } }
    fun setGeoNotify(enabled: Boolean) { viewModelScope.launch { settings.setGeoNotify(enabled) } }

    fun subscribePremium() {
        viewModelScope.launch {
            authRepo.activatePremium()
            _message.value = "Premium активирован на 30 дней! 🎉"
        }
    }
}
