package com.fuelmap.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuelmap.app.data.local.RegionEntity
import com.fuelmap.app.data.local.StationWithCurrentMark
import com.fuelmap.app.data.local.UserEntity
import com.fuelmap.app.data.repository.AuthRepository
import com.fuelmap.app.data.repository.StationRepository
import com.fuelmap.app.domain.model.FuelType
import com.fuelmap.app.domain.model.MarkFreshness
import com.fuelmap.app.domain.model.StationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StationMarker(
    val station: StationWithCurrentMark,
    val freshness: MarkFreshness
)

/** Сохранённое положение камеры между перекомпозициями (чтобы карта не «сбрасывалась»). */
data class CameraState(val lat: Double, val lng: Double, val zoom: Float)

class MapViewModel(
    private val repo: StationRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    /** Активный фильтр по типу топлива (null = показывать все). */
    val fuelFilter = MutableStateFlow<FuelType?>(null)

    val currentUser: StateFlow<UserEntity?> =
        authRepo.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Последняя позиция камеры — переживает уход/возврат на экран. */
    var lastCamera: CameraState? = null

    val regions: StateFlow<List<RegionEntity>> = repo.regions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val markers: StateFlow<List<StationMarker>> =
        combine(repo.stations, fuelFilter) { stations, filter ->
            stations
                .filter { s ->
                    if (filter == null) true
                    else s.currentMark?.items?.any { it.type == filter && it.available } == true
                }
                .map { s ->
                    val mark = s.currentMark
                    StationMarker(
                        station = s,
                        freshness = MarkFreshness.of(
                            createdAt = mark?.mark?.createdAt,
                            hasAvailableFuel = mark?.hasAvailableFuel ?: false
                        )
                    )
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(type: FuelType?) {
        fuelFilter.value = type
    }

    fun clearMessage() { _message.value = null }

    /** Добавление АЗС по долгому нажатию на карте. */
    fun addStationAt(name: String, brand: String, lat: Double, lng: Double) {
        val user = currentUser.value
        if (user == null) {
            _message.value = "Войдите, чтобы добавлять АЗС"
            return
        }
        viewModelScope.launch {
            repo.proposeStation(name, brand, lat, lng, user.role.isAdmin, user.id)
                .onSuccess { status ->
                    _message.value = if (status == StationStatus.APPROVED)
                        "АЗС добавлена и видна всем"
                    else
                        "АЗС отправлена на модерацию администратору"
                }
                .onFailure { _message.value = it.message }
        }
    }
}
