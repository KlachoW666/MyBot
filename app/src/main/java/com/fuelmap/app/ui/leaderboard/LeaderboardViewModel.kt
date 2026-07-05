package com.fuelmap.app.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuelmap.app.data.local.UserEntity
import com.fuelmap.app.data.repository.AdminRepository
import com.fuelmap.app.data.repository.AuthRepository
import com.fuelmap.app.domain.model.SupportedRegions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    authRepo: AuthRepository,
    private val adminRepo: AdminRepository
) : ViewModel() {

    val region = MutableStateFlow(SupportedRegions.names.first())

    init {
        viewModelScope.launch {
            authRepo.currentUser.first()?.region?.let { region.value = it }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val leaders: StateFlow<List<UserEntity>> =
        region.flatMapLatest { adminRepo.leaderboard(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setRegion(r: String) { region.value = r }
}
