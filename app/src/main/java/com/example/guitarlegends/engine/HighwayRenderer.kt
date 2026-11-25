package com.example.guitarlegends.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.guitarlegends.model.*
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
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
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 3f,
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
            p.vy += 0.4f
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

data class HighwayLayoutGeometry(
    val canvasWidth: Float,
    val canvasHeight: Float,
    val numLanes: Int,
    val centerX: Float,
    val horizonY: Float,
    val strikeY: Float,
    val topWidth: Float,
    val bottomWidth: Float,
    val highwayBottomY: Float,
    val receptorRx: Float,
    val receptorRy: Float
) {
    val topHalfW: Float get() = topWidth * 0.5f
    val bottomHalfW: Float get() = bottomWidth * 0.5f
    val laneWidthAtStrike: Float get() = bottomWidth / numLanes.coerceAtLeast(1)

    fun pointAt(lane: Int, depth01: Float): Offset {
        val curvedT = depth01.coerceIn(0f, 1.15f).toDouble().pow(1.65).toFloat()
        val currentY = horizonY + (strikeY - horizonY) * curvedT
        val currentWidth = topWidth + (bottomWidth - topWidth) * curvedT
        val startX = centerX - currentWidth * 0.5f
        val laneWidth = currentWidth / numLanes.coerceAtLeast(1)
        val laneCenterX = startX + laneWidth * (lane + 0.5f)
        return Offset(laneCenterX, currentY)
    }

    fun laneCenterXAtStrike(lane: Int): Float =
        (centerX - bottomHalfW) + laneWidthAtStrike * (lane + 0.5f)

    fun laneDividerTopX(dividerIndex: Int): Float =
        (centerX - topHalfW) + (topWidth / numLanes.coerceAtLeast(1)) * dividerIndex

    fun laneDividerBottomX(dividerIndex: Int): Float =
        (centerX - bottomHalfW) + laneWidthAtStrike * dividerIndex
}

fun computeHighwayGeometry(
    canvasWidth: Float,
    canvasHeight: Float,
    numLanes: Int
): HighwayLayoutGeometry {
    val safeW = canvasWidth.coerceAtLeast(1f)
    val safeH = canvasHeight.coerceAtLeast(1f)
    val safeLanes = numLanes.coerceIn(1, 5)
    val isPortrait = safeH > safeW

    val centerX = safeW * 0.50f
    val horizonY = if (isPortrait) safeH * 0.22f else safeH * 0.14f
    val strikeY = if (isPortrait) safeH * 0.84f else safeH * 0.82f

    val bottomWidth = if (isPortrait) {
        (safeW * 0.86f).coerceIn(safeW * 0.72f, safeW * 0.92f)
    } else {
        (safeW * 0.37f).coerceIn(safeW * 0.33f, safeW * 0.42f)
    }
    val topWidth = if (isPortrait) {
        bottomWidth * 0.42f
    } else {
        (safeW * 0.155f).coerceIn(bottomWidth * 0.38f, bottomWidth * 0.48f)
    }

    val laneW = bottomWidth / safeLanes
    val receptorRx = (laneW * 0.41f).coerceAtMost(safeH * 0.10f)
    val receptorRy = receptorRx * 0.74f
    val highwayBottomY = (strikeY + receptorRy * 1.45f).coerceAtMost(safeH - 4f)

    return HighwayLayoutGeometry(
        canvasWidth = safeW,
        canvasHeight = safeH,
        numLanes = safeLanes,
        centerX = centerX,
        horizonY = horizonY,
        strikeY = strikeY,
        topWidth = topWidth,
        bottomWidth = bottomWidth,
        highwayBottomY = highwayBottomY,
        receptorRx = receptorRx,
        receptorRy = receptorRy
    )
}

