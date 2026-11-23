package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.guitarlegends.analysis.AudioAnalyzer
import com.example.guitarlegends.data.MultiplayerCloudRepository
import com.example.guitarlegends.model.Difficulty
import com.example.guitarlegends.model.Instrument
import com.example.guitarlegends.model.MultiplayerMode
import com.example.guitarlegends.model.ParticipantConnectionState
import com.example.guitarlegends.model.RoomStatus
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel
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
import org.junit.Test
import org.robolectric.annotation.LooperMode
import java.util.UUID

@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class MultiplayerOnlineIntegrationTest : FirestoreEmulatorTestBase() {

    private lateinit var app: Application

    @Before
    fun prepareApplication() {
        app = ApplicationProvider.getApplicationContext()
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
    fun `03 - full online room creation, second player join, live sync, disconnect detection, reconnect and exit`() = runBlocking(Dispatchers.Default) {
        val tag = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
        val roomCode = "ROCK-$tag"
        val aliceUid = signInTestUser("mp_alice_$tag@test.com")

        val vm = GameViewModel(app)
        vm.updateGuestNickname("AliceLead")
        vm.enterGame()

        // Este teste exercita apenas o fluxo online pelo ViewModel/repositórios.
        // A interface e a navegação são cobertas separadamente pelos testes Compose.
        vm.navigateTo(GameScreen.CompetitiveMultiplayer, pushStack = false)
        assertTrue(vm.currentScreen.value is GameScreen.CompetitiveMultiplayer)

        // 1. Cria sala online
        vm.createMultiplayerRoom(MultiplayerMode.BATTLE_1V1, customRoomCode = roomCode)
        withTimeout(6000L) {
            while (vm.multiplayerRoom.value == null) delay(60L)
        }
        assertEquals(roomCode, vm.multiplayerRoom.value?.roomCode)

        // 2. Segundo jogador (Bob) entra na sala pelo código
        val (bobAuth, bobRepo) = getOrInitSecondaryBobRepo(app)
        val bobUid = signInSecondary(bobAuth, "mp_bob_$tag@test.com")
        val bobJoinRes = withTimeout(6000L) {
            bobRepo.joinRoom(roomCode, "BobBass", Instrument.BASS, Difficulty.HARD)
        }
        assertTrue("Bob must join Alice's room: ${bobJoinRes.exceptionOrNull()?.message}", bobJoinRes.isSuccess)

        // Espera sincronização em tempo real no ViewModel da Alice
        withTimeout(6000L) {
            while (vm.multiplayerParticipants.value.size < 2) delay(60L)
        }
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
 
        // 5. Código inexistente gera erro claro sem quebrar fluxo
        vm.joinMultiplayerRoom("NOPE-9999")
        withTimeout(5000L) {
            while (vm.multiplayerError.value == null) delay(60L)
        }
 
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
        assertNull(vm.multiplayerRoom.value)
        assertTrue(vm.currentScreen.value is GameScreen.MainMenu)

        bobAuth.signOut()
    }
}
