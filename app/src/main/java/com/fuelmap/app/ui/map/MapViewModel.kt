package com.fuelmap.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuelmap.app.data.local.RegionEntity
import com.fuelmap.app.data.local.StationWithCurrentMark
import com.fuelmap.app.data.repository.StationRepository
import com.fuelmap.app.domain.model.FuelType
import com.fuelmap.app.domain.model.MarkFreshness
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StationMarker(
    val station: StationWithCurrentMark,
    val freshness: MarkFreshness
)

class MapViewModel(private val repo: StationRepository) : ViewModel() {

    /** Активный фильтр по типу топлива (null = показывать все). */
    val fuelFilter = MutableStateFlow<FuelType?>(null)

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
}
