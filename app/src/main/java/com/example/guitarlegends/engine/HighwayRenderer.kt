package com.example.guitarlegends.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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
                    vy = sin(angle) * speed - 3f, // upward bias
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
 * Renderizador de Palco 3D e Esteira de Notas Estilo PlayStation 2 (Guitar Hero II Era)
 * Combina iluminação dinâmica de refletores, amplificadores Marshall com VU-meters,
 * silhueta animada do integrante da banda no palco, plateia vibrante e esteira balanceada.
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
    characterName: String = "Axel Shredder",
    characterArtStyle: CharacterArtStyle = CharacterArtStyle.STYLIZED_REALISTIC,
    instrumentName: String = "Flame Caster"
) {
    val numLanes = difficulty.lanesUsed
    val horizonY = canvasHeight * 0.16f
    val strikeY = canvasHeight * 0.82f

    // 1. DESENHO DO PALCO 3D PS2 (ILUMINAÇÃO DINÂMICA & REFLETORES)
    drawStageEnvironment(
        drawScope = drawScope,
        width = canvasWidth,
        height = canvasHeight,
        currentTimeMs = currentTimeMs,
        isStarPower = isStarPowerActive,
        rockMeter = rockMeter,
        instrument = instrument,
        characterName = characterName,
        characterArtStyle = characterArtStyle,
        instrumentName = instrumentName
    )

    // 2. ESTEIRA DE NOTAS PROPORCIONAL & BALANCEADA (NÃO TOMA A TELA INTEIRA)
    // Largura balanceada: ~20% no topo e ~46% na base, posicionada no centro
    val topWidth = (canvasWidth * 0.22f).coerceIn(120f, 240f)
    val bottomWidth = (canvasWidth * 0.48f).coerceIn(280f, 520f)
    val centerX = canvasWidth * 0.50f

    val topHalfW = topWidth * 0.5f
    val bottomHalfW = bottomWidth * 0.5f

    val highwayTopLeft = Offset(centerX - topHalfW, horizonY)
    val highwayTopRight = Offset(centerX + topHalfW, horizonY)
    val highwayBottomRight = Offset(centerX + bottomHalfW, strikeY + 45f)
    val highwayBottomLeft = Offset(centerX - bottomHalfW, strikeY + 45f)

    // Corpo da Esteira (Plano Trapezoidal com gradiente do tema selecionado)
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
                Color(0xFF0D2538),
                Color(0xFF1A4971),
                Color(0xFF00B0FF).copy(alpha = 0.65f),
                Color(0xFF00E5FF).copy(alpha = 0.85f)
            ),
            startY = horizonY,
            endY = strikeY
        )
    } else {
        Brush.verticalGradient(
            colors = themeColors,
            startY = horizonY,
            endY = strikeY
        )
    }

    drawScope.drawPath(highwayPath, brush = highwayBrush, style = Fill)

    // Trilhos Laterais em Aço / Neon
    val railColor = if (isStarPowerActive) Color(0xFF00E5FF) else Color(highwayTheme.primaryColorHex)
    drawScope.drawLine(
        color = railColor,
        start = highwayTopLeft,
        end = highwayBottomLeft,
        strokeWidth = 6f
    )
    drawScope.drawLine(
        color = railColor,
        start = highwayTopRight,
        end = highwayBottomRight,
        strokeWidth = 6f
    )

    // Função de projeção de perspectiva 3D cônica
    fun get3dPosition(lane: Int, depth01: Float): Offset {
        val curvedT = depth01.coerceIn(0f, 1f).toDouble().pow(1.65).toFloat()
        val currentY = horizonY + (strikeY - horizonY) * curvedT
        val currentWidth = topWidth + (bottomWidth - topWidth) * curvedT
        val startX = centerX - currentWidth * 0.5f
        val laneWidth = currentWidth / numLanes
        val laneCenterX = startX + laneWidth * (lane + 0.5f)
        return Offset(laneCenterX, currentY)
    }

    // Linhas divisórias das pistas
    for (i in 1 until numLanes) {
        val topLaneX = (centerX - topHalfW) + (topWidth / numLanes) * i
        val bottomLaneX = (centerX - bottomHalfW) + (bottomWidth / numLanes) * i
        drawScope.drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(topLaneX, horizonY),
            end = Offset(bottomLaneX, strikeY + 40f),
            strokeWidth = 2f
        )
    }

    // Barras rítmicas de compasso ao longo da esteira
    val beatDistanceMs = 500L
    val lookAheadMs = (1600L / difficulty.noteSpeedMultiplier).toLong()
    val firstBeat = ((currentTimeMs / beatDistanceMs) * beatDistanceMs)
    for (b in 0..5) {
        val beatTime = firstBeat + b * beatDistanceMs
        val timeUntilStrike = beatTime - currentTimeMs
        if (timeUntilStrike in 0L..lookAheadMs) {
            val depth = 1.0f - (timeUntilStrike.toFloat() / lookAheadMs.toFloat())
            val y = horizonY + (strikeY - horizonY) * depth.toDouble().pow(1.65).toFloat()
            val w = topWidth + (bottomWidth - topWidth) * depth.toDouble().pow(1.65).toFloat()
            drawScope.drawLine(
                color = Color.White.copy(alpha = (depth * 0.28f)),
                start = Offset(centerX - w * 0.5f, y),
                end = Offset(centerX + w * 0.5f, y),
                strokeWidth = (3.5f * depth).coerceAtLeast(1f)
            )
        }
    }

    // Linha de Fogo / Zona de Impacto
    val strikeLineColor = if (isStarPowerActive) Color(0xFF80D8FF) else Color.White
    drawScope.drawLine(
        color = strikeLineColor,
        start = Offset(centerX - bottomHalfW, strikeY),
        end = Offset(centerX + bottomHalfW, strikeY),
        strokeWidth = 4.5f
    )

    // Alvos de Traste (Target Rings)
    for (lane in 0 until numLanes) {
        val targetPos = get3dPosition(lane, 1.0f)
        val color = LaneColors[lane]
        val isPressed = activeFretLanes.contains(lane)
        val radius = (bottomWidth / numLanes) * 0.36f

        if (isPressed) {
            drawScope.drawCircle(
                color = color.copy(alpha = 0.5f),
                radius = radius * 1.35f,
                center = targetPos
            )
        }

        drawScope.drawCircle(
            color = if (isPressed) Color.White else color,
            radius = radius,
            center = targetPos,
            style = Stroke(width = if (isPressed) 7f else 4f)
        )

        drawScope.drawCircle(
            color = if (isPressed) color else color.copy(alpha = 0.35f),
            radius = radius * 0.45f,
            center = targetPos,
            style = Fill
        )
    }

    // Notas e Sustains em movimento 3D
    for (note in notes) {
        if (note.isHit || note.isMissed) continue
        if (note.lane >= numLanes) continue

        val timeUntilHit = note.timeMs - currentTimeMs
        if (timeUntilHit in -250L..lookAheadMs) {
            val depth = (1.0f - (timeUntilHit.toFloat() / lookAheadMs.toFloat())).coerceIn(0f, 1.15f)
            val notePos = get3dPosition(note.lane, depth)
            val laneW = (topWidth + (bottomWidth - topWidth) * depth.toDouble().pow(1.65).toFloat()) / numLanes
            val noteRadius = (laneW * 0.33f).coerceIn(6f, 36f)

            // Cauda do Sustain
            if (note.durationMs > 50L) {
                val tailEndDepth = (1.0f - ((timeUntilHit + note.durationMs).toFloat() / lookAheadMs.toFloat())).coerceIn(0f, 1.15f)
                val tailEndPos = get3dPosition(note.lane, tailEndDepth)
                val sustainColor = if (note.isStarPower) Color(0xFF80D8FF) else LaneColors[note.lane]

                drawScope.drawLine(
                    color = sustainColor.copy(alpha = 0.75f),
                    start = notePos,
                    end = tailEndPos,
                    strokeWidth = (noteRadius * 0.55f).coerceAtLeast(3.5f)
                )
            }

            // Gema da Nota
            val gemColor = if (note.isStarPower) Color(0xFFE0F7FA) else LaneColors[note.lane]
            val innerColor = if (note.isStarPower) Color(0xFF00E5FF) else Color.White

            // Corpo da gema 3D
            drawScope.drawCircle(
                color = gemColor,
                radius = noteRadius,
                center = notePos,
                style = Fill
            )

            // Reflexo de luz na gema
            drawScope.drawCircle(
                color = innerColor,
                radius = noteRadius * 0.48f,
                center = Offset(notePos.x, notePos.y - noteRadius * 0.25f),
                style = Fill
            )

            // Aro de contraste
            drawScope.drawCircle(
                color = Color.Black.copy(alpha = 0.55f),
                radius = noteRadius,
                center = notePos,
                style = Stroke(width = 2.5f)
            )
        }
    }

    // Partículas de faíscas ao acertar notas
    particleSystem.updateAndDraw(drawScope)
}

