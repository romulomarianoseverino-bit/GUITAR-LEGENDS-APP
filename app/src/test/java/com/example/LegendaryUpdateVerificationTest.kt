package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.guitarlegends.analysis.AudioAnalyzer
import com.example.guitarlegends.data.MusicLibraryRepository
import com.example.guitarlegends.data.PlaylistAddResult
import com.example.guitarlegends.engine.computeHighwayGeometry
import com.example.guitarlegends.model.Difficulty
import com.example.guitarlegends.model.Instrument
import com.example.guitarlegends.model.Playlist
import com.example.guitarlegends.ui.screens.DifficultySelectScreen
import com.example.guitarlegends.ui.screens.LiveStageGameplayScreen
import com.example.guitarlegends.ui.screens.MusicLibraryScreen
import com.example.guitarlegends.ui.screens.SplashScreen
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land")
class LegendaryUpdateVerificationTest {

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
        File(app.filesDir, "library_manifest.json").delete()
        File(app.filesDir, "library_manifest.json.tmp").delete()
    }

    @Test
    fun `01 - HighwayLayoutGeometry aligns strike receptors, lanes and both orientations`() {
        for (lanes in listOf(3, 4, 5)) {
            val landscape = computeHighwayGeometry(canvasWidth = 2400f, canvasHeight = 1080f, numLanes = lanes)
            assertEquals(1200f, landscape.centerX, 0.5f)
            assertTrue("Landscape bottomWidth must stay compact around center", landscape.bottomWidth in 790f..1010f)
            assertTrue("Top width must stay narrower than bottom width", landscape.topWidth < landscape.bottomWidth)
            assertTrue("Receptors must fit inside each lane", landscape.receptorRx * 2f < landscape.laneWidthAtStrike)
            for (lane in 0 until lanes) {
                val pointAtStrike = landscape.pointAt(lane, 1.0f)
                assertEquals(landscape.laneCenterXAtStrike(lane), pointAtStrike.x, 0.5f)
                assertEquals(landscape.strikeY, pointAtStrike.y, 0.5f)
            }

            val portrait = computeHighwayGeometry(canvasWidth = 1080f, canvasHeight = 2340f, numLanes = lanes)
            assertEquals(540f, portrait.centerX, 0.5f)
            assertTrue("Portrait bottomWidth uses wider ratio", portrait.bottomWidth > portrait.canvasWidth * 0.70f)
            assertTrue("Portrait topWidth < bottomWidth", portrait.topWidth < portrait.bottomWidth)
            assertTrue("Portrait strikeY stays comfortably above bottom edge", portrait.highwayBottomY < portrait.canvasHeight)
        }
    }

    @Test
    fun `02 - LiveStageGameplayScreen aligns fret_touch_controls without PISTA labels and exposes pause mixer`() {
        val vm = GameViewModel(app)
        vm.selectDifficulty(Difficulty.MEDIUM) // 4 pistas

        composeTestRule.setContent {
            MyApplicationTheme {
                LiveStageGameplayScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("fret_touch_controls").assertIsDisplayed()
        for (lane in 0..3) {
            composeTestRule.onNodeWithTag("fret_touch_button_$lane").assertExists()
            assertTrue(
                composeTestRule.onAllNodesWithText("PISTA ${lane + 1}", ignoreCase = true)
                    .fetchSemanticsNodes().isEmpty()
            )
        }

        // Abre pausa e valida sliders de volume dentro da partida
        composeTestRule.onNodeWithTag("game_pause_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pause_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pause_volume_master").assertExists()
        composeTestRule.onNodeWithTag("pause_volume_music").assertExists()
        composeTestRule.onNodeWithTag("pause_volume_effects").assertExists()
    }

    @Test
    fun `03 - Nova Tela de Start shows launch context, advances next song directly, and has zero volume controls`() {
        val vm = GameViewModel(app)
        val firstTrack = vm.selectedTrackFlow.value
        val secondTrack = AudioAnalyzer.BUILT_IN_TRACKS[1]

        composeTestRule.setContent {
            MyApplicationTheme {
                SplashScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("start_match_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("start_current_track_title").assertTextEquals(firstTrack.title)
        composeTestRule.onNodeWithTag("start_next_track_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("start_play_now_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("enter_game_button").assertIsDisplayed()

        // Proibição explícita de controles de volume na Tela de Start
        assertTrue(
            "Start screen must have zero volume controls",
            composeTestRule.onAllNodesWithText("volume", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeTestRule.onAllNodesWithTag("pause_volume_master").fetchSemanticsNodes().isEmpty()
        )

        // Avança diretamente para a próxima música sem sair da Start screen
        composeTestRule.onNodeWithTag("start_next_track_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(secondTrack.id, vm.selectedTrackFlow.value.id)
        composeTestRule.onNodeWithTag("start_current_track_title").assertTextEquals(secondTrack.title)
    }

    @Test
    fun `04 - DifficultySelectScreen updates RESUMO DO SHOW when pressing PRÓXIMA MÚSICA`() {
        val vm = GameViewModel(app)
        vm.selectInstrument(Instrument.BASS)
        vm.selectDifficulty(Difficulty.HARD)
        val initialId = vm.selectedTrackFlow.value.id

        composeTestRule.setContent {
            MyApplicationTheme {
                DifficultySelectScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("prematch_next_track_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("prematch_next_track_button").performClick()
        composeTestRule.waitForIdle()

        assertNotEquals(initialId, vm.selectedTrackFlow.value.id)
        composeTestRule.onNodeWithText("Música: ${vm.selectedTrackFlow.value.title}").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `05 - Data layer blocks duplicate trackId in same playlist, allows in different playlists, and persists`(): Unit = runBlocking {
        val repo = MusicLibraryRepository(app)
        val pl1 = Playlist(id = "pl_1", name = "Rock Clássico")
        val pl2 = Playlist(id = "pl_2", name = "Treino Rítmico")
        assertTrue(repo.savePlaylists(listOf(pl1, pl2)).isSuccess)

        val trackId = AudioAnalyzer.BUILT_IN_TRACKS[0].id
        val firstAdd = repo.addTrackToPlaylist("pl_1", trackId)
        assertTrue("First add must succeed", firstAdd is PlaylistAddResult.Added)

        val duplicateAdd = repo.addTrackToPlaylist("pl_1", trackId)
        assertTrue("Second add of same trackId in same playlist must be blocked", duplicateAdd is PlaylistAddResult.DuplicateTrack)

        val differentPlaylistAdd = repo.addTrackToPlaylist("pl_2", trackId)
        assertTrue("Same trackId in another playlist must be allowed", differentPlaylistAdd is PlaylistAddResult.Added)

        // Reinstancia repositório e valida persistência sem duplicatas
        val reloaded = MusicLibraryRepository(app).loadPlaylists()
        assertEquals(listOf(trackId), reloaded.first { it.id == "pl_1" }.trackIds)
        assertEquals(listOf(trackId), reloaded.first { it.id == "pl_2" }.trackIds)

        // Também valida camada GameViewModel + persistência após reinício
        File(app.filesDir, "playlists.json").delete()
        val vm = GameViewModel(app)
        delay(120L)
        val created = vm.createPlaylist("Favoritas")
        assertTrue(vm.addTrackToPlaylist(created.id, trackId))
        assertFalse("ViewModel must block duplicate trackId synchronously", vm.addTrackToPlaylist(created.id, trackId))
        assertNotNull(vm.playlistStatusMessage.value)
        assertTrue(vm.playlistStatusIsError.value)
        delay(120L)

        val restartedVm = GameViewModel(app)
        delay(120L)
        val persistedPlaylist = restartedVm.playlists.value.find { it.id == created.id }
        assertNotNull(persistedPlaylist)
        assertEquals(listOf(trackId), persistedPlaylist!!.trackIds)
    }

    @Test
    fun `06 - MusicLibraryScreen displays playlists as folders, isolates tracks inside folder, and blocks duplicates`(): Unit = runBlocking {
        val vm = GameViewModel(app)
        delay(120L)
        val track0 = AudioAnalyzer.BUILT_IN_TRACKS[0]
        val track1 = AudioAnalyzer.BUILT_IN_TRACKS[1]
        val pl = vm.createPlaylist("Show Sabado")
        assertTrue(vm.addTrackToPlaylist(pl.id, track0.id))

        composeTestRule.setContent {
            MyApplicationTheme {
                MusicLibraryScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        // Vai para aba Playlists e verifica formato de Pasta
        composeTestRule.onNodeWithTag("library_tab_playlists").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlist_folder_${pl.id}").assertIsDisplayed()

        // Na visão de pastas fechadas, não exibe a faixa diretamente solta como item de pasta aberta
        assertTrue(composeTestRule.onAllNodesWithTag("folder_track_${track0.id}").fetchSemanticsNodes().isEmpty())

        // Abre a pasta e confirma isolamento: somente track0 dentro, track1 (do catálogo geral) NÃO aparece na lista da pasta
        composeTestRule.onNodeWithTag("playlist_folder_${pl.id}").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlist_folder_detail_${pl.id}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("folder_track_${track0.id}").assertIsDisplayed()
        assertTrue(composeTestRule.onAllNodesWithTag("folder_track_${track1.id}").fetchSemanticsNodes().isEmpty())

        // Tentar adicionar a mesma faixa dentro da pasta bloqueia duplicata e exibe aviso
        vm.selectTrack(track0)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add_selected_track_to_playlist_${pl.id}").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlist_status_banner").assertIsDisplayed()
        assertEquals(1, vm.playlists.value.first { it.id == pl.id }.trackIds.size)

        // Volta para a grade de pastas
        composeTestRule.onNodeWithTag("playlist_back_to_folders_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlist_folder_${pl.id}").assertIsDisplayed()
    }
}
