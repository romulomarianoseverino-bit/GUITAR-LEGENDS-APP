package com.example.guitarlegends.analysis

import com.example.guitarlegends.model.Difficulty
import com.example.guitarlegends.model.HitRating
import com.example.guitarlegends.model.Instrument
import com.example.guitarlegends.model.Note
import kotlin.math.abs

/**
 * Guitar Legends Adaptive System (GLAS)
 * Avalia precisão, sequência de acertos/erros e consistência de tempo (ms) em tempo real:
 * - Calibra janelas de precisão e penalidades sem invadir outras dificuldades.
 * - Adapta densidade e complexidade das próximas notas futuras (> 2.2s à frente) mantendo a sincronia musical.
 * - Pode ser desativado integralmente para jogabilidade 100% estática.
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
    var sequenceAdjustmentsApplied = 0
        private set

    private val recentDeltasMs = ArrayDeque<Long>()
    private var lastSequenceAdaptationTimeMs: Long = -2500L

    // Escala adaptativa de -1.0f (Assistência Dinâmica) a +1.0f (Modo Virtuoso)
    var adaptiveIndex: Float = 0.0f
        private set

    // Limites rígidos de janela por dificuldade para impedir invasão entre dificuldades
    val minHitWindowMs: Long
    val maxHitWindowMs: Long

    init {
        when (baseDifficulty) {
            Difficulty.EASY -> {
                minHitWindowMs = 165L
                maxHitWindowMs = 240L
            }
            Difficulty.MEDIUM -> {
                minHitWindowMs = 125L
                maxHitWindowMs = 160L
            }
            Difficulty.HARD -> {
                minHitWindowMs = 85L
                maxHitWindowMs = 120L
            }
            Difficulty.EXPERT -> {
                minHitWindowMs = 55L
                maxHitWindowMs = 84L
            }
        }
    }

    val statusText: String
        get() {
            if (!isEnabled) return "GLAS: Desativado (${baseDifficulty.displayName})"
            return when {
                adaptiveIndex > 0.40f -> "GLAS [${baseDifficulty.displayName}]: Modo Virtuoso (+Desafio)"
                adaptiveIndex < -0.30f -> "GLAS [${baseDifficulty.displayName}]: Assistência Dinâmica"
                else -> "GLAS [${baseDifficulty.displayName}]: Ritmo Equilibrado"
            }
        }

    fun getAverageReactionDeltaMs(): Long {
        if (recentDeltasMs.isEmpty()) return 0L
        return recentDeltasMs.sum() / recentDeltasMs.size
    }

    fun onNoteResult(rating: HitRating, deltaMs: Long) {
        if (!isEnabled) return
        totalEvaluated++

        if (rating != HitRating.MISS) {
            recentDeltasMs.addLast(abs(deltaMs))
            while (recentDeltasMs.size > 16) recentDeltasMs.removeFirst()
        }

        when (rating) {
            HitRating.PERFECT -> {
                perfectCount++
                consecutiveHits++
                consecutiveMisses = 0
                val consistencyBonus = if (getAverageReactionDeltaMs() in 1L..28L) 0.01f else 0f
                val step = (if (baseDifficulty == Difficulty.EASY) 0.02f else 0.035f) + consistencyBonus
                if (consecutiveHits >= 8) {
                    adaptiveIndex = (adaptiveIndex + step).coerceAtMost(1.0f)
                }
            }
            HitRating.GREAT, HitRating.GOOD -> {
                consecutiveHits++
                consecutiveMisses = 0
                val step = if (baseDifficulty == Difficulty.EASY) 0.01f else 0.018f
                if (consecutiveHits >= 15) {
                    adaptiveIndex = (adaptiveIndex + step).coerceAtMost(0.55f)
                }
            }
            HitRating.MISS -> {
                consecutiveHits = 0
                consecutiveMisses++
                val missThreshold = if (baseDifficulty == Difficulty.EASY) 2 else 3
                val dropStep = if (baseDifficulty == Difficulty.EASY) 0.15f else 0.09f
                if (consecutiveMisses >= missThreshold) {
                    adaptiveIndex = (adaptiveIndex - dropStep).coerceIn(-1.0f, 1.0f)
                }
            }
        }
    }

    /**
     * Adapta dinamicamente as notas futuras ainda fora da área visível da esteira (`> currentTimeMs + 2200ms`)
     * respeitando estritamente a dificuldade selecionada e preservando o tempo real da música.
     */
    fun adaptUpcomingNotes(
        currentNotes: List<Note>,
        currentTimeMs: Long,
        instrument: Instrument
    ): List<Note> {
        if (!isEnabled) return currentNotes
        if (currentTimeMs - lastSequenceAdaptationTimeMs < 2400L) return currentNotes

        val futureThresholdMs = currentTimeMs + 2200L
        val horizonWindowEndMs = futureThresholdMs + 8000L
        val maxLane = baseDifficulty.lanesUsed - 1

        // Modo Assistência: suaviza acordes e sequências muito próximas sem remover o pulso principal
        if (adaptiveIndex <= -0.35f) {
            lastSequenceAdaptationTimeMs = currentTimeMs
            val result = mutableListOf<Note>()
            var lastKeptFutureTime = -10_000L
            val minProtectedGap = when (baseDifficulty) {
                Difficulty.EASY -> 520L
                Difficulty.MEDIUM -> 360L
                Difficulty.HARD -> 250L
                Difficulty.EXPERT -> 190L
            }

            for (note in currentNotes) {
                if (note.isHit || note.isMissed || note.timeMs <= futureThresholdMs || note.timeMs > horizonWindowEndMs) {
                    result.add(note)
                    continue
                }
                // Remove acordes duplos simultâneos ou notas rápidas redundantes no horizonte próximo
                if (note.timeMs == lastKeptFutureTime || (note.timeMs - lastKeptFutureTime) < minProtectedGap) {
                    continue
                }
                result.add(note)
                lastKeptFutureTime = note.timeMs
            }
            if (result.size != currentNotes.size) sequenceAdjustmentsApplied++
            return result
        }

        // Modo Virtuoso: adiciona acento harmônico ocasional dentro do limite de pistas da dificuldade
        if (adaptiveIndex >= 0.50f && baseDifficulty != Difficulty.EASY && maxLane >= 2) {
            lastSequenceAdaptationTimeMs = currentTimeMs
            val existingTimestamps = currentNotes.groupBy { it.timeMs }
            val maxId = (currentNotes.maxOfOrNull { it.id } ?: 0L) + 1L
            var nextId = maxId
            val additions = mutableListOf<Note>()
            var countAdded = 0

            for (note in currentNotes) {
                if (countAdded >= 4) break
                if (note.isHit || note.isMissed || note.timeMs <= futureThresholdMs || note.timeMs > horizonWindowEndMs) continue
                val notesAtSameTime = existingTimestamps[note.timeMs].orEmpty()
                if (notesAtSameTime.size == 1 && note.id % 5L == 0L && instrument != Instrument.VOCALS) {
                    val harmonyLane = ((note.lane + 2) % baseDifficulty.lanesUsed).coerceIn(0, maxLane)
                    if (harmonyLane != note.lane) {
                        additions.add(
                            Note(
                                id = nextId++,
                                lane = harmonyLane,
                                timeMs = note.timeMs,
                                durationMs = note.durationMs,
                                isStarPower = note.isStarPower
                            )
                        )
                        countAdded++
                    }
                }
            }
            if (additions.isNotEmpty()) {
                sequenceAdjustmentsApplied++
                return (currentNotes + additions).sortedBy { it.timeMs }
            }
        }

        return currentNotes
    }

    fun getEffectiveHitWindowMs(): Long {
        val base = baseDifficulty.hitWindowMs
        if (!isEnabled) return base
        val delta = (-adaptiveIndex * (if (baseDifficulty == Difficulty.EASY) 25f else 20f)).toLong()
        return (base + delta).coerceIn(minHitWindowMs, maxHitWindowMs)
    }

    fun getMissPenalty(): Float {
        if (!isEnabled) return 0.08f
        return when (baseDifficulty) {
            Difficulty.EASY -> 0.04f
            Difficulty.MEDIUM -> 0.065f
            Difficulty.HARD -> 0.08f
            Difficulty.EXPERT -> 0.095f
        }
    }

    fun getScoreBoost(): Float {
        if (!isEnabled) return 1.0f
        return 1.0f + (adaptiveIndex * 0.12f).coerceAtLeast(0f)
    }

    fun reset() {
        consecutiveHits = 0
        consecutiveMisses = 0
        totalEvaluated = 0
        perfectCount = 0
        sequenceAdjustmentsApplied = 0
        lastSequenceAdaptationTimeMs = -2500L
        recentDeltasMs.clear()
        adaptiveIndex = 0.0f
    }
}
