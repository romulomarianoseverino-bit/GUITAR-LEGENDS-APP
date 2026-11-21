package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.guitarlegends.analysis.AudioAnalyzer
import com.example.guitarlegends.data.AccountCloudRepository
import com.example.guitarlegends.model.ControlMode
import com.example.guitarlegends.model.Difficulty
import com.example.guitarlegends.model.DailyRewardWeeklyLimit
import com.example.guitarlegends.model.Instrument
import com.example.guitarlegends.model.MatchResult
import com.example.guitarlegends.model.PlayerStats
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class AccountCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun uniqueSuffix(): String = UUID.randomUUID().toString().replace("-", "").take(8)

    @Test
    fun claimNicknameAndSyncPlayerStats_persistsAndRestoresAcrossSessions() = runBlocking {
        val tag = uniqueSuffix()
        val aliceEmail = "alice_$tag@test.com"
        val nick = "Rocker_$tag"
        val uid = signInTestUser(aliceEmail)

        val repo = AccountCloudRepository(firestore, auth)
        val availBefore = withTimeout(DEFAULT_TIMEOUT_MS) { repo.isNicknameAvailable(nick) }
        assertTrue("Nickname should be available before claiming", availBefore.getOrNull() == true)

        val initialStats = PlayerStats(
            coins = 1200,
            totalScore = 54000L,
            unlockedStars = 14,
            selectedGuitarId = "neon_thunder",
            controlMode = ControlMode.FRET_AND_STRUM,
            glassAdaptiveEnabled = false
        )

        val claimRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.claimNicknameAndSaveAccount(nick, initialStats)
        }
        assertTrue("Claiming nickname & creating account must succeed: ${claimRes.exceptionOrNull()?.message}", claimRes.isSuccess)
        val created = claimRes.getOrThrow()
        assertEquals(uid, created.userId)
        assertEquals(nick, created.nickname)
        assertEquals("New cloud accounts must not import client-controlled coins", 0, created.coins)
        assertEquals("New cloud accounts must not import client-controlled score", 0L, created.totalScore)
        assertEquals("Unowned cosmetics must not become unlocked during registration", "flame_caster", created.selectedGuitarId)

        // Atualiza e sincroniza sem destruir progresso
        val updatedStats = created.toPlayerStats().copy(coins = 1550, songsCompleted = 3)
        val syncRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.syncPlayerStats(updatedStats)
        }
        assertTrue("Cloud sync must succeed: ${syncRes.exceptionOrNull()?.message}", syncRes.isSuccess)
        assertEquals(1550, syncRes.getOrThrow().coins)
        assertTrue("Sync revision must advance", syncRes.getOrThrow().syncRevision > created.syncRevision)

        // Simula login em novo dispositivo com a mesma conta e restauração dos dados
        auth.signOut()
        signInTestUser(aliceEmail)
        val restored = withTimeout(DEFAULT_TIMEOUT_MS) { repo.fetchAccount().getOrNull() }
        assertNotNull("Account must be restored from Firestore", restored)
        assertEquals(nick, restored!!.nickname)
        assertEquals(1550, restored.coins)
        assertEquals(ControlMode.FRET_AND_STRUM.name, restored.controlMode)
    }

    @Test
    fun duplicateNickname_rejectedAcrossDifferentUsers() = runBlocking {
        val tag = uniqueSuffix()
        val sharedNick = "Legend_$tag"

        signInTestUser("alice_$tag@test.com")
        val aliceRepo = AccountCloudRepository(firestore, auth)
        val aliceClaim = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.claimNicknameAndSaveAccount(sharedNick, PlayerStats())
        }
        assertTrue("First user claim must succeed", aliceClaim.isSuccess)

        // Segundo jogador tenta usar exatamente o mesmo nickname (case-insensitive)
        signInTestUser("bob_$tag@test.com")
        val bobRepo = AccountCloudRepository(firestore, auth)
        val bobCheck = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.isNicknameAvailable(sharedNick.uppercase())
        }
        assertEquals(false, bobCheck.getOrNull())

        val bobClaim = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.claimNicknameAndSaveAccount(sharedNick.uppercase(), PlayerStats())
        }
        assertTrue("Duplicate nickname reservation must fail", bobClaim.isFailure)
    }

    @Test
    fun crossUserReadWrite_usersDocument_failsWithPermissionDenied() = runBlocking {
        val tag = uniqueSuffix()
        val aliceUid = signInTestUser("alice_iso_$tag@test.com")
        val aliceRepo = AccountCloudRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.claimNicknameAndSaveAccount("Alice_$tag", PlayerStats()).getOrThrow()
        }

        // Bob tenta ler ou alterar diretamente o documento users/{aliceUid}
        signInTestUser("bob_iso_$tag@test.com")
        val readException = try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).get().await()
            }
            null
        } catch (e: Exception) {
            e
        }
        assertNotNull("Cross-user read must throw", readException)
        val fsEx = readException?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)

        val writeException = try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).update("coins", 999999).await()
            }
            null
        } catch (e: Exception) {
            e
        }
        assertNotNull("Cross-user write must throw", writeException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, writeException?.unwrapFirestoreException()?.code)
    }

    @Test
    fun observeUserAccount_unauthenticatedCaller_failsAccess() = runBlocking {
        auth.signOut()
        val repo = AccountCloudRepository(firestore, auth)

        val exception = try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeUserAccount().first()
            }
            null
        } catch (e: Exception) {
            e
        }
        assertNotNull("Unauthenticated observer must fail", exception)
    }

    @Test
    fun dailyReward_validUtcClaimSucceedsOnce_tamperedDateAndReplayRejected() = runBlocking {
        val tag = uniqueSuffix()
        signInTestUser("daily_$tag@test.com")
        val repo = AccountCloudRepository(firestore, auth)
        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.claimNicknameAndSaveAccount("Daily_$tag", PlayerStats(coins = 100)).getOrThrow()
        }

        // 1. Tentativa de burlar data com dia futuro deve ser barrada pelas Firestore Rules
        val spoofRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.claimDailyReward(created.toPlayerStats(), dayKeyOverride = "2099-01-01")
        }
        assertTrue("Spoofed UTC date must be rejected by security rules", spoofRes.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            spoofRes.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        // 2. Resgate legítimo na data UTC atual deve funcionar
        val claimRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.claimDailyReward(created.toPlayerStats())
        }
        assertTrue("Legitimate UTC daily reward claim must succeed: ${claimRes.exceptionOrNull()?.message}", claimRes.isSuccess)
        val (claim, updatedUser) = claimRes.getOrThrow()
        assertEquals(1, claim.dayNumber)
        assertEquals(100, claim.coinsGranted) // Dia 1 solicita 150, mas o teto semanal é 100
        assertEquals(100, updatedUser.coins) // Conta nova começa com 0 moedas no fluxo seguro
        assertEquals(100, updatedUser.dailyRewardCoinsClaimed)
        assertEquals(DailyRewardWeeklyLimit.weekKey(repo.utcTodayKey()), updatedUser.dailyRewardWeekKey)

        // 3. Segundo resgate no mesmo dia UTC deve ser bloqueado
        val replayRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.claimDailyReward(updatedUser.toPlayerStats())
        }
        assertTrue("Duplicate daily reward claim on same UTC day must be blocked", replayRes.isFailure)

        // 4. Histórico de recompensas persiste corretamente
        val historyRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.fetchRewardHistory() }
        assertTrue(historyRes.isSuccess)
        assertEquals(1, historyRes.getOrThrow().size)
    }

    @Test
    fun progressionHistory_createAndQuery_succeedsForOwner() = runBlocking {
        val tag = uniqueSuffix()
        signInTestUser("prog_$tag@test.com")
        val repo = AccountCloudRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.claimNicknameAndSaveAccount("Prog_$tag", PlayerStats()).getOrThrow()
        }

        val match = MatchResult(
            track = AudioAnalyzer.BUILT_IN_TRACKS[0],
            instrument = Instrument.GUITAR,
            difficulty = Difficulty.EXPERT,
            score = 88400,
            maxCombo = 42,
            totalNotes = 100,
            hitNotes = 98,
            missedNotes = 2,
            accuracyPercent = 98f,
            stars = 5,
            coinsEarned = 410,
            isAdaptiveUsed = true,
            isCompetitive = true
        )
        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.recordProgressionMatch(match) }
        assertTrue("Recording progression entry must succeed: ${saveRes.exceptionOrNull()?.message}", saveRes.isSuccess)

        val listRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.fetchProgressionHistory() }
        assertTrue("Listing progression history must succeed: ${listRes.exceptionOrNull()?.message}", listRes.isSuccess)
        val entries = listRes.getOrThrow()
        assertEquals(1, entries.size)
        assertEquals(88400, entries[0].score)
        assertTrue(entries[0].isCompetitive)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
