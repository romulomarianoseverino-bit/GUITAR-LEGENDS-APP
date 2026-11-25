package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.guitarlegends.analysis.AudioAnalyzer
import com.example.guitarlegends.data.AvatarPhotoAnalyzer
import com.example.guitarlegends.data.MusicLibraryRepository
import com.example.guitarlegends.model.*
import com.example.guitarlegends.ui.screens.CustomizationScreen
import com.example.guitarlegends.ui.screens.InstrumentSelectScreen
import com.example.guitarlegends.ui.screens.MusicLibraryScreen
import com.example.guitarlegends.ui.screens.ResultsScreen
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
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
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land")
class ReconstructedFeaturesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        File(app.filesDir, "local_player_progress.json").delete()
        File(app.filesDir, "local_player_progress.json.tmp").delete()
        File(app.filesDir, "playlists.json").delete()
        File(app.filesDir, "playlists.json.tmp").delete()
        File(app.filesDir, "music_library_metadata.json").delete()
        File(app.filesDir, "avatar_photos").deleteRecursively()
    }

    @Test
    fun `01 - acoustic guitar chart generation, studio codec roundtrip and cosmetic equip`() {
        val builtIn = AudioAnalyzer.BUILT_IN_TRACKS[0]
        val mediumChart = AudioAnalyzer.generateChart(builtIn, Instrument.ACOUSTIC_GUITAR, Difficulty.MEDIUM)
        assertTrue("Acoustic guitar MEDIUM chart must generate notes", mediumChart.isNotEmpty())
        assertTrue("Acoustic notes must stay within MEDIUM 4 lanes", mediumChart.all { it.lane in 0 until Difficulty.MEDIUM.lanesUsed })

        val hardChart = AudioAnalyzer.generateChart(builtIn, Instrument.ACOUSTIC_GUITAR, Difficulty.HARD)
        assertTrue("Acoustic guitar HARD chart must generate notes", hardChart.isNotEmpty())
        assertTrue("Acoustic notes must stay within HARD 5 lanes", hardChart.all { it.lane in 0 until Difficulty.HARD.lanesUsed })

        val studio = StudioProfileConfig(
            selectedAcousticId = "violao_aco_folk",
            acousticConfig = InstrumentCustomConfig(
                modelStyleId = "violao_aco_folk",
                primaryColorHex = 0xFF8D6E63,
                secondaryColorHex = 0xFF3E2723,
                hardwareColorHex = 0xFFFFD54F,
                finish = InstrumentFinish.GLOSS
            )
        )
        val encoded = StudioProfileJsonCodec.toJsonString(studio)
        val decoded = StudioProfileJsonCodec.fromJsonString(encoded)
        assertEquals("violao_aco_folk", decoded.selectedAcousticId)
        assertEquals(0xFF8D6E63, decoded.acousticConfig.primaryColorHex)

        val statsWithCutaway = StudioCatalog.applyCosmeticLook(PlayerStats(), "violao_eletro_cutaway")
        assertEquals("violao_eletro_cutaway", statsWithCutaway.studioProfile.selectedAcousticId)
        assertEquals("violao_eletro_cutaway", statsWithCutaway.studioProfile.acousticConfig.modelStyleId)
    }

    @Test
    fun `02 - custom playlists CRUD, disk persistence, cascade delete and queue playback`() = runBlocking {
        val repo = MusicLibraryRepository(app)
        val importedAudio = File(app.filesDir, "temp_song.wav").apply {
            writeBytes(ByteArray(128))
        }
        val customTrack = Track(
            id = "custom_playlist_track_1",
            title = "Acústico Teste",
            artist = "Usuário",
            bpm = 115.0,
            durationMs = 90_000L,
            genre = "Acústico",
            isBuiltIn = false,
            uriString = Uri.fromFile(importedAudio).toString()
        )

        val savedPlaylists = listOf(
            Playlist(
                id = "pl_setlist",
                name = "Setlist Acústico",
                trackIds = listOf("track_thunderstrike", customTrack.id)
            )
        )
        repo.savePlaylists(savedPlaylists)

        val reloaded = MusicLibraryRepository(app).loadPlaylists()
        assertEquals(1, reloaded.size)
        assertEquals(listOf("track_thunderstrike", customTrack.id), reloaded.first().trackIds)

        // Cascade delete removes customTrack.id from saved playlists
        repo.deleteTrack(customTrack)
        val afterCascade = repo.loadPlaylists()
        assertEquals(listOf("track_thunderstrike"), afterCascade.first().trackIds)

        // ViewModel queue progression & Results next button gating
        val vm = GameViewModel(app)
        delay(120L)
        val pl = vm.createPlaylist("Show Arena")
        vm.addTrackToPlaylist(pl.id, AudioAnalyzer.BUILT_IN_TRACKS[0].id)
        vm.addTrackToPlaylist(pl.id, AudioAnalyzer.BUILT_IN_TRACKS[1].id)
        assertTrue(vm.startPlaylistPlayback(pl.id, shuffle = false))
        assertTrue(vm.hasNextInPlaylist())
        assertEquals(AudioAnalyzer.BUILT_IN_TRACKS[0].id, vm.selectedTrack.id)
        assertTrue(vm.playNextInPlaylist())
        assertEquals(AudioAnalyzer.BUILT_IN_TRACKS[1].id, vm.selectedTrack.id)
        assertFalse(vm.hasNextInPlaylist())
    }

    @Test
    fun `03 - photo avatar analyzer caches thumbnail and extracts palette without broad storage permission`() {
        val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        // Top region bright cyan hair
        canvas.drawColor(Color.rgb(0, 229, 255))
        // Center face region warm skin tone
        val skinPaint = android.graphics.Paint().apply {
            color = Color.rgb(228, 178, 146)
        }
        canvas.drawRect(24f, 36f, 96f, 96f, skinPaint)

        val photoFile = File(app.filesDir, "synthetic_portrait.jpg")
        FileOutputStream(photoFile).use { fos ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }
        bmp.recycle()

        val result = AvatarPhotoAnalyzer.analyzeAndCachePhoto(app, Uri.fromFile(photoFile).toString())
        assertTrue(result.extractedFromPixels)
        assertTrue("Cached thumbnail should exist", File(result.savedPathOrUri).exists())
        assertTrue("Skin tone must come from StudioCatalog", StudioCatalog.skinTones.contains(result.skinToneHex))
        assertTrue("Hair color must come from StudioCatalog", StudioCatalog.paletteColors.contains(result.hairColorHex))

        // Unreadable URI safely falls back without throwing
        val fallback = AvatarPhotoAnalyzer.analyzeAndCachePhoto(app, "content://invalid/missing/avatar.jpg")
        assertFalse(fallback.extractedFromPixels)
        assertEquals("content://invalid/missing/avatar.jpg", fallback.savedPathOrUri)
    }

    @Test
    fun `04 - economy parity matches rewardLogic caps across match, daily rewards and challenges`() {
        assertEquals(0, MatchRewardCalculator.calculateMatchCoins(100f, Difficulty.EXPERT, 120_000L, failed = true))
        assertEquals(27, MatchRewardCalculator.calculateMatchCoins(100f, Difficulty.EXPERT, 180_000L, failed = false))
        assertEquals(50, MatchRewardCalculator.calculateMatchCoins(100f, Difficulty.EXPERT, 600_000L, failed = false))
        assertTrue(MatchRewardCalculator.calculateMatchCoins(80f, Difficulty.EASY, 60_000L, failed = false) in 1..50)
        assertEquals(100, DailyRewardCatalog.WEEKLY_REWARD_CAP)
        assertTrue(DailyRewardCatalog.cycleRewards.all { it.coinsGranted == 100 })
        assertTrue(StudioCatalog.challenges.all { it.rewardCoins == 100 })
    }

    @Test
    fun `05 - UI exposes acoustic guitar, 3D styles, playlists tab and next playlist button`() {
        val vm = GameViewModel(app)
        vm.enterGame()

        // 1. InstrumentSelectScreen mostra Violão Acústico
        composeTestRule.setContent {
            val screen by vm.currentScreen.collectAsState()
            MyApplicationTheme {
                when (screen) {
                    is GameScreen.InstrumentSelect -> InstrumentSelectScreen(vm)
                    is GameScreen.Customization -> CustomizationScreen(vm)
                    is GameScreen.MusicLibrary -> MusicLibraryScreen(vm)
                    is GameScreen.Results -> ResultsScreen(vm)
                    else -> InstrumentSelectScreen(vm)
                }
            }
        }
        vm.navigateTo(GameScreen.InstrumentSelect)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("instrument_acoustic_guitar").assertIsDisplayed()

        // 2. CustomizationScreen exibe aba Violão e estilos 3D
        vm.navigateTo(GameScreen.Customization)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("studio_lazy_column")
            .performScrollToNode(hasTestTag("art_style_anime"))
        composeTestRule.onNodeWithTag("art_style_anime").assertIsDisplayed()
        composeTestRule.onNodeWithTag("custom_tab_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("studio_lazy_column")
            .performScrollToNode(hasTestTag("inst_studio_tab_ACOUSTIC_GUITAR"))
        composeTestRule.onNodeWithTag("inst_studio_tab_ACOUSTIC_GUITAR").assertIsDisplayed()

        // 3. MusicLibraryScreen aba Playlists e ResultsScreen botão Próxima da Playlist
        vm.navigateTo(GameScreen.MusicLibrary)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_tab_playlists").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("create_playlist_button").assertIsDisplayed()

        val pl = vm.createPlaylist("Rock Arena")
        vm.addTrackToPlaylist(pl.id, AudioAnalyzer.BUILT_IN_TRACKS[0].id)
        vm.addTrackToPlaylist(pl.id, AudioAnalyzer.BUILT_IN_TRACKS[1].id)
        vm.startPlaylistPlayback(pl.id, shuffle = false)
        vm.finishMatch(failed = false)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("results_next_playlist_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("PRÓXIMA DA PLAYLIST ▶").assertIsDisplayed()
    }
}
