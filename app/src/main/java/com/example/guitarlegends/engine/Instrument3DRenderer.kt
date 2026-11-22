package com.example.guitarlegends.engine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guitarlegends.model.*
import com.example.guitarlegends.ui.components.RockAccentCyan
import com.example.guitarlegends.ui.components.RockAccentGold
import kotlin.math.*

data class Vec3(val x: Float, val y: Float, val z: Float)

fun projectVec3(
    p: Vec3,
    yawDeg: Float,
    pitchDeg: Float,
    centerX: Float,
    centerY: Float,
    scale: Float
): Pair<Offset, Float> {
    val yaw = Math.toRadians(yawDeg.toDouble()).toFloat()
    val pitch = Math.toRadians(pitchDeg.toDouble()).toFloat()

    // Rotação Y (360° horizontal)
    val cosY = cos(yaw)
    val sinY = sin(yaw)
    val x1 = p.x * cosY + p.z * sinY
    val z1 = -p.x * sinY + p.z * cosY

    // Rotação X (inclinação sutil)
    val cosP = cos(pitch)
    val sinP = sin(pitch)
    val y2 = p.y * cosP - z1 * sinP
    val z2 = p.y * sinP + z1 * cosP

    val perspective = 520f / (520f + z2 * scale * 0.65f).coerceAtLeast(180f)
    val sx = centerX + x1 * scale * perspective
    val sy = centerY + y2 * scale * perspective
    return Offset(sx, sy) to z2
}

fun effectiveInstrumentPalette(config: InstrumentCustomConfig): Triple<Color, Color, Color> {
    return when (config.fullSkinId) {
        "skin_chrome_hero" -> Triple(Color(0xFFD8DCE6), Color(0xFF454C5C), Color(0xFFFFFFFF))
        "skin_golden_tour" -> Triple(Color(0xFFFFC107), Color(0xFF3E2723), Color(0xFFFFF8E1))
        "skin_plasma_neon" -> Triple(Color(0xFF1A0E3A), Color(0xFF00E5FF), Color(0xFF7C4DFF))
        else -> Triple(
            Color(config.primaryColorHex),
            Color(config.secondaryColorHex),
            Color(config.hardwareColorHex)
        )
    }
}

