package com.fuelmap.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fuelmap.app.FuelMapApplication
import com.fuelmap.app.ui.admin.AdminViewModel
import com.fuelmap.app.ui.auth.AuthViewModel
import com.fuelmap.app.ui.leaderboard.LeaderboardViewModel
import com.fuelmap.app.ui.map.MapViewModel
import com.fuelmap.app.ui.profile.ProfileViewModel
import com.fuelmap.app.ui.station.StationViewModel

fun CreationExtras.app(): FuelMapApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FuelMapApplication)

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { AuthViewModel(app().container.authRepository) }
        initializer { MapViewModel(app().container.stationRepository) }
        initializer {
            StationViewModel(
                app().container.stationRepository,
                app().container.markRepository,
                app().container.authRepository
            )
        }
        initializer {
            ProfileViewModel(
                app().container.authRepository,
                app().container.markRepository
            )
        }
        initializer {
            LeaderboardViewModel(
                app().container.authRepository,
                app().container.adminRepository
            )
        }
        initializer {
            AdminViewModel(
                app().container.adminRepository,
                app().container.stationRepository,
                app().container.markRepository,
                app().container.authRepository
            )
        }
    }
}
