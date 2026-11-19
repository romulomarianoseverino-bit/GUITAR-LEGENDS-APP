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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

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
                    .setApplicationId("1:1234567890:android:abcdef123456")
                    .setProjectId("guitar-legends-d7708")
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

    @Test
    fun `03 - full online room creation, second player join, live sync, disconnect detection, reconnect and exit`() = runBlocking {
        val tag = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
        val roomCode = "ROCK-$tag"
        println("[MP-TEST] stage 0: starting Alice Auth emulator sign-in")
        val aliceUid = signInTestUser("mp_alice_$tag@test.com")
        println("[MP-TEST] stage 0 complete: Alice authenticated uid=$aliceUid")

        assertEquals("Alice deve estar autenticada na instância padrão", aliceUid, auth.currentUser?.uid)
        val vm = GameViewModel(app)
        vm.updateGuestNickname("AliceLead")
        vm.enterGame()

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(width = 900.dp, height = 440.dp)) {
                    GuitarLegendsApp(viewModel = vm)
                }
            }
        }
        composeTestRule.waitForIdle()

        // 1. Abre Multiplayer e cria sala real
        composeTestRule.onNodeWithTag("menu_multiplayer").performClick()
        composeTestRule.waitForIdle()
        assertTrue(vm.currentScreen.value is GameScreen.CompetitiveMultiplayer)

        println("[MP-TEST] stage 1: requesting room creation code=$roomCode")
        vm.createMultiplayerRoom(MultiplayerMode.BATTLE_1V1, customRoomCode = roomCode)
        withTimeout(6000L) {
            while (vm.multiplayerRoom.value == null && vm.multiplayerError.value == null) delay(60L)
        }
        println("[MP-TEST] stage 1 result: room=${vm.multiplayerRoom.value?.roomCode} error=${vm.multiplayerError.value}")
        assertNotNull(
            "A criação da sala falhou ou ficou sem resposta: ${vm.multiplayerError.value}",
            vm.multiplayerRoom.value
        )
        composeTestRule.waitForIdle()
        assertEquals(roomCode, vm.multiplayerRoom.value?.roomCode)
        composeTestRule.onNodeWithTag("multiplayer_room_code_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("multiplayer_invite_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("participant_card_$aliceUid").assertIsDisplayed()

        // 2. Segundo jogador (Bob) entra na sala pelo código
        val (bobAuth, bobRepo) = getOrInitSecondaryBobRepo(app)
        println("[MP-TEST] stage 2: starting Bob Auth emulator sign-in")
        val bobUid = signInSecondary(bobAuth, "mp_bob_$tag@test.com")
        println("[MP-TEST] stage 2 complete: Bob authenticated uid=$bobUid")
        println("[MP-TEST] stage 2: Bob joining room")
        val bobJoinRes = withTimeout(6000L) {
            bobRepo.joinRoom(roomCode, "BobBass", Instrument.BASS, Difficulty.HARD)
        }
        assertTrue("Bob must join Alice's room: ${bobJoinRes.exceptionOrNull()?.message}", bobJoinRes.isSuccess)

        // Espera sincronização em tempo real no ViewModel da Alice
        println("[MP-TEST] stage 2 complete: Bob joined; waiting for Alice participant listener")
        withTimeout(6000L) {
            while (vm.multiplayerParticipants.value.size < 2) delay(60L)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("participant_card_$bobUid").assertIsDisplayed()

        // 3. Comunicação/Sincronização: Bob confirma pronto & envia pontuação; Alice troca faixa
        withTimeout(5000L) {
            bobRepo.updateParticipantLoadout(roomCode, "BobBass", Instrument.BASS, Difficulty.HARD, ready = true).getOrThrow()
            bobRepo.updateParticipantProgress(roomCode, score = 18500, combo = 27, rockMeter = 0.88f, starPowerActive = true).getOrThrow()
        }
        vm.updateMultiplayerTrack(AudioAnalyzer.BUILT_IN_TRACKS[1])

        withTimeout(6000L) {
            while (vm.multiplayerParticipants.value.find { it.userId == bobUid }?.ready != true) delay(60L)
            while (vm.multiplayerRoom.value?.trackId != AudioAnalyzer.BUILT_IN_TRACKS[1].id) delay(60L)
        }
        assertEquals(18500, vm.multiplayerParticipants.value.find { it.userId == bobUid }?.score)

        // 4. Tratamento de Desconexão & Reconexão
        withTimeout(5000L) {
            bobRepo.sendHeartbeat(roomCode, ParticipantConnectionState.DISCONNECTED).getOrThrow()
        }
        withTimeout(6000L) {
            while (!vm.disconnectedPeerNicknames.value.contains("BobBass")) delay(60L)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("multiplayer_connection_warning").assertIsDisplayed()
        composeTestRule.onNodeWithTag("multiplayer_reconnect_button").assertIsDisplayed()

        // 5. Código inexistente gera erro claro sem quebrar fluxo
        vm.joinMultiplayerRoom("NOPE-9999")
        withTimeout(5000L) {
            while (vm.multiplayerError.value == null) delay(60L)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("multiplayer_error_banner").assertIsDisplayed()

        // 6. Inicia partida sincronizada após Bob voltar online e sai limpo para o Menu Principal
        withTimeout(5000L) {
            bobRepo.sendHeartbeat(roomCode, ParticipantConnectionState.ONLINE).getOrThrow()
        }
        vm.startMultiplayerMatch()
        withTimeout(6000L) {
            while (vm.multiplayerRoom.value?.statusEnum != RoomStatus.PLAYING) delay(60L)
        }

        // Encerra partida/sala e retorna à página principal
        vm.stop()
        vm.leaveMultiplayerRoom()
        vm.navigateTo(GameScreen.MainMenu, pushStack = false)
        composeTestRule.waitForIdle()
        assertNull(vm.multiplayerRoom.value)
        assertTrue(vm.currentScreen.value is GameScreen.MainMenu)
        composeTestRule.onNodeWithTag("menu_multiplayer").assertIsDisplayed()

        bobAuth.signOut()
    }
}
