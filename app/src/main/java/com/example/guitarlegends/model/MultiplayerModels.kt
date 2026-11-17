package com.example.guitarlegends.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MultiplayerMode(val displayName: String, val maxPlayers: Int) {
    BATTLE_1V1("Batalha 1v1", 2),
    BAND_COOP("Banda Cooperativa", 4)
}

enum class RoomStatus(val label: String) {
    WAITING("Aguardando Jogadores"),
    PLAYING("Show em Andamento"),
    FINISHED("Show Finalizado"),
    CLOSED("Sala Encerrada")
}

enum class ParticipantConnectionState(val label: String) {
    ONLINE("Conectado"),
    RECONNECTING("Reconectando..."),
    DISCONNECTED("Desconectado")
}

@Keep
@IgnoreExtraProperties
data class MultiplayerRoomCloud(
    @DocumentId
    val id: String = "",
    val roomCode: String = "",
    val hostUid: String = "",
    val hostNickname: String = "",
    val mode: String = MultiplayerMode.BATTLE_1V1.name,
    val status: String = RoomStatus.WAITING.name,
    val trackId: String = "track_thunderstrike",
    val trackTitle: String = "Thunderstrike Anthem",
    val maxPlayers: Int = 2,
    val memberUids: List<String> = emptyList(),
    val winnerUid: String = "",
    val inviteMessage: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    val modeEnum: MultiplayerMode
        get() = runCatching { MultiplayerMode.valueOf(mode) }.getOrDefault(MultiplayerMode.BATTLE_1V1)

    val statusEnum: RoomStatus
        get() = runCatching { RoomStatus.valueOf(status) }.getOrDefault(RoomStatus.WAITING)

    fun toCreateMap(): Map<String, Any?> = mapOf(
        "roomCode" to roomCode.take(24),
        "hostUid" to hostUid.take(128),
        "hostNickname" to hostNickname.take(24),
        "mode" to modeEnum.name,
        "status" to statusEnum.name,
        "trackId" to trackId.take(128),
        "trackTitle" to trackTitle.take(160),
        "maxPlayers" to maxPlayers.coerceIn(2, 4),
        "memberUids" to memberUids.distinct().take(4),
        "winnerUid" to winnerUid.take(128),
        "inviteMessage" to inviteMessage.take(160),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class MultiplayerParticipantCloud(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val roomCode: String = "",
    val nickname: String = "",
    val instrument: String = Instrument.GUITAR.name,
    val difficulty: String = Difficulty.MEDIUM.name,
    val ready: Boolean = false,
    val connectionState: String = ParticipantConnectionState.ONLINE.name,
    val score: Int = 0,
    val combo: Int = 0,
    val rockMeter: Double = 0.6,
    val starPowerActive: Boolean = false,
    val lastHeartbeatMs: Long = 0L,
    @ServerTimestamp
    val joinedAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    val instrumentEnum: Instrument
        get() = runCatching { Instrument.valueOf(instrument) }.getOrDefault(Instrument.GUITAR)

    val difficultyEnum: Difficulty
        get() = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.MEDIUM)

    val connectionStateEnum: ParticipantConnectionState
        get() = runCatching { ParticipantConnectionState.valueOf(connectionState) }
            .getOrDefault(ParticipantConnectionState.ONLINE)

    fun isStale(nowMs: Long, thresholdMs: Long = 14_000L): Boolean {
        if (connectionStateEnum == ParticipantConnectionState.DISCONNECTED) return true
        return lastHeartbeatMs > 0L && (nowMs - lastHeartbeatMs) > thresholdMs
    }

    fun toCreateMap(): Map<String, Any?> = mapOf(
        "userId" to userId.take(128),
        "roomCode" to roomCode.take(24),
        "nickname" to nickname.take(24),
        "instrument" to instrumentEnum.name,
        "difficulty" to difficultyEnum.name,
        "ready" to ready,
        "connectionState" to connectionStateEnum.name,
        "score" to score.coerceIn(0, 50_000_000),
        "combo" to combo.coerceIn(0, 100_000),
        "rockMeter" to rockMeter.coerceIn(0.0, 1.0),
        "starPowerActive" to starPowerActive,
        "lastHeartbeatMs" to lastHeartbeatMs.coerceAtLeast(0L),
        "joinedAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
