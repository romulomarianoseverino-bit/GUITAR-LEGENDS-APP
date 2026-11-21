package com.example.guitarlegends.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class CustomRockerCloud(
    val photoUri: String? = null,
    val creationMethod: String = CharacterCreationMethod.INSPIRED_BY_PHOTO.name,
    val artStyle: String = CharacterArtStyle.STYLIZED_REALISTIC.name,
    val hairStyle: String = "Moicano Punk Clássico",
    val hairColorHex: Long = 0xFFFF5722L,
    val topClothing: String = "Jaqueta de Couro c/ Rebites",
    val pantsClothing: String = "Calça Jeans Rasgada",
    val shoesClothing: String = "Coturnos de Couro Pesado",
    val accessories: String = "Munhequeiras de Espinhos & Óculos Escuros Aviador",
    val bodyTattoos: String = "Tatuagem Tribal e Chamas nos Braços"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "photoUri" to photoUri?.take(512),
        "creationMethod" to creationMethod,
        "artStyle" to artStyle,
        "hairStyle" to hairStyle,
        "hairColorHex" to hairColorHex,
        "topClothing" to topClothing,
        "pantsClothing" to pantsClothing,
        "shoesClothing" to shoesClothing,
        "accessories" to accessories,
        "bodyTattoos" to bodyTattoos
    )

    fun toAppearance(): CustomRockerAppearance = CustomRockerAppearance(
        photoUri = photoUri,
        creationMethod = runCatching { CharacterCreationMethod.valueOf(creationMethod) }
            .getOrDefault(CharacterCreationMethod.INSPIRED_BY_PHOTO),
        artStyle = runCatching { CharacterArtStyle.valueOf(artStyle) }
            .getOrDefault(CharacterArtStyle.STYLIZED_REALISTIC),
        hairStyle = hairStyle,
        hairColorHex = hairColorHex,
        topClothing = topClothing,
        pantsClothing = pantsClothing,
        shoesClothing = shoesClothing,
        accessories = accessories,
        bodyTattoos = bodyTattoos
    )

    companion object {
        fun fromAppearance(a: CustomRockerAppearance) = CustomRockerCloud(
            photoUri = a.photoUri,
            creationMethod = a.creationMethod.name,
            artStyle = a.artStyle.name,
            hairStyle = a.hairStyle,
            hairColorHex = a.hairColorHex,
            topClothing = a.topClothing,
            pantsClothing = a.pantsClothing,
            shoesClothing = a.shoesClothing,
            accessories = a.accessories,
            bodyTattoos = a.bodyTattoos
        )
    }
}

