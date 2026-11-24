package com.example.guitarlegends.model

enum class Instrument(
    val displayName: String,
    val subtitle: String,
    val iconName: String,
    val description: String,
    val primaryLaneFocus: String
) {
    GUITAR(
        displayName = "Guitarra Solo",
        subtitle = "Solo e Base Elétrica",
        iconName = "guitar",
        description = "Solos rápidos, riffs marcantes e acordes vibrantes.",
        primaryLaneFocus = "Agudos e Médios (Notas rápidas e solos)"
    ),
    ACOUSTIC_GUITAR(
        displayName = "Violão Acústico",
        subtitle = "Acordes e Base Acústica",
        iconName = "guitar",
        description = "Ritmos, dedilhados e acordes no violão acústico tradicional.",
        primaryLaneFocus = "Cordas médias e agudas (ritmo, acordes e dedilhados)"
    ),
    BASS(
        displayName = "Baixo Groove",
        subtitle = "Fundação de Graves",
        iconName = "bass",
        description = "Linhas melódicas graves, peso e sincronia rítmica com a bateria.",
        primaryLaneFocus = "Graves e Sub-graves (Padrões rítmicos consistentes)"
    ),
    DRUMS(
        displayName = "Bateria Furiosa",
        subtitle = "Ritmo e Percussão",
        iconName = "drums",
        description = "Bumbo, caixa, pratos e viradas intensas para ditar o ritmo da banda.",
        primaryLaneFocus = "Transientes percussivos e batidas por minuto"
    ),
    VOCALS(
        displayName = "Vocal Melódico",
        subtitle = "Guia Melódico Vocal",
        iconName = "mic",
        description = "Siga a melodia vocal da música por notas rítmicas sem precisar cantar no microfone.",
        primaryLaneFocus = "Frequências vocais médias e notas sustentadas"
    )
}

enum class Difficulty(
    val displayName: String,
    val lanesUsed: Int,
    val noteSpeedMultiplier: Float,
    val hitWindowMs: Long,
    val scoreMultiplier: Float,
    val description: String
) {
    EASY(
        displayName = "Fácil",
        lanesUsed = 3, // Verde, Vermelho, Amarelo
        noteSpeedMultiplier = 1.0f,
        hitWindowMs = 180L,
        scoreMultiplier = 1.0f,
        description = "Ideal para iniciantes. Apenas 3 botões e ritmo cadenciado."
    ),
    MEDIUM(
        displayName = "Médio",
        lanesUsed = 4, // Verde, Vermelho, Amarelo, Azul
        noteSpeedMultiplier = 1.25f,
        hitWindowMs = 140L,
        scoreMultiplier = 1.5f,
        description = "4 botões, mais notas consecutivas e acordes duplos ocasionais."
    ),
    HARD(
        displayName = "Difícil",
        lanesUsed = 5, // Todas as 5 pistas
        noteSpeedMultiplier = 1.55f,
        hitWindowMs = 100L,
        scoreMultiplier = 2.0f,
        description = "Todas as 5 pistas ativas, sequências velozes e acordes complexos."
    ),
    EXPERT(
        displayName = "Especialista",
        lanesUsed = 5,
        noteSpeedMultiplier = 1.95f,
        hitWindowMs = 70L,
        scoreMultiplier = 3.0f,
        description = "Desafio extremo estilo console clássico para verdadeiros deuses do rock."
    )
}

enum class HitRating(val label: String, val score: Int, val healthDelta: Float, val colorHex: Long) {
    PERFECT("PERFEITO!", 100, 0.05f, 0xFF00FF66),
    GREAT("EXCELENTE!", 75, 0.03f, 0xFF00E5FF),
    GOOD("BOM", 50, 0.01f, 0xFFFFCC00),
    MISS("ERROU!", 0, -0.09f, 0xFFFF2244)
}

enum class BpmConfidence(val label: String) {
    HIGH("Alta Precisão"),
    MEDIUM("Precisão Moderada"),
    LOW_UNRELIABLE("Estimativa Incerta"),
    MANUAL("Ajuste Manual")
}

enum class AudioClassification(val badgeLabel: String) {
    BUILT_IN_SYNTH("FAIXA ORIGINAL DO JOGO"),
    MUSIC_FULL("MÚSICA IMPORTADA"),
    SHORT_CLIP("CLIPE CURTO (<15s)"),
    UNVERIFIED_STREAM("ANÁLISE PARCIAL")
}

data class Note(
    val id: Long,
    val lane: Int, // 0..4 (Verde, Vermelho, Amarelo, Azul, Laranja)
    val timeMs: Long,
    val durationMs: Long = 0L,
    val isStarPower: Boolean = false,
    var isHit: Boolean = false,
    var isMissed: Boolean = false
)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val bpm: Double,
    val genre: String,
    val isBuiltIn: Boolean = true,
    val uriString: String? = null,
    val difficultyRatings: Map<Difficulty, Int> = mapOf(
        Difficulty.EASY to 2,
        Difficulty.MEDIUM to 4,
        Difficulty.HARD to 7,
        Difficulty.EXPERT to 9
    ),
    val bpmConfidence: BpmConfidence = BpmConfidence.HIGH,
    val bpmIsEstimated: Boolean = false,
    val manualBpmOverride: Double? = null,
    val onsetTimesMs: List<Long> = emptyList(),
    val isShortAudio: Boolean = false,
    val audioClassification: AudioClassification = if (isBuiltIn) AudioClassification.BUILT_IN_SYNTH else AudioClassification.MUSIC_FULL,
    val mimeType: String = "",
    val syncOffsetMs: Long = 0L
) {
    val effectiveBpm: Double
        get() = manualBpmOverride?.takeIf { it in 40.0..260.0 } ?: bpm.coerceIn(40.0, 260.0)
}
