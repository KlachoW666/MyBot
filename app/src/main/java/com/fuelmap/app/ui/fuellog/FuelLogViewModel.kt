package com.fuelmap.app.ui.fuellog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuelmap.app.data.local.FuelLogEntity
import com.fuelmap.app.data.repository.AuthRepository
import com.fuelmap.app.data.repository.FuelLogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FuelStats(
    val totalLiters: Double,
    val totalCost: Double,
    val avgConsumption: Double? // л/100 км
)

class FuelLogViewModel(
    private val authRepo: AuthRepository,
    private val fuelLogRepo: FuelLogRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val entries: StateFlow<List<FuelLogEntity>> =
        authRepo.currentUser.flatMapLatest { u ->
            if (u == null) flowOf(emptyList()) else fuelLogRepo.entries(u.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<FuelStats> = entries.map { list ->
        val totalLiters = list.sumOf { it.liters }
        val totalCost = list.sumOf { it.cost }
        val byOdo = list.sortedBy { it.odometer }
        val avg = if (byOdo.size >= 2) {
            val dist = byOdo.last().odometer - byOdo.first().odometer
            val litersAfterFirst = byOdo.drop(1).sumOf { it.liters }
            if (dist > 0) litersAfterFirst / dist * 100 else null
        } else null
        FuelStats(totalLiters, totalCost, avg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FuelStats(0.0, 0.0, null))

    fun clearMessage() { _message.value = null }

    fun add(liters: Double, cost: Double, odometer: Double) {
        viewModelScope.launch {
            val user = authRepo.currentUserOnce()
            if (user == null) {
                _message.value = "Войдите, чтобы вести бортжурнал"
                return@launch
            }
            fuelLogRepo.add(user.id, liters, cost, odometer)
            _message.value = "Запись добавлена"
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { fuelLogRepo.delete(id) }
    }
}