data class UserAccountCloud(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val email: String = "",
    val nickname: String = "",
    val nicknameNormalized: String = "",
    val coins: Int = 450,
    val totalScore: Long = 0L,
    val notesHitTotal: Long = 0L,
    val maxStreakOverall: Int = 0,
    val songsCompleted: Int = 0,
    val unlockedStars: Int = 6,
    val competitiveWins: Int = 0,
    val competitiveMatches: Int = 0,
    val careerStageMax: Int = 1,
    val selectedCharacterId: String = "axel_rocker",
    val selectedGuitarId: String = "flame_caster",
    val selectedBassId: String = "thunder_bass",
    val selectedDrumsId: String = "arena_beast_kit",
    val selectedVocalMicId: String = "vintage_shure_55s",
    val selectedHighwayThemeId: String = "theme_ps2_wood",
    val unlockedCosmetics: List<String> = DEFAULT_UNLOCKED_COSMETICS,
    val achievementsUnlocked: List<String> = emptyList(),
    val customRocker: CustomRockerCloud = CustomRockerCloud(),
    val glassAdaptiveEnabled: Boolean = true,
    val audioLatencyOffsetMs: Long = 0L,
    val controlMode: String = ControlMode.DIRECT_TOUCH.name,
    val graphicsQuality: String = GraphicsQuality.HIGH.name,
    val rewardStreak: Int = 0,
    val lastRewardDayKey: String = "",
    val dailyRewardWeekKey: String = "",
    val dailyRewardCoinsClaimed: Int = 0,
    val schemaVersion: Int = 3,
    val syncRevision: Long = 1L,
    val studioProfileJson: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "email" to email.take(200),
        "nickname" to nickname.take(24),
        "nicknameNormalized" to nicknameNormalized.take(24),
        "coins" to coins.coerceIn(0, 10_000_000),
        "totalScore" to totalScore.coerceAtLeast(0L),
        "notesHitTotal" to notesHitTotal.coerceAtLeast(0L),
        "maxStreakOverall" to maxStreakOverall.coerceAtLeast(0),
        "songsCompleted" to songsCompleted.coerceAtLeast(0),
        "unlockedStars" to unlockedStars.coerceAtLeast(0),
        "competitiveWins" to competitiveWins.coerceAtLeast(0),
        "competitiveMatches" to competitiveMatches.coerceAtLeast(0),
        "careerStageMax" to careerStageMax.coerceAtLeast(0),
        "selectedCharacterId" to selectedCharacterId.take(64),
        "selectedGuitarId" to selectedGuitarId.take(64),
        "selectedBassId" to selectedBassId.take(64),
        "selectedDrumsId" to selectedDrumsId.take(64),
        "selectedVocalMicId" to selectedVocalMicId.take(64),
        "selectedHighwayThemeId" to selectedHighwayThemeId.take(64),
        "unlockedCosmetics" to unlockedCosmetics.distinct().take(150),
        "achievementsUnlocked" to achievementsUnlocked.distinct().take(60),
        "customRocker" to customRocker.toMap(),
        "glassAdaptiveEnabled" to glassAdaptiveEnabled,
        "audioLatencyOffsetMs" to audioLatencyOffsetMs.coerceIn(-500L, 500L),
        "controlMode" to controlMode.take(32),
        "graphicsQuality" to graphicsQuality.take(32),
        "rewardStreak" to rewardStreak.coerceIn(0, 3650),
        "lastRewardDayKey" to lastRewardDayKey.take(20),
        "dailyRewardWeekKey" to dailyRewardWeekKey.take(16),
        "dailyRewardCoinsClaimed" to dailyRewardCoinsClaimed.coerceIn(0, DailyRewardWeeklyLimit.MAX_COINS_PER_WEEK),
        "schemaVersion" to schemaVersion.coerceAtLeast(1),
        "syncRevision" to syncRevision.coerceAtLeast(1L),
        "studioProfileJson" to studioProfileJson.take(24576),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any?> = toCreateMap()
        .filterKeys { it != "userId" && it != "createdAt" }

    fun toPlayerStats(): PlayerStats = PlayerStats(
        coins = coins,
        totalScore = totalScore,
        notesHitTotal = notesHitTotal,
        maxStreakOverall = maxStreakOverall,
        songsCompleted = songsCompleted,
        unlockedStars = unlockedStars,
        selectedCharacterId = selectedCharacterId,
        selectedGuitarId = selectedGuitarId,
        selectedBassId = selectedBassId,
        selectedDrumsId = selectedDrumsId,
        selectedVocalMicId = selectedVocalMicId,
        selectedHighwayThemeId = selectedHighwayThemeId,
        customRocker = customRocker.toAppearance(),
        glassAdaptiveEnabled = glassAdaptiveEnabled,
        audioLatencyOffsetMs = audioLatencyOffsetMs,
        controlMode = runCatching { ControlMode.valueOf(controlMode) }.getOrDefault(ControlMode.DIRECT_TOUCH),
        graphicsQuality = runCatching { GraphicsQuality.valueOf(graphicsQuality) }.getOrDefault(GraphicsQuality.HIGH),
        nickname = nickname,
        email = email,
        unlockedCosmetics = unlockedCosmetics.ifEmpty { DEFAULT_UNLOCKED_COSMETICS },
        achievementsUnlocked = achievementsUnlocked,
        competitiveWins = competitiveWins,
        competitiveMatches = competitiveMatches,
        careerStageMax = careerStageMax,
        rewardStreak = rewardStreak,
        lastRewardDayKey = lastRewardDayKey,
        schemaVersion = schemaVersion,
        syncRevision = syncRevision,
        studioProfile = StudioProfileJsonCodec.fromJsonString(studioProfileJson)
    )

    companion object {
        fun fromPlayerStats(
            uid: String,
            email: String,
            nickname: String,
            stats: PlayerStats,
            syncRevision: Long = stats.syncRevision
        ): UserAccountCloud {
            val cleanNick = nickname.trim().ifBlank { stats.nickname.trim() }
            val cloudSafeStudio = stats.studioProfile.copy(
                highwayConfig = stats.studioProfile.highwayConfig.copy(customImagePath = null)
            )
            return UserAccountCloud(
                id = uid,
                userId = uid,
                email = email.ifBlank { stats.email },
                nickname = cleanNick,
                nicknameNormalized = cleanNick.lowercase(),
                coins = stats.coins,
                totalScore = stats.totalScore,
                notesHitTotal = stats.notesHitTotal,
                maxStreakOverall = stats.maxStreakOverall,
                songsCompleted = stats.songsCompleted,
                unlockedStars = stats.unlockedStars,
                competitiveWins = stats.competitiveWins,
                competitiveMatches = stats.competitiveMatches,
                careerStageMax = stats.careerStageMax,
                selectedCharacterId = stats.selectedCharacterId,
                selectedGuitarId = stats.selectedGuitarId,
                selectedBassId = stats.selectedBassId,
                selectedDrumsId = stats.selectedDrumsId,
                selectedVocalMicId = stats.selectedVocalMicId,
                selectedHighwayThemeId = stats.selectedHighwayThemeId,
                unlockedCosmetics = (stats.unlockedCosmetics + DEFAULT_UNLOCKED_COSMETICS).distinct(),
                achievementsUnlocked = stats.achievementsUnlocked.distinct(),
                customRocker = CustomRockerCloud.fromAppearance(stats.customRocker),
                glassAdaptiveEnabled = stats.glassAdaptiveEnabled,
                audioLatencyOffsetMs = stats.audioLatencyOffsetMs,
                controlMode = stats.controlMode.name,
                graphicsQuality = stats.graphicsQuality.name,
                rewardStreak = stats.rewardStreak,
                lastRewardDayKey = stats.lastRewardDayKey,
                schemaVersion = stats.schemaVersion,
                syncRevision = syncRevision,
                studioProfileJson = StudioProfileJsonCodec.toJsonString(cloudSafeStudio).take(24576)
            )
        }
    }
}

