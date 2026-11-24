package com.example

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.guitarlegends.analysis.AudioAnalyzer
import com.example.guitarlegends.data.MultiplayerCloudRepository
import com.example.guitarlegends.model.Difficulty
import com.example.guitarlegends.model.Instrument
import com.example.guitarlegends.model.MultiplayerMode
import com.example.guitarlegends.model.ParticipantConnectionState
import com.example.guitarlegends.model.RoomStatus
import com.example.guitarlegends.ui.screens.MainMenuScreen
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.LooperMode
import java.io.File
import java.util.UUID

@LooperMode(LooperMode.Mode.PAUSED)
class MultiplayerRecoveryTest : FirestoreEmulatorTestBase() {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: Application

    @Before
    fun cleanLocalState() {
        app = ApplicationProvider.getApplicationContext()
        File(app.filesDir, "local_player_progress.json").delete()
        File(app.filesDir, "local_player_progress.json.tmp").delete()
    }

    private fun getOrInitSecondaryBobRepo(context: Context): Pair<FirebaseAuth, MultiplayerCloudRepository> {
        val bobAppName = "bob-mp-test-app"
        val bobApp = FirebaseApp.getApps(context).firstOrNull { it.name == bobAppName }
            ?: FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.applet.kpfcic")
                    .setProjectId("demo-no-project")
                    .setApiKey("fake-api-key-for-emulator")
                    .build(),
                bobAppName
            ).also { initialized ->
                val bobDb = FirebaseFirestore.getInstance(initialized, databaseId)
                bobDb.useEmulator("127.0.0.1", 8085)
                bobDb.firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
                val bobAuth = FirebaseAuth.getInstance(initialized)
                bobAuth.useEmulator("127.0.0.1", 9099)
            }
        val bobAuth = FirebaseAuth.getInstance(bobApp)
        val bobDb = FirebaseFirestore.getInstance(bobApp, databaseId)
        return bobAuth to MultiplayerCloudRepository(bobDb, bobAuth)
    }

    private suspend fun signInSecondary(bobAuth: FirebaseAuth, email: String): String {
        val result = try {
            bobAuth.createUserWithEmailAndPassword(email, "password123").await()
        } catch (_: FirebaseAuthUserCollisionException) {
            bobAuth.signInWithEmailAndPassword(email, "password123").await()
        }
        return checkNotNull(result.user?.uid)
    }

    @Test
    fun `01 - MULTIPLAYER button stays visible without scrolling on 800x360 landscape and opens screen`() {
        val vm = GameViewModel(app)
        vm.enterGame()

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(width = 800.dp, height = 360.dp)) {
                    MainMenuScreen(viewModel = vm)
                }
            }
        }
        composeTestRule.waitForIdle()

        // Garante visibilidade simultânea sem rolagem dos botões principais e MULTIPLAYER
        composeTestRule.onNodeWithTag("menu_quick_play").assertIsDisplayed()
        composeTestRule.onNodeWithTag("menu_multiplayer").assertIsDisplayed()
        composeTestRule.onNodeWithTag("menu_career").assertIsDisplayed()
        composeTestRule.onNodeWithTag("menu_settings").assertIsDisplayed()

        // Clica em MULTIPLAYER e valida abertura
        composeTestRule.onNodeWithTag("menu_multiplayer").performClick()
        composeTestRule.waitForIdle()
        assertTrue(vm.currentScreen.value is GameScreen.CompetitiveMultiplayer)
    }

    @Test
    fun `02 - guest opening MultiplayerScreen sees auth option, local challenge, and returns cleanly to MainMenu`() {
        val vm = GameViewModel(app)
        vm.enterGame()

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(width = 840.dp, height = 400.dp)) {
                    GuitarLegendsApp(viewModel = vm)
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("menu_multiplayer").performClick()
        composeTestRule.waitForIdle()
        assertTrue(vm.currentScreen.value is GameScreen.CompetitiveMultiplayer)

        composeTestRule.onNodeWithTag("multiplayer_google_signin_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("multiplayer_tab_battle").assertIsDisplayed()
        composeTestRule.onNodeWithTag("multiplayer_tab_band").assertIsDisplayed()
        composeTestRule.onNodeWithTag("multiplayer_start_button").assertIsDisplayed()

        // Alterna abas e retorna para a página principal
        composeTestRule.onNodeWithTag("multiplayer_tab_band").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MultiplayerMode.BAND_COOP, vm.multiplayerMode.value)

        vm.navigateBack()
        composeTestRule.waitForIdle()
        assertTrue(vm.currentScreen.value is GameScreen.MainMenu)
        composeTestRule.onNodeWithTag("menu_multiplayer").assertIsDisplayed()
    }


}