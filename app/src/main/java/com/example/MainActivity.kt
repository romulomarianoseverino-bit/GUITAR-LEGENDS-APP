package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.guitarlegends.ui.components.RockPurpleBg
import com.example.guitarlegends.ui.screens.*
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Force sensor landscape orientation (auto-rotates both horizontal ways)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Keep screen on during concert gameplay
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Fullscreen immersive mode (hides system navigation and status bars)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = RockPurpleBg
                ) {
                    GuitarLegendsApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.audioPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stop()
    }
}

@Composable
fun GuitarLegendsApp(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    when (currentScreen) {
        is GameScreen.Splash -> SplashScreen(viewModel = viewModel)
        is GameScreen.MainMenu -> MainMenuScreen(viewModel = viewModel)
        is GameScreen.Career -> CareerScreen(viewModel = viewModel)
        is GameScreen.CompetitiveMultiplayer,
        is GameScreen.CoopMultiplayer -> MultiplayerScreen(viewModel = viewModel)
        is GameScreen.MusicLibrary,
        is GameScreen.SongDetail,
        is GameScreen.MusicPreview -> MusicLibraryScreen(viewModel = viewModel)
        is GameScreen.InstrumentSelect -> InstrumentSelectScreen(viewModel = viewModel)
        is GameScreen.DifficultySelect -> DifficultySelectScreen(viewModel = viewModel)
        is GameScreen.Customization -> CustomizationScreen(viewModel = viewModel)
        is GameScreen.Gameplay -> LiveStageGameplayScreen(viewModel = viewModel)
        is GameScreen.Results -> ResultsScreen(viewModel = viewModel)
        is GameScreen.Statistics -> StatisticsScreen(viewModel = viewModel)
        is GameScreen.Settings -> SettingsScreen(viewModel = viewModel)
    }
}
