package com.example.guitarlegends.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.guitarlegends.model.Difficulty
import com.example.guitarlegends.model.Note
import kotlin.math.pow
import kotlin.random.Random

// Lane color palette (Green, Red, Yellow, Blue, Orange)
val LaneColors = listOf(
    Color(0xFF00E676), // 0: Green
    Color(0xFFFF1744), // 1: Red
    Color(0xFFFFEA00), // 2: Yellow
    Color(0xFF2979FF), // 3: Blue
    Color(0xFFFF9100)  // 4: Orange
)

val LaneFretBgColors = listOf(
    Color(0x3300E676),
    Color(0x33FF1744),
    Color(0x33FFEA00),
    Color(0x332979FF),
    Color(0x33FF9100)
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float, // 1.0 down to 0.0
    val color: Color,
    val size: Float
)

class ParticleSystem {
    private val particles = mutableListOf<Particle>()
    private val random = Random(42)

    fun spawnBurst(centerX: Float, centerY: Float, color: Color, count: Int = 18) {
        for (i in 0 until count) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = random.nextFloat() * 12f + 4f
            particles.add(
                Particle(
                    x = centerX,
                    y = centerY,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - 3f, // upward bias
                    life = 1.0f,
                    color = color,
                    size = random.nextFloat() * 8f + 4f
                )
            )
        }
    }

    fun updateAndDraw(drawScope: DrawScope) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.4f // gravity
            p.life -= 0.05f

            if (p.life <= 0f) {
                iter.remove()
            } else {
                drawScope.drawCircle(
                    color = p.color.copy(alpha = p.life),
                    radius = p.size * p.life,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}

/**
 * High-performance 3D Perspective Note Highway Painter
 */
fun drawHighway(
    drawScope: DrawScope,
    canvasWidth: Float,
    canvasHeight: Float,
    currentTimeMs: Long,
    notes: List<Note>,
    difficulty: Difficulty,
    activeFretLanes: Set<Int>,
    isStarPowerActive: Boolean,
    particleSystem: ParticleSystem
) {
    val numLanes = difficulty.lanesUsed
    val horizonY = canvasHeight * 0.16f
    val strikeY = canvasHeight * 0.82f

    // 3D Perspective highway trapezoid widths
    val topWidth = (canvasWidth * 0.32f).coerceAtLeast(180f)
    val bottomWidth = (canvasWidth * 0.78f).coerceAtLeast(420f)

    val centerX = canvasWidth * 0.5f

    val topHalfW = topWidth * 0.5f
    val bottomHalfW = bottomWidth * 0.5f

    val highwayTopLeft = Offset(centerX - topHalfW, horizonY)
    val highwayTopRight = Offset(centerX + topHalfW, horizonY)
    val highwayBottomRight = Offset(centerX + bottomHalfW, strikeY + 50f)
    val highwayBottomLeft = Offset(centerX - bottomHalfW, strikeY + 50f)

    // Highway bed background with dark concert metallic gradient
    val highwayPath = Path().apply {
        moveTo(highwayTopLeft.x, highwayTopLeft.y)
        lineTo(highwayTopRight.x, highwayTopRight.y)
        lineTo(highwayBottomRight.x, highwayBottomRight.y)
        lineTo(highwayBottomLeft.x, highwayBottomLeft.y)
        close()
    }

    val highwayBrush = if (isStarPowerActive) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D2538),
                Color(0xFF1A4971),
                Color(0xFF00B0FF).copy(alpha = 0.55f),
                Color(0xFF00E5FF).copy(alpha = 0.75f)
            ),
            startY = horizonY,
            endY = strikeY
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF120C1F),
                Color(0xFF1E1530),
                Color(0xFF281E3E),
                Color(0xFF1A122B)
            ),
            startY = horizonY,
            endY = strikeY
        )
    }

    drawScope.drawPath(highwayPath, brush = highwayBrush, style = Fill)

    // Neon edge rails
    val railGlowColor = if (isStarPowerActive) Color(0xFF00E5FF) else Color(0xFFFF5252)
    drawScope.drawLine(
        color = railGlowColor,
        start = highwayTopLeft,
        end = highwayBottomLeft,
        strokeWidth = 6f
    )
    drawScope.drawLine(
        color = railGlowColor,
        start = highwayTopRight,
        end = highwayBottomRight,
        strokeWidth = 6f
    )

    // Helper functions for 3D perspective mapping
    fun get3dPosition(lane: Int, depth01: Float): Offset {
        // depth01: 0.0 = at horizon, 1.0 = at strike line
        val curvedT = depth01.coerceIn(0f, 1f).toDouble().pow(1.6).toFloat()
        val currentY = horizonY + (strikeY - horizonY) * curvedT
        val currentWidth = topWidth + (bottomWidth - topWidth) * curvedT
        val startX = centerX - currentWidth * 0.5f
        val laneWidth = currentWidth / numLanes
        val laneCenterX = startX + laneWidth * (lane + 0.5f)
        return Offset(laneCenterX, currentY)
    }

    // Lane divider lines
    for (i in 1 until numLanes) {
        val topLaneX = (centerX - topHalfW) + (topWidth / numLanes) * i
        val bottomLaneX = (centerX - bottomHalfW) + (bottomWidth / numLanes) * i
        drawScope.drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(topLaneX, horizonY),
            end = Offset(bottomLaneX, strikeY + 40f),
            strokeWidth = 2f
        )
    }

    // Moving rhythmic fret bars along the highway
    val beatDistanceMs = 500L
    val lookAheadMs = (1600L / difficulty.noteSpeedMultiplier).toLong()
    val firstBeat = ((currentTimeMs / beatDistanceMs) * beatDistanceMs)
    for (b in 0..5) {
        val beatTime = firstBeat + b * beatDistanceMs
        val timeUntilStrike = beatTime - currentTimeMs
        if (timeUntilStrike in 0L..lookAheadMs) {
            val depth = 1.0f - (timeUntilStrike.toFloat() / lookAheadMs.toFloat())
            val y = horizonY + (strikeY - horizonY) * depth.toDouble().pow(1.6).toFloat()
            val w = topWidth + (bottomWidth - topWidth) * depth.toDouble().pow(1.6).toFloat()
            drawScope.drawLine(
                color = Color.White.copy(alpha = (depth * 0.25f)),
                start = Offset(centerX - w * 0.5f, y),
                end = Offset(centerX + w * 0.5f, y),
                strokeWidth = 3f * depth
            )
        }
    }

    // Strike Zone Base Line
    drawScope.drawLine(
        color = Color(0xFFFFFFFF),
        start = Offset(centerX - bottomHalfW, strikeY),
        end = Offset(centerX + bottomHalfW, strikeY),
        strokeWidth = 4f
    )

    // Draw Target Strike Rings
    for (lane in 0 until numLanes) {
        val targetPos = get3dPosition(lane, 1.0f)
        val color = LaneColors[lane]
        val isPressed = activeFretLanes.contains(lane)
        val radius = (bottomWidth / numLanes) * 0.38f

        // Pressed glow background
        if (isPressed) {
            drawScope.drawCircle(
                color = color.copy(alpha = 0.45f),
                radius = radius * 1.35f,
                center = targetPos
            )
        }

        // Target Ring
        drawScope.drawCircle(
            color = if (isPressed) Color.White else color,
            radius = radius,
            center = targetPos,
            style = Stroke(width = if (isPressed) 7f else 4f)
        )
        // Center jewel
        drawScope.drawCircle(
            color = if (isPressed) color else color.copy(alpha = 0.35f),
            radius = radius * 0.45f,
            center = targetPos,
            style = Fill
        )
    }

    // Draw Notes and Sustains
    for (note in notes) {
        if (note.isHit || note.isMissed) continue
        if (note.lane >= numLanes) continue

        val timeUntilHit = note.timeMs - currentTimeMs
        // If note is within horizon -> strike view window
        if (timeUntilHit in -250L..lookAheadMs) {
            val depth = (1.0f - (timeUntilHit.toFloat() / lookAheadMs.toFloat())).coerceIn(0f, 1.15f)
            val notePos = get3dPosition(note.lane, depth)
            val laneW = (topWidth + (bottomWidth - topWidth) * depth.toDouble().pow(1.6).toFloat()) / numLanes
            val noteRadius = (laneW * 0.34f).coerceIn(6f, 38f)

            // Draw sustain tail if note is held
            if (note.durationMs > 50L) {
                val tailEndDepth = (1.0f - ((timeUntilHit + note.durationMs).toFloat() / lookAheadMs.toFloat())).coerceIn(0f, 1.15f)
                val tailEndPos = get3dPosition(note.lane, tailEndDepth)
                val sustainColor = if (note.isStarPower) Color(0xFF80D8FF) else LaneColors[note.lane]

                drawScope.drawLine(
                    color = sustainColor.copy(alpha = 0.75f),
                    start = notePos,
                    end = tailEndPos,
                    strokeWidth = (noteRadius * 0.5f).coerceAtLeast(3f)
                )
            }

            // Note Gem
            val gemColor = if (note.isStarPower) Color(0xFFE0F7FA) else LaneColors[note.lane]
            val innerColor = if (note.isStarPower) Color(0xFF00E5FF) else Color.White

            // Outer 3D Gem Body
            drawScope.drawCircle(
                color = gemColor,
                radius = noteRadius,
                center = notePos,
                style = Fill
            )

            // Inner Highlight (3D sphere effect)
            drawScope.drawCircle(
                color = innerColor,
                radius = noteRadius * 0.5f,
                center = Offset(notePos.x, notePos.y - noteRadius * 0.25f),
                style = Fill
            )

            // Dark rim for contrast
            drawScope.drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = noteRadius,
                center = notePos,
                style = Stroke(width = 2.5f)
            )
        }
    }

    // Update and draw active particle sparks
    particleSystem.updateAndDraw(drawScope)
}
