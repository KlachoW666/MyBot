package com.fuelmap.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuelmap.app.data.local.GasStationEntity
import com.fuelmap.app.data.local.MarkWithItems
import com.fuelmap.app.data.local.RegionEntity
import com.fuelmap.app.data.local.UserEntity
import com.fuelmap.app.data.local.ReportEntity
import com.fuelmap.app.data.repository.AdminRepository
import com.fuelmap.app.data.repository.AuthRepository
import com.fuelmap.app.data.repository.MarkRepository
import com.fuelmap.app.data.repository.ReportRepository
import com.fuelmap.app.data.repository.StationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val adminRepo: AdminRepository,
    private val stationRepo: StationRepository,
    private val markRepo: MarkRepository,
    authRepo: AuthRepository,
    private val reportRepo: ReportRepository
) : ViewModel() {

    val reports: StateFlow<List<ReportEntity>> =
        reportRepo.reports.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissReport(id: Long) {
        viewModelScope.launch {
            reportRepo.dismiss(id)
            _message.value = "Жалоба отклонена"
        }
    }

    fun deleteReportedMark(markId: Long) {
        viewModelScope.launch {
            reportRepo.deleteMark(markId)
            _message.value = "Метка удалена"
        }
    }

    val currentUser: StateFlow<UserEntity?> =
        authRepo.currentUser.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val query = MutableStateFlow("")

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    @OptIn(ExperimentalCoroutinesApi::class)
    val users: StateFlow<List<UserEntity>> =
        query.flatMapLatest { q ->
            if (q.isBlank()) adminRepo.allUsers else adminRepo.searchUsers(q)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val regions: StateFlow<List<RegionEntity>> =
        stationRepo.regions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingStations: StateFlow<List<GasStationEntity>> =
        stationRepo.pendingStations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun approveStation(id: Long) {
        viewModelScope.launch {
            stationRepo.approveStation(id)
            _message.value = "АЗС одобрена и видна всем"
        }
    }

    fun rejectStation(id: Long) {
        viewModelScope.launch {
            stationRepo.rejectStation(id)
            _message.value = "АЗС отклонена"
        }
    }

    fun userHistory(userId: Long): Flow<List<MarkWithItems>> = markRepo.userHistory(userId)

    fun observeUser(id: Long): StateFlow<UserEntity?> =
        adminRepo.observeUser(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun getUser(id: Long): UserEntity? = adminRepo.getUser(id)

    fun clearMessage() { _message.value = null }

    fun setQuery(q: String) { query.value = q }

    fun toggleRegion(region: RegionEntity) {
        viewModelScope.launch {
            stationRepo.setRegionEnabled(region, !region.enabled)
            _message.value = if (!region.enabled) "Регион включён" else "Регион выключен"
        }
    }

    fun setBanned(user: UserEntity, banned: Boolean) {
        viewModelScope.launch {
            adminRepo.setBanned(user, banned)
                .onSuccess { _message.value = if (banned) "Пользователь забанен" else "Пользователь разбанен" }
                .onFailure { _message.value = it.message }
        }
    }

    fun setAdmin(actor: UserEntity, user: UserEntity, makeAdmin: Boolean) {
        if (actor.role != com.fuelmap.app.domain.model.Role.SUPER_ADMIN) {
            _message.value = "Только супер-администратор может менять роли"
            return
        }
        viewModelScope.launch {
            adminRepo.setAdmin(user, makeAdmin)
                .onSuccess { _message.value = if (makeAdmin) "Назначен администратором" else "Роль снята" }
                .onFailure { _message.value = it.message }
        }
    }

    fun grantPremium(user: UserEntity) {
        viewModelScope.launch {
            adminRepo.grantPremium(user)
            _message.value = "Premium выдан на 30 дней"
        }
    }

    fun revokePremium(user: UserEntity) {
        viewModelScope.launch {
            adminRepo.revokePremium(user)
            _message.value = "Premium снят"
        }
    }

    fun deleteMark(markId: Long) {
        viewModelScope.launch {
            markRepo.deleteMark(markId)
            _message.value = "Отметка удалена"
        }
    }

    fun importStations() {
        viewModelScope.launch {
            _message.value = "Загружаю АЗС из OpenStreetMap…"
            stationRepo.importFromOsm()
                .onSuccess { _message.value = "Готово: загружено $it АЗС из OSM" }
                .onFailure { _message.value = "Ошибка загрузки: ${it.message ?: "нет сети"}" }
        }
    }

    fun addStation(station: GasStationEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            stationRepo.addStation(station)
            _message.value = "АЗС добавлена"
            onDone()
        }
    }
}