/**
 * Renderizador de Palco 3D e Esteira de Notas Estilo Console Clássico Modernizado.
 * - Perspectiva fixa clássica centralizada e proporcional (sistema único de coordenadas).
 * - Cenários temáticos de show com banda completa (guitarrista customizado, baterista e baixista).
 * - Notas 3D chanfradas, alvos únicos alinhados à strikeline, notas estrela Star Power e chamas de impacto.
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
    particleSystem: ParticleSystem,
    highwayTheme: HighwayTheme = HighwayTheme.PS2_WOOD_STEEL,
    rockMeter: Float = 0.6f,
    combo: Int = 0,
    instrument: Instrument = Instrument.GUITAR,
    playerStats: PlayerStats = PlayerStats(),
    highwayOverlayBitmap: ImageBitmap? = null,
    overlayOpacity: Float = 0.55f
) {
    val numLanes = difficulty.lanesUsed
    val geometry = computeHighwayGeometry(canvasWidth, canvasHeight, numLanes)
    val horizonY = geometry.horizonY
    val strikeY = geometry.strikeY
    val centerX = geometry.centerX
    val topWidth = geometry.topWidth
    val bottomWidth = geometry.bottomWidth
    val topHalfW = geometry.topHalfW
    val bottomHalfW = geometry.bottomHalfW
    val highwayBottomY = geometry.highwayBottomY
    val timeSec = currentTimeMs / 1000f

    // 1. DESENHO DO PALCO DE SHOW TEMÁTICO COM BANDA COMPLETA AO REDOR DA ESTEIRA
    drawStageEnvironment(
        drawScope = drawScope,
        width = canvasWidth,
        height = canvasHeight,
        currentTimeMs = currentTimeMs,
        isStarPower = isStarPowerActive,
        rockMeter = rockMeter,
        combo = combo,
        instrument = instrument,
        playerStats = playerStats,
        highwayTheme = highwayTheme,
        horizonY = horizonY,
        centerX = centerX
    )

    // 2. ESTEIRA DE NOTAS CLÁSSICA COM PERSPECTIVA FIXA CENTRALIZADA
    val highwayTopLeft = Offset(centerX - topHalfW, horizonY)
    val highwayTopRight = Offset(centerX + topHalfW, horizonY)
    val highwayBottomRight = Offset(centerX + bottomHalfW, highwayBottomY)
    val highwayBottomLeft = Offset(centerX - bottomHalfW, highwayBottomY)

    val highwayPath = Path().apply {
        moveTo(highwayTopLeft.x, highwayTopLeft.y)
        lineTo(highwayTopRight.x, highwayTopRight.y)
        lineTo(highwayBottomRight.x, highwayBottomRight.y)
        lineTo(highwayBottomLeft.x, highwayBottomLeft.y)
        close()
    }

    val themeColors = highwayTheme.surfaceGradientHex.map { Color(it) }
    val highwayBrush = if (isStarPowerActive) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF071A2F),
                Color(0xFF103F68),
                Color(0xFF00B0FF).copy(alpha = 0.78f),
                Color(0xFF00E5FF).copy(alpha = 0.90f)
            ),
            startY = horizonY,
            endY = highwayBottomY
        )
    } else {
        Brush.verticalGradient(
            colors = themeColors,
            startY = horizonY,
            endY = highwayBottomY
        )
    }

    drawScope.drawPath(highwayPath, brush = highwayBrush, style = Fill)

    // Estampa ou imagem customizada aplicada apenas dentro da esteira fixa
    if (highwayOverlayBitmap != null) {
        drawScope.clipPath(highwayPath) {
            val destLeft = (centerX - bottomHalfW).toInt()
            val destTop = horizonY.toInt()
            val destW = bottomWidth.toInt().coerceAtLeast(1)
            val destH = (highwayBottomY - horizonY).toInt().coerceAtLeast(1)
            drawImage(
                image = highwayOverlayBitmap,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(highwayOverlayBitmap.width, highwayOverlayBitmap.height),
                dstOffset = IntOffset(destLeft, destTop),
                dstSize = IntSize(destW, destH),
                alpha = overlayOpacity.coerceIn(0.15f, 0.90f)
            )
            // Camada sutil para preservar legibilidade total das notas coloridas
            drawRect(
                color = Color.Black.copy(alpha = 0.28f),
                topLeft = Offset(destLeft.toFloat(), destTop.toFloat()),
                size = Size(destW.toFloat(), destH.toFloat())
            )
        }
    }

    // Revestimento da pista: micro-riscos de aço escovado e divisões de traste em perspectiva
    drawScope.clipPath(highwayPath) {
        for (grain in 0..22) {
            val frac = grain / 22f
            val x = centerX - bottomHalfW + frac * bottomWidth
            val topX = centerX - topHalfW + frac * topWidth
            drawLine(
                color = Color.White.copy(alpha = if (grain % 2 == 0) 0.035f else 0.018f),
                start = Offset(topX, horizonY + 3f),
                end = Offset(x, highwayBottomY - 2f),
                strokeWidth = 1f
            )
        }
        for (fret in 1..5) {
            val depth = fret / 6f
            val curved = depth.toDouble().pow(1.65).toFloat()
            val fy = horizonY + (strikeY - horizonY) * curved
            val fw = topWidth + (bottomWidth - topWidth) * curved
            drawLine(
                color = Color(0xFF8C8794).copy(alpha = 0.22f),
                start = Offset(centerX - fw * 0.5f, fy),
                end = Offset(centerX + fw * 0.5f, fy),
                strokeWidth = 2.0f + curved * 2.2f
            )
        }
    }

    // Trilhos Laterais em Aço Cromado / Neon com fileiras de LEDs Star Power nas bordas
    val railColor = if (isStarPowerActive) Color(0xFF00E5FF) else Color(highwayTheme.primaryColorHex)
    drawScope.drawLine(color = Color(0xFF14101A), start = highwayTopLeft, end = highwayBottomLeft, strokeWidth = 12f)
    drawScope.drawLine(color = Color(0xFF14101A), start = highwayTopRight, end = highwayBottomRight, strokeWidth = 12f)
    drawScope.drawLine(color = railColor, start = highwayTopLeft, end = highwayBottomLeft, strokeWidth = 6.5f)
    drawScope.drawLine(color = railColor, start = highwayTopRight, end = highwayBottomRight, strokeWidth = 6.5f)
    drawScope.drawLine(color = Color.White.copy(alpha = 0.6f), start = highwayTopLeft, end = highwayBottomLeft, strokeWidth = 1.8f)
    drawScope.drawLine(color = Color.White.copy(alpha = 0.6f), start = highwayTopRight, end = highwayBottomRight, strokeWidth = 1.8f)

    // Pinos/LEDs laterais nos trilhos (pulsam em ciano durante Star Power)
    for (led in 1..8) {
        val t = led / 9f
        val curved = t.toDouble().pow(1.65).toFloat()
        val ly = horizonY + (strikeY - horizonY) * curved
        val halfW = (topWidth + (bottomWidth - topWidth) * curved) * 0.5f + 2f
        val ledColor = if (isStarPowerActive) Color(0xFF80D8FF) else Color(0xFFE0E0E0).copy(alpha = 0.55f)
        val ledR = (2.0f + curved * 2.4f)
        drawScope.drawCircle(color = ledColor, radius = ledR, center = Offset(centerX - halfW, ly))
        drawScope.drawCircle(color = ledColor, radius = ledR, center = Offset(centerX + halfW, ly))
    }

    // Cordas metálicas enroladas ao longo de cada pista + divisórias alinhadas à strikeline
    for (lane in 0 until numLanes) {
        val topFrac = (lane + 0.5f) / numLanes
        val topStrX = (centerX - topHalfW) + topWidth * topFrac
        val botStrX = geometry.laneCenterXAtStrike(lane)
        drawScope.drawLine(
            color = Color(0xFFB8C0CC).copy(alpha = 0.34f),
            start = Offset(topStrX, horizonY),
            end = Offset(botStrX, highwayBottomY),
            strokeWidth = 2.0f
        )
    }
    for (i in 1 until numLanes) {
        val topLaneX = geometry.laneDividerTopX(i)
        val bottomLaneX = geometry.laneDividerBottomX(i)
        drawScope.drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(topLaneX, horizonY),
            end = Offset(bottomLaneX, highwayBottomY),
            strokeWidth = 1.6f
        )
    }

    // Barras de compasso horizontais em perspectiva
    val beatDistanceMs = 500L
    val lookAheadMs = (1450L / difficulty.noteSpeedMultiplier).toLong()
    val firstBeat = ((currentTimeMs / beatDistanceMs) * beatDistanceMs)
    for (b in 0..5) {
        val beatTime = firstBeat + b * beatDistanceMs
        val timeUntilStrike = beatTime - currentTimeMs
        if (timeUntilStrike in 0L..lookAheadMs) {
            val depth = 1.0f - (timeUntilStrike.toFloat() / lookAheadMs.toFloat())
            val curved = depth.toDouble().pow(1.65).toFloat()
            val y = horizonY + (strikeY - horizonY) * curved
            val w = topWidth + (bottomWidth - topWidth) * curved
            drawScope.drawLine(
                color = Color.White.copy(alpha = depth * 0.34f),
                start = Offset(centerX - w * 0.5f, y),
                end = Offset(centerX + w * 0.5f, y),
                strokeWidth = (3.8f * depth).coerceAtLeast(1f)
            )
        }
    }

    // Barra metálica da Zona de Impacto (Strikeline) conectando os alvos
    val strikeLineColor = if (isStarPowerActive) Color(0xFF80D8FF) else Color(0xFFD6DAE2)
    drawScope.drawLine(
        color = strikeLineColor.copy(alpha = 0.85f),
        start = Offset(centerX - bottomHalfW, strikeY),
        end = Offset(centerX + bottomHalfW, strikeY),
        strokeWidth = 4.0f
    )

    // Alvos de Traste 3D Chanfrados ÚNICOS (Target Rings estilo PS2 diretamente na base das pistas)
    for (lane in 0 until numLanes) {
        val targetPos = geometry.pointAt(lane, 1.0f)
        val color = if (isStarPowerActive) Color(0xFF00E5FF) else LaneColors[lane]
        val isPressed = activeFretLanes.contains(lane)
        val rx = geometry.receptorRx
        val ry = geometry.receptorRy

        // Base escura do anel receptor
        drawScope.drawOval(
            color = Color(0xFF0C0912),
            topLeft = Offset(targetPos.x - rx * 1.12f, targetPos.y - ry * 1.04f + 3f),
            size = Size(rx * 2.24f, ry * 2.12f)
        )

        if (isPressed) {
            // Brilho e chama estilizada ao segurar/acertar o traste
            drawScope.drawOval(
                color = color.copy(alpha = 0.58f),
                topLeft = Offset(targetPos.x - rx * 1.38f, targetPos.y - ry * 1.38f),
                size = Size(rx * 2.76f, ry * 2.76f)
            )
            drawFretFlameBurst(drawScope, targetPos, rx, color, timeSec + lane)
        }

        // Aro externo metálico prateado (estilo PS2 clássico)
        drawScope.drawOval(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFF3F4F6), Color(0xFF8E929B), Color(0xFF1F1D24)),
                start = Offset(targetPos.x - rx, targetPos.y - ry),
                end = Offset(targetPos.x + rx, targetPos.y + ry)
            ),
            topLeft = Offset(targetPos.x - rx, targetPos.y - ry),
            size = Size(rx * 2f, ry * 2f),
            style = Stroke(width = (rx * 0.22f).coerceAtLeast(3.5f))
        )

        // Poço colorido interno com gradiente radial 3D
        drawScope.drawOval(
            brush = Brush.radialGradient(
                colors = if (isPressed) {
                    listOf(Color.White, color, Color(0xFF12101A))
                } else {
                    listOf(color.copy(alpha = 0.96f), color.copy(alpha = 0.68f), Color(0xFF14111B))
                },
                center = Offset(targetPos.x, targetPos.y),
                radius = rx * 0.92f
            ),
            topLeft = Offset(targetPos.x - rx * 0.80f, targetPos.y - ry * 0.80f),
            size = Size(rx * 1.60f, ry * 1.60f)
        )

        // Borda interna luminosa + centro afundado
        drawScope.drawOval(
            color = Color.White.copy(alpha = if (isPressed) 0.95f else 0.55f),
            topLeft = Offset(targetPos.x - rx * 0.78f, targetPos.y - ry * 0.78f),
            size = Size(rx * 1.56f, ry * 1.56f),
            style = Stroke(width = 1.6f)
        )
        drawScope.drawOval(
            color = if (isPressed) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.35f),
            topLeft = Offset(targetPos.x - rx * 0.22f, targetPos.y - ry * 0.34f),
            size = Size(rx * 0.44f, ry * 0.34f)
        )
    }

    // Notas 3D (Discos Chanfrados, Estrelas Star Power & Sustains Brilhantes)
    for (note in notes) {
        if (note.isHit || note.isMissed) continue
        if (note.lane >= numLanes) continue

        val timeUntilHit = note.timeMs - currentTimeMs
        if (timeUntilHit in -250L..lookAheadMs) {
            val depth = (1.0f - (timeUntilHit.toFloat() / lookAheadMs.toFloat())).coerceIn(0f, 1.15f)
            val curved = depth.toDouble().pow(1.65).toFloat()
            val notePos = geometry.pointAt(note.lane, depth)
            val laneW = (topWidth + (bottomWidth - topWidth) * curved) / numLanes
            val rx = (laneW * 0.35f).coerceIn(6f, geometry.receptorRx * 0.92f)
            val ry = rx * 0.70f

            if (note.durationMs > 50L) {
                val tailEndDepth = (1.0f - ((timeUntilHit + note.durationMs).toFloat() / lookAheadMs.toFloat())).coerceIn(0f, 1.15f)
                val tailEndPos = geometry.pointAt(note.lane, tailEndDepth)
                val sustainColor = if (note.isStarPower || isStarPowerActive) Color(0xFF00E5FF) else LaneColors[note.lane]

                // Aura externa + núcleo brilhante da cauda sustain
                drawScope.drawLine(
                    color = sustainColor.copy(alpha = 0.45f),
                    start = notePos,
                    end = tailEndPos,
                    strokeWidth = (rx * 0.72f).coerceAtLeast(6f)
                )
                drawScope.drawLine(
                    color = sustainColor.copy(alpha = 0.92f),
                    start = notePos,
                    end = tailEndPos,
                    strokeWidth = (rx * 0.38f).coerceAtLeast(3f)
                )
                drawScope.drawLine(
                    color = Color.White.copy(alpha = 0.75f),
                    start = notePos,
                    end = tailEndPos,
                    strokeWidth = (rx * 0.14f).coerceAtLeast(1.5f)
                )
            }

            if (note.isStarPower) {
                drawStarPowerNote3D(drawScope, notePos, rx, ry, isStarPowerActive)
            } else {
                val gemColor = if (isStarPowerActive) Color(0xFF00E5FF) else LaneColors[note.lane]
                // Pedestal escuro 3D da nota
                drawScope.drawOval(
                    color = Color(0xFF0C0912),
                    topLeft = Offset(notePos.x - rx * 1.06f, notePos.y - ry * 0.95f + 4f),
                    size = Size(rx * 2.12f, ry * 2.04f)
                )
                // Anel colorido principal com brilho 3D chanfrado
                drawScope.drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.96f), gemColor, Color(0xFF101018)),
                        center = Offset(notePos.x - rx * 0.24f, notePos.y - ry * 0.34f),
                        radius = rx * 1.65f
                    ),
                    topLeft = Offset(notePos.x - rx, notePos.y - ry),
                    size = Size(rx * 2f, ry * 2f)
                )
                // Borda cromada
                drawScope.drawOval(
                    color = Color.White.copy(alpha = 0.88f),
                    topLeft = Offset(notePos.x - rx, notePos.y - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = (rx * 0.13f).coerceAtLeast(1.8f))
                )
                // Domo interno escuro + núcleo branco superior clássico
                drawScope.drawOval(
                    color = Color(0xFF17131E),
                    topLeft = Offset(notePos.x - rx * 0.56f, notePos.y - ry * 0.54f),
                    size = Size(rx * 1.12f, ry * 1.08f)
                )
                drawScope.drawOval(
                    color = Color.White,
                    topLeft = Offset(notePos.x - rx * 0.36f, notePos.y - ry * 0.42f),
                    size = Size(rx * 0.72f, ry * 0.58f)
                )
            }
        }
    }

    particleSystem.updateAndDraw(drawScope)
}

private fun drawStarPowerNote3D(
    drawScope: DrawScope,
    center: Offset,
    rx: Float,
    ry: Float,
    isStarPowerActive: Boolean
) {
    val starColor = if (isStarPowerActive) Color(0xFFFFFFFF) else Color(0xFF00E5FF)
    drawScope.drawCircle(
        color = Color(0x5500E5FF),
        radius = rx * 1.35f,
        center = center
    )
    val starPath = Path()
    val outerR = rx * 1.15f
    val innerR = rx * 0.50f
    for (i in 0 until 10) {
        val angle = -Math.PI / 2.0 + i * (Math.PI / 5.0)
        val rad = if (i % 2 == 0) outerR else innerR
        val px = center.x + (cos(angle) * rad).toFloat()
        val py = center.y + (sin(angle) * rad * (ry / rx).coerceAtLeast(0.6f)).toFloat()
        if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
    }
    starPath.close()
    drawScope.drawPath(starPath, color = Color(0xFF06253A), style = Stroke(width = 4.5f))
    drawScope.drawPath(starPath, color = starColor, style = Fill)
    drawScope.drawPath(starPath, color = Color.White, style = Stroke(width = 1.8f))
    drawScope.drawCircle(color = Color.White, radius = rx * 0.24f, center = center)
}

private fun drawFretFlameBurst(
    drawScope: DrawScope,
    targetPos: Offset,
    rx: Float,
    color: Color,
    phase: Float
) {
    val flicker = sin(phase * 18f) * (rx * 0.14f)
    val flameH = rx * 1.55f + flicker
    val outerFlame = Path().apply {
        moveTo(targetPos.x - rx * 0.65f, targetPos.y - rx * 0.12f)
        lineTo(targetPos.x - rx * 0.35f, targetPos.y - flameH * 0.72f)
        lineTo(targetPos.x, targetPos.y - flameH)
        lineTo(targetPos.x + rx * 0.35f, targetPos.y - flameH * 0.68f)
        lineTo(targetPos.x + rx * 0.65f, targetPos.y - rx * 0.12f)
        close()
    }
    drawScope.drawPath(outerFlame, color = color.copy(alpha = 0.72f), style = Fill)

    val coreFlame = Path().apply {
        moveTo(targetPos.x - rx * 0.34f, targetPos.y - rx * 0.10f)
        lineTo(targetPos.x, targetPos.y - flameH * 0.78f)
        lineTo(targetPos.x + rx * 0.34f, targetPos.y - rx * 0.10f)
        close()
    }
    drawScope.drawPath(coreFlame, color = Color(0xFFFFF59D).copy(alpha = 0.90f), style = Fill)
}

/**
 * Desenha uma prévia compacta da Esteira Fixa (sem gameplay) para o Estúdio de Customização.
 */