data class DailyRewardClaimCloud(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val dayKey: String = "",
    val dayNumber: Int = 1,
    val streakCount: Int = 1,
    val rewardTitle: String = "",
    val rewardType: String = "coins",
    val coinsGranted: Int = 0,
    val cosmeticId: String = "",
    @ServerTimestamp
    val claimedAt: Timestamp? = null
)

data class ProgressionHistoryEntryCloud(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val trackId: String = "",
    val trackTitle: String = "",
    val instrument: String = "",
    val difficulty: String = "",
    val score: Int = 0,
    val stars: Int = 1,
    val accuracyPercent: Double = 0.0,
    val coinsEarned: Int = 0,
    @get:PropertyName("isCompetitive")
    @field:PropertyName("isCompetitive")
    val isCompetitive: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

data class DailyRewardDefinition(
    val dayNumber: Int,
    val title: String,
    val description: String,
    val rewardType: String, // "coins", "cosmetic", "skin", "bundle"
    val coinsGranted: Int,
    val cosmeticId: String = "",
    val isMilestone: Boolean = false
)

/**
 * Limite de moedas concedidas exclusivamente por resgates diários.
 * A semana segue ISO-8601 em UTC (segunda-feira a domingo), igual ao relógio diário já usado no app.
 */
object DailyRewardWeeklyLimit {
    const val MAX_COINS_PER_WEEK = 100

    fun weekKey(dayKey: String): String? = runCatching {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            isLenient = false
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(dayKey) ?: return null
        val calendar = java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("UTC"), java.util.Locale.US).apply {
            firstDayOfWeek = java.util.Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            time = date
        }
        java.lang.String.format(
            java.util.Locale.US,
            "%04d-W%02d",
            calendar.getWeekYear(),
            calendar.get(java.util.Calendar.WEEK_OF_YEAR)
        )
    }.getOrNull()

    fun grant(requestedCoins: Int, alreadyClaimedThisWeek: Int): Int {
        val remaining = (MAX_COINS_PER_WEEK - alreadyClaimedThisWeek.coerceAtLeast(0)).coerceAtLeast(0)
        return requestedCoins.coerceAtLeast(0).coerceAtMost(remaining)
    }
}

object DailyRewardCatalog {
    val cycleRewards: List<DailyRewardDefinition> = listOf(
        DailyRewardDefinition(1, "150 Moedas de Lenda", "Reforço inicial para sua turnê", "coins", 150),
        DailyRewardDefinition(2, "Jaqueta Chamas de Palco", "Roupa estilizada + 100 moedas", "cosmetic", 100, "outfit_chamas_palco"),
        DailyRewardDefinition(3, "250 Moedas de Lenda", "Cache de show intermediário", "coins", 250),
        DailyRewardDefinition(4, "Óculos Neon Holográficos", "Acessório exclusivo de palco", "cosmetic", 120, "acc_oculos_neon"),
        DailyRewardDefinition(5, "Moicano Turquesa Elétrico", "Penteado vibrante para customização", "cosmetic", 150, "hair_moicano_turquesa"),
        DailyRewardDefinition(6, "400 Moedas de Lenda", "Preparação para o grande festival", "coins", 400),
        DailyRewardDefinition(7, "Bônus Semanal: Kai Neo-Anime", "Personagem lendário + 600 moedas", "bundle", 600, "kai_cyber", isMilestone = true),
        DailyRewardDefinition(8, "200 Moedas de Lenda", "Ritmo constante na segunda semana", "coins", 200),
        DailyRewardDefinition(9, "Tatuagem Raio Carmesim", "Arte corporal para o seu roqueiro", "cosmetic", 150, "tattoo_raio_carmesim"),
        DailyRewardDefinition(10, "350 Moedas de Lenda", "Recompensa de dedicação diária", "coins", 350),
        DailyRewardDefinition(11, "Neon Cyber Strat", "Guitarra exclusiva c/ LEDs reativos", "skin", 200, "neon_thunder"),
        DailyRewardDefinition(12, "300 Moedas de Lenda", "Energia extra para a loja", "coins", 300),
        DailyRewardDefinition(13, "Cyber Wave Fretless", "Baixo holográfico desbloqueado", "skin", 200, "cyber_bass"),
        DailyRewardDefinition(14, "Bônus Quinzenal: Bateria Cyber", "Kit eletrônico + 800 moedas", "bundle", 800, "electronic_neon_kit", isMilestone = true),
        DailyRewardDefinition(15, "Microfone Riot Distress", "Item raro para Vocal Melódico", "skin", 250, "punk_distress_mic")
    )

