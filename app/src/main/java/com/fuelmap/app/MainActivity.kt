package com.fuelmap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
            FuelMapTheme {
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