fun drawHighwayStudioPreview(
    drawScope: DrawScope,
    width: Float,
    height: Float,
    highwayTheme: HighwayTheme,
    overlayBitmap: ImageBitmap?,
    overlayOpacity: Float
) {
    val horizonY = height * 0.10f
    val strikeY = height * 0.88f
    val topW = width * 0.42f
    val botW = width * 0.88f
    val cx = width * 0.5f

    val path = Path().apply {
        moveTo(cx - topW * 0.5f, horizonY)
        lineTo(cx + topW * 0.5f, horizonY)
        lineTo(cx + botW * 0.5f, strikeY)
        lineTo(cx - botW * 0.5f, strikeY)
        close()
    }
    drawScope.drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = highwayTheme.surfaceGradientHex.map { Color(it) },
            startY = horizonY,
            endY = strikeY
        )
    )
    if (overlayBitmap != null) {
        drawScope.clipPath(path) {
            drawImage(
                image = overlayBitmap,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(overlayBitmap.width, overlayBitmap.height),
                dstOffset = IntOffset((cx - botW * 0.5f).toInt(), horizonY.toInt()),
                dstSize = IntSize(botW.toInt().coerceAtLeast(1), (strikeY - horizonY).toInt().coerceAtLeast(1)),
                alpha = overlayOpacity.coerceIn(0.15f, 0.90f)
            )
            drawRect(Color.Black.copy(alpha = 0.25f))
        }
    }
    val railColor = Color(highwayTheme.primaryColorHex)
    drawScope.drawLine(railColor, Offset(cx - topW * 0.5f, horizonY), Offset(cx - botW * 0.5f, strikeY), strokeWidth = 5f)
    drawScope.drawLine(railColor, Offset(cx + topW * 0.5f, horizonY), Offset(cx + botW * 0.5f, strikeY), strokeWidth = 5f)
    for (i in 1..4) {
        val frac = i / 5f
        drawScope.drawLine(
            Color.White.copy(alpha = 0.25f),
            Offset((cx - topW * 0.5f) + topW * frac, horizonY),
            Offset((cx - botW * 0.5f) + botW * frac, strikeY),
            strokeWidth = 1.5f
        )
    }
    for (lane in 0..4) {
        val x = (cx - botW * 0.5f) + (botW / 5f) * (lane + 0.5f)
        drawScope.drawOval(
            color = LaneColors[lane],
            topLeft = Offset(x - botW * 0.055f, strikeY - 22f),
            size = Size(botW * 0.11f, botW * 0.07f),
            style = Stroke(width = 4f)
        )
    }
}