fun drawInstrument3D(
    drawScope: DrawScope,
    instrument: Instrument,
    config: InstrumentCustomConfig,
    centerX: Float,
    centerY: Float,
    scale: Float,
    yawDeg: Float,
    pitchDeg: Float,
    pulsePhase: Float = 0f
) {
    val (primary, secondary, hardware) = effectiveInstrumentPalette(config)
    val yawNorm = ((yawDeg % 360f) + 360f) % 360f
    val facingFront = yawNorm <= 90f || yawNorm >= 270f

    when (instrument) {
        Instrument.GUITAR, Instrument.ACOUSTIC_GUITAR, Instrument.BASS -> {
            val isBass = instrument == Instrument.BASS
            val outline = when (config.modelStyleId) {
                "acoustic_dreadnought" -> listOf(
                    Vec3(-0.20f, -0.18f, 0f),
                    Vec3(0.20f, -0.18f, 0f),
                    Vec3(0.34f, 0.02f, 0f),
                    Vec3(0.30f, 0.20f, 0f),
                    Vec3(0.42f, 0.42f, 0f),
                    Vec3(0.36f, 0.66f, 0f),
                    Vec3(0.22f, 0.78f, 0f),
                    Vec3(-0.22f, 0.78f, 0f),
                    Vec3(-0.36f, 0.66f, 0f),
                    Vec3(-0.42f, 0.42f, 0f),
                    Vec3(-0.30f, 0.20f, 0f),
                    Vec3(-0.34f, 0.02f, 0f)
                )
                "flying_v" -> listOf(
                    Vec3(-0.22f, -0.05f, 0f),
                    Vec3(0.22f, -0.05f, 0f),
                    Vec3(0.55f, 0.78f, 0f),
                    Vec3(0.24f, 0.80f, 0f),
                    Vec3(0f, 0.34f, 0f),
                    Vec3(-0.24f, 0.80f, 0f),
                    Vec3(-0.55f, 0.78f, 0f)
                )
                "explorer_metal" -> listOf(
                    Vec3(-0.34f, -0.10f, 0f),
                    Vec3(0.24f, -0.16f, 0f),
                    Vec3(0.46f, 0.25f, 0f),
                    Vec3(0.20f, 0.72f, 0f),
                    Vec3(-0.48f, 0.76f, 0f),
                    Vec3(-0.56f, 0.26f, 0f)
                )
                "cyber_laser", "cyber_fretless" -> listOf(
                    Vec3(-0.18f, -0.14f, 0f),
                    Vec3(0.18f, -0.14f, 0f),
                    Vec3(0.44f, 0.16f, 0f),
                    Vec3(0.32f, 0.66f, 0f),
                    Vec3(0.08f, 0.76f, 0f),
                    Vec3(-0.08f, 0.76f, 0f),
                    Vec3(-0.32f, 0.66f, 0f),
                    Vec3(-0.44f, 0.16f, 0f)
                )
                else -> listOf(
                    Vec3(-0.20f, -0.16f, 0f),
                    Vec3(0.18f, -0.12f, 0f),
                    Vec3(0.38f, 0.14f, 0f),
                    Vec3(0.26f, 0.32f, 0f),
                    Vec3(0.42f, 0.56f, 0f),
                    Vec3(0.22f, 0.74f, 0f),
                    Vec3(-0.22f, 0.74f, 0f),
                    Vec3(-0.42f, 0.56f, 0f),
                    Vec3(-0.26f, 0.32f, 0f),
                    Vec3(-0.38f, 0.14f, 0f)
                )
            }

            val thickness = 0.12f
            val frontPts = outline.map { projectVec3(it.copy(z = -thickness), yawDeg, pitchDeg, centerX, centerY, scale).first }
            val backPts = outline.map { projectVec3(it.copy(z = thickness), yawDeg, pitchDeg, centerX, centerY, scale).first }

            // Extrusão 3D lateral (visível ao girar em 360°)
            for (i in outline.indices) {
                val next = (i + 1) % outline.size
                val sidePath = Path().apply {
                    moveTo(frontPts[i].x, frontPts[i].y)
                    lineTo(frontPts[next].x, frontPts[next].y)
                    lineTo(backPts[next].x, backPts[next].y)
                    lineTo(backPts[i].x, backPts[i].y)
                    close()
                }
                drawScope.drawPath(sidePath, color = secondary.copy(alpha = 0.92f), style = Fill)
                drawScope.drawPath(sidePath, color = Color.Black.copy(alpha = 0.35f), style = Stroke(width = 1.2f))
            }

            // Face principal visível conforme ângulo 360°
            val mainFacePts = if (facingFront) frontPts else backPts
            val bodyPath = Path().apply {
                mainFacePts.forEachIndexed { idx, offset ->
                    if (idx == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                }
                close()
            }

            val bodyBrush = when (config.finish) {
                InstrumentFinish.SUNBURST -> Brush.radialGradient(
                    colors = listOf(primary, secondary),
                    center = projectVec3(Vec3(0f, 0.34f, 0f), yawDeg, pitchDeg, centerX, centerY, scale).first,
                    radius = scale * 0.75f
                )
                InstrumentFinish.METALLIC -> Brush.linearGradient(
                    colors = listOf(primary, hardware.copy(alpha = 0.55f), primary, secondary)
                )
                InstrumentFinish.NEON_GLOW -> Brush.verticalGradient(
                    colors = listOf(primary, secondary, primary.copy(alpha = 0.9f))
                )
                InstrumentFinish.GLOSS -> Brush.linearGradient(
                    colors = listOf(primary.copy(alpha = 0.95f), primary, secondary.copy(alpha = 0.85f))
                )
                InstrumentFinish.MATTE -> Brush.verticalGradient(listOf(primary.copy(alpha = 0.88f), primary.copy(alpha = 0.78f)))
            }

            if (facingFront) {
                drawScope.drawPath(bodyPath, brush = bodyBrush, style = Fill)
            } else {
                // Costas do corpo (madeira/tampa traseira de molas)
                drawScope.drawPath(bodyPath, color = secondary, style = Fill)
                val coverTop = projectVec3(Vec3(-0.14f, 0.24f, thickness + 0.01f), yawDeg, pitchDeg, centerX, centerY, scale).first
                val coverBot = projectVec3(Vec3(0.14f, 0.52f, thickness + 0.01f), yawDeg, pitchDeg, centerX, centerY, scale).first
                drawScope.drawRect(
                    color = Color(0xFF0E0C12),
                    topLeft = Offset(minOf(coverTop.x, coverBot.x), minOf(coverTop.y, coverBot.y)),
                    size = Size(abs(coverBot.x - coverTop.x).coerceAtLeast(8f), abs(coverBot.y - coverTop.y).coerceAtLeast(12f))
                )
            }

            drawScope.drawPath(
                bodyPath,
                color = if (config.finish == InstrumentFinish.NEON_GLOW) hardware else Color.Black.copy(alpha = 0.55f),
                style = Stroke(width = if (config.finish == InstrumentFinish.NEON_GLOW) 3.5f else 2.2f)
            )

            // Braço & Escala (com espessura 3D)
            val neckW = if (isBass) 0.085f else 0.072f
            val neckLength = if (isBass) -1.24f else -1.08f
            val nFront = listOf(
                Vec3(-neckW, neckLength, -thickness * 0.55f),
                Vec3(neckW, neckLength, -thickness * 0.55f),
                Vec3(neckW, 0.05f, -thickness * 0.55f),
                Vec3(-neckW, 0.05f, -thickness * 0.55f)
            ).map { projectVec3(it, yawDeg, pitchDeg, centerX, centerY, scale).first }
            val nBack = listOf(
                Vec3(-neckW, neckLength, thickness * 0.4f),
                Vec3(neckW, neckLength, thickness * 0.4f),
                Vec3(neckW, 0.05f, thickness * 0.4f),
                Vec3(-neckW, 0.05f, thickness * 0.4f)
            ).map { projectVec3(it, yawDeg, pitchDeg, centerX, centerY, scale).first }

            val neckPath = Path().apply {
                val pts = if (facingFront) nFront else nBack
                pts.forEachIndexed { i, pt -> if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y) }
                close()
            }
            drawScope.drawPath(
                neckPath,
                color = if (facingFront) Color(0xFF281915) else Color(0xFFC89D6B),
                style = Fill
            )

            // Headstock 3D + Tarraxas
            val headPts = listOf(
                Vec3(-neckW * 1.1f, neckLength, -thickness * 0.5f),
                Vec3(neckW * 1.8f, neckLength - 0.16f, -thickness * 0.5f),
                Vec3(neckW * 1.4f, neckLength - 0.34f, -thickness * 0.5f),
                Vec3(-neckW * 1.2f, neckLength - 0.24f, -thickness * 0.5f)
            ).map { projectVec3(it, yawDeg, pitchDeg, centerX, centerY, scale).first }
            val headPath = Path().apply {
                headPts.forEachIndexed { i, pt -> if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y) }
                close()
            }
            drawScope.drawPath(headPath, color = primary, style = Fill)
            drawScope.drawPath(headPath, color = Color.Black.copy(alpha = 0.5f), style = Stroke(width = 1.5f))

            if (facingFront) {
                // Estampa no corpo
                drawInstrumentStamp(drawScope, config.stampId, centerX, centerY, scale, yawDeg, pitchDeg, hardware, primary)

                // Braço & Escala (com espessura 3D + Inlays Madre-Pérola estilo Sharkfin)
                val fretsCount = 12
                val inlayFrets = setOf(2, 4, 6, 8, 11)
                for (f in 1..fretsCount) {
                    val fy = neckLength + (0.04f - neckLength) * (f / fretsCount.toFloat())
                    val fL = projectVec3(Vec3(-neckW, fy, -thickness * 0.58f), yawDeg, pitchDeg, centerX, centerY, scale).first
                    val fR = projectVec3(Vec3(neckW, fy, -thickness * 0.58f), yawDeg, pitchDeg, centerX, centerY, scale).first
                    drawScope.drawLine(color = hardware.copy(alpha = 0.78f), start = fL, end = fR, strokeWidth = 1.6f)
                    if (f in inlayFrets) {
                        val fyPrev = neckLength + (0.04f - neckLength) * ((f - 0.68f) / fretsCount.toFloat())
                        val in1 = projectVec3(Vec3(-neckW * 0.52f, fy - 0.015f, -thickness * 0.59f), yawDeg, pitchDeg, centerX, centerY, scale).first
                        val in2 = projectVec3(Vec3(neckW * 0.52f, fy - 0.015f, -thickness * 0.59f), yawDeg, pitchDeg, centerX, centerY, scale).first
                        val in3 = projectVec3(Vec3(neckW * 0.52f, fyPrev, -thickness * 0.59f), yawDeg, pitchDeg, centerX, centerY, scale).first
                        val sharkPath = Path().apply {
                            moveTo(in1.x, in1.y)
                            lineTo(in2.x, in2.y)
                            lineTo(in3.x, in3.y)
                            close()
                        }
                        drawScope.drawPath(sharkPath, color = Color(0xFFEAF6FF).copy(alpha = 0.85f), style = Fill)
                    }
                }

                // Escudo (Pickguard) sutil em guitarras clássicas
                if (config.modelStyleId == "strat_classic" || config.modelStyleId == "flying_v") {
                    val pgPts = listOf(
                        Vec3(0.02f, 0.08f, -thickness - 0.004f),
                        Vec3(0.24f, 0.16f, -thickness - 0.004f),
                        Vec3(0.22f, 0.44f, -thickness - 0.004f),
                        Vec3(-0.08f, 0.46f, -thickness - 0.004f),
                        Vec3(-0.12f, 0.22f, -thickness - 0.004f)
                    ).map { projectVec3(it, yawDeg, pitchDeg, centerX, centerY, scale).first }
                    val pgPath = Path().apply {
                        pgPts.forEachIndexed { idx, off -> if (idx == 0) moveTo(off.x, off.y) else lineTo(off.x, off.y) }
                        close()
                    }
                    drawScope.drawPath(pgPath, color = secondary.copy(alpha = 0.65f), style = Fill)
                    drawScope.drawPath(pgPath, color = Color.White.copy(alpha = 0.35f), style = Stroke(width = 1f))
                }

                // Captadores Humbucker Duplos (EMG/Chrome) & Ponte Floyd-Rose + Alavanca Tremolo
                for (py in listOf(0.18f, 0.36f)) {
                    val pTL = projectVec3(Vec3(-0.12f, py - 0.032f, -thickness - 0.012f), yawDeg, pitchDeg, centerX, centerY, scale).first
                    val pTR = projectVec3(Vec3(0.12f, py - 0.032f, -thickness - 0.012f), yawDeg, pitchDeg, centerX, centerY, scale).first
                    val pBR = projectVec3(Vec3(0.12f, py + 0.032f, -thickness - 0.012f), yawDeg, pitchDeg, centerX, centerY, scale).first
                    val pBL = projectVec3(Vec3(-0.12f, py + 0.032f, -thickness - 0.012f), yawDeg, pitchDeg, centerX, centerY, scale).first
                    val pickupPath = Path().apply {
                        moveTo(pTL.x, pTL.y); lineTo(pTR.x, pTR.y); lineTo(pBR.x, pBR.y); lineTo(pBL.x, pBL.y); close()
                    }
                    drawScope.drawPath(pickupPath, color = Color(0xFF14141A), style = Fill)
                    drawScope.drawPath(pickupPath, color = hardware, style = Stroke(width = 1.6f))
                    // Polos magnéticos
                    for (pole in -2..2) {
                        val polePt = projectVec3(Vec3(pole * 0.04f, py, -thickness - 0.014f), yawDeg, pitchDeg, centerX, centerY, scale).first
                        drawScope.drawCircle(color = hardware.copy(alpha = 0.85f), radius = (scale * 0.012f).coerceAtLeast(1.3f), center = polePt)
                    }
                }
                // Ponte & Whammy Bar
                val brL = projectVec3(Vec3(-0.13f, 0.53f, -thickness - 0.014f), yawDeg, pitchDeg, centerX, centerY, scale).first
                val brR = projectVec3(Vec3(0.13f, 0.53f, -thickness - 0.014f), yawDeg, pitchDeg, centerX, centerY, scale).first
                drawScope.drawLine(color = hardware, start = brL, end = brR, strokeWidth = (scale * 0.06f).coerceAtLeast(5f))
                val whammyStart = projectVec3(Vec3(0.11f, 0.54f, -thickness - 0.016f), yawDeg, pitchDeg, centerX, centerY, scale).first
                val whammyMid = projectVec3(Vec3(0.17f, 0.43f, -thickness - 0.045f), yawDeg, pitchDeg, centerX, centerY, scale).first
                val whammyTip = projectVec3(Vec3(0.22f, 0.34f, -thickness - 0.055f), yawDeg, pitchDeg, centerX, centerY, scale).first
                drawScope.drawLine(color = hardware, start = whammyStart, end = whammyMid, strokeWidth = 2.2f)
                drawScope.drawLine(color = hardware, start = whammyMid, end = whammyTip, strokeWidth = 2.2f)

                // Knobs de Volume / Tone + Chave Seletora
                listOf(Vec3(0.16f, 0.58f, -thickness - 0.014f), Vec3(0.23f, 0.52f, -thickness - 0.014f)).forEach { kp ->
                    val kPos = projectVec3(kp, yawDeg, pitchDeg, centerX, centerY, scale).first
                    drawScope.drawCircle(color = Color(0xFF121018), radius = (scale * 0.028f).coerceAtLeast(2.5f), center = kPos)
                    drawScope.drawCircle(color = hardware, radius = (scale * 0.028f).coerceAtLeast(2.5f), center = kPos, style = Stroke(width = 1.4f))
                }

                // Cordas esticadas
                val numStrings = if (isBass) 4 else 6
                val stringColor = if (config.finish == InstrumentFinish.NEON_GLOW || config.fullSkinId == "skin_plasma_neon") {
                    Color(0xFF00E5FF)
                } else hardware.copy(alpha = 0.85f)
                for (s in 0 until numStrings) {
                    val frac = (s + 0.5f) / numStrings - 0.5f
                    val topS = projectVec3(Vec3(frac * neckW * 1.6f, neckLength, -thickness * 0.62f), yawDeg, pitchDeg, centerX, centerY, scale).first
                    val botS = projectVec3(Vec3(frac * neckW * 1.9f, 0.54f, -thickness - 0.015f), yawDeg, pitchDeg, centerX, centerY, scale).first
                    drawScope.drawLine(color = stringColor, start = topS, end = botS, strokeWidth = if (isBass) 2.2f else 1.2f)
                }

                // Tarraxas metálicas no headstock
                val pegsCount = if (isBass) 4 else 6
                for (pIdx in 0 until pegsCount) {
                    val pegPt = projectVec3(
                        Vec3(neckW * (0.85f + pIdx * 0.14f), neckLength - 0.06f - pIdx * 0.04f, -thickness * 0.5f),
                        yawDeg, pitchDeg, centerX, centerY, scale
                    ).first
                    drawScope.drawCircle(color = hardware, radius = (scale * 0.022f).coerceAtLeast(2f), center = pegPt)
                }

                // Brilho Gloss Especular
                if (config.finish.specularity > 0.6f) {
                    val g1 = projectVec3(Vec3(-0.24f, 0.14f, -thickness - 0.02f), yawDeg, pitchDeg, centerX, centerY, scale).first
                    val g2 = projectVec3(Vec3(-0.08f, 0.44f, -thickness - 0.02f), yawDeg, pitchDeg, centerX, centerY, scale).first
                    drawScope.drawLine(
                        color = Color.White.copy(alpha = config.finish.specularity * 0.38f),
                        start = g1,
                        end = g2,
                        strokeWidth = (scale * 0.045f).coerceAtLeast(4f)
                    )
                }

                // Adesivo personalizado
                drawInstrumentSticker(drawScope, config.stickerId, centerX, centerY, scale, yawDeg, pitchDeg, -thickness - 0.025f)
            }
        }

        Instrument.DRUMS -> {
            // Bateria 3D Completa: Bumbo(s) central, caixa, tons e pratos em órbita 360°
            val kickR = scale * 0.42f
            val kickCenter = projectVec3(Vec3(0f, 0.30f, 0.05f), yawDeg, pitchDeg, centerX, centerY, scale).first
            val kickDepth = projectVec3(Vec3(0f, 0.30f, 0.42f), yawDeg, pitchDeg, centerX, centerY, scale).first

            // Casco traseiro 3D
            drawScope.drawCircle(color = secondary, radius = kickR, center = kickDepth)
            drawScope.drawCircle(color = primary, radius = kickR, center = kickCenter)
            drawScope.drawCircle(color = hardware, radius = kickR, center = kickCenter, style = Stroke(width = 6f))
            // Pele frontal
            drawScope.drawCircle(color = Color(0xFFF3EFE6), radius = kickR * 0.84f, center = kickCenter)
            drawScope.drawCircle(color = Color.Black, radius = kickR * 0.22f, center = Offset(kickCenter.x + kickR * 0.28f, kickCenter.y + kickR * 0.25f))

            // Tons superiores & Caixa
            val tomPositions = listOf(Vec3(-0.34f, -0.18f, -0.08f), Vec3(0.34f, -0.18f, -0.08f), Vec3(-0.46f, 0.16f, -0.22f))
            tomPositions.forEach { tp ->
                val p = projectVec3(tp, yawDeg, pitchDeg, centerX, centerY, scale).first
                drawScope.drawOval(color = primary, topLeft = Offset(p.x - scale * 0.16f, p.y - scale * 0.10f), size = Size(scale * 0.32f, scale * 0.20f))
                drawScope.drawOval(color = Color(0xFFECE7DF), topLeft = Offset(p.x - scale * 0.14f, p.y - scale * 0.08f), size = Size(scale * 0.28f, scale * 0.12f))
                drawScope.drawOval(color = hardware, topLeft = Offset(p.x - scale * 0.16f, p.y - scale * 0.10f), size = Size(scale * 0.32f, scale * 0.20f), style = Stroke(width = 2.5f))
            }

            // Pratos de Bronze / Dourados em hastes cromadas
            val cymbals = listOf(Vec3(-0.62f, -0.44f, 0.10f), Vec3(0.60f, -0.40f, 0.08f), Vec3(-0.72f, -0.12f, -0.20f))
            cymbals.forEachIndexed { idx, cp ->
                val top = projectVec3(cp, yawDeg, pitchDeg, centerX, centerY, scale).first
                val base = projectVec3(cp.copy(y = 0.65f), yawDeg, pitchDeg, centerX, centerY, scale).first
                drawScope.drawLine(color = hardware, start = top, end = base, strokeWidth = 3f)
                val wobble = sin(pulsePhase + idx) * 2f
                drawScope.drawOval(
                    color = Color(0xFFFFD54F),
                    topLeft = Offset(top.x - scale * 0.22f, top.y - scale * 0.055f + wobble),
                    size = Size(scale * 0.44f, scale * 0.11f)
                )
                drawScope.drawOval(
                    color = Color(0xFF7A5901),
                    topLeft = Offset(top.x - scale * 0.22f, top.y - scale * 0.055f + wobble),
                    size = Size(scale * 0.44f, scale * 0.11f),
                    style = Stroke(width = 1.5f)
                )
            }
            if (facingFront) {
                drawInstrumentSticker(drawScope, config.stickerId, kickCenter.x, kickCenter.y - scale * 0.06f, scale * 0.7f, 0f, 0f, 0f)
            }
        }

        Instrument.VOCALS -> {
            // Pedestal Tripé & Microfone Vintage / Supreme 3D
            val micTop = projectVec3(Vec3(0f, -0.78f, -0.05f), yawDeg, pitchDeg, centerX, centerY, scale).first
            val micMid = projectVec3(Vec3(0f, 0.10f, 0f), yawDeg, pitchDeg, centerX, centerY, scale).first
            val foot1 = projectVec3(Vec3(-0.34f, 0.72f, -0.22f), yawDeg, pitchDeg, centerX, centerY, scale).first
            val foot2 = projectVec3(Vec3(0.34f, 0.72f, -0.22f), yawDeg, pitchDeg, centerX, centerY, scale).first
            val foot3 = projectVec3(Vec3(0f, 0.72f, 0.34f), yawDeg, pitchDeg, centerX, centerY, scale).first

            drawScope.drawLine(color = hardware, start = micMid, end = foot1, strokeWidth = 5f)
            drawScope.drawLine(color = hardware, start = micMid, end = foot2, strokeWidth = 5f)
            drawScope.drawLine(color = hardware, start = micMid, end = foot3, strokeWidth = 5f)
            drawScope.drawLine(color = hardware, start = micTop, end = micMid, strokeWidth = 6f)

            // Cápsula Vintage 55S ou Dinâmica
            val capW = scale * 0.24f
            val capH = scale * 0.34f
            drawScope.drawRoundRect(
                color = primary,
                topLeft = Offset(micTop.x - capW * 0.5f, micTop.y - capH * 0.6f),
                size = Size(capW, capH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(capW * 0.45f, capW * 0.45f)
            )
            for (g in -2..2) {
                drawScope.drawLine(
                    color = secondary,
                    start = Offset(micTop.x - capW * 0.38f, micTop.y - capH * 0.12f + g * (capH * 0.13f)),
                    end = Offset(micTop.x + capW * 0.38f, micTop.y - capH * 0.12f + g * (capH * 0.13f)),
                    strokeWidth = 2f
                )
            }
            drawScope.drawRoundRect(
                color = hardware,
                topLeft = Offset(micTop.x - capW * 0.5f, micTop.y - capH * 0.6f),
                size = Size(capW, capH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(capW * 0.45f, capW * 0.45f),
                style = Stroke(width = 2.5f)
            )
        }
    }
}

private fun drawInstrumentStamp(
    drawScope: DrawScope,
    stampId: String,
    cx: Float,
    cy: Float,
    scale: Float,
    yawDeg: Float,
    pitchDeg: Float,
    accent: Color,
    primary: Color
) {
    val z = -0.135f
    when (stampId) {
        "inst_stamp_flames" -> {
            // Chamas clássicas em camadas laranja/branco/amarelo como guitarra hot-rod
            val outerPts = listOf(
                Vec3(-0.30f, 0.66f, z),
                Vec3(-0.22f, 0.28f, z),
                Vec3(-0.10f, 0.46f, z),
                Vec3(0.02f, 0.12f, z),
                Vec3(0.14f, 0.44f, z),
                Vec3(0.28f, 0.24f, z),
                Vec3(0.30f, 0.68f, z)
            ).map { projectVec3(it, yawDeg, pitchDeg, cx, cy, scale).first }
            val outerPath = Path().apply {
                outerPts.forEachIndexed { idx, off -> if (idx == 0) moveTo(off.x, off.y) else lineTo(off.x, off.y) }
                close()
            }
            drawScope.drawPath(outerPath, color = Color(0xFFFF5722).copy(alpha = 0.85f), style = Fill)
            drawScope.drawPath(outerPath, color = Color.White.copy(alpha = 0.72f), style = Stroke(width = 2.0f))

            val innerPts = listOf(
                Vec3(-0.18f, 0.65f, z - 0.002f),
                Vec3(-0.12f, 0.40f, z - 0.002f),
                Vec3(0.01f, 0.24f, z - 0.002f),
                Vec3(0.12f, 0.44f, z - 0.002f),
                Vec3(0.18f, 0.65f, z - 0.002f)
            ).map { projectVec3(it, yawDeg, pitchDeg, cx, cy, scale).first }
            val innerPath = Path().apply {
                innerPts.forEachIndexed { idx, off -> if (idx == 0) moveTo(off.x, off.y) else lineTo(off.x, off.y) }
                close()
            }
            drawScope.drawPath(innerPath, color = Color(0xFFFFEB3B).copy(alpha = 0.90f), style = Fill)
        }
        "inst_stamp_stripes" -> {
            for (dx in listOf(-0.14f, -0.07f)) {
                val p1 = projectVec3(Vec3(dx, 0.08f, z), yawDeg, pitchDeg, cx, cy, scale).first
                val p2 = projectVec3(Vec3(dx, 0.70f, z), yawDeg, pitchDeg, cx, cy, scale).first
                drawScope.drawLine(color = accent.copy(alpha = 0.75f), start = p1, end = p2, strokeWidth = (scale * 0.032f).coerceAtLeast(2.5f))
            }
        }
        "inst_stamp_lightning" -> {
            val lPts = listOf(
                Vec3(0.12f, 0.14f, z),
                Vec3(-0.04f, 0.38f, z),
                Vec3(0.08f, 0.40f, z),
                Vec3(-0.14f, 0.66f, z)
            ).map { projectVec3(it, yawDeg, pitchDeg, cx, cy, scale).first }
            for (i in 0 until lPts.size - 1) {
                drawScope.drawLine(color = Color(0xFF00E5FF), start = lPts[i], end = lPts[i + 1], strokeWidth = 4f)
            }
        }
        "inst_stamp_checker" -> {
            for (r in 0..2) {
                for (c in 0..2) {
                    if ((r + c) % 2 == 0) {
                        val pt = projectVec3(Vec3(-0.16f + c * 0.12f, 0.22f + r * 0.12f, z), yawDeg, pitchDeg, cx, cy, scale).first
                        drawScope.drawCircle(color = Color.White.copy(alpha = 0.38f), radius = scale * 0.035f, center = pt)
                    }
                }
            }
        }
    }
}

private fun drawInstrumentSticker(
    drawScope: DrawScope,
    stickerId: String,
    cx: Float,
    cy: Float,
    scale: Float,
    yawDeg: Float,
    pitchDeg: Float,
    z: Float
) {
    if (stickerId == "sticker_none") return
    val center = projectVec3(Vec3(0.18f, 0.56f, z), yawDeg, pitchDeg, cx, cy, scale).first
    val r = (scale * 0.065f).coerceAtLeast(5f)
    when (stickerId) {
        "sticker_lightning" -> {
            drawScope.drawCircle(color = Color(0xFF1A1423), radius = r * 1.1f, center = center)
            drawScope.drawLine(color = Color(0xFFFFEA00), start = Offset(center.x + r * 0.2f, center.y - r * 0.7f), end = Offset(center.x - r * 0.3f, center.y + r * 0.1f), strokeWidth = 3f)
            drawScope.drawLine(color = Color(0xFFFFEA00), start = Offset(center.x - r * 0.3f, center.y + r * 0.1f), end = Offset(center.x + r * 0.2f, center.y + r * 0.7f), strokeWidth = 3f)
        }
        "sticker_skull" -> {
            drawScope.drawCircle(color = Color.White, radius = r, center = center)
            drawScope.drawCircle(color = Color.Black, radius = r * 0.25f, center = Offset(center.x - r * 0.35f, center.y - r * 0.15f))
            drawScope.drawCircle(color = Color.Black, radius = r * 0.25f, center = Offset(center.x + r * 0.35f, center.y - r * 0.15f))
        }
        "sticker_star" -> {
            drawScope.drawCircle(color = Color(0xFFFFD700), radius = r, center = center)
            drawScope.drawCircle(color = Color(0xFF1A1423), radius = r * 0.45f, center = center)
        }
        "sticker_pick" -> {
            val pickPath = Path().apply {
                moveTo(center.x - r, center.y - r * 0.6f)
                lineTo(center.x + r, center.y - r * 0.6f)
                lineTo(center.x, center.y + r)
                close()
            }
            drawScope.drawPath(pickPath, color = Color(0xFFFF1744))
        }
    }
}

private fun drawGuitarShopVelvetCaseBackdrop(
    drawScope: DrawScope,
    width: Float,
    height: Float,
    accent: Color
) {
    // Parede de tijolos escuros de estúdio + silhuetas de guitarras penduradas ao fundo + case de veludo roxo/vinho
    drawScope.drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF1A1221), Color(0xFF23162D), Color(0xFF120B18))
        ),
        size = Size(width, height)
    )
    // Linhas sutis de parede de tijolos
    val rowH = (height / 8f).coerceAtLeast(18f)
    for (r in 0..8) {
        val y = r * rowH
        drawScope.drawLine(Color.Black.copy(alpha = 0.28f), Offset(0f, y), Offset(width, y), strokeWidth = 1.5f)
    }
    // Silhuetas de guitarras na prateleira de fundo (esquerda/direita)
    listOf(width * 0.12f, width * 0.88f).forEach { gx ->
        drawScope.drawLine(Color(0x33FFFFFF), Offset(gx, height * 0.08f), Offset(gx, height * 0.42f), strokeWidth = 5f)
        drawScope.drawOval(
            color = Color(0x28FFFFFF),
            topLeft = Offset(gx - width * 0.055f, height * 0.36f),
            size = Size(width * 0.11f, height * 0.22f)
        )
    }
    // Estojo aberto em veludo estilo clássico atrás do instrumento central
    val caseLeft = width * 0.22f
    val caseTop = height * 0.07f
    val caseW = width * 0.56f
    val caseH = height * 0.86f
    drawScope.drawRoundRect(
        color = Color(0xFF1C161D),
        topLeft = Offset(caseLeft - 6f, caseTop - 6f),
        size = Size(caseW + 12f, caseH + 12f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(22f, 22f)
    )
    drawScope.drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF6A1B59), Color(0xFF3E0F37), Color(0xFF22081E)),
            center = Offset(width * 0.5f, height * 0.5f),
            radius = maxOf(caseW, caseH) * 0.6f
        ),
        topLeft = Offset(caseLeft, caseTop),
        size = Size(caseW, caseH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
    )
    drawScope.drawRoundRect(
        color = accent.copy(alpha = 0.45f),
        topLeft = Offset(caseLeft, caseTop),
        size = Size(caseW, caseH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
        style = Stroke(width = 2f)
    )
}

