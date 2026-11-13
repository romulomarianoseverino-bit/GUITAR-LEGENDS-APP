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
    val style: String, // "Classic Rock", "Punk Riot", "Heavy Metal", "Anime Neo-Rock"
    val outfitName: String,
    val priceCoins: Int,
    val isUnlocked: Boolean,
    val bio: String
)

data class GuitarGear(
    val id: String,
    val name: String,
    val type: String, // Strat, Flying V, Les Paul, Bass Thunder
    val colorHex: Long,
    val priceCoins: Int,
    val isUnlocked: Boolean,
    val description: String
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
    val unlockedCharacterIds: Set<String> = setOf("axel_rocker", "roxie_riot"),
    val unlockedGuitarIds: Set<String> = setOf("flame_caster", "void_v"),
    val glassAdaptiveEnabled: Boolean = true,
    val audioLatencyOffsetMs: Long = 0L,
    val controlMode: ControlMode = ControlMode.DIRECT_TOUCH,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH
)

enum class ControlMode(val title: String, val subtitle: String) {
    DIRECT_TOUCH(
        "Toque Direto nas Trilhas",
        "Toque diretamente nos 5 botões de traste na zona de impacto (ideal para telas touch)"
    ),
    FRET_AND_STRUM(
        "Traste + Palhetada (Clássico)",
        "Segure as notas com a mão esquerda e palhete com a mão direita (estilo controle Guitar Hero)"
    )
}

enum class GraphicsQuality(val label: String, val fpsTarget: Int) {
    HIGH("Alto (90Hz / Efeitos Completos)", 90),
    BALANCED("Equilibrado (60Hz / Otimizado A15)", 60),
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
    val stars: Int, // 1..5
    val coinsEarned: Int,
    val isAdaptiveUsed: Boolean
)
