package com.example.guitarlegends.analysis

/**
 * Calcula pontos incrementais de notas sustentadas sem premiar tempo não segurado.
 * O limite por quadro evita saltos de pontuação após travamentos ou retomadas.
 */
class SustainScoreAccumulator(
    private val pointsPerSecond: Float = 10f,
    private val maxFrameDeltaMs: Long = 40L
) {
    private var lastPositionMs: Long? = null
    private var fractionalPoints = 0f

    fun update(currentPositionMs: Long, isHeld: Boolean, multiplier: Int): Int {
        val previous = lastPositionMs
        lastPositionMs = currentPositionMs
        val deltaMs = previous?.let {
            (currentPositionMs - it).coerceIn(0L, maxFrameDeltaMs)
        } ?: 0L

        if (!isHeld) {
            fractionalPoints = 0f
            return 0
        }
        if (deltaMs <= 0L) return 0

        fractionalPoints += deltaMs * (pointsPerSecond / 1000f) * multiplier.coerceAtLeast(1)
        val earned = fractionalPoints.toInt()
        fractionalPoints -= earned
        return earned
    }

    fun reset() {
        lastPositionMs = null
        fractionalPoints = 0f
    }
}
