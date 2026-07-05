package com.fuelmap.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.fuelmap.app.data.settings.AppSettings
import com.fuelmap.app.data.settings.ThemeMode
import com.fuelmap.app.ui.navigation.NavGraph
import com.fuelmap.app.ui.onboarding.OnboardingScreen
import com.fuelmap.app.ui.theme.FuelMapTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as FuelMapApplication).container

        setContent {
            val settings by container.settingsManager.settings.collectAsState(initial = AppSettings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // Иконки системных баров подстраиваются под тему (тёмные на светлой, светлые на тёмной).
            LaunchedEffect(darkTheme) {
                val style = if (darkTheme) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            FuelMapTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val onboardingDone by container.session.onboardingDone.collectAsState(initial = null)
                    val currentUser by container.authRepository.currentUser.collectAsState(initial = null)
                    val scope = rememberCoroutineScope()

                    when (onboardingDone) {
                        null -> Unit // короткая загрузка флага — держим фон
                        false -> OnboardingScreen(onFinish = { scope.launch { container.session.setOnboardingDone() } })
                        else -> NavGraph(currentUser = currentUser)
                    }
                }
            }
        }
    }
}