@Composable
fun InteractiveInstrument3DViewer(
    instrument: Instrument,
    config: InstrumentCustomConfig,
    modifier: Modifier = Modifier
) {
    var yawDeg by remember { mutableFloatStateOf(24f) }
    var pitchDeg by remember { mutableFloatStateOf(8f) }
    var zoom by remember { mutableFloatStateOf(1.05f) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF120B18), RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        yawDeg = (yawDeg + dragAmount.x * 0.55f) % 360f
                        pitchDeg = (pitchDeg - dragAmount.y * 0.25f).coerceIn(-25f, 30f)
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(0.75f, 1.65f)
                    }
                }
                .testTag("instrument_3d_viewer")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawGuitarShopVelvetCaseBackdrop(
                    drawScope = this,
                    width = size.width,
                    height = size.height,
                    accent = Color(config.primaryColorHex)
                )
                val baseScale = minOf(size.width, size.height) * 0.44f * zoom
                // Spotlight de chão 3D
                drawOval(
                    color = Color(config.primaryColorHex).copy(alpha = 0.24f),
                    topLeft = Offset(size.width * 0.22f, size.height * 0.80f),
                    size = Size(size.width * 0.56f, size.height * 0.12f)
                )
                drawInstrument3D(
                    drawScope = this,
                    instrument = instrument,
                    config = config,
                    centerX = size.width * 0.5f,
                    centerY = size.height * 0.54f,
                    scale = baseScale,
                    yawDeg = yawDeg,
                    pitchDeg = pitchDeg
                )
            }

            Surface(
                color = Color(0x99000000),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
            ) {
                Text(
                    text = "360° • ${(((yawDeg % 360f) + 360f) % 360f).toInt()}°",
                    color = RockAccentCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Frente" to 18f, "Lado" to 90f, "Costas" to 180f).forEach { (label, angle) ->
                TextButton(
                    onClick = { yawDeg = angle; pitchDeg = 6f },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text(label, color = RockAccentGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Slider(
                value = zoom,
                onValueChange = { zoom = it },
                valueRange = 0.75f..1.65f,
                modifier = Modifier.weight(1f).height(24.dp).testTag("instrument_3d_zoom_slider"),
                colors = SliderDefaults.colors(thumbColor = RockAccentCyan, activeTrackColor = RockAccentCyan)
            )
        }
    }
}
