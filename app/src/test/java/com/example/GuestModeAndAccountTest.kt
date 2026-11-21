package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.guitarlegends.data.LocalMatchRecord
import com.example.guitarlegends.data.LocalPlayerProgressRepository
import com.example.guitarlegends.data.LocalProgressSnapshot
import com.example.guitarlegends.model.*
import com.example.guitarlegends.viewmodel.AuthUiState
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GuestModeAndAccountTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        File(app.filesDir, "local_player_progress.json").delete()
        File(app.filesDir, "local_player_progress.json.tmp").delete()
    }

    @Test
    fun `01 - app opens without authentication and never auto redirects to Auth`() = runBlocking {
        val vm = GameViewModel(app)
        delay(1600L) // garante que mesmo após tempo de splash não há redirecionamento para Auth

        assertTrue("Auth state should be SignedOut for guest start", vm.authUiState.value is AuthUiState.SignedOut)
        assertTrue("Initial screen must remain Splash waiting for ENTRAR NO JOGO", vm.currentScreen.value is GameScreen.Splash)
        assertFalse("Must never auto-redirect to Auth", vm.currentScreen.value is GameScreen.Auth)
    }

    @Test
    fun `02 - tapping ENTRAR NO JOGO opens MainMenu and navigates menus including Minha Conta`() {
        val vm = GameViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                GuitarLegendsApp(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        // 1. Botão de destaque na tela inicial
        composeTestRule.onNodeWithTag("enter_game_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("ENTRAR NO JOGO").assertIsDisplayed()
        composeTestRule.onNodeWithTag("enter_game_button").performClick()
        composeTestRule.waitForIdle()

        // 2. Direcionado imediatamente para MainMenu sem login
        assertTrue(vm.currentScreen.value is GameScreen.MainMenu)
        composeTestRule.onNodeWithTag("menu_profile").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_account_pill").assertIsDisplayed()

        // 3. Acessar menus no modo visitante (Carreira -> Voltar -> Configurações -> Minha Conta)
        composeTestRule.onNodeWithTag("menu_career").performClick()
        composeTestRule.waitForIdle()
        assertTrue(vm.currentScreen.value is GameScreen.Career)

        vm.navigateBack()
        composeTestRule.waitForIdle()
        assertTrue(vm.currentScreen.value is GameScreen.MainMenu)

        composeTestRule.onNodeWithTag("menu_settings").performClick()
        composeTestRule.waitForIdle()
        assertTrue(vm.currentScreen.value is GameScreen.Settings)
        composeTestRule.onNodeWithTag("settings_my_account_button").assertIsDisplayed()

        // 4. Abrir Minha Conta a partir das Configurações
        composeTestRule.onNodeWithTag("settings_my_account_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(vm.currentScreen.value is GameScreen.Profile)
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("MODO VISITANTE ATIVO").assertIsDisplayed()

        // 5. Voltar nunca redireciona para Auth bloqueante
        vm.navigateBack()
        vm.navigateBack()
        composeTestRule.waitForIdle()
        assertTrue(vm.currentScreen.value is GameScreen.MainMenu)
    }

    @Test
    fun `daily reward weekly cap clamps grants and resets at ISO week boundary`() {
        assertEquals("2026-W48", DailyRewardWeeklyLimit.weekKey("2026-11-23"))
        assertEquals(100, DailyRewardWeeklyLimit.grant(150, 0))
        assertEquals(20, DailyRewardWeeklyLimit.grant(600, 80))
        assertEquals(0, DailyRewardWeeklyLimit.grant(250, 100))
        assertEquals("2026-W49", DailyRewardWeeklyLimit.weekKey("2026-11-30"))
        assertEquals("2026-11-23", DailyRewardWeeklyLimit.weekStartDayKey("2026-11-28"))
        assertEquals("2026-11-23", DailyRewardWeeklyLimit.weekStartDayKey("2026-11-23"))
        assertEquals("2026-11-16", DailyRewardWeeklyLimit.weekStartDayKey("2026-11-22"))
        assertEquals(
            listOf("2026-11-23", "2026-11-24", "2026-11-25", "2026-11-26", "2026-11-27", "2026-11-28"),
            DailyRewardWeeklyLimit.dayKeysInWeekThrough("2026-11-28")
        )
        assertEquals(emptyList<String>(), DailyRewardWeeklyLimit.dayKeysInWeekThrough("data-invalida"))
        assertEquals(100, DailyRewardWeeklyLimit.grant(150, 0))
    }

    @Test
    fun `03 - guest gameplay, daily reward and local customization persist across restart`() {
        val vm = GameViewModel(app)
        vm.enterGame()
        assertTrue(vm.currentScreen.value is GameScreen.MainMenu)

        val initialCoins = vm.playerStats.value.coins
        vm.updateGuestNickname("RockerLocal")
        vm.equipGuitar("void_v")
        vm.setLatencyOffset(25L)

        // Joga e conclui partida no modo visitante
        vm.finishMatch(failed = false)
        assertTrue(vm.currentScreen.value is GameScreen.Results)
        assertTrue("Guest must earn coins after match", vm.playerStats.value.coins > initialCoins)
        assertEquals(1, vm.playerStats.value.songsCompleted)
        assertEquals(1, vm.progressionHistory.value.size)

        // Resgata recompensa diária no modo visitante (e bloqueia replay no mesmo dia UTC)
        val coinsBeforeReward = vm.playerStats.value.coins
        vm.claimDailyReward()
        assertEquals(coinsBeforeReward + 100, vm.playerStats.value.coins)
        assertEquals(1, vm.playerStats.value.rewardStreak)

        val coinsAfterFirstClaim = vm.playerStats.value.coins
        vm.claimDailyReward()
        assertEquals("Same-day guest daily reward replay must be blocked", coinsAfterFirstClaim, vm.playerStats.value.coins)

        // Simula reinício do aplicativo (nova instância de GameViewModel)
        val restartedVm = GameViewModel(app)
        assertEquals("RockerLocal", restartedVm.playerStats.value.nickname)
        assertEquals("void_v", restartedVm.playerStats.value.selectedGuitarId)
        assertEquals(25L, restartedVm.playerStats.value.audioLatencyOffsetMs)
        assertEquals(coinsAfterFirstClaim, restartedVm.playerStats.value.coins)
        assertEquals(1, restartedVm.playerStats.value.songsCompleted)
        assertEquals(1, restartedVm.progressionHistory.value.size)
        assertEquals(1, restartedVm.rewardHistory.value.size)
        assertTrue(restartedVm.hasUnlinkedGuestProgress.value)
    }

    @Test
    fun `04 - safe guest to cloud merge preserves progress without duplicating coins`() {
        val cloudStats = PlayerStats(
            coins = 900,
            totalScore = 25000L,
            notesHitTotal = 300L,
            maxStreakOverall = 45,
            songsCompleted = 4,
            unlockedStars = 15,
            nickname = "LendaNuvem",
            email = "lenda@gmail.com",
            unlockedCosmetics = DEFAULT_UNLOCKED_COSMETICS + listOf("kai_cyber"),
            achievementsUnlocked = listOf("ach_first_show"),
            rewardStreak = 5,
            lastRewardDayKey = "2026-11-16",
            syncRevision = 3L
        )

        val guestStats = PlayerStats(
            coins = 650, // +200 acima dos 450 iniciais ganhos como visitante
            totalScore = 4000L,
            notesHitTotal = 80L,
            maxStreakOverall = 22,
            songsCompleted = 1,
            unlockedStars = 9, // +3 acima das 6 iniciais
            selectedGuitarId = "void_v",
            audioLatencyOffsetMs = 30L,
            unlockedCosmetics = DEFAULT_UNLOCKED_COSMETICS + listOf("neon_thunder"),
            achievementsUnlocked = listOf("ach_first_show", "ach_custom_import")
        )

        val snapshot = LocalProgressSnapshot(
            stats = guestStats,
            hasUnlinkedGuestProgress = true,
            hasLocalCustomizationChanges = true,
            localMatches = listOf(
                LocalMatchRecord(
                    id = "match_guest_1",
                    trackId = "track_thunderstrike",
                    trackTitle = "Thunderstrike",
                    instrument = Instrument.GUITAR.name,
                    difficulty = Difficulty.MEDIUM.name,
                    score = 4000,
                    stars = 3,
                    accuracyPercent = 91.0,
                    coinsEarned = 200,
                    isCompetitive = false
                )
            )
        )

        val merged = LocalPlayerProgressRepository.mergeGuestIntoCloud(cloudStats, snapshot)

        // 900 da nuvem + 200 ganhos como visitante (sem duplicar os 450 iniciais!)
        assertEquals(1100, merged.coins)
        // 15 da nuvem + 3 ganhos como visitante (sem duplicar as 6 iniciais!)
        assertEquals(18, merged.unlockedStars)
        assertEquals(29000L, merged.totalScore)
        assertEquals(5, merged.songsCompleted)
        assertEquals(45, merged.maxStreakOverall)
        assertTrue(merged.unlockedCosmetics.contains("kai_cyber"))
        assertTrue(merged.unlockedCosmetics.contains("neon_thunder"))
        assertTrue(merged.achievementsUnlocked.contains("ach_custom_import"))
        assertEquals("void_v", merged.selectedGuitarId)
        assertEquals(30L, merged.audioLatencyOffsetMs)
        assertTrue(merged.syncRevision > cloudStats.syncRevision)
    }

    @Test
    fun `05 - signOut returns cleanly to Guest Mode without forcing Auth screen`() = runBlocking {
        val vm = GameViewModel(app)
        vm.enterGame()
        vm.navigateTo(GameScreen.Profile)
        assertTrue(vm.currentScreen.value is GameScreen.Profile)

        vm.signOut()
        delay(200L)

        assertTrue("SignOut must stay in SignedOut guest mode", vm.authUiState.value is AuthUiState.SignedOut)
        assertFalse("SignOut must not force GameScreen.Auth", vm.currentScreen.value is GameScreen.Auth)
        assertTrue("User stays in Profile / Minha Conta as Guest", vm.currentScreen.value is GameScreen.Profile)
    }
}