private fun drawStageEnvironment(
    drawScope: DrawScope,
    width: Float,
    height: Float,
    currentTimeMs: Long,
    isStarPower: Boolean,
    rockMeter: Float,
    combo: Int,
    instrument: Instrument,
    playerStats: PlayerStats,
    highwayTheme: HighwayTheme,
    horizonY: Float,
    centerX: Float
) {
    val timeSec = currentTimeMs / 1000f

    // 0. Elementos Temáticos de Palco (Synthwave Sunset, Inferno Pyro, Catedral Gótica ou Club PS2)
    when (highwayTheme) {
        HighwayTheme.NEON_CYBER_OVERDRIVE -> {
            // Sol Synthwave atrás do horizonte da esteira + montanhas wireframe + grade laser
            val sunR = minOf(width, height) * 0.24f
            drawScope.drawCircle(
                brush = Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF6D00), Color(0xFFFF1744))),
                radius = sunR,
                center = Offset(centerX, horizonY + sunR * 0.35f)
            )
            // Linhas horizontais retro-grid no chão
            for (g in 0..5) {
                val gy = height * (0.42f + g * 0.10f)
                drawScope.drawLine(Color(0x5500E5FF), Offset(0f, gy), Offset(width, gy), strokeWidth = 1.5f)
            }
            // Montanhas wireframe ciano
            val mPath = Path().apply {
                moveTo(0f, height * 0.42f)
                lineTo(width * 0.14f, height * 0.22f)
                lineTo(width * 0.28f, height * 0.39f)
                lineTo(width * 0.72f, height * 0.38f)
                lineTo(width * 0.86f, height * 0.20f)
                lineTo(width, height * 0.42f)
            }
            drawScope.drawPath(mPath, color = Color(0xFF00E5FF).copy(alpha = 0.55f), style = Stroke(width = 2.2f))
        }
        HighwayTheme.FIRE_AND_BRIMSTONE -> {
            // Pilares de fogo pirotécnicos ao fundo do palco (como Arena Inferno)
            listOf(width * 0.28f, width * 0.72f).forEachIndexed { idx, fx ->
                val pulseH = height * (0.28f + 0.08f * sin(timeSec * 7f + idx * 1.3f))
                drawScope.drawOval(
                    brush = Brush.verticalGradient(listOf(Color(0xAAFFEA00), Color(0xAAFF3D00), Color.Transparent)),
                    topLeft = Offset(fx - 26f, height * 0.55f - pulseH),
                    size = Size(52f, pulseH)
                )
            }
        }
        HighwayTheme.GALACTIC_STAR -> {
            // Rosácea de vitral / catedral de arena atrás do centro
            val roseCenter = Offset(width * 0.82f, height * 0.34f)
            val roseR = minOf(width, height) * 0.16f
            drawScope.drawCircle(
                brush = Brush.radialGradient(listOf(Color(0x66FF4081), Color(0x447C4DFF), Color.Transparent)),
                radius = roseR,
                center = roseCenter
            )
            drawScope.drawCircle(color = Color(0x55CE93D8), radius = roseR, center = roseCenter, style = Stroke(width = 2.5f))
        }
        HighwayTheme.PS2_WOOD_STEEL -> {
            // Placa luminosa de clube rock clássico no fundo esquerdo/direito
            drawScope.drawRoundRect(
                color = Color(0x33FF1744),
                topLeft = Offset(width * 0.78f, height * 0.16f),
                size = Size(width * 0.14f, height * 0.09f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
            )
            drawScope.drawRoundRect(
                color = Color(0xAAFF2A55),
                topLeft = Offset(width * 0.78f, height * 0.16f),
                size = Size(width * 0.14f, height * 0.09f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                style = Stroke(width = 2.2f)
            )
        }
    }

    // 1. Treliça superior de alumínio do palco
    drawScope.drawLine(
        color = Color(0xFF2D2A3A),
        start = Offset(0f, 18f),
        end = Offset(width, 18f),
        strokeWidth = 8f
    )
    for (x in 0..(width.toInt()) step 40) {
        drawScope.drawLine(
            color = Color(0xFF1F1D2A),
            start = Offset(x.toFloat(), 14f),
            end = Offset(x.toFloat() + 20f, 32f),
            strokeWidth = 2.5f
        )
    }

    // 2. Refletores Volumétricos Dinâmicos
    val spotlightColors = if (isStarPower) {
        listOf(Color(0x5500E5FF), Color(0x6600B0FF), Color(0x5580D8FF), Color(0x6600E5FF))
    } else {
        listOf(
            Color(0x44FF1744),
            Color(0x44FFEA00),
            Color(0x447C4DFF),
            Color(0x4400E676)
        )
    }

    val spotPositions = listOf(width * 0.14f, width * 0.34f, width * 0.66f, width * 0.86f)
    for (i in spotPositions.indices) {
        val spotX = spotPositions[i]
        val swing = sin(timeSec * 1.8f + i * 1.2f) * (width * 0.10f)
        val beamPath = Path().apply {
            moveTo(spotX, 22f)
            lineTo(spotX + swing - 60f, height * 0.90f)
            lineTo(spotX + swing + 60f, height * 0.90f)
            close()
        }
        drawScope.drawPath(
            path = beamPath,
            brush = Brush.verticalGradient(
                colors = listOf(spotlightColors[i], Color.Transparent),
                startY = 20f,
                endY = height * 0.90f
            ),
            style = Fill
        )
    }

    // 3. Torres de Amplificadores Clássicos nos dois lados do palco
    val bassPulse = (sin(timeSec * 8f) * 3.5f).coerceAtLeast(0f)
    listOf(width * 0.03f, width * 0.87f).forEach { ampLeft ->
        val ampTop = height * 0.32f
        val ampWidth = width * 0.10f
        val ampHeight = height * 0.40f
        drawScope.drawRect(color = Color(0xFF15121B), topLeft = Offset(ampLeft, ampTop), size = Size(ampWidth, ampHeight))
        drawScope.drawRect(
            color = Color(highwayTheme.primaryColorHex).copy(alpha = 0.55f),
            topLeft = Offset(ampLeft, ampTop),
            size = Size(ampWidth, ampHeight),
            style = Stroke(width = 2.2f)
        )
        val speakerRadius = ampWidth * 0.17f
        for (r in 0..1) {
            for (c in 0..1) {
                val cx = ampLeft + ampWidth * (0.28f + c * 0.44f)
                val cy = ampTop + ampHeight * (0.28f + r * 0.44f)
                drawScope.drawCircle(color = Color(0xFF0D0C10), radius = speakerRadius + bassPulse, center = Offset(cx, cy))
                drawScope.drawCircle(
                    color = if (highwayTheme == HighwayTheme.FIRE_AND_BRIMSTONE) Color(0xFF9E1B1B) else Color(0xFF34313F),
                    radius = (speakerRadius + bassPulse) * 0.48f,
                    center = Offset(cx, cy)
                )
            }
        }
    }

    // 4. Banda no Palco: Baterista + Baixista à direita + Músico Principal 3D à esquerda
    drawStageBandCompanions(
        drawScope = drawScope,
        width = width,
        height = height,
        timeSec = timeSec,
        isStarPowerActive = isStarPower,
        primaryInstrument = instrument,
        stats = playerStats
    )

    val rockerX = width * 0.22f
    val rockerBaseY = height * 0.79f
    val rockerScale = (height * 0.31f).coerceIn(85f, 210f)
    val idleYaw = 22f + sin(timeSec * 1.4f) * 8f

    drawRockerCharacter3D(
        drawScope = drawScope,
        stats = playerStats,
        instrument = instrument,
        centerX = rockerX,
        baseY = rockerBaseY,
        scale = rockerScale,
        yawDeg = idleYaw,
        pitchDeg = 4f,
        animTimeSec = timeSec,
        isStarPowerActive = isStarPower,
        combo = combo
    )

    // 5. Silhueta da Plateia Reativa na Base do Palco (vibra no Star Power / combo, retrai em risco de vaia)
    val highEnergy = isStarPower || combo >= 20 || rockMeter > 0.72f
    val crowdDanger = rockMeter < 0.30f
    val crowdEdgeColor = when {
        isStarPower -> Color(0x4400E5FF)
        highEnergy -> Color(0x33FFD54F)
        crowdDanger -> Color(0x44FF1744)
        else -> Color.Transparent
    }
    if (crowdEdgeColor != Color.Transparent) {
        drawScope.drawRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, crowdEdgeColor)),
            topLeft = Offset(0f, height - 72f),
            size = Size(width, 72f)
        )
    }

    val crowdColor = Color(0xEE090510)
    val crowdFreq = when {
        isStarPower -> 10.5f
        highEnergy -> 8.0f
        crowdDanger -> 2.4f
        else -> 5.0f
    }
    val crowdAmp = when {
        isStarPower -> 14f
        highEnergy -> 10f
        crowdDanger -> 3f
        else -> 7f
    }
    val armsOffset = when {
        isStarPower -> 34f
        highEnergy -> 26f
        crowdDanger -> 10f
        else -> 20f
    }
    val crowdPath = Path().apply {
        moveTo(0f, height)
        for (cx in 0..(width.toInt()) step 34) {
            val crowdBob = sin(timeSec * crowdFreq + cx * 0.08f) * crowdAmp
            val cy = (if (crowdDanger) height - 18f else height - 28f) + crowdBob
            lineTo(cx.toFloat(), cy)
            if (cx % 68 == 0) {
                lineTo(cx.toFloat() - 5f, cy - armsOffset)
                lineTo(cx.toFloat() + 3f, cy - armsOffset * 0.72f)
                lineTo(cx.toFloat() + 9f, cy - armsOffset * 1.06f)
            }
        }
        lineTo(width, height)
        close()
    }
    drawScope.drawPath(crowdPath, color = crowdColor, style = Fill)
}
