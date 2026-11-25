package com.example.guitarlegends.model

data class CareerVenue(
    val id: String,
    val name: String,
    val location: String,
    val requiredStars: Int,
    val rewardCoins: Int,
    val description: String,
    val trackIds: List<String>,
    val isUnlocked: Boolean = false
)

data class CharacterCustomization(
    val id: String,
    val name: String,
    val style: String,
    val outfitName: String,
    val priceCoins: Int,
    val isUnlocked: Boolean,
    val bio: String
)

data class GuitarGear(
    val id: String,
    val name: String,
    val type: String,
    val colorHex: Long,
    val priceCoins: Int,
    val isUnlocked: Boolean,
    val description: String
)

enum class CharacterCreationMethod(val label: String, val description: String) {
    INSPIRED_BY_PHOTO("Inspirado na Fotografia", "Utiliza a foto como referência artística estilizada, gerando um roqueiro autêntico"),
    PRESERVE_FACIAL_FEATURES("Preservar Traços Faciais", "Procura preservar ao máximo a fisionomia, olhos e contornos da foto adaptando ao modelo 3D")
}

enum class CharacterArtStyle(val displayName: String, val eraDescription: String, val badgeColorHex: Long) {
    STYLIZED_REALISTIC("3D Estilizado", "Estética PlayStation 2 / clássico dos anos 2000", 0xFFFFD700),
    CARTOON("3D Cartoon", "Estilo HQ e animação anos 2000 marcante", 0xFFFF4081),
    ANIME("3D Anime", "Estilo visual vibrante com traços expressivos", 0xFF00E5FF)
}

data class CustomRockerAppearance(
    val photoUri: String? = null,
    val creationMethod: CharacterCreationMethod = CharacterCreationMethod.INSPIRED_BY_PHOTO,
    val artStyle: CharacterArtStyle = CharacterArtStyle.STYLIZED_REALISTIC,
    val hairStyle: String = "Moicano Punk Clássico",
    val hairColorHex: Long = 0xFFFF5722,
    val topClothing: String = "Jaqueta de Couro c/ Rebites",
    val pantsClothing: String = "Calça Jeans Rasgada",
    val shoesClothing: String = "Coturnos de Couro Pesado",
    val accessories: String = "Munhequeiras de Espinhos & Óculos Escuros Aviador",
    val bodyTattoos: String = "Tatuagem Tribal e Chamas nos Braços"
)

enum class HighwayTheme(
    val id: String,
    val title: String,
    val description: String,
    val primaryColorHex: Long,
    val surfaceGradientHex: List<Long>
) {
    PS2_WOOD_STEEL(
        "theme_ps2_wood",
        "Clássico PS2 (Madeira & Aço)",
        "Visual nostálgico dos anos 2000, madeira polida, trilhos cromados e chamas sutis",
        0xFFFF8F00,
        listOf(0xFF1E1428, 0xFF2A1C38, 0xFF35254A, 0xFF1C1329)
    ),
    NEON_CYBER_OVERDRIVE(
        "theme_neon_cyber",
        "Neon Cyber Overdrive",
        "Pistas com circuitos luminosos ciano e roxo fluorescente com rastro de plasma",
        0xFF00E5FF,
        listOf(0xFF0A192F, 0xFF102A4C, 0xFF173D68, 0xFF0D213B)
    ),
    FIRE_AND_BRIMSTONE(
        "theme_inferno_flame",
        "Chamas do Inferno Rock",
        "Esteira vulcânica com brasas incandescentes, fumaça preta e bordas de magma",
        0xFFFF1744,
        listOf(0xFF2A0808, 0xFF450D0D, 0xFF631414, 0xFF2F0A0A)
    ),
    GALACTIC_STAR(
        "theme_galactic_star",
        "Galáxia Estelar 90Hz",
        "Nebulosas púrpuras, estrelas cintilantes e reflexos cósmicos para telas AMOLED",
        0xFF7C4DFF,
        listOf(0xFF120A2A, 0xFF1E1045, 0xFF2F1A6B, 0xFF150C30)
    )
}

data class InstrumentGear(
    val id: String,
    val instrumentType: Instrument,
    val name: String,
    val modelStyle: String,
    val colorHex: Long,
    val priceCoins: Int,
    val isUnlocked: Boolean,
    val description: String
)

val DEFAULT_UNLOCKED_COSMETICS = listOf(
    "axel_rocker",
    "roxie_riot",
    "flame_caster",
    "void_v",
    "violao_nylon_classico",
    "violao_aco_folk",
    "thunder_bass",
    "skull_bass",
    "arena_beast_kit",
    "punk_garage_kit",
    "vintage_shure_55s",
    "golden_supreme_mic",
    "theme_ps2_wood",
    "theme_neon_cyber",
    "theme_inferno_flame",
    "theme_galactic_star",
    "outfit_skin_none",
    "outfit_skin_ps2_legend",
    "skin_none",
    "stamp_none",
    "stamp_ps2_flames",
    "stamp_checker_punk"
)

data class PlayerStats(
    val coins: Int = 450,
    val totalScore: Long = 0L,
    val notesHitTotal: Long = 0L,
    val maxStreakOverall: Int = 0,
    val songsCompleted: Int = 0,
    val unlockedStars: Int = 6,
    val selectedCharacterId: String = "axel_rocker",
    val selectedGuitarId: String = "flame_caster",
    val selectedBassId: String = "thunder_bass",
    val selectedDrumsId: String = "arena_beast_kit",
    val selectedVocalMicId: String = "vintage_shure_55s",
    val selectedHighwayThemeId: String = "theme_ps2_wood",
    val customRocker: CustomRockerAppearance = CustomRockerAppearance(),
    val glassAdaptiveEnabled: Boolean = true,
    val audioLatencyOffsetMs: Long = 0L,
    val controlMode: ControlMode = ControlMode.DIRECT_TOUCH,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val nickname: String = "",
    val email: String = "",
    val unlockedCosmetics: List<String> = DEFAULT_UNLOCKED_COSMETICS,
    val achievementsUnlocked: List<String> = emptyList(),
    val competitiveWins: Int = 0,
    val competitiveMatches: Int = 0,
    val careerStageMax: Int = 1,
    val rewardStreak: Int = 0,
    val lastRewardDayKey: String = "",
    val schemaVersion: Int = 3,
    val syncRevision: Long = 1L,
    val studioProfile: StudioProfileConfig = StudioProfileConfig()
)

enum class ControlMode(val title: String, val subtitle: String) {
    DIRECT_TOUCH(
        "Toque Direto nas Trilhas",
        "Toque diretamente nos 5 botões de traste na zona de impacto (ideal para telas touch)"
    ),
    FRET_AND_STRUM(
        "Traste + Palhetada (Clássico)",
        "Segure as notas com a mão esquerda e palhete com a mão direita (estilo controle clássico)"
    )
}

enum class GraphicsQuality(val label: String, val fpsTarget: Int) {
    HIGH("Alto (90Hz / Efeitos Completos)", 90),
    BALANCED("Equilibrado (60Hz / Otimizado)", 60),
    BATTERY_SAVER("Econômico (Efeitos Leves)", 45)
}

data class MatchResult(
    val track: Track,
    val instrument: Instrument,
    val difficulty: Difficulty,
    val score: Int,
    val maxCombo: Int,
    val totalNotes: Int,
    val hitNotes: Int,
    val missedNotes: Int,
    val accuracyPercent: Float,
    val stars: Int,
    val coinsEarned: Int,
    val isAdaptiveUsed: Boolean,
    val isCompetitive: Boolean = false
)
