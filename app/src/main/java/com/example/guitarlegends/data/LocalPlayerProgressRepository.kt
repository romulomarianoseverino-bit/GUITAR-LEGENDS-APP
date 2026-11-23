package com.example.guitarlegends.data

import android.content.Context
import com.example.guitarlegends.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class LocalMatchRecord(
    val id: String = "match_${UUID.randomUUID().toString().take(12)}",
    val trackId: String,
    val trackTitle: String,
    val instrument: String,
    val difficulty: String,
    val score: Int,
    val stars: Int,
    val accuracyPercent: Double,
    val coinsEarned: Int,
    val isCompetitive: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun toCloudEntry(userId: String = "visitante"): ProgressionHistoryEntryCloud =
        ProgressionHistoryEntryCloud(
            id = id,
            userId = userId,
            trackId = trackId,
            trackTitle = trackTitle,
            instrument = instrument,
            difficulty = difficulty,
            score = score,
            stars = stars,
            accuracyPercent = accuracyPercent,
            coinsEarned = coinsEarned,
            isCompetitive = isCompetitive
        )

    fun toMatchResult(track: Track): MatchResult = MatchResult(
        track = track,
        instrument = runCatching { Instrument.valueOf(instrument) }.getOrDefault(Instrument.GUITAR),
        difficulty = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.MEDIUM),
        score = score,
        maxCombo = 0,
        totalNotes = 1,
        hitNotes = 1,
        missedNotes = 0,
        accuracyPercent = accuracyPercent.toFloat(),
        stars = stars,
        coinsEarned = coinsEarned,
        isAdaptiveUsed = true,
        isCompetitive = isCompetitive
    )
}

data class LocalRewardRecord(
    val dayKey: String,
    val dayNumber: Int,
    val streakCount: Int,
    val rewardTitle: String,
    val rewardType: String,
    val coinsGranted: Int,
    val cosmeticId: String
) {
    fun toCloudClaim(userId: String = "visitante"): DailyRewardClaimCloud =
        DailyRewardClaimCloud(
            id = dayKey,
            userId = userId,
            dayKey = dayKey,
            dayNumber = dayNumber,
            streakCount = streakCount,
            rewardTitle = rewardTitle,
            rewardType = rewardType,
            coinsGranted = coinsGranted,
            cosmeticId = cosmeticId
        )
}

data class LocalProgressSnapshot(
    val stats: PlayerStats = PlayerStats(),
    val hasUnlinkedGuestProgress: Boolean = false,
    val hasLocalCustomizationChanges: Boolean = false,
    val localMatches: List<LocalMatchRecord> = emptyList(),
    val localRewards: List<LocalRewardRecord> = emptyList()
)

/**
 * Repositório local atômico para preservar o progresso do jogador no Modo Visitante
 * e realizar a associação segura com uma conta Firebase/Firestore sem perda ou sobrescrita.
 */
class LocalPlayerProgressRepository(private val context: Context) {

    private val progressFile: File by lazy {
        File(context.filesDir, "local_player_progress.json")
    }

    fun loadSnapshot(): LocalProgressSnapshot {
        if (!progressFile.exists()) return LocalProgressSnapshot()
        return try {
            val root = JSONObject(progressFile.readText())
            val statsObj = root.optJSONObject("stats")
            val stats = if (statsObj != null) parseStats(statsObj) else PlayerStats()
            val matches = parseMatches(root.optJSONArray("localMatches"))
            val rewards = parseRewards(root.optJSONArray("localRewards"))

            LocalProgressSnapshot(
                stats = stats,
                hasUnlinkedGuestProgress = root.optBoolean("hasUnlinkedGuestProgress", false),
                hasLocalCustomizationChanges = root.optBoolean("hasLocalCustomizationChanges", false),
                localMatches = matches,
                localRewards = rewards
            )
        } catch (_: Exception) {
            LocalProgressSnapshot()
        }
    }

