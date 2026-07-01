package com.fuelmap.app.ui.station

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuelmap.app.data.local.StationWithCurrentMark
import com.fuelmap.app.data.local.UserEntity
import com.fuelmap.app.data.repository.AuthRepository
import com.fuelmap.app.data.repository.FavoriteRepository
import com.fuelmap.app.data.repository.FuelEntry
import com.fuelmap.app.data.repository.MarkRepository
import com.fuelmap.app.data.repository.StationRepository
import com.fuelmap.app.domain.model.ConfirmationType
import com.fuelmap.app.domain.model.Queue
import com.fuelmap.app.util.GeoUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StationViewModel(
    private val stationRepo: StationRepository,
    private val markRepo: MarkRepository,
    private val authRepo: AuthRepository,
    private val favoriteRepo: FavoriteRepository
) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> =
        authRepo.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val favorites: StateFlow<Set<Long>> =
        authRepo.currentUser.flatMapLatest { u ->
            if (u == null) flowOf(emptySet()) else favoriteRepo.favorites(u.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleFavorite(stationId: Long) {
        val user = currentUser.value
        if (user == null) {
            _message.value = "Войдите, чтобы добавлять в избранное"
            return
        }
        viewModelScope.launch { favoriteRepo.toggle(user.id, stationId) }
    }

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun observeStation(id: Long): StateFlow<StationWithCurrentMark?> =
        stationRepo.observeStation(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun stationHistory(id: Long): StateFlow<List<com.fuelmap.app.data.local.MarkWithItems>> =
        markRepo.stationHistory(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearMessage() { _message.value = null }

    fun showMessage(msg: String) { _message.value = msg }

    fun submitMark(
        stationId: Long,
        entries: List<FuelEntry>,
        queue: Queue,
        userLat: Double?,
        userLng: Double?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val user = authRepo.currentUserOnce()
            if (user == null) {
                _message.value = "Войдите, чтобы оставлять отметки"
                return@launch
            }
            val station = stationRepo.getStation(stationId)
            if (station == null) {
                _message.value = "АЗС не найдена"
                return@launch
            }

            // Админы и супер-админы ставят метки на любые АЗС без проверки расстояния и региона.
            if (!user.role.isAdmin) {
                if (stationRepo.enabledRegionAt(station.lat, station.lng) == null) {
                    _message.value = "Регион пока не поддерживается"
                    return@launch
                }
                if (userLat == null || userLng == null) {
                    _message.value = "Не удалось определить геолокацию. Включите GPS и разрешите доступ к местоположению."
                    return@launch
                }
                val distance = GeoUtils.distanceMeters(userLat, userLng, station.lat, station.lng)
                if (distance > MAX_MARK_DISTANCE_METERS) {
                    _message.value = "Вы слишком далеко от АЗС (%.1f км). Отметку можно ставить только рядом с заправкой."
                        .format(distance / 1000.0)
                    return@launch
                }
            }

            markRepo.submitMark(stationId, user.id, entries, queue)
                .onSuccess {
                    _message.value = "Спасибо! Отметка сохранена"
                    onDone()
                }
                .onFailure { _message.value = it.message }
        }
    }

    companion object {
        /** Максимальное расстояние до АЗС, при котором разрешена отметка (метры). */
        const val MAX_MARK_DISTANCE_METERS = 750.0
    }

    fun confirm(markId: Long, type: ConfirmationType) {
        val user = currentUser.value
        if (user == null) {
            _message.value = "Войдите, чтобы подтверждать отметки"
            return
        }
        viewModelScope.launch {
            markRepo.confirm(markId, user.id, type)
                .onSuccess {
                    _message.value = if (type == ConfirmationType.CONFIRM) "Подтверждено" else "Отмечено как закончилось"
                }
                .onFailure { _message.value = it.message }
        }
    }
}
