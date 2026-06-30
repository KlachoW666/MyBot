package com.fuelmap.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fuelmap.app.data.local.UserEntity
import com.fuelmap.app.ui.admin.AdminAddStationScreen
import com.fuelmap.app.ui.admin.AdminModerationScreen
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

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun NavGraph(currentUser: UserEntity?) {
    val nav = rememberNavController()
    val isLoggedIn = currentUser != null
    val isAdmin = currentUser?.role?.isAdmin == true

    val backStackEntry by nav.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    val tabs = buildList {
        add(Tab(Routes.MAP, "Карта", Icons.Filled.Map))
        add(Tab(Routes.LEADERBOARD, "Лидеры", Icons.Filled.Leaderboard))
        add(Tab(Routes.PROFILE, "Профиль", Icons.Filled.Person))
        if (isAdmin) add(Tab(Routes.ADMIN, "Админ", Icons.Filled.AdminPanelSettings))
    }
    val showBottomBar = tabs.any { it.route == route }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    NavigationBar(containerColor = Color.Transparent) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = route == tab.route,
                                onClick = {
                                    if (route != tab.route) {
                                        nav.navigate(tab.route) {
                                            popUpTo(Routes.MAP) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = Routes.MAP,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) + scaleOut(targetScale = 0.96f, animationSpec = tween(160)) }
        ) {
            composable(Routes.MAP) {
                MapScreen(
                    onStationDetails = { nav.navigate(Routes.station(it)) },
                    onMark = { stationId ->
                        if (isLoggedIn) nav.navigate(Routes.mark(stationId)) else nav.navigate(Routes.LOGIN)
                    }
                )
            }

            composable(Routes.LEADERBOARD) { LeaderboardScreen() }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onLogin = { nav.navigate(Routes.LOGIN) },
                    onRegister = { nav.navigate(Routes.REGISTER) }
                )
            }

            composable(Routes.ADMIN) {
                if (!isAdmin) {
                    LeaderboardScreen()
                } else {
                    AdminScreen(
                        onUserClick = { nav.navigate(Routes.adminUser(it)) },
                        onRegions = { nav.navigate(Routes.ADMIN_REGIONS) },
                        onAddStation = { nav.navigate(Routes.ADMIN_ADD_STATION) },
                        onModeration = { nav.navigate(Routes.ADMIN_MODERATION) }
                    )
                }
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

            composable(Routes.ADMIN_REGIONS) { AdminRegionsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.ADMIN_ADD_STATION) { AdminAddStationScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.ADMIN_MODERATION) { AdminModerationScreen(onBack = { nav.popBackStack() }) }
            composable(
                Routes.ADMIN_USER,
                arguments = listOf(navArgument(Routes.ARG_USER_ID) { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong(Routes.ARG_USER_ID) ?: 0L
                AdminUserScreen(userId = id, onBack = { nav.popBackStack() })
            }
        }
    }
}
