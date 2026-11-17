package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.guitarlegends.analysis.AudioAnalyzer
import com.example.guitarlegends.data.MultiplayerCloudRepository
import com.example.guitarlegends.model.Difficulty
import com.example.guitarlegends.model.Instrument
import com.example.guitarlegends.model.MultiplayerMode
import com.example.guitarlegends.model.ParticipantConnectionState
import com.example.guitarlegends.model.RoomStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class MultiplayerCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun uniqueSuffix(): String = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()

    @Test
    fun createAndJoinRoom_syncsParticipantsAndRejectsInvalidCodeOrOverflow() = runBlocking {
        val tag = uniqueSuffix()
        val roomCode = "ROCK-$tag"
        val hostUid = signInTestUser("host_$tag@test.com")
        val repo = MultiplayerCloudRepository(firestore, auth)

        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createRoom(
                mode = MultiplayerMode.BATTLE_1V1,
                track = AudioAnalyzer.BUILT_IN_TRACKS[0],
                nickname = "AliceHost",
                instrument = Instrument.GUITAR,
                difficulty = Difficulty.HARD,
                customRoomCode = roomCode
            )
        }
        assertTrue("Host room creation must succeed: ${created.exceptionOrNull()?.message}", created.isSuccess)
        assertEquals(roomCode, created.getOrThrow().roomCode)
        assertEquals(hostUid, created.getOrThrow().hostUid)

        // Duplicate code rejected
        val dup = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createRoom(
                mode = MultiplayerMode.BATTLE_1V1,
                track = AudioAnalyzer.BUILT_IN_TRACKS[0],
                nickname = "AliceHost",
                instrument = Instrument.GUITAR,
                difficulty = Difficulty.MEDIUM,
                customRoomCode = roomCode
            )
        }
        assertTrue("Duplicate room code must fail", dup.isFailure)

        // Second player Bob joins
        val bobUid = signInTestUser("bob_$tag@test.com")
        val missingJoin = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.joinRoom("NONE-9999", "BobRiff", Instrument.BASS, Difficulty.MEDIUM)
        }
        assertTrue("Missing room code must fail gracefully", missingJoin.isFailure)

        val bobJoin = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.joinRoom(roomCode, "BobRiff", Instrument.BASS, Difficulty.EXPERT)
        }
        assertTrue("Second player join must succeed: ${bobJoin.exceptionOrNull()?.message}", bobJoin.isSuccess)
        assertTrue(bobJoin.getOrThrow().memberUids.containsAll(listOf(hostUid, bobUid)))

        // Bob updates loadout, ready state, and score
        val readyRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateParticipantLoadout(roomCode, "BobRiff", Instrument.DRUMS, Difficulty.EXPERT, ready = true)
        }
        assertTrue(readyRes.isSuccess)

        val progRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateParticipantProgress(
                roomCode = roomCode,
                score = 24600,
                combo = 51,
                rockMeter = 0.92f,
                starPowerActive = true,
                connectionState = ParticipantConnectionState.ONLINE
            )
        }
        assertTrue(progRes.isSuccess)

        // Third player Eve rejected because 1v1 maxPlayers == 2
        signInTestUser("eve_$tag@test.com")
        val overflow = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.joinRoom(roomCode, "EveVocal", Instrument.VOCALS, Difficulty.EASY)
        }
        assertTrue("3rd player must be rejected from 1v1 room", overflow.isFailure)

        // Participant list reflects both musicians & Bob's live score
        signInTestUser("host_$tag@test.com")
        val participants = withTimeout(FLOW_TIMEOUT_MS) { repo.observeParticipants(roomCode).first() }
        assertEquals(2, participants.size)
        val bobState = participants.first { it.userId == bobUid }
        assertEquals("BobRiff", bobState.nickname)
        assertEquals(Instrument.DRUMS, bobState.instrumentEnum)
        assertTrue(bobState.ready)
        assertEquals(24600, bobState.score)
        assertTrue(bobState.starPowerActive)
    }

    @Test
    fun securityIsolation_guestCannotSpoofRoomStatusOrAnotherParticipant_andLeaveUpdatesCleanly() = runBlocking {
        val tag = uniqueSuffix()
        val roomCode = "BAND-$tag"
        val hostUid = signInTestUser("band_host_$tag@test.com")
        val repo = MultiplayerCloudRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createRoom(
                mode = MultiplayerMode.BAND_COOP,
                track = AudioAnalyzer.BUILT_IN_TRACKS[1],
                nickname = "HostBand",
                instrument = Instrument.GUITAR,
                difficulty = Difficulty.MEDIUM,
                customRoomCode = roomCode
            ).getOrThrow()
        }

        val bobUid = signInTestUser("band_bob_$tag@test.com")
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.joinRoom(roomCode, "BobDrums", Instrument.DRUMS, Difficulty.HARD).getOrThrow()
        }

        // Bob cannot tamper with host participant doc or force room status to PLAYING
        val tamperParticipantErr = try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("multiplayer_rooms").document(roomCode)
                    .collection("participants").document(hostUid)
                    .update("score", 0)
                    .await()
            }
            null
        } catch (e: Exception) {
            e
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, tamperParticipantErr?.unwrapFirestoreException()?.code)

        val spoofStartRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateRoomHostConfig(
                roomCode = roomCode,
                mode = MultiplayerMode.BAND_COOP,
                track = AudioAnalyzer.BUILT_IN_TRACKS[1],
                status = RoomStatus.PLAYING
            )
        }
        assertTrue("Non-host must be denied starting room", spoofStartRes.isFailure)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, spoofStartRes.exceptionOrNull()?.unwrapFirestoreException()?.code)

        // Bob leaves cleanly -> removed from memberUids & participants
        withTimeout(DEFAULT_TIMEOUT_MS) { repo.leaveOrCloseRoom(roomCode).getOrThrow() }

        signInTestUser("band_host_$tag@test.com")
        val roomAfterBobLeft = withTimeout(FLOW_TIMEOUT_MS) { repo.observeRoom(roomCode).first() }
        assertNotNull(roomAfterBobLeft)
        assertFalse(roomAfterBobLeft!!.memberUids.contains(bobUid))
        assertEquals(listOf(hostUid), roomAfterBobLeft.memberUids)

        // Host leaves -> room switches to CLOSED
        withTimeout(DEFAULT_TIMEOUT_MS) { repo.leaveOrCloseRoom(roomCode).getOrThrow() }
        val closedSnap = withTimeout(FLOW_TIMEOUT_MS) { repo.observeRoom(roomCode).first() }
        assertEquals(RoomStatus.CLOSED, closedSnap?.statusEnum)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
