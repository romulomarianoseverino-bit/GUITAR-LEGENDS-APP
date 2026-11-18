package com.example.guitarlegends.data

import com.example.guitarlegends.model.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
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
    private val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }
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
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Faça login para resgatar recompensas diárias."))
        return try {
            // A data é determinada pelo servidor; dayKeyOverride é ignorado para impedir falsificação.
            val response = functions.getHttpsCallable("claimDailyReward").call().await().data as? Map<*, *>
                ?: throw IllegalStateException("Resposta inválida do servidor.")
            val account = fetchAccount().getOrThrow()
                ?: throw IllegalStateException("Perfil de conta não encontrado após o resgate.")
            val dayKey = response["dayKey"] as? String ?: utcTodayKey()
            val claim = DailyRewardClaimCloud(
                id = dayKey,
                userId = uid,
                dayKey = dayKey,
                dayNumber = (response["dayNumber"] as? Number)?.toInt() ?: 1,
                streakCount = (response["streakCount"] as? Number)?.toInt() ?: 1,
                rewardTitle = response["rewardTitle"] as? String ?: "Recompensa diária",
                rewardType = response["rewardType"] as? String ?: "coins",
                coinsGranted = (response["coinsGranted"] as? Number)?.toInt() ?: 0,
                cosmeticId = response["cosmeticId"] as? String ?: "",
                claimedAt = Timestamp.now()
            )
            Result.success(claim to account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun claimChallengeReward(challengeId: String): Result<Pair<Int, UserAccountCloud>> {
        return try {
            val response = functions.getHttpsCallable("claimChallengeReward")
                .call(mapOf("challengeId" to challengeId)).await().data as? Map<*, *>
                ?: throw IllegalStateException("Resposta inválida do servidor.")
            val granted = (response["coinsGranted"] as? Number)?.toInt() ?: 0
            val account = fetchAccount().getOrThrow()
                ?: throw IllegalStateException("Perfil de conta não encontrado após o resgate.")
            Result.success(granted to account)
        } catch (e: Exception) {
            Result.failure(e)
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
        entryIdOverride: String? = null,
        failed: Boolean = false
    ): Result<String> {
        val safeIdRegex = Regex("^[a-zA-Z0-9_-]{1,128}$")
        val entryId = entryIdOverride?.takeIf { safeIdRegex.matches(it) }
            ?: "match_${UUID.randomUUID().toString().take(12)}"
        return try {
            val payload = mapOf(
                "entryId" to entryId,
                "trackId" to result.track.id.take(128),
                "trackTitle" to result.track.title.take(160),
                "instrument" to result.instrument.name,
                "difficulty" to result.difficulty.name,
                "score" to result.score,
                "stars" to result.stars.coerceIn(1, 5),
                "accuracyPercent" to result.accuracyPercent.toDouble().coerceIn(0.0, 100.0),
                "hitNotes" to result.hitNotes.coerceAtLeast(0),
                "maxCombo" to result.maxCombo.coerceAtLeast(0),
                "durationMs" to result.track.durationMs.coerceAtLeast(0L),
                "failed" to failed,
                "isCompetitive" to result.isCompetitive
            )
            val response = functions.getHttpsCallable("recordProgressionMatch").call(payload).await().data as? Map<*, *>
                ?: throw IllegalStateException("Resposta inválida do servidor.")
            Result.success(response["entryId"] as? String ?: entryId)
        } catch (e: Exception) {
            Result.failure(e)
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