    fun saveSnapshot(snapshot: LocalProgressSnapshot): Result<Unit> {
        val tempFile = File(context.filesDir, "local_player_progress.json.tmp")
        return try {
            val root = JSONObject().apply {
                put("hasUnlinkedGuestProgress", snapshot.hasUnlinkedGuestProgress)
                put("hasLocalCustomizationChanges", snapshot.hasLocalCustomizationChanges)
                put("stats", statsToJson(snapshot.stats))
                put("localMatches", matchesToJson(snapshot.localMatches))
                put("localRewards", rewardsToJson(snapshot.localRewards.take(30)))
            }
            val payload = root.toString()
            tempFile.writeText(payload)
            if (progressFile.exists() && !progressFile.delete()) {
                progressFile.writeText(payload)
                tempFile.delete()
            } else if (!tempFile.renameTo(progressFile)) {
                progressFile.writeText(payload)
                tempFile.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            runCatching { tempFile.delete() }
            Result.failure(e)
        }
    }

    fun claimGuestDailyReward(
        todayKey: String,
        yesterdayKey: String
    ): Result<Pair<LocalRewardRecord, LocalProgressSnapshot>> {
        val current = loadSnapshot()
        if (current.stats.lastRewardDayKey == todayKey ||
            current.localRewards.any { it.dayKey == todayKey }
        ) {
            return Result.failure(
                IllegalStateException("Você já resgatou a recompensa diária de hoje ($todayKey). Volte amanhã!")
            )
        }

        val newStreak = if (current.stats.lastRewardDayKey == yesterdayKey) {
            current.stats.rewardStreak + 1
        } else {
            1
        }
        val dayNumber = ((newStreak - 1) % 30) + 1
        val rewardDef = DailyRewardCatalog.rewardForDay(dayNumber)

        val updatedCosmetics = buildList {
            addAll(current.stats.unlockedCosmetics)
            if (rewardDef.cosmeticId.isNotBlank()) add(rewardDef.cosmeticId)
        }.distinct()

        val updatedAchievements = buildList {
            addAll(current.stats.achievementsUnlocked)
            if (newStreak >= 3) add("ach_streak_3")
        }.distinct()

        val updatedStats = current.stats.copy(
            coins = current.stats.coins + rewardDef.coinsGranted,
            unlockedCosmetics = updatedCosmetics,
            achievementsUnlocked = updatedAchievements,
            rewardStreak = newStreak,
            lastRewardDayKey = todayKey
        )

        val record = LocalRewardRecord(
            dayKey = todayKey,
            dayNumber = dayNumber,
            streakCount = newStreak,
            rewardTitle = rewardDef.title,
            rewardType = rewardDef.rewardType,
            coinsGranted = rewardDef.coinsGranted,
            cosmeticId = rewardDef.cosmeticId
        )

        val nextSnapshot = current.copy(
            stats = updatedStats,
            hasUnlinkedGuestProgress = true,
            localRewards = (listOf(record) + current.localRewards.filter { it.dayKey != todayKey }).take(30)
        )
        saveSnapshot(nextSnapshot)
        return Result.success(record to nextSnapshot)
    }

    companion object {
        private val BASELINE_DEFAULT_STATS = PlayerStats()

        /**
         * Mescla o progresso obtido no modo visitante com uma conta existente na nuvem
         * sem sobrescrever conquistas/moedas da nuvem nem duplicar o saldo inicial padrão.
         */
        fun mergeGuestIntoCloud(
            cloudStats: PlayerStats,
            guestSnapshot: LocalProgressSnapshot
        ): PlayerStats {
            if (!guestSnapshot.hasUnlinkedGuestProgress) return cloudStats

            val guest = guestSnapshot.stats
            val extraGuestCoins = (guest.coins - BASELINE_DEFAULT_STATS.coins).coerceAtLeast(0)
            val extraGuestStars = (guest.unlockedStars - BASELINE_DEFAULT_STATS.unlockedStars).coerceAtLeast(0)
            val mergedCosmetics = (cloudStats.unlockedCosmetics + guest.unlockedCosmetics + DEFAULT_UNLOCKED_COSMETICS).distinct()
            val mergedAchievements = (cloudStats.achievementsUnlocked + guest.achievementsUnlocked).distinct()

            return cloudStats.copy(
                coins = (cloudStats.coins + extraGuestCoins).coerceIn(0, 10_000_000),
                unlockedStars = (cloudStats.unlockedStars + extraGuestStars).coerceAtLeast(0),
                totalScore = (cloudStats.totalScore + guest.totalScore).coerceAtLeast(0L),
                notesHitTotal = (cloudStats.notesHitTotal + guest.notesHitTotal).coerceAtLeast(0L),
                maxStreakOverall = maxOf(cloudStats.maxStreakOverall, guest.maxStreakOverall),
                songsCompleted = cloudStats.songsCompleted + guest.songsCompleted,
                competitiveWins = cloudStats.competitiveWins + guest.competitiveWins,
                competitiveMatches = cloudStats.competitiveMatches + guest.competitiveMatches,
                careerStageMax = maxOf(cloudStats.careerStageMax, guest.careerStageMax),
                rewardStreak = maxOf(cloudStats.rewardStreak, guest.rewardStreak),
                lastRewardDayKey = maxOf(cloudStats.lastRewardDayKey, guest.lastRewardDayKey),
                unlockedCosmetics = mergedCosmetics,
                achievementsUnlocked = mergedAchievements,
                selectedCharacterId = if (guestSnapshot.hasLocalCustomizationChanges) guest.selectedCharacterId else cloudStats.selectedCharacterId,
                selectedGuitarId = if (guestSnapshot.hasLocalCustomizationChanges) guest.selectedGuitarId else cloudStats.selectedGuitarId,
                selectedBassId = if (guestSnapshot.hasLocalCustomizationChanges) guest.selectedBassId else cloudStats.selectedBassId,
                selectedDrumsId = if (guestSnapshot.hasLocalCustomizationChanges) guest.selectedDrumsId else cloudStats.selectedDrumsId,
                selectedVocalMicId = if (guestSnapshot.hasLocalCustomizationChanges) guest.selectedVocalMicId else cloudStats.selectedVocalMicId,
                selectedHighwayThemeId = if (guestSnapshot.hasLocalCustomizationChanges) guest.selectedHighwayThemeId else cloudStats.selectedHighwayThemeId,
                customRocker = if (guestSnapshot.hasLocalCustomizationChanges) guest.customRocker else cloudStats.customRocker,
                glassAdaptiveEnabled = if (guestSnapshot.hasLocalCustomizationChanges) guest.glassAdaptiveEnabled else cloudStats.glassAdaptiveEnabled,
                audioLatencyOffsetMs = if (guestSnapshot.hasLocalCustomizationChanges) guest.audioLatencyOffsetMs else cloudStats.audioLatencyOffsetMs,
                controlMode = if (guestSnapshot.hasLocalCustomizationChanges) guest.controlMode else cloudStats.controlMode,
                graphicsQuality = if (guestSnapshot.hasLocalCustomizationChanges) guest.graphicsQuality else cloudStats.graphicsQuality,
                studioProfile = run {
                    val base = if (guestSnapshot.hasLocalCustomizationChanges) guest.studioProfile else cloudStats.studioProfile
                    val mergedPresets = (base.savedPresets + cloudStats.studioProfile.savedPresets).distinctBy { it.id }.take(12)
                    val mergedChallenges = (base.claimedChallenges + cloudStats.studioProfile.claimedChallenges).distinct()
                    base.copy(savedPresets = mergedPresets, claimedChallenges = mergedChallenges)
                },
                schemaVersion = maxOf(cloudStats.schemaVersion, guest.schemaVersion),
                syncRevision = maxOf(cloudStats.syncRevision, guest.syncRevision) + 1L
            )
        }
    }

    private fun statsToJson(s: PlayerStats): JSONObject = JSONObject().apply {
        put("coins", s.coins)
        put("totalScore", s.totalScore)
        put("notesHitTotal", s.notesHitTotal)
        put("maxStreakOverall", s.maxStreakOverall)
        put("songsCompleted", s.songsCompleted)
        put("unlockedStars", s.unlockedStars)
        put("selectedCharacterId", s.selectedCharacterId)
        put("selectedGuitarId", s.selectedGuitarId)
        put("selectedBassId", s.selectedBassId)
        put("selectedDrumsId", s.selectedDrumsId)
        put("selectedVocalMicId", s.selectedVocalMicId)
        put("selectedHighwayThemeId", s.selectedHighwayThemeId)
        put("glassAdaptiveEnabled", s.glassAdaptiveEnabled)
        put("audioLatencyOffsetMs", s.audioLatencyOffsetMs)
        put("controlMode", s.controlMode.name)
        put("graphicsQuality", s.graphicsQuality.name)
        put("nickname", s.nickname)
        put("email", s.email)
        put("unlockedCosmetics", JSONArray(s.unlockedCosmetics))
        put("achievementsUnlocked", JSONArray(s.achievementsUnlocked))
        put("competitiveWins", s.competitiveWins)
        put("competitiveMatches", s.competitiveMatches)
        put("careerStageMax", s.careerStageMax)
        put("rewardStreak", s.rewardStreak)
        put("lastRewardDayKey", s.lastRewardDayKey)
        put("schemaVersion", s.schemaVersion)
        put("syncRevision", s.syncRevision)
        put("customRocker", JSONObject().apply {
            put("photoUri", s.customRocker.photoUri ?: JSONObject.NULL)
            put("creationMethod", s.customRocker.creationMethod.name)
            put("artStyle", s.customRocker.artStyle.name)
            put("hairStyle", s.customRocker.hairStyle)
            put("hairColorHex", s.customRocker.hairColorHex)
            put("topClothing", s.customRocker.topClothing)
            put("pantsClothing", s.customRocker.pantsClothing)
            put("shoesClothing", s.customRocker.shoesClothing)
            put("accessories", s.customRocker.accessories)
            put("bodyTattoos", s.customRocker.bodyTattoos)
        })
        put("studioProfile", StudioProfileJsonCodec.toJsonObject(s.studioProfile))
    }

    private fun parseStats(o: JSONObject): PlayerStats {
        val defaults = PlayerStats()
        val crObj = o.optJSONObject("customRocker")
        val customRocker = if (crObj != null) {
            CustomRockerAppearance(
                photoUri = if (crObj.isNull("photoUri")) null else crObj.optString("photoUri", "").ifBlank { null },
                creationMethod = runCatching {
                    CharacterCreationMethod.valueOf(crObj.optString("creationMethod", defaults.customRocker.creationMethod.name))
                }.getOrDefault(defaults.customRocker.creationMethod),
                artStyle = runCatching {
                    CharacterArtStyle.valueOf(crObj.optString("artStyle", defaults.customRocker.artStyle.name))
                }.getOrDefault(defaults.customRocker.artStyle),
                hairStyle = crObj.optString("hairStyle", defaults.customRocker.hairStyle),
                hairColorHex = crObj.optLong("hairColorHex", defaults.customRocker.hairColorHex),
                topClothing = crObj.optString("topClothing", defaults.customRocker.topClothing),
                pantsClothing = crObj.optString("pantsClothing", defaults.customRocker.pantsClothing),
                shoesClothing = crObj.optString("shoesClothing", defaults.customRocker.shoesClothing),
                accessories = crObj.optString("accessories", defaults.customRocker.accessories),
                bodyTattoos = crObj.optString("bodyTattoos", defaults.customRocker.bodyTattoos)
            )
        } else defaults.customRocker

        return PlayerStats(
            coins = o.optInt("coins", defaults.coins),
            totalScore = o.optLong("totalScore", defaults.totalScore),
            notesHitTotal = o.optLong("notesHitTotal", defaults.notesHitTotal),
            maxStreakOverall = o.optInt("maxStreakOverall", defaults.maxStreakOverall),
            songsCompleted = o.optInt("songsCompleted", defaults.songsCompleted),
            unlockedStars = o.optInt("unlockedStars", defaults.unlockedStars),
            selectedCharacterId = o.optString("selectedCharacterId", defaults.selectedCharacterId),
            selectedGuitarId = o.optString("selectedGuitarId", defaults.selectedGuitarId),
            selectedBassId = o.optString("selectedBassId", defaults.selectedBassId),
            selectedDrumsId = o.optString("selectedDrumsId", defaults.selectedDrumsId),
            selectedVocalMicId = o.optString("selectedVocalMicId", defaults.selectedVocalMicId),
            selectedHighwayThemeId = o.optString("selectedHighwayThemeId", defaults.selectedHighwayThemeId),
            customRocker = customRocker,
            glassAdaptiveEnabled = o.optBoolean("glassAdaptiveEnabled", defaults.glassAdaptiveEnabled),
            audioLatencyOffsetMs = o.optLong("audioLatencyOffsetMs", defaults.audioLatencyOffsetMs),
            controlMode = runCatching {
                ControlMode.valueOf(o.optString("controlMode", defaults.controlMode.name))
            }.getOrDefault(defaults.controlMode),
            graphicsQuality = runCatching {
                GraphicsQuality.valueOf(o.optString("graphicsQuality", defaults.graphicsQuality.name))
            }.getOrDefault(defaults.graphicsQuality),
            nickname = o.optString("nickname", ""),
            email = o.optString("email", ""),
            unlockedCosmetics = jsonArrayToStrings(o.optJSONArray("unlockedCosmetics"))
                .ifEmpty { DEFAULT_UNLOCKED_COSMETICS },
            achievementsUnlocked = jsonArrayToStrings(o.optJSONArray("achievementsUnlocked")),
            competitiveWins = o.optInt("competitiveWins", defaults.competitiveWins),
            competitiveMatches = o.optInt("competitiveMatches", defaults.competitiveMatches),
            careerStageMax = o.optInt("careerStageMax", defaults.careerStageMax),
            rewardStreak = o.optInt("rewardStreak", defaults.rewardStreak),
            lastRewardDayKey = o.optString("lastRewardDayKey", ""),
            schemaVersion = o.optInt("schemaVersion", defaults.schemaVersion),
            syncRevision = o.optLong("syncRevision", defaults.syncRevision),
            studioProfile = StudioProfileJsonCodec.fromJsonObject(o.optJSONObject("studioProfile"))
        )
    }

    private fun jsonArrayToStrings(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val s = arr.optString(i, "")
                if (s.isNotBlank()) add(s)
            }
        }
    }

    private fun matchesToJson(list: List<LocalMatchRecord>): JSONArray {
        val arr = JSONArray()
        for (m in list) {
            arr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("trackId", m.trackId)
                    put("trackTitle", m.trackTitle)
                    put("instrument", m.instrument)
                    put("difficulty", m.difficulty)
                    put("score", m.score)
                    put("stars", m.stars)
                    put("accuracyPercent", m.accuracyPercent)
                    put("coinsEarned", m.coinsEarned)
                    put("isCompetitive", m.isCompetitive)
                    put("timestampMs", m.timestampMs)
                }
            )
        }
        return arr
    }

    private fun parseMatches(arr: JSONArray?): List<LocalMatchRecord> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    LocalMatchRecord(
                        id = o.optString("id", "match_$i"),
                        trackId = o.optString("trackId", ""),
                        trackTitle = o.optString("trackTitle", ""),
                        instrument = o.optString("instrument", Instrument.GUITAR.name),
                        difficulty = o.optString("difficulty", Difficulty.MEDIUM.name),
                        score = o.optInt("score", 0),
                        stars = o.optInt("stars", 1),
                        accuracyPercent = o.optDouble("accuracyPercent", 0.0),
                        coinsEarned = o.optInt("coinsEarned", 0),
                        isCompetitive = o.optBoolean("isCompetitive", false),
                        timestampMs = o.optLong("timestampMs", 0L)
                    )
                )
            }
        }
    }

    private fun rewardsToJson(list: List<LocalRewardRecord>): JSONArray {
        val arr = JSONArray()
        for (r in list) {
            arr.put(
                JSONObject().apply {
                    put("dayKey", r.dayKey)
                    put("dayNumber", r.dayNumber)
                    put("streakCount", r.streakCount)
                    put("rewardTitle", r.rewardTitle)
                    put("rewardType", r.rewardType)
                    put("coinsGranted", r.coinsGranted)
                    put("cosmeticId", r.cosmeticId)
                }
            )
        }
        return arr
    }

    private fun parseRewards(arr: JSONArray?): List<LocalRewardRecord> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    LocalRewardRecord(
                        dayKey = o.optString("dayKey", ""),
                        dayNumber = o.optInt("dayNumber", 1),
                        streakCount = o.optInt("streakCount", 1),
                        rewardTitle = o.optString("rewardTitle", ""),
                        rewardType = o.optString("rewardType", "coins"),
                        coinsGranted = o.optInt("coinsGranted", 0),
                        cosmeticId = o.optString("cosmeticId", "")
                    )
                )
            }
        }
    }
}
