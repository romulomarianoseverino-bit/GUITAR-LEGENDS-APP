package com.example

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.guitarlegends.data.HighwayImageManager
import com.example.guitarlegends.data.LocalPlayerProgressRepository
import com.example.guitarlegends.data.LocalProgressSnapshot
import com.example.guitarlegends.model.*
import com.example.guitarlegends.ui.screens.CustomizationScreen
import com.example.guitarlegends.ui.screens.LiveStageGameplayScreen
import com.example.guitarlegends.ui.screens.ProfileScreen
import com.example.guitarlegends.ui.screens.SettingsScreen
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudioAndHardwareTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        File(app.filesDir, "local_player_progress.json").delete()
        File(app.filesDir, "local_player_progress.json.tmp").delete()
        File(app.filesDir, "highway_images").deleteRecursively()
    }

    @Test
    fun `01 - studio customizations, 3D instrument config and presets save apply delete across restart`() {
        val vm = GameViewModel(app)

        vm.updateRockerBodyAndSkin("Neo Cyberpunk", 0xFFC98E66L)
        vm.updateRockerStudioColors(
            hairColorHex = 0xFF00E5FFL,
            topColorHex = 0xFFFF1744L,
            pantsColorHex = 0xFF1F1B26L,
            shoesColorHex = 0xFFE0E0E0L,
            accessoryColorHex = 0xFFFFD700L
        )
        vm.updateRockerExpression(StagePoseExpression.SHOUTING_ROCK)
        vm.updateInstrumentCustomConfig(
            Instrument.GUITAR,
            InstrumentCustomConfig(
                modelStyleId = "flying_v",
                primaryColorHex = 0xFF7C4DFFL,
                secondaryColorHex = 0xFF00E5FFL,
                hardwareColorHex = 0xFFFFD700L,
                finish = InstrumentFinish.NEON_GLOW,
                stampId = "inst_stamp_lightning",
                stickerId = "sticker_skull",
                fullSkinId = "skin_none"
            )
        )
        vm.updateHighwayStampOpacity(0.75f)
        vm.saveAppearancePreset("Show Neo")

        assertEquals(1, vm.playerStats.value.studioProfile.savedPresets.size)
        val savedPreset = vm.playerStats.value.studioProfile.savedPresets.first()
        assertEquals("Show Neo", savedPreset.name)

        // Muda aparência e restaura via preset
        vm.updateRockerBodyAndSkin("Punk Esguio", 0xFFF6D5B8L)
        vm.applyAppearancePreset(savedPreset.id)
        assertEquals("Neo Cyberpunk", vm.playerStats.value.studioProfile.rockerBodyModel)
        assertEquals(StagePoseExpression.SHOUTING_ROCK, vm.playerStats.value.studioProfile.stageExpression)
        assertEquals("flying_v", vm.playerStats.value.studioProfile.guitarConfig.modelStyleId)

        // Simula reinício
        val restarted = GameViewModel(app)
        assertEquals(1, restarted.playerStats.value.studioProfile.savedPresets.size)
        assertEquals(InstrumentFinish.NEON_GLOW, restarted.playerStats.value.studioProfile.guitarConfig.finish)
        assertEquals(0.75f, restarted.playerStats.value.studioProfile.highwayConfig.stampOpacity, 0.01f)

        restarted.deleteAppearancePreset(savedPreset.id)
        assertTrue(restarted.playerStats.value.studioProfile.savedPresets.isEmpty())
    }

    @Test
    fun `02 - new players start with zero coins and reward claims stay capped`() {
        val vm = GameViewModel(app)
        val initialCoins = vm.playerStats.value.coins

        assertEquals("Conta nova deve começar sem moedas", 0, initialCoins)

        val expensiveSkin = StudioCatalog.shopCosmetics.first { it.id == "kai_cyber" }
        assertFalse("Item sem saldo deve ser bloqueado", vm.unlockAndEquipCosmetic(expensiveSkin))
        assertEquals(initialCoins, vm.playerStats.value.coins)

        val affordableHair = StudioCatalog.shopCosmetics.first { it.id == "hair_moicano_turquesa" }
        assertFalse("Jogador sem moedas não deve comprar item de 150 LC", vm.unlockAndEquipCosmetic(affordableHair))

        // Progresso local de visitante não é aceito como progresso verificado para resgatar moedas.
        repeat(3) { vm.finishMatch(failed = false) }
        val showChallenge = StudioCatalog.challenges.first { it.id == "chl_first_3_shows" }
        val coinsBeforeClaim = vm.playerStats.value.coins
        assertEquals(0, showChallenge.progressProvider(vm.playerStats.value))
        vm.claimChallengeReward(showChallenge)
        assertEquals(coinsBeforeClaim, vm.playerStats.value.coins)

        // O visitante não pode resgatar desafios sem validação da conta no servidor.
        assertEquals(0, PlayerStats().verifiedSongsCompleted)

        assertTrue("Nenhuma recompensa de desafio pode exceder 100 moedas",
            StudioCatalog.challenges.all { it.rewardCoins in 0..100 })
        assertTrue("Nenhuma recompensa diária pode exceder 100 moedas",
            DailyRewardCatalog.cycleRewards.all { it.coinsGranted in 0..100 })
        assertEquals("Modelo de jogador novo deve iniciar zerado", 0, PlayerStats().coins)
        assertEquals("Modelo de conta novo deve iniciar zerado", 0, UserAccountCloud().coins)

        val mediumPerfect = MatchRewardCalculator.calculate(100f, Difficulty.MEDIUM, 240_000L)
        val expertPerfect = MatchRewardCalculator.calculate(100f, Difficulty.EXPERT, 240_000L)
        val mediumLowerAccuracy = MatchRewardCalculator.calculate(60f, Difficulty.MEDIUM, 240_000L)
        val maximumSongReward = MatchRewardCalculator.calculate(100f, Difficulty.EXPERT, 360_000L)
        assertTrue("Uma música completa deve respeitar o teto de 50 moedas", mediumPerfect in 1..50)
        assertEquals("Uma faixa longa no Especialista pode atingir o teto exato de 50 moedas", 50, maximumSongReward)
        assertTrue("Maior dificuldade deve pagar mais que a média", expertPerfect > mediumPerfect)
        assertTrue("Precisão menor deve reduzir a premiação", mediumLowerAccuracy < mediumPerfect)
        assertEquals("Partida abandonada/falha não deve gerar moedas", 0,
            MatchRewardCalculator.calculate(100f, Difficulty.EXPERT, 240_000L, failed = true))

        // Moedas locais/visitantes não são confiáveis e nunca são creditadas na conta cloud,
        // mesmo em snapshots legados com baseline histórico.
        val existingCloud = PlayerStats(coins = 200)
        val legacyGuest = LocalProgressSnapshot(
            stats = PlayerStats(coins = 550),
            hasUnlinkedGuestProgress = true,
            startingCoinsBaseline = 450
        )
        assertEquals(
            "Saldo cloud deve permanecer intacto ao vincular progresso visitante legado",
            200,
            LocalPlayerProgressRepository.mergeGuestIntoCloud(existingCloud, legacyGuest).coins
        )

        val newGuest = LocalProgressSnapshot(
            stats = PlayerStats(coins = 100),
            hasUnlinkedGuestProgress = true,
            startingCoinsBaseline = 0
        )
        assertEquals(
            "Saldo cloud deve permanecer intacto ao vincular progresso visitante novo",
            200,
            LocalPlayerProgressRepository.mergeGuestIntoCloud(existingCloud, newGuest).coins
        )
    }

    @Test
    fun `03 - HighwayImageManager validates real PNG, rejects spoofed text and tiny images`() = runBlocking {
        val manager = HighwayImageManager(app)

        // 1. Rejeita arquivo de texto renomeado
        val fakeFile = File(app.cacheDir, "fake_highway.png").apply {
            writeText("NOT_A_REAL_PNG_FILE_CONTENT_LONGER_THAN_64_BYTES_PADDING_PADDING_PADDING")
        }
        val fakeRes = manager.importAndValidateHighwayImage(Uri.fromFile(fakeFile))
        assertTrue("Arquivo falso deve falhar na validação de assinatura", fakeRes.isFailure)

        // 2. Rejeita imagem menor que 128x128
        val tinyFile = File(app.cacheDir, "tiny.png")
        val tinyBmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        FileOutputStream(tinyFile).use { tinyBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        tinyBmp.recycle()
        val tinyRes = manager.importAndValidateHighwayImage(Uri.fromFile(tinyFile))
        assertTrue("Imagem minúscula deve ser rejeitada", tinyRes.isFailure)

        // 3. Aceita PNG válido 256x256 e salva em filesDir/highway_images/
        val validFile = File(app.cacheDir, "valid_stage.png")
        val validBmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        FileOutputStream(validFile).use { validBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        validBmp.recycle()

        val okRes = manager.importAndValidateHighwayImage(Uri.fromFile(validFile))
        assertTrue("PNG válido deve ser aceito: ${okRes.exceptionOrNull()?.message}", okRes.isSuccess)
        val savedPath = okRes.getOrThrow()
        assertTrue(savedPath.contains("highway_images"))
        assertTrue(File(savedPath).exists())

        manager.deleteCustomImage(savedPath)
        assertFalse(File(savedPath).exists())
    }

    @Test
    fun `04 - DualShock 4 gamepad handles simultaneous chords, pause release and remapping persistence`() {
        val vm = GameViewModel(app)
        vm.enterGame()
        vm.navigateTo(GameScreen.Gameplay)

        fun gamepadKey(action: Int, keyCode: Int): KeyEvent {
            return KeyEvent(0L, 0L, action, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_GAMEPAD)
        }

        // Acorde simultâneo PS4: L2 (Pista 0) + R1 (Pista 2)
        assertTrue(vm.onHardwareKeyEvent(gamepadKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_L2)))
        assertTrue(vm.onHardwareKeyEvent(gamepadKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_R1)))
        assertEquals(setOf(0, 2), vm.activeFretLanes.value)

        // Pause via OPTIONS / START libera pistas pressionadas
        assertTrue(vm.onHardwareKeyEvent(gamepadKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START)))
        assertTrue(vm.isPaused.value)
        assertTrue("Ao pausar as pistas seguradas devem ser liberadas", vm.activeFretLanes.value.isEmpty())

        // Despausa com START
        assertTrue(vm.onHardwareKeyEvent(gamepadKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START)))
        assertFalse(vm.isPaused.value)

        // Remapeia pista 0 para Quadrado (BUTTON_X) e valida persistência após reinício
        vm.assignControllerLaneKey(0, KeyEvent.KEYCODE_BUTTON_X)
        val restarted = GameViewModel(app)
        assertEquals(KeyEvent.KEYCODE_BUTTON_X, restarted.playerStats.value.studioProfile.controllerSettings.laneKeyCodes[0])

        // Volta ao preset PS4 padrão
        restarted.applyDualShock4DefaultPreset()
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, restarted.playerStats.value.studioProfile.controllerSettings.laneKeyCodes[0])
    }

    @Test
    fun `05 - audio mixer updates gains, mute state, reset defaults and persists across restart`() {
        val vm = GameViewModel(app)

        vm.updateAudioSettings(
            AudioSettings(
                masterVolume = 0.6f,
                musicVolume = 0.5f,
                gameplaySfxVolume = 0.8f,
                uiSfxVolume = 0.4f,
                stageFxVolume = 0.25f,
                isMuted = false
            )
        )
        assertEquals(0.30f, vm.audioPlayer.getCurrentMusicGain(), 0.01f)
        assertEquals(0.48f, vm.soundFx.currentSettings.effectiveGameplaySfx(), 0.01f)

        vm.toggleMuteAudio()
        assertTrue(vm.playerStats.value.studioProfile.audioSettings.isMuted)
        assertEquals(0f, vm.audioPlayer.getCurrentMusicGain(), 0.001f)

        val restarted = GameViewModel(app)
        assertTrue(restarted.playerStats.value.studioProfile.audioSettings.isMuted)
        assertEquals(0.6f, restarted.playerStats.value.studioProfile.audioSettings.masterVolume, 0.01f)

        restarted.resetAudioSettings()
        assertFalse(restarted.playerStats.value.studioProfile.audioSettings.isMuted)
        assertEquals(1.0f, restarted.playerStats.value.studioProfile.audioSettings.masterVolume, 0.01f)
    }

    @Test
    fun `06 - LiveStageGameplayScreen displays clean fret pads without PISTA labels`() {
        val vm = GameViewModel(app)
        vm.selectDifficulty(Difficulty.EXPERT) // 5 pistas visíveis
        vm.selectInstrument(Instrument.VOCALS)

        composeTestRule.setContent {
            MyApplicationTheme {
                LiveStageGameplayScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("amp_score_cabinet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("analog_rock_meter").assertIsDisplayed()
        composeTestRule.onNodeWithTag("vocal_pitch_bar").assertIsDisplayed()

        // Os botões de toque existem sem rótulos de texto "PISTA 1..5"
        for (lane in 0..4) {
            composeTestRule.onNodeWithTag("fret_touch_button_$lane").assertExists()
            assertTrue(
                "Não deve exibir rótulo textual PISTA ${lane + 1}",
                composeTestRule.onAllNodesWithText("PISTA ${lane + 1}", ignoreCase = true)
                    .fetchSemanticsNodes().isEmpty()
            )
        }

        vm.togglePause()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pause_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pause_volume_master").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pause_volume_music").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pause_volume_effects").assertIsDisplayed()
    }

    @Test
    fun `07 - CustomizationScreen 3D viewers and SettingsScreen mixer and PS4 controls render cleanly`() {
        val vm = GameViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                CustomizationScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("character_3d_viewer").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("color_wheel_button").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("color_wheel_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("FECHAR").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("custom_tab_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("instrument_3d_viewer").assertIsDisplayed()

        composeTestRule.onNodeWithTag("custom_tab_2").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("upload_highway_image_button").assertExists()

        composeTestRule.onNodeWithTag("custom_tab_3").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("custom_tab_4").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("save_preset_button").assertExists()
    }

    @Test
    fun `08 - SettingsScreen renders audio mixer and DualShock remapper and Auth cancel recovers safely`() {
        val vm = GameViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                SettingsScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("audio_mute_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("audio_preview_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("controller_status_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("controller_remap_lane_0").assertIsDisplayed()
    }

    @Test
    fun `09 - cancelPendingAuth cleanly exits loading back to SignedOut guest mode`() {
        val vm = GameViewModel(app)
        vm.cancelPendingAuth()

        composeTestRule.setContent {
            MyApplicationTheme {
                ProfileScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MODO VISITANTE ATIVO").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }
}
