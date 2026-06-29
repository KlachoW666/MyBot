package com.fuelmap.app.ui.station

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuelmap.app.data.local.StationWithCurrentMark
import com.fuelmap.app.data.local.UserEntity
import com.fuelmap.app.data.repository.AuthRepository
import com.fuelmap.app.data.repository.FuelEntry
import com.fuelmap.app.data.repository.MarkRepository
import com.fuelmap.app.data.repository.StationRepository
import com.fuelmap.app.domain.model.ConfirmationType
import com.fuelmap.app.domain.model.Queue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StationViewModel(
    private val stationRepo: StationRepository,
    private val markRepo: MarkRepository,
    authRepo: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> =
        authRepo.currentUser.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun observeStation(id: Long): StateFlow<StationWithCurrentMark?> =
        stationRepo.observeStation(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun clearMessage() { _message.value = null }

    fun submitMark(
        stationId: Long,
        entries: List<FuelEntry>,
        queue: Queue,
        onDone: () -> Unit
    ) {
        val user = currentUser.value
        if (user == null) {
            _message.value = "Войдите, чтобы оставлять отметки"
            return
        }
        viewModelScope.launch {
            val station = stationRepo.getStation(stationId)
            if (station != null && stationRepo.enabledRegionAt(station.lat, station.lng) == null) {
                _message.value = "Регион пока не поддерживается"
                return@launch
            }
            markRepo.submitMark(stationId, user.id, entries, queue)
                .onSuccess {
                    _message.value = "Спасибо! Отметка сохранена"
                    onDone()
                }
                .onFailure { _message.value = it.message }
        }
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