    fun rewardForDay(dayNumber: Int): DailyRewardDefinition {
        val index = ((dayNumber - 1).coerceAtLeast(0)) % cycleRewards.size
        val template = cycleRewards[index]
        return template.copy(dayNumber = dayNumber.coerceIn(1, 30))
    }
}

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String
)

object AchievementCatalog {
    val all = listOf(
        AchievementItem("ach_first_show", "Estreia no Palco", "Concluiu sua primeira apresentação no Guitar Legends."),
        AchievementItem("ach_five_stars", "Show 5 Estrelas", "Alcançou 98% ou mais de precisão em uma música."),
        AchievementItem("ach_combo_30", "Mão de Aço", "Atingiu um combo contínuo de 30+ notas seguidas."),
        AchievementItem("ach_custom_import", "Acervo Próprio", "Importou uma música do aparelho para tocar no palco."),
        AchievementItem("ach_streak_3", "Turnê Fiel (3 Dias)", "Resgatou recompensas diárias por 3 dias consecutivos.")
    )
}

data class AppReleaseInfo(
    @DocumentId
    val id: String = "",
    val versionCode: Int = 1,
    val versionName: String = "1.0",
    val releaseTitle: String = "",
    val changelog: String = "",
    val bugFixes: String = "",
    val performanceNotes: String = "",
    @get:PropertyName("isMandatory")
    @field:PropertyName("isMandatory")
    val isMandatory: Boolean = false,
    val downloadUrl: String = "",
    val sha256Checksum: String = "",
    val minSupportedVersionCode: Int = 1,
    val publishedAt: Timestamp? = null,
    val releaseDateLabel: String = ""
)

object LocalReleaseCatalog {
    val history: List<AppReleaseInfo> = listOf(
        AppReleaseInfo(
            id = "v1_2_0",
            versionCode = 2,
            versionName = "1.2.0-Lenda",
            releaseTitle = "Edição Lendária — Contas, PCM Real & Recompensas",
            changelog = "• Contas individuais com nickname único e sincronização na nuvem\n• Calendário de recompensas diárias com validação temporal\n• Análise real de áudio PCM (RMS e transientes) + ajuste manual de BPM",
            bugFixes = "• Corrigido pausar/retomar da prévia musical\n• Corrigida validação de formatos e erro explícito se áudio falhar\n• Removido bloqueio indevido para áudios curtos legítimos (<15s)",
            performanceNotes = "• Gravação atômica do manifesto local e decodificação em thread dedicada\n• Adaptação dinâmica de sequência GLAS sem alterar velocidade do áudio",
            isMandatory = false,
            downloadUrl = "https://github.com/romulomarianoseverino-bit/GUITAR-LEGENDS-APP/releases",
            releaseDateLabel = "Edição Atual / Referência"
        ),
        AppReleaseInfo(
            id = "v1_1_0",
            versionCode = 1,
            versionName = "1.0",
            releaseTitle = "Revisão Consolidada — Biblioteca & GLAS",
            changelog = "• Persistência da biblioteca musical local\n• Sistema Adaptativo GLAS com limites por dificuldade\n• Customização expandida dos 4 instrumentos",
            bugFixes = "• Proteção das faixas embutidas contra exclusão\n• Isolamento de cópia privada em filesDir",
            performanceNotes = "• Renderização horizontal 60-90 FPS em Canvas Compose",
            isMandatory = false,
            downloadUrl = "https://github.com/romulomarianoseverino-bit/GUITAR-LEGENDS-APP/releases",
            releaseDateLabel = "Versão Instalada Base"
        )
    )
}
