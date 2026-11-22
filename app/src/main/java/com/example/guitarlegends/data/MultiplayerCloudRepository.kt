package com.example.guitarlegends.data

import com.example.guitarlegends.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class MultiplayerCloudRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val roomCodeRegex = Regex("^[A-Z0-9_-]{4,24}$")

    fun currentUidOrNull(): String? = auth.currentUser?.uid

    fun normalizeRoomCode(raw: String): String {
        return raw.trim()
            .uppercase()
            .replace(Regex("\\s+"), "-")
            .filter { it in 'A'..'Z' || it in '0'..'9' || it == '-' || it == '_' }
            .take(24)
    }

    fun generateRoomCode(): String {
        val number = Random.nextInt(1000, 9999)
        val tag = listOf("ROCK", "AXEL", "AMP", "RIFF", "SOLO").random()
        return "$tag-$number"
    }

    private fun safeNickname(raw: String, uid: String): String {
        val clean = raw.trim().take(24)
        return if (clean.length >= 2) clean else "Rocker_${uid.take(4)}"
    }

    suspend fun createRoom(
        mode: MultiplayerMode,
        track: Track,
        nickname: String,
        instrument: Instrument,
        difficulty: Difficulty,
        customRoomCode: String? = null
    ): Result<MultiplayerRoomCloud> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Entre com sua conta Google antes de criar uma sala online."))
        val uid = user.uid
        val hostNick = safeNickname(nickname, uid)
        val roomCode = customRoomCode?.let(::normalizeRoomCode)?.takeIf { roomCodeRegex.matches(it) }
            ?: generateRoomCode()

        val roomPath = "multiplayer_rooms/$roomCode"
        return try {
            val roomRef = firestore.collection("multiplayer_rooms").document(roomCode)
            val createdRoom = firestore.runTransaction { tx ->
                val snap = tx.get(roomRef)
                if (snap.exists()) {
                    throw IllegalStateException("Código de sala '$roomCode' já está em uso. Tente gerar outro.")
                }
                val room = MultiplayerRoomCloud(
                    id = roomCode,
                    roomCode = roomCode,
                    hostUid = uid,
                    hostNickname = hostNick,
                    mode = mode.name,
                    status = RoomStatus.WAITING.name,
                    trackId = track.id.ifBlank { "track_thunderstrike" }.take(128),
                    trackTitle = track.title.ifBlank { "Música de Palco" }.take(160),
                    maxPlayers = mode.maxPlayers,
                    memberUids = listOf(uid),
                    winnerUid = "",
                    inviteMessage = "Entre na sala $roomCode no Guitar Legends!"
                )
                tx.set(roomRef, room.toCreateMap())
                room
            }.await()

            val participant = MultiplayerParticipantCloud(
                id = uid,
                userId = uid,
                roomCode = roomCode,
                nickname = hostNick,
                instrument = instrument.name,
                difficulty = difficulty.name,
                ready = false,
                connectionState = ParticipantConnectionState.ONLINE.name,
                score = 0,
                combo = 0,
                rockMeter = 0.6,
                starPowerActive = false,
                lastHeartbeatMs = System.currentTimeMillis()
            )
            roomRef.collection("participants").document(uid)
                .set(participant.toCreateMap())
                .await()

            Result.success(createdRoom)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(runCatching { handleFirestoreError(e, OperationType.WRITE, roomPath, auth) }.exceptionOrNull() ?: e)
        }
    }

    suspend fun joinRoom(
        rawCode: String,
        nickname: String,
        instrument: Instrument,
        difficulty: Difficulty
    ): Result<MultiplayerRoomCloud> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Entre com sua conta Google para entrar em uma sala online."))
        val uid = user.uid
        val roomCode = normalizeRoomCode(rawCode)
        if (!roomCodeRegex.matches(roomCode)) {
            return Result.failure(IllegalArgumentException("Código de sala inválido. Use de 4 a 24 caracteres (ex: ROCK-4829)."))
        }

        val nick = safeNickname(nickname, uid)
        val roomPath = "multiplayer_rooms/$roomCode"
        return try {
            val roomRef = firestore.collection("multiplayer_rooms").document(roomCode)
            val joinedRoom = firestore.runTransaction { tx ->
                val snap = tx.get(roomRef)
                if (!snap.exists()) {
                    throw IllegalStateException("Sala '$roomCode' não encontrada. Confira o código com o anfitrião.")
                }
                val room = snap.toObject(
                    MultiplayerRoomCloud::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: throw IllegalStateException("Dados da sala '$roomCode' inválidos.")

                if (room.statusEnum == RoomStatus.CLOSED) {
                    throw IllegalStateException("Esta sala foi encerrada pelo anfitrião.")
                }

                val alreadyMember = room.memberUids.contains(uid)
                if (!alreadyMember) {
                    if (room.statusEnum != RoomStatus.WAITING) {
                        throw IllegalStateException("A partida na sala '$roomCode' já está em andamento.")
                    }
                    if (room.memberUids.size >= room.maxPlayers) {
                        throw IllegalStateException("A sala '$roomCode' já atingiu o limite máximo (${room.maxPlayers} jogadores).")
                    }
                    val updatedMembers = room.memberUids + uid
                    tx.update(
                        roomRef,
                        mapOf(
                            "memberUids" to updatedMembers,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    room.copy(memberUids = updatedMembers)
                } else {
                    room
                }
            }.await()

            val participantRef = roomRef.collection("participants").document(uid)
            val existingPart = participantRef.get().await()
            if (existingPart.exists()) {
                participantRef.update(
                    mapOf(
                        "nickname" to nick,
                        "instrument" to instrument.name,
                        "difficulty" to difficulty.name,
                        "connectionState" to ParticipantConnectionState.ONLINE.name,
                        "lastHeartbeatMs" to System.currentTimeMillis(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val participant = MultiplayerParticipantCloud(
                    id = uid,
                    userId = uid,
                    roomCode = roomCode,
                    nickname = nick,
                    instrument = instrument.name,
                    difficulty = difficulty.name,
                    ready = false,
                    connectionState = ParticipantConnectionState.ONLINE.name,
                    score = 0,
                    combo = 0,
                    rockMeter = 0.6,
                    starPowerActive = false,
                    lastHeartbeatMs = System.currentTimeMillis()
                )
                participantRef.set(participant.toCreateMap()).await()
            }

            Result.success(joinedRoom)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(runCatching { handleFirestoreError(e, OperationType.WRITE, roomPath, auth) }.exceptionOrNull() ?: e)
        }
    }

    fun observeRoom(roomCode: String): Flow<MultiplayerRoomCloud?> = callbackFlow {
        val normalized = normalizeRoomCode(roomCode)
        if (!roomCodeRegex.matches(normalized) || auth.currentUser == null) {
            close(IllegalStateException("Sala ou autenticação inválida."))
            return@callbackFlow
        }
        val listener = firestore.collection("multiplayer_rooms")
            .document(normalized)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val room = if (snap != null && snap.exists()) {
                    snap.toObject(
                        MultiplayerRoomCloud::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
                trySend(room)
            }
        awaitClose { listener.remove() }
    }

    fun observeParticipants(roomCode: String): Flow<List<MultiplayerParticipantCloud>> = callbackFlow {
        val normalized = normalizeRoomCode(roomCode)
        if (!roomCodeRegex.matches(normalized) || auth.currentUser == null) {
            close(IllegalStateException("Sala ou autenticação inválida."))
            return@callbackFlow
        }
        val listener = firestore.collection("multiplayer_rooms")
            .document(normalized)
            .collection("participants")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(
                        MultiplayerParticipantCloud::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }?.sortedBy { it.joinedAt?.toDate()?.time ?: 0L } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateParticipantLoadout(
        roomCode: String,
        nickname: String,
        instrument: Instrument,
        difficulty: Difficulty,
        ready: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuário não autenticado."))
        val normalized = normalizeRoomCode(roomCode)
        val path = "multiplayer_rooms/$normalized/participants/$uid"
        return try {
            firestore.collection("multiplayer_rooms")
                .document(normalized)
                .collection("participants")
                .document(uid)
                .update(
                    mapOf(
                        "nickname" to safeNickname(nickname, uid),
                        "instrument" to instrument.name,
                        "difficulty" to difficulty.name,
                        "ready" to ready,
                        "connectionState" to ParticipantConnectionState.ONLINE.name,
                        "lastHeartbeatMs" to System.currentTimeMillis(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(runCatching { handleFirestoreError(e, OperationType.UPDATE, path, auth) }.exceptionOrNull() ?: e)
        }
    }

    suspend fun updateParticipantProgress(
        roomCode: String,
        score: Int,
        combo: Int,
        rockMeter: Float,
        starPowerActive: Boolean,
        connectionState: ParticipantConnectionState = ParticipantConnectionState.ONLINE
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuário não autenticado."))
        val normalized = normalizeRoomCode(roomCode)
        val path = "multiplayer_rooms/$normalized/participants/$uid"
        return try {
            firestore.collection("multiplayer_rooms")
                .document(normalized)
                .collection("participants")
                .document(uid)
                .update(
                    mapOf(
                        "score" to score.coerceIn(0, 50_000_000),
                        "combo" to combo.coerceIn(0, 100_000),
                        "rockMeter" to rockMeter.toDouble().coerceIn(0.0, 1.0),
                        "starPowerActive" to starPowerActive,
                        "connectionState" to connectionState.name,
                        "lastHeartbeatMs" to System.currentTimeMillis(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(runCatching { handleFirestoreError(e, OperationType.UPDATE, path, auth) }.exceptionOrNull() ?: e)
        }
    }

    suspend fun sendHeartbeat(
        roomCode: String,
        connectionState: ParticipantConnectionState = ParticipantConnectionState.ONLINE
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuário não autenticado."))
        val normalized = normalizeRoomCode(roomCode)
        val path = "multiplayer_rooms/$normalized/participants/$uid"
        return try {
            firestore.collection("multiplayer_rooms")
                .document(normalized)
                .collection("participants")
                .document(uid)
                .update(
                    mapOf(
                        "connectionState" to connectionState.name,
                        "lastHeartbeatMs" to System.currentTimeMillis(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(runCatching { handleFirestoreError(e, OperationType.UPDATE, path, auth) }.exceptionOrNull() ?: e)
        }
    }

    suspend fun updateRoomHostConfig(
        roomCode: String,
        mode: MultiplayerMode,
        track: Track,
        status: RoomStatus? = null,
        winnerUid: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuário não autenticado."))
        val normalized = normalizeRoomCode(roomCode)
        val path = "multiplayer_rooms/$normalized"
        return try {
            val updates = mutableMapOf<String, Any>(
                "mode" to mode.name,
                "maxPlayers" to mode.maxPlayers,
                "trackId" to track.id.ifBlank { "track_thunderstrike" }.take(128),
                "trackTitle" to track.title.ifBlank { "Música de Palco" }.take(160),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (status != null) {
                updates["status"] = status.name
            }
            if (winnerUid != null) {
                updates["winnerUid"] = winnerUid.take(128)
            }
            firestore.collection("multiplayer_rooms")
                .document(normalized)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(runCatching { handleFirestoreError(e, OperationType.UPDATE, path, auth) }.exceptionOrNull() ?: e)
        }
    }

    suspend fun leaveOrCloseRoom(roomCode: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuário não autenticado."))
        val normalized = normalizeRoomCode(roomCode)
        if (!roomCodeRegex.matches(normalized)) return Result.success(Unit)
        val path = "multiplayer_rooms/$normalized"

        return try {
            val roomRef = firestore.collection("multiplayer_rooms").document(normalized)
            val partRef = roomRef.collection("participants").document(uid)

            firestore.runTransaction { tx ->
                val snap = tx.get(roomRef)
                if (!snap.exists()) {
                    return@runTransaction Unit
                }

                val room = snap.toObject(
                    MultiplayerRoomCloud::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: return@runTransaction Unit

                if (room.hostUid == uid) {
                    // Antes do início, transfere a liderança ao próximo participante válido.
                    // Depois do início, encerra a sala para não deixar uma partida sem autoridade.
                    var nextHostUid: String? = null
                    var nextHostNickname: String? = null
                    if (room.statusEnum == RoomStatus.WAITING) {
                        for (candidateUid in room.memberUids.filterNot { it == uid }) {
                            val candidateRef = roomRef.collection("participants").document(candidateUid)
                            val candidate = tx.get(candidateRef)
                            val connection = candidate.getString("connectionState")
                            val heartbeat = candidate.getLong("lastHeartbeatMs")
                            val recentlyActive = heartbeat > 0L &&
                                (System.currentTimeMillis() - heartbeat) <= 14_000L
                            if (
                                candidate.exists() &&
                                connection != ParticipantConnectionState.DISCONNECTED.name &&
                                recentlyActive
                            ) {
                                nextHostUid = candidateUid
                                nextHostNickname = candidate.getString("nickname")
                                    ?.trim()
                                    ?.takeIf { it.length >= 2 }
                                    ?.take(24)
                                    ?: "Rocker_${candidateUid.take(4)}"
                                break
                            }
                        }
                    }

                    if (nextHostUid != null) {
                        tx.update(
                            roomRef,
                            mapOf(
                                "hostUid" to nextHostUid,
                                "hostNickname" to nextHostNickname,
                                "memberUids" to room.memberUids.filterNot { it == uid },
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    } else if (room.statusEnum != RoomStatus.CLOSED) {
                        tx.update(
                            roomRef,
                            mapOf(
                                "status" to RoomStatus.CLOSED.name,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    tx.delete(partRef)
                } else {
                    if (room.statusEnum != RoomStatus.CLOSED && room.memberUids.contains(uid)) {
                        tx.update(
                            roomRef,
                            mapOf(
                                "memberUids" to room.memberUids.filterNot { it == uid },
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    tx.delete(partRef)
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(runCatching { handleFirestoreError(e, OperationType.WRITE, path, auth) }.exceptionOrNull() ?: e)
        }
    }
}
