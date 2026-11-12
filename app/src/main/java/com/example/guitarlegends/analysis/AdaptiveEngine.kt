package com.example.guitarlegends.analysis

import com.example.guitarlegends.model.Difficulty
import com.example.guitarlegends.model.HitRating

/**
 * Guitar Legends Adaptive System (GLAS)
 * Evaluates real-time player performance and subtly adjusts dynamic tolerances,
 * multiplier assistance, and note density without modifying the song audio speed or original pitch.
 */
class AdaptiveEngine(
    val baseDifficulty: Difficulty,
    val isEnabled: Boolean = true
) {
    var consecutiveHits = 0
        private set
    var consecutiveMisses = 0
        private set
    var totalEvaluated = 0
        private set
    var perfectCount = 0
        private set

    // Adaptive scalar from -1.0 (Forgiving assist) to +1.0 (Peak mastery challenge)
    var adaptiveIndex: Float = 0.0f
        private set

    val statusText: String
        get() {
            if (!isEnabled) return "GLAS: Desativado"
            return when {
                adaptiveIndex > 0.45f -> "GLAS: Modo Virtuoso (+Desafio)"
                adaptiveIndex < -0.35f -> "GLAS: Assistência Dinâmica (+Janela)"
                else -> "GLAS: Desempenho Equilibrado"
            }
        }

    fun onNoteResult(rating: HitRating, deltaMs: Long) {
        if (!isEnabled) return
        totalEvaluated++

        when (rating) {
            HitRating.PERFECT -> {
                perfectCount++
                consecutiveHits++
                consecutiveMisses = 0
                // Gradually climb towards mastery if maintaining high accuracy
                if (consecutiveHits >= 15) {
                    adaptiveIndex = (adaptiveIndex + 0.04f).coerceAtMost(1.0f)
                }
            }
            HitRating.GREAT, HitRating.GOOD -> {
                consecutiveHits++
                consecutiveMisses = 0
                if (consecutiveHits >= 25) {
                    adaptiveIndex = (adaptiveIndex + 0.02f).coerceAtMost(0.6f)
                }
            }
            HitRating.MISS -> {
                consecutiveHits = 0
                consecutiveMisses++
                // If player is struggling with 3+ consecutive misses, gently expand hit window
                if (consecutiveMisses >= 3) {
                    adaptiveIndex = (adaptiveIndex - 0.08f).coerceAtMost(1.0f).coerceAtLeast(-1.0f)
                }
            }
        }
    }

    /**
     * Dynamically adjusts hit window ms based on GLAS index
     * Up to +35ms when struggling, or up to -15ms when on an insane streak
     */
    fun getEffectiveHitWindowMs(): Long {
        val base = baseDifficulty.hitWindowMs
        if (!isEnabled) return base
        val delta = (-adaptiveIndex * 25f).toLong()
        return (base + delta).coerceIn(45L, 220L)
    }

    /**
     * Multiplier bonus modifier
     */
    fun getScoreBoost(): Float {
        if (!isEnabled) return 1.0f
        return 1.0f + (adaptiveIndex * 0.15f).coerceAtLeast(0f)
    }

    fun reset() {
        consecutiveHits = 0
        consecutiveMisses = 0
        totalEvaluated = 0
        perfectCount = 0
        adaptiveIndex = 0.0f
    }
}
