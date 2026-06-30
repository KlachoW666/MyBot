package com.fuelmap.app.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuelmap.app.data.local.StationWithCurrentMark
import com.fuelmap.app.data.repository.AuthRepository
import com.fuelmap.app.data.repository.FavoriteRepository
import com.fuelmap.app.data.repository.StationRepository
import com.fuelmap.app.domain.model.Queue
import com.fuelmap.app.util.GeoUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NearbySort { DISTANCE, PRICE }

data class NearbyItem(
    val station: StationWithCurrentMark,
    val distanceMeters: Double?,
    val minAvailablePrice: Double?,
    val isFavorite: Boolean
)

private data class Controls(
    val query: String,
    val brand: String?,
    val onlyFavorites: Boolean,
    val onlyNoQueue: Boolean,
    val sort: NearbySort
)

class NearbyViewModel(
    private val stationRepo: StationRepository,
    private val favoriteRepo: FavoriteRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    val query = MutableStateFlow("")
    val brand = MutableStateFlow<String?>(null)
    val onlyFavorites = MutableStateFlow(false)
    val onlyNoQueue = MutableStateFlow(false)
    val sort = MutableStateFlow(NearbySort.DISTANCE)
    private val userLocation = MutableStateFlow<Pair<Double, Double>?>(null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val brands: StateFlow<List<String>> =
        stationRepo.brands.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val favoriteIds: Flow<Set<Long>> =
        authRepo.currentUser.flatMapLatest { u ->
            if (u == null) flowOf(emptySet()) else favoriteRepo.favorites(u.id)
        }

    private val controls: Flow<Controls> =
        combine(query, brand, onlyFavorites, onlyNoQueue, sort) { q, b, fav, noQ, s ->
            Controls(q, b, fav, noQ, s)
        }

    val items: StateFlow<List<NearbyItem>> =
        combine(stationRepo.stations, favoriteIds, userLocation, controls) { stations, favs, loc, c ->
            stations.asSequence()
                .filter { s ->
                    val q = c.query.trim()
                    q.isBlank() ||
                        s.station.name.contains(q, true) ||
                        s.station.brand.contains(q, true) ||
                        s.station.address.contains(q, true)
                }
                .filter { s -> c.brand == null || s.station.brand.equals(c.brand, true) }
                .filter { s -> !c.onlyFavorites || favs.contains(s.station.id) }
                .filter { s ->
                    !c.onlyNoQueue || (s.currentMark?.mark?.queue ?: Queue.NONE) == Queue.NONE
                }
                .map { s ->
                    val dist = loc?.let { (lat, lng) ->
                        GeoUtils.distanceMeters(lat, lng, s.station.lat, s.station.lng)
                    }
                    val minPrice = s.currentMark?.items
                        ?.filter { it.available }
                        ?.minOfOrNull { it.price }
                    NearbyItem(s, dist, minPrice, favs.contains(s.station.id))
                }
                .sortedWith(
                    when (c.sort) {
                        NearbySort.DISTANCE -> compareBy(nullsLast<Double>()) { it.distanceMeters }
                        NearbySort.PRICE -> compareBy(nullsLast<Double>()) { it.minAvailablePrice }
                    }
                )
                .take(120)
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setLocation(lat: Double, lng: Double) { userLocation.value = lat to lng }
    fun setQuery(q: String) { query.value = q }
    fun setBrand(b: String?) { brand.value = b }
    fun setSort(s: NearbySort) { sort.value = s }
    fun toggleOnlyFavorites() { onlyFavorites.value = !onlyFavorites.value }
    fun toggleNoQueue() { onlyNoQueue.value = !onlyNoQueue.value }
    fun clearMessage() { _message.value = null }

    fun toggleFavorite(stationId: Long) {
        viewModelScope.launch {
            val user = authRepo.currentUserOnce()
            if (user == null) {
                _message.value = "Войдите, чтобы добавлять в избранное"
                return@launch
            }
            favoriteRepo.toggle(user.id, stationId)
        }
    }
}
