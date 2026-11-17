package com.example.guitarlegends.data

import com.example.guitarlegends.model.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class AccountCloudRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val nicknameRegex = Regex("^[a-zA-Z0-9_-]{3,24}$")

    fun currentUidOrNull(): String? = auth.currentUser?.uid
    fun currentEmailOrNull(): String? = auth.currentUser?.email

    fun isValidNicknameFormat(nickname: String): Boolean =
        nicknameRegex.matches(nickname.trim())

    fun utcTodayKey(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(nowMillis))
    }

    fun utcYesterdayKey(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(cal.time)
    }

    suspend fun isNicknameAvailable(nickname: String): Result<Boolean> {
        val trimmed = nickname.trim()
        if (!isValidNicknameFormat(trimmed)) {
            return Result.failure(
                IllegalArgumentException("O nickname deve ter de 3 a 24 caracteres (letras, números, _ ou -).")
            )
        }
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Autenticação necessária para verificar nickname."))
        val normalized = trimmed.lowercase()
        val path = "nicknames/$normalized"
        return try {
            val snap = firestore.collection("nicknames").document(normalized).get().await()
            val available = !snap.exists() || snap.getString("userId") == uid
            Result.success(available)
        } catch (e: Exception) {
            Result.failure(runCatching { handleFirestoreError(e, OperationType.GET, path, auth) }.exceptionOrNull() ?: e)
        }
    }

    suspend fun claimNicknameAndSaveAccount(
        nickname: String,
        localStats: PlayerStats
    ): Result<UserAccountCloud> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Faça login antes de definir seu nickname."))
        val trimmed = nickname.trim()
        if (!isValidNicknameFormat(trimmed)) {
            return Result.failure(
                IllegalArgumentException("O nickname deve ter de 3 a 24 caracteres (letras, números, _ ou -).")
            )
        }
        val normalized = trimmed.lowercase()
        val uid = user.uid
        val email = user.email ?: localStats.email

        return try {
            val saved = firestore.runTransaction { tx ->
                val nickRef = firestore.collection("nicknames").document(normalized)
                val userRef = firestore.collection("users").document(uid)

                val nickSnap = tx.get(nickRef)
                val userSnap = tx.get(userRef)

                if (nickSnap.exists() && nickSnap.getString("userId") != uid) {
                    throw IllegalStateException("O nickname '$trimmed' já está em uso por outro jogador.")
                }

                val existingUser = if (userSnap.exists()) {
                    userSnap.toObject(UserAccountCloud::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null

                val oldNormalized = existingUser?.nicknameNormalized.orEmpty()
                if (oldNormalized.isNotBlank() && oldNormalized != normalized) {
                    val oldNickRef = firestore.collection("nicknames").document(oldNormalized)
                    tx.delete(oldNickRef)
                }

                val nextRevision = maxOf(existingUser?.syncRevision ?: 0L, localStats.syncRevision) + 1L
                val mergedStats = if (existingUser != null) {
                    mergeStats(localStats, existingUser.toPlayerStats())
                } else {
                    localStats
                }.copy(nickname = trimmed, email = email, syncRevision = nextRevision)

                val cloudObj = UserAccountCloud.fromPlayerStats(
                    uid = uid,
                    email = email,
                    nickname = trimmed,
                    stats = mergedStats,
                    syncRevision = nextRevision
                )

                tx.set(
                    nickRef,
                    mapOf(
                        "userId" to uid,
                        "nickname" to trimmed,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                if (existingUser == null) {
                    tx.set(userRef, cloudObj.toCreateMap())
                } else {
                    tx.update(userRef, cloudObj.toUpdateMap())
                }
                cloudObj
            }.await()

            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(runCatching { handleFirestoreError(e, OperationType.WRITE, "users/$uid", auth) }.exceptionOrNull() ?: e)
        }
    }

    suspend fun fetchAccount(): Result<UserAccountCloud?> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuário não autenticado."))
        val path = "users/$uid"
        return try {
            val snap = firestore.collection("users").document(uid).get().await()
            val obj = if (snap.exists()) {
                snap.toObject(UserAccountCloud::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(obj)
        } catch (e: Exception) {
            Result.failure(runCatching { handleFirestoreError(e, OperationType.GET, path, auth) }.exceptionOrNull() ?: e)
        }
    }

    suspend fun syncPlayerStats(localStats: PlayerStats): Result<UserAccountCloud> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Usuário não autenticado para sincronização."))
        val uid = user.uid
        val email = user.email ?: localStats.email

        return try {
            val synced = firestore.runTransaction { tx ->
                val userRef = firestore.collection("users").document(uid)
                val snap = tx.get(userRef)
                val existing = if (snap.exists()) {
                    snap.toObject(UserAccountCloud::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null

                if (existing == null) {
                    val nick = localStats.nickname.trim()
                    if (!isValidNicknameFormat(nick)) {
                        throw IllegalStateException("Defina um nickname exclusivo antes de sincronizar na nuvem.")
                    }
                    val cloudObj = UserAccountCloud.fromPlayerStats(
                        uid = uid,
                        email = email,
                        nickname = nick,
                        stats = localStats,
                        syncRevision = maxOf(1L, localStats.syncRevision)
                    )
                    val nickRef = firestore.collection("nicknames").document(nick.lowercase())
                    val nickSnap = tx.get(nickRef)
                    if (nickSnap.exists() && nickSnap.getString("userId") != uid) {
                        throw IllegalStateException("Nickname já reservado por outra conta.")
                    }
                    tx.set(
                        nickRef,
                        mapOf(
                            "userId" to uid,
                            "nickname" to nick,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(userRef, cloudObj.toCreateMap())
                    cloudObj
                } else {
                    // Resolução não destrutiva de conflitos por maior revisão e união de desbloqueios
                    val resolved = if (existing.syncRevision > localStats.syncRevision) {
                        mergeStats(existing.toPlayerStats(), localStats)
                    } else {
                        mergeStats(localStats, existing.toPlayerStats())
                    }
                    val nextRevision = maxOf(existing.syncRevision, localStats.syncRevision) + 1L
                    val cloudObj = UserAccountCloud.fromPlayerStats(
                        uid = uid,
                        email = email,
                        nickname = existing.nickname.ifBlank { resolved.nickname },
                        stats = resolved.copy(syncRevision = nextRevision),
                        syncRevision = nextRevision
                    )
                    tx.update(userRef, cloudObj.toUpdateMap())
                    cloudObj
                }
            }.await()

            Result.success(synced)
        } catch (e: Exception) {
            Result.failure(runCatching { handleFirestoreError(e, OperationType.UPDATE, "users/$uid", auth) }.exceptionOrNull() ?: e)
        }
    }

    fun observeUserAccount(): Flow<UserAccountCloud?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close(IllegalStateException("Acesso negado: faça login primeiro."))
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val obj = snap?.toObject(
                    UserAccountCloud::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                trySend(obj)
            }
        awaitClose { listener.remove() }
    }

    suspend fun claimDailyReward(
        localStats: PlayerStats,
        dayKeyOverride: String? = null
    ): Result<Pair<DailyRewardClaimCloud, UserAccountCloud>> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Faça login para resgatar recompensas diárias."))
        val uid = user.uid
        val todayKey = dayKeyOverride ?: utcTodayKey()
        val yesterdayKey = utcYesterdayKey()
        val claimPath = "users/$uid/reward_claims/$todayKey"

        return try {
            val resultPair = firestore.runTransaction { tx ->
                val userRef = firestore.collection("users").document(uid)
                val claimRef = userRef.collection("reward_claims").document(todayKey)

                val userSnap = tx.get(userRef)
                val claimSnap = tx.get(claimRef)

                if (!userSnap.exists()) {
                    throw IllegalStateException("Finalize a criação da sua conta com nickname antes de resgatar recompensas.")
                }
                if (claimSnap.exists()) {
                    throw IllegalStateException("Você já resgatou a recompensa diária de hoje ($todayKey). Volte amanhã!")
                }

                val existingUser = userSnap.toObject(
                    UserAccountCloud::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: throw IllegalStateException("Perfil de conta inválido.")

                val newStreak = if (existingUser.lastRewardDayKey == yesterdayKey) {
                    existingUser.rewardStreak + 1
                } else {
                    1
                }
                val dayNumber = ((newStreak - 1) % 30) + 1
                val rewardDef = DailyRewardCatalog.rewardForDay(dayNumber)

                val claimPayload = mapOf(
                    "userId" to uid,
                    "dayKey" to todayKey,
                    "dayNumber" to dayNumber,
                    "streakCount" to newStreak,
                    "rewardTitle" to rewardDef.title,
                    "rewardType" to rewardDef.rewardType,
                    "coinsGranted" to rewardDef.coinsGranted,
                    "cosmeticId" to rewardDef.cosmeticId,
                    "claimedAt" to FieldValue.serverTimestamp()
                )

                val updatedCosmetics = buildList {
                    addAll(existingUser.unlockedCosmetics)
                    if (rewardDef.cosmeticId.isNotBlank()) add(rewardDef.cosmeticId)
                }.distinct()

                val updatedAchievements = buildList {
                    addAll(existingUser.achievementsUnlocked)
                    if (newStreak >= 3) add("ach_streak_3")
                }.distinct()

                val nextRevision = maxOf(existingUser.syncRevision, localStats.syncRevision) + 1L
                val updatedUser = existingUser.copy(
                    coins = existingUser.coins + rewardDef.coinsGranted,
                    unlockedCosmetics = updatedCosmetics,
                    achievementsUnlocked = updatedAchievements,
                    rewardStreak = newStreak,
                    lastRewardDayKey = todayKey,
                    syncRevision = nextRevision
                )

                tx.set(claimRef, claimPayload)
                tx.update(userRef, updatedUser.toUpdateMap())

                val claimModel = DailyRewardClaimCloud(
                    id = todayKey,
                    userId = uid,
                    dayKey = todayKey,
                    dayNumber = dayNumber,
                    streakCount = newStreak,
                    rewardTitle = rewardDef.title,
                    rewardType = rewardDef.rewardType,
                    coinsGranted = rewardDef.coinsGranted,
                    cosmeticId = rewardDef.cosmeticId,
                    claimedAt = Timestamp.now()
                )
                claimModel to updatedUser
            }.await()

            Result.success(resultPair)
        } catch (e: Exception) {
            Result.failure(runCatching { handleFirestoreError(e, OperationType.WRITE, claimPath, auth) }.exceptionOrNull() ?: e)
        }
    }

    suspend fun fetchRewardHistory(): Result<List<DailyRewardClaimCloud>> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuário não autenticado."))
        val path = "users/$uid/reward_claims"
        return try {
            val snap = firestore.collection("users").document(uid)
                .collection("reward_claims")
                .orderBy("dayKey", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .await()
            val items = snap.documents.mapNotNull {
                it.toObject(DailyRewardClaimCloud::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(runCatching { handleFirestoreError(e, OperationType.LIST, path, auth) }.exceptionOrNull() ?: e)
        }
    }

    suspend fun recordProgressionMatch(
        result: MatchResult,
        entryIdOverride: String? = null
    ): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuário não autenticado."))
        val safeIdRegex = Regex("^[a-zA-Z0-9_-]{1,128}$")
        val entryId = entryIdOverride?.takeIf { safeIdRegex.matches(it) }
            ?: "match_${UUID.randomUUID().toString().take(12)}"
        val path = "users/$uid/progression_history/$entryId"
        return try {
            val payload = mapOf(
                "userId" to uid,
                "trackId" to result.track.id.take(128),
                "trackTitle" to result.track.title.take(160),
                "instrument" to result.instrument.name,
                "difficulty" to result.difficulty.name,
                "score" to result.score,
                "stars" to result.stars.coerceIn(1, 5),
                "accuracyPercent" to result.accuracyPercent.toDouble().coerceIn(0.0, 100.0),
                "coinsEarned" to result.coinsEarned.coerceAtLeast(0),
                "isCompetitive" to result.isCompetitive,
                "createdAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("users").document(uid)
                .collection("progression_history").document(entryId)
                .set(payload)
                .await()
            Result.success(entryId)
        } catch (e: Exception) {
            Result.failure(runCatching { handleFirestoreError(e, OperationType.CREATE, path, auth) }.exceptionOrNull() ?: e)
        }
    }

    suspend fun fetchProgressionHistory(): Result<List<ProgressionHistoryEntryCloud>> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuário não autenticado."))
        val path = "users/$uid/progression_history"
        return try {
            val snap = firestore.collection("users").document(uid)
                .collection("progression_history")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(25)
                .get()
                .await()
            val items = snap.documents.mapNotNull {
                it.toObject(ProgressionHistoryEntryCloud::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(runCatching { handleFirestoreError(e, OperationType.LIST, path, auth) }.exceptionOrNull() ?: e)
        }
    }

    suspend fun fetchReleasesCatalog(): Result<List<AppReleaseInfo>> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            return Result.success(LocalReleaseCatalog.history)
        }
        return try {
            val snap = firestore.collection("app_releases")
                .orderBy("versionCode", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
            val remote = snap.documents.mapNotNull {
                it.toObject(AppReleaseInfo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            Result.success(remote.ifEmpty { LocalReleaseCatalog.history })
        } catch (e: Exception) {
            Result.failure(runCatching { handleFirestoreError(e, OperationType.LIST, "app_releases", auth) }.exceptionOrNull() ?: e)
        }
    }

    private fun mergeStats(primary: PlayerStats, secondary: PlayerStats): PlayerStats {
        return primary.copy(
            coins = maxOf(primary.coins, secondary.coins),
            totalScore = maxOf(primary.totalScore, secondary.totalScore),
            notesHitTotal = maxOf(primary.notesHitTotal, secondary.notesHitTotal),
            maxStreakOverall = maxOf(primary.maxStreakOverall, secondary.maxStreakOverall),
            songsCompleted = maxOf(primary.songsCompleted, secondary.songsCompleted),
            unlockedStars = maxOf(primary.unlockedStars, secondary.unlockedStars),
            competitiveWins = maxOf(primary.competitiveWins, secondary.competitiveWins),
            competitiveMatches = maxOf(primary.competitiveMatches, secondary.competitiveMatches),
            careerStageMax = maxOf(primary.careerStageMax, secondary.careerStageMax),
            rewardStreak = maxOf(primary.rewardStreak, secondary.rewardStreak),
            lastRewardDayKey = maxOf(primary.lastRewardDayKey, secondary.lastRewardDayKey),
            unlockedCosmetics = (primary.unlockedCosmetics + secondary.unlockedCosmetics + DEFAULT_UNLOCKED_COSMETICS).distinct(),
            achievementsUnlocked = (primary.achievementsUnlocked + secondary.achievementsUnlocked).distinct(),
            schemaVersion = maxOf(primary.schemaVersion, secondary.schemaVersion)
        )
    }
}