/**
 * Desenha o ambiente do palco 3D no estilo Guitar Hero II (PS2):
 * Refletores móveis volumétricos, treliça de luz, amplificadores de guitarra,
 * bateria ao fundo, integrante da banda animado e silhueta da plateia.
 */
private fun drawStageEnvironment(
    drawScope: DrawScope,
    width: Float,
    height: Float,
    currentTimeMs: Long,
    isStarPower: Boolean,
    rockMeter: Float,
    instrument: Instrument,
    characterName: String,
    characterArtStyle: CharacterArtStyle,
    instrumentName: String
) {
    val timeSec = currentTimeMs / 1000f

    // 1. Treliça superior de alumínio do palco
    drawScope.drawLine(
        color = Color(0xFF2A2A38),
        start = Offset(0f, 18f),
        end = Offset(width, 18f),
        strokeWidth = 8f
    )
    for (x in 0..(width.toInt()) step 40) {
        drawScope.drawLine(
            color = Color(0xFF1E1E28),
            start = Offset(x.toFloat(), 14f),
            end = Offset(x.toFloat() + 20f, 32f),
            strokeWidth = 2.5f
        )
    }

    // 2. Refletores de Luz Dinâmicos (Spotlights oscilantes)
    val spotlightColors = if (isStarPower) {
        listOf(Color(0x5500E5FF), Color(0x6600B0FF), Color(0x5580D8FF), Color(0x6600E5FF))
    } else {
        listOf(
            Color(0x44FF1744), // Vermelho Rock
            Color(0x44FFEA00), // Dourado
            Color(0x447C4DFF), // Púrpura PS2
            Color(0x4400E676)  // Verde
        )
    }

    val spotPositions = listOf(width * 0.15f, width * 0.35f, width * 0.65f, width * 0.85f)
    for (i in spotPositions.indices) {
        val spotX = spotPositions[i]
        val swing = sin(timeSec * 1.8f + i * 1.2f) * (width * 0.12f)
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

    // 3. Pilha de Amplificadores Marshall à Esquerda (Estilo PS2)
    val ampLeft = width * 0.05f
    val ampTop = height * 0.36f
    val ampWidth = width * 0.15f
    val ampHeight = height * 0.44f

    // Gabinete dos amplificadores
    drawScope.drawRect(
        color = Color(0xFF18151D),
        topLeft = Offset(ampLeft, ampTop),
        size = Size(ampWidth, ampHeight)
    )
    drawScope.drawRect(
        color = Color(0xFFFFD700).copy(alpha = 0.5f),
        topLeft = Offset(ampLeft, ampTop),
        size = Size(ampWidth, ampHeight),
        style = Stroke(width = 2.5f)
    )
    // Cones dos falantes (4 falantes vibrando com o beat)
    val bassPulse = (sin(timeSec * 8f) * 4f).coerceAtLeast(0f)
    val speakerRadius = ampWidth * 0.18f
    for (r in 0..1) {
        for (c in 0..1) {
            val cx = ampLeft + ampWidth * (0.28f + c * 0.44f)
            val cy = ampTop + ampHeight * (0.28f + r * 0.44f)
            drawScope.drawCircle(
                color = Color(0xFF0F0E12),
                radius = speakerRadius + bassPulse,
                center = Offset(cx, cy)
            )
            drawScope.drawCircle(
                color = Color(0xFF33303D),
                radius = (speakerRadius + bassPulse) * 0.5f,
                center = Offset(cx, cy)
            )
        }
    }

    // 4. Integrante da Banda no Palco (Visível à Esquerda com Instrumento)
    val charX = width * 0.26f
    val charBaseY = height * 0.72f
    val headBob = sin(timeSec * 7.5f) * 6f

    // Sombra no chão do palco
    drawScope.drawOval(
        color = Color(0x66000000),
        topLeft = Offset(charX - 45f, charBaseY - 10f),
        size = Size(90f, 25f)
    )

    // Corpo / Jaqueta do Rocker
    val bodyColor = when (characterArtStyle) {
        CharacterArtStyle.STYLIZED_REALISTIC -> Color(0xFF211E26) // Couro preto PS2
        CharacterArtStyle.CARTOON -> Color(0xFFFF1744) // Vermelho vivo
        CharacterArtStyle.ANIME -> Color(0xFF00E5FF) // Holográfico neon
    }

    // Pernas (calça jeans rasgada)
    drawScope.drawLine(
        color = Color(0xFF2C3E50),
        start = Offset(charX - 12f, charBaseY - 45f),
        end = Offset(charX - 22f, charBaseY),
        strokeWidth = 14f
    )
    drawScope.drawLine(
        color = Color(0xFF2C3E50),
        start = Offset(charX + 12f, charBaseY - 45f),
        end = Offset(charX + 22f, charBaseY),
        strokeWidth = 14f
    )

    // Tronco
    drawScope.drawRect(
        color = bodyColor,
        topLeft = Offset(charX - 20f, charBaseY - 105f + headBob * 0.5f),
        size = Size(40f, 62f)
    )

    // Braços segurando o instrumento
    val armColor = Color(0xFFE0BB9B)
    drawScope.drawLine(
        color = armColor,
        start = Offset(charX - 20f, charBaseY - 95f + headBob * 0.5f),
        end = Offset(charX - 35f, charBaseY - 65f + headBob),
        strokeWidth = 10f
    )
    drawScope.drawLine(
        color = armColor,
        start = Offset(charX + 20f, charBaseY - 95f + headBob * 0.5f),
        end = Offset(charX + 10f, charBaseY - 65f + headBob),
        strokeWidth = 10f
    )

    // Cabeça & Cabelo com Headbanging
    val headY = charBaseY - 125f + headBob
    drawScope.drawCircle(
        color = armColor,
        radius = 16f,
        center = Offset(charX, headY)
    )
    // Cabelo Moicano / Metalhead
    val hairColor = Color(0xFFFF5722)
    val hairPath = Path().apply {
        moveTo(charX - 14f, headY - 10f)
        lineTo(charX, headY - 32f)
        lineTo(charX + 14f, headY - 10f)
        close()
    }
    drawScope.drawPath(hairPath, color = hairColor, style = Fill)

    // Instrumento no colo do integrante (Guitarra, Baixo, Baquetas ou Microfone)
    when (instrument) {
        Instrument.GUITAR, Instrument.BASS -> {
            val instColor = if (isStarPower) Color(0xFF00E5FF) else Color(0xFFFF3D00)
            // Braço da guitarra inclinado
            drawScope.drawLine(
                color = Color(0xFFC89860),
                start = Offset(charX - 42f, charBaseY - 82f + headBob),
                end = Offset(charX + 28f, charBaseY - 55f + headBob),
                strokeWidth = 6f
            )
            // Corpo da guitarra (Flying V / Strat)
            val guitarBody = Path().apply {
                moveTo(charX + 10f, charBaseY - 75f + headBob)
                lineTo(charX + 42f, charBaseY - 88f + headBob)
                lineTo(charX + 35f, charBaseY - 45f + headBob)
                close()
            }
            drawScope.drawPath(guitarBody, color = instColor, style = Fill)
            // Arcos elétricos no instrumento se Star Power ativo
            if (isStarPower) {
                drawScope.drawLine(
                    color = Color.White,
                    start = Offset(charX - 40f, charBaseY - 80f),
                    end = Offset(charX + 30f, charBaseY - 52f),
                    strokeWidth = 3f
                )
            }
        }
        Instrument.DRUMS -> {
            // Pratos de bronze e baquetas
            drawScope.drawCircle(
                color = Color(0xFFFFD700),
                radius = 24f,
                center = Offset(charX - 25f, charBaseY - 55f),
                style = Fill
            )
            drawScope.drawLine(
                color = Color(0xFFF5DEB3),
                start = Offset(charX - 35f, charBaseY - 75f + headBob),
                end = Offset(charX - 20f, charBaseY - 55f),
                strokeWidth = 4f
            )
        }
        Instrument.VOCALS -> {
            // Pedestal e microfone cromado vintage
            drawScope.drawLine(
                color = Color(0xFFB0BEC5),
                start = Offset(charX - 10f, charBaseY),
                end = Offset(charX - 10f, charBaseY - 110f),
                strokeWidth = 5f
            )
            drawScope.drawCircle(
                color = Color(0xFFE0E0E0),
                radius = 8f,
                center = Offset(charX - 10f, charBaseY - 112f)
            )
        }
    }

    // 5. Silhueta da Plateia Vibrante na Base do Palco
    val crowdColor = Color(0xEE090510)
    val crowdPath = Path().apply {
        moveTo(0f, height)
        for (cx in 0..(width.toInt()) step 35) {
            val crowdBob = sin(timeSec * 6.5f + cx * 0.08f) * 8f
            // Cabeça da plateia
            val cy = height - 35f + crowdBob
            lineTo(cx.toFloat(), cy)
            // Mãos e dedos fazendo Rock 'n' Roll 🤘 nos picos de onda
            if (cx % 70 == 0) {
                lineTo(cx.toFloat() - 4f, cy - 24f)
                lineTo(cx.toFloat() + 4f, cy - 20f)
                lineTo(cx.toFloat() + 8f, cy - 26f)
            }
        }
        lineTo(width, height)
        close()
    }
    drawScope.drawPath(crowdPath, color = crowdColor, style = Fill)
}
