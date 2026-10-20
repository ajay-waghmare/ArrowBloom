package com.aweagle.arrowbloom

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aweagle.arrowbloom.ui.GameViewModel
import com.aweagle.arrowbloom.ui.LocalPressHaptic
import com.aweagle.arrowbloom.ui.LocalReduceMotion
import com.aweagle.arrowbloom.ui.Screen
import com.aweagle.arrowbloom.ui.screens.GameScreen
import com.aweagle.arrowbloom.ui.screens.HomeScreen
import com.aweagle.arrowbloom.ui.screens.SettingsScreen
import com.aweagle.arrowbloom.ui.theme.ArrowBloomTheme

class MainActivity : ComponentActivity() {

    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Always-light app: dark status and navigation bar icons on the paper background.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        val reduceMotion = runCatching {
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) <= 0f
        }.getOrDefault(false)
        setContent {
            ArrowBloomTheme {
                CompositionLocalProvider(
                    LocalPressHaptic provides { vm.feedback.press() },
                    LocalReduceMotion provides reduceMotion,
                ) {
                    Root(vm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onForeground()
    }

    override fun onPause() {
        vm.onBackground()
        super.onPause()
    }
}

@Composable
private fun Root(vm: GameViewModel) {
    val state by vm.uiState.collectAsState()
    BackHandler(enabled = state.screen == Screen.GAME) { vm.goHome() }
    BackHandler(enabled = state.screen == Screen.SETTINGS) { vm.closeSettings() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(state.palette.bgTop, state.palette.bgBottom))),
    ) {
        Crossfade(targetState = state.screen, label = "screen") { screen ->
            when (screen) {
                Screen.HOME -> HomeScreen(state, vm)
                Screen.GAME -> GameScreen(state, vm)
                Screen.SETTINGS -> SettingsScreen(state, vm)
            }
        }
    }
}
