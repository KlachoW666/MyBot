package com.fuelmap.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuelmap.app.data.repository.AuthRepository
import com.fuelmap.app.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun login(loginOrEmail: String, password: String, onSuccess: () -> Unit) {
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            handle(repo.login(loginOrEmail, password), onSuccess)
        }
    }

    fun register(
        login: String,
        password: String,
        email: String,
        carPlate: String,
        region: String,
        onSuccess: () -> Unit
    ) {
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            handle(repo.register(login, password, email, carPlate, region), onSuccess)
        }
    }

    fun resetPassword(loginOrEmail: String, email: String, newPassword: String, onSuccess: () -> Unit) {
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            handle(repo.resetPassword(loginOrEmail, email, newPassword), onSuccess)
        }
    }

    private fun handle(result: AuthResult, onSuccess: () -> Unit) {
        when (result) {
            is AuthResult.Success -> {
                _state.value = AuthUiState()
                onSuccess()
            }
            is AuthResult.Error -> _state.value = AuthUiState(error = result.message)
        }
    }
}
