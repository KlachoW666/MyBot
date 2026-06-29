package com.fuelmap.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuelmap.app.data.local.MarkWithItems
import com.fuelmap.app.data.local.UserEntity
import com.fuelmap.app.data.repository.AuthRepository
import com.fuelmap.app.data.repository.MarkRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepo: AuthRepository,
    private val markRepo: MarkRepository
) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> =
        authRepo.currentUser.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<List<MarkWithItems>> =
        authRepo.currentUser.flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else markRepo.userHistory(user.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepo.logout()
            onDone()
        }
    }
}
