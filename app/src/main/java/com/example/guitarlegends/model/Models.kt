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
        subtitle = "Lead & Rhythm Shredder",
        iconName = "guitar",
        description = "Solos rápidos, riffs marcantes e acordes vibrantes.",
        primaryLaneFocus = "Agudos e Médios (Notas rápidas e solos)"
    ),
    BASS(
        displayName = "Baixo Groove",
        subtitle = "Low-end Foundation",
        iconName = "bass",
        description = "Linhas melódicas graves, peso e sincronia rítmica com a bateria.",
        primaryLaneFocus = "Graves e Sub-bass (Padrões rítmicos consistentes)"
    ),
    DRUMS(
        displayName = "Bateria Furiosa",
        subtitle = "Beat & Percussion Master",
        iconName = "drums",
        description = "Bumbo, caixa, pratos e viradas intensas para ditar o ritmo da banda.",
        primaryLaneFocus = "Transientes percussivos e batidas por minuto"
    ),
    VOCALS(
        displayName = "Vocal Melódico",
        subtitle = "Voice Rhythm Guide",
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
        displayName = "Expert",
        lanesUsed = 5,
        noteSpeedMultiplier = 1.95f,
        hitWindowMs = 70L,
        scoreMultiplier = 3.0f,
        description = "Desafio extremo estilo Guitar Hero clássico para verdadeiros deuses do rock."
    )
}

enum class HitRating(val label: String, val score: Int, val healthDelta: Float, val colorHex: Long) {
    PERFECT("PERFEITO!", 100, 0.05f, 0xFF00FF66),
    GREAT("EXCELENTE!", 75, 0.03f, 0xFF00E5FF),
    GOOD("BOM", 50, 0.01f, 0xFFFFCC00),
    MISS("ERROU!", 0, -0.09f, 0xFFFF2244)
}

data class Note(
    val id: Long,
    val lane: Int, // 0..4 (Green, Red, Yellow, Blue, Orange)
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
    )
)
