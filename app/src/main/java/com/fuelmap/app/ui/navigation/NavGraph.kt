package com.fuelmap.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fuelmap.app.data.local.UserEntity
import com.fuelmap.app.ui.admin.AdminAddStationScreen
import com.fuelmap.app.ui.admin.AdminRegionsScreen
import com.fuelmap.app.ui.admin.AdminScreen
import com.fuelmap.app.ui.admin.AdminUserScreen
import com.fuelmap.app.ui.auth.LoginScreen
import com.fuelmap.app.ui.auth.RegisterScreen
import com.fuelmap.app.ui.auth.ResetScreen
import com.fuelmap.app.ui.leaderboard.LeaderboardScreen
import com.fuelmap.app.ui.map.MapScreen
import com.fuelmap.app.ui.profile.ProfileScreen
import com.fuelmap.app.ui.station.MarkScreen
import com.fuelmap.app.ui.station.StationScreen

@Composable
fun NavGraph(currentUser: UserEntity?) {
    val nav = rememberNavController()
    val isLoggedIn = currentUser != null
    val isAdmin = currentUser?.role?.isAdmin == true

    NavHost(navController = nav, startDestination = Routes.MAP) {

        composable(Routes.MAP) {
            MapScreen(
                isLoggedIn = isLoggedIn,
                isAdmin = isAdmin,
                onStationClick = { nav.navigate(Routes.station(it)) },
                onLoginClick = { nav.navigate(Routes.LOGIN) },
                onProfileClick = { nav.navigate(Routes.PROFILE) },
                onAdminClick = { nav.navigate(Routes.ADMIN) }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onBack = { nav.popBackStack() },
                onLoggedIn = { nav.popBackStack(Routes.MAP, inclusive = false) },
                onRegister = { nav.navigate(Routes.REGISTER) },
                onReset = { nav.navigate(Routes.RESET) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onBack = { nav.popBackStack() },
                onRegistered = { nav.popBackStack(Routes.MAP, inclusive = false) }
            )
        }
        composable(Routes.RESET) {
            ResetScreen(
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack(Routes.LOGIN, inclusive = false) }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { nav.popBackStack() },
                onLoggedOut = { nav.popBackStack(Routes.MAP, inclusive = false) },
                onLeaderboard = { nav.navigate(Routes.LEADERBOARD) }
            )
        }
        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(onBack = { nav.popBackStack() })
        }

        composable(
            Routes.STATION,
            arguments = listOf(navArgument(Routes.ARG_STATION_ID) { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong(Routes.ARG_STATION_ID) ?: 0L
            StationScreen(
                stationId = id,
                onBack = { nav.popBackStack() },
                onMark = { stationId ->
                    if (isLoggedIn) nav.navigate(Routes.mark(stationId)) else nav.navigate(Routes.LOGIN)
                }
            )
        }
        composable(
            Routes.MARK,
            arguments = listOf(navArgument(Routes.ARG_STATION_ID) { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong(Routes.ARG_STATION_ID) ?: 0L
            MarkScreen(stationId = id, onBack = { nav.popBackStack() })
        }

        composable(Routes.ADMIN) {
            // Защита: только админ/супер-админ
            if (!isAdmin) {
                nav.popBackStack(Routes.MAP, inclusive = false)
            } else {
                AdminScreen(
                    onBack = { nav.popBackStack() },
                    onUserClick = { nav.navigate(Routes.adminUser(it)) },
                    onRegions = { nav.navigate(Routes.ADMIN_REGIONS) },
                    onAddStation = { nav.navigate(Routes.ADMIN_ADD_STATION) }
                )
            }
        }
        composable(Routes.ADMIN_REGIONS) {
            AdminRegionsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.ADMIN_ADD_STATION) {
            AdminAddStationScreen(onBack = { nav.popBackStack() })
        }
        composable(
            Routes.ADMIN_USER,
            arguments = listOf(navArgument(Routes.ARG_USER_ID) { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong(Routes.ARG_USER_ID) ?: 0L
            AdminUserScreen(userId = id, onBack = { nav.popBackStack() })
        }
    }
}
