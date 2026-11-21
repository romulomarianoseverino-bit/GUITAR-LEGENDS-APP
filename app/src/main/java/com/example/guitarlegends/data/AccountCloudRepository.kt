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
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {
    private val nicknameRegex = Regex("^[a-zA-Z0-9_-]{3,24}$")

    fun currentUidOrNull(): String? = auth.currentUser?.uid
    fun currentEmailOrNull(): String? = auth.currentUser?.email

    fun isValidNicknameFormat(nickname: String): Boolean =
        nicknameRegex.matches(nickname.trim())

    fun utcTodayKey(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
        }
        return sdf.format(Date(nowMillis))
    }

    fun utcYesterdayKey(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Sao_Paulo")).apply {
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
            return Result.failure(IllegalArgumentException(
                "O nickname deve ter de 3 a 24 caracteres (letras, números, _ ou -)."
            ))
        }

        val uid = user.uid
        val normalized = trimmed.lowercase()
        val userRef = firestore.collection("users").document(uid)
        val nicknameRef = firestore.collection("nicknames").document(normalized)
        val safeStats = safeInitialAccountStats(localStats)
        val initialAccount = UserAccountCloud.fromPlayerStats(
            uid = uid,
            email = user.email.orEmpty(),
            nickname = trimmed,
            stats = safeStats,
            syncRevision = 1L
        )

        return try {
            val (created, _) = firestore.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val nicknameSnap = tx.get(nicknameRef)

                if (nicknameSnap.exists() && nicknameSnap.getString("userId") != uid) {
                    throw IllegalStateException("Este nickname já está em uso.")
                }

                if (userSnap.exists()) {
                    val existing = userSnap.toObject(
                        UserAccountCloud::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: throw IllegalStateException("O perfil existente está inválido.")
                    if (existing.nicknameNormalized != normalized ||
                        !nicknameSnap.exists() || nicknameSnap.getString("userId") != uid
                    ) {
                        throw IllegalStateException(
                            "Esta conta já possui outro nickname. Não foi feita nenhuma alteração."
                        )
                    }
                    false to existing
                } else {
                    tx.set(nicknameRef, mapOf(
                        "userId" to uid,
                        "nickname" to trimmed,
                        "createdAt" to FieldValue.serverTimestamp()
                    ))
                    tx.set(userRef, initialAccount.toCreateMap())
                    true to initialAccount
                }
            }.await()

            if (created) {
                Result.success(fetchAccount().getOrThrow()
                    ?: throw IllegalStateException("A conta foi criada, mas o perfil não pôde ser lido."))
            } else {
                syncPlayerStats(localStats)
            }
        } catch (e: Exception) {
            Result.failure(
                runCatching {
                    handleFirestoreError(e, OperationType.WRITE, "users/$uid", auth)
                }.exceptionOrNull() ?: e
            )
        }
    }

    private fun safeInitialAccountStats(stats: PlayerStats): PlayerStats {
        val defaults = DEFAULT_UNLOCKED_COSMETICS.toSet()
        return stats.copy(
            coins = 0,
            totalScore = 0L,
            notesHitTotal = 0L,
            maxStreakOverall = 0,
            songsCompleted = 0,
            unlockedStars = 6,
            selectedCharacterId = stats.selectedCharacterId.takeIf {
                it == "custom_rocker" || it in defaults
            } ?: "axel_rocker",
            selectedGuitarId = stats.selectedGuitarId.takeIf { it in defaults } ?: "flame_caster",
            selectedBassId = stats.selectedBassId.takeIf { it in defaults } ?: "thunder_bass",
            selectedDrumsId = stats.selectedDrumsId.takeIf { it in defaults } ?: "arena_beast_kit",
            selectedVocalMicId = stats.selectedVocalMicId.takeIf { it in defaults } ?: "vintage_shure_55s",
            selectedHighwayThemeId = stats.selectedHighwayThemeId.takeIf { it in defaults } ?: "theme_ps2_wood",
            unlockedCosmetics = DEFAULT_UNLOCKED_COSMETICS,
            achievementsUnlocked = emptyList(),
            competitiveWins = 0,
            competitiveMatches = 0,
            verifiedSongsCompleted = 0,
            verifiedNotesHitTotal = 0L,
            verifiedMaxStreakOverall = 0,
            verifiedUnlockedStars = 0,
            verifiedCompetitiveWins = 0,
            careerStageMax = 1,
            rewardStreak = 0,
            lastRewardDayKey = "",
            syncRevision = 1L
        )
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

    suspend fun importLegacyProgress(localStats: PlayerStats): Result<UserAccountCloud> {
        return try {
            val legacyStats = mapOf(
                "totalScore" to localStats.totalScore.coerceAtLeast(0L),
                "notesHitTotal" to localStats.notesHitTotal.coerceAtLeast(0L),
                "maxStreakOverall" to localStats.maxStreakOverall.coerceAtLeast(0),
                "songsCompleted" to localStats.songsCompleted.coerceAtLeast(0),
                "unlockedStars" to localStats.unlockedStars.coerceAtLeast(0),
                "competitiveWins" to localStats.competitiveWins.coerceAtLeast(0),
                "competitiveMatches" to localStats.competitiveMatches.coerceAtLeast(0),
                "careerStageMax" to localStats.careerStageMax.coerceAtLeast(1),
                "achievementsUnlocked" to localStats.achievementsUnlocked.distinct().take(60)
            )
            functions.getHttpsCallable("importLegacyProgress")
                .call(mapOf("legacyStats" to legacyStats)).await()
            Result.success(fetchAccount().getOrThrow()
                ?: throw IllegalStateException("Perfil de conta não encontrado após importar o progresso."))
        } catch (e: Exception) {
            Result.failure<UserAccountCloud>(e)
        }
    }

    suspend fun syncPlayerStats(localStats: PlayerStats): Result<UserAccountCloud> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Usuário não autenticado para sincronização."))
        val uid = user.uid
        return try {
            val current = fetchAccount().getOrThrow()
                ?: throw IllegalStateException("Conclua o cadastro com nickname antes de sincronizar.")

            // O cliente sincroniza somente preferências de perfil. Saldo, inventário,
            // recompensas, histórico e progresso econômico permanecem imutáveis no app.
            val unlocked = current.unlockedCosmetics.toSet()
            val safeStats = localStats.copy(
                coins = current.coins,
                totalScore = current.totalScore,
                notesHitTotal = current.notesHitTotal,
                maxStreakOverall = current.maxStreakOverall,
                songsCompleted = current.songsCompleted,
                unlockedStars = current.unlockedStars,
                selectedCharacterId = localStats.selectedCharacterId.takeIf {
                    it == "custom_rocker" || it in unlocked
                } ?: current.selectedCharacterId,
                selectedGuitarId = localStats.selectedGuitarId.takeIf { it in unlocked } ?: current.selectedGuitarId,
                selectedBassId = localStats.selectedBassId.takeIf { it in unlocked } ?: current.selectedBassId,
                selectedDrumsId = localStats.selectedDrumsId.takeIf { it in unlocked } ?: current.selectedDrumsId,
                selectedVocalMicId = localStats.selectedVocalMicId.takeIf { it in unlocked } ?: current.selectedVocalMicId,
                selectedHighwayThemeId = localStats.selectedHighwayThemeId.takeIf { it in unlocked } ?: current.selectedHighwayThemeId,
                nickname = current.nickname,
                email = current.email,
                unlockedCosmetics = current.unlockedCosmetics,
                achievementsUnlocked = current.achievementsUnlocked,
                competitiveWins = current.competitiveWins,
                competitiveMatches = current.competitiveMatches,
                verifiedSongsCompleted = current.verifiedSongsCompleted,
                verifiedNotesHitTotal = current.verifiedNotesHitTotal,
                verifiedMaxStreakOverall = current.verifiedMaxStreakOverall,
                verifiedUnlockedStars = current.verifiedUnlockedStars,
                verifiedCompetitiveWins = current.verifiedCompetitiveWins,
                careerStageMax = current.careerStageMax,
                rewardStreak = current.rewardStreak,
                lastRewardDayKey = current.lastRewardDayKey,
                syncRevision = current.syncRevision
            )
            val cloud = UserAccountCloud.fromPlayerStats(
                uid = uid,
                email = current.email,
                nickname = current.nickname,
                stats = safeStats,
                syncRevision = current.syncRevision + 1L
            )
            val profileUpdate = mapOf(
                "selectedCharacterId" to cloud.selectedCharacterId,
                "selectedGuitarId" to cloud.selectedGuitarId,
                "selectedBassId" to cloud.selectedBassId,
                "selectedDrumsId" to cloud.selectedDrumsId,
                "selectedVocalMicId" to cloud.selectedVocalMicId,
                "selectedHighwayThemeId" to cloud.selectedHighwayThemeId,
                "customRocker" to cloud.customRocker.toMap(),
                "glassAdaptiveEnabled" to cloud.glassAdaptiveEnabled,
                "audioLatencyOffsetMs" to cloud.audioLatencyOffsetMs,
                "controlMode" to cloud.controlMode,
                "graphicsQuality" to cloud.graphicsQuality,
                "studioProfileJson" to cloud.studioProfileJson,
                "syncRevision" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("users").document(uid).update(profileUpdate).await()

            Result.success(fetchAccount().getOrThrow()
                ?: throw IllegalStateException("Perfil não encontrado após a sincronização."))
        } catch (e: Exception) {
            Result.failure(
                runCatching {
                    handleFirestoreError(e, OperationType.UPDATE, "users/$uid", auth)
                }.exceptionOrNull() ?: e
            )
        }
    }

    suspend fun purchaseCosmetic(cosmeticId: String): Result<UserAccountCloud> {
        return try {
            functions.getHttpsCallable("purchaseCosmetic")
                .call(mapOf("cosmeticId" to cosmeticId)).await()
            Result.success(fetchAccount().getOrThrow()
                ?: throw IllegalStateException("Perfil de conta não encontrado após a compra."))
        } catch (e: Exception) {
            Result.failure(e)
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
                "totalNotes" to result.totalNotes.coerceAtLeast(1),
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
