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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renderizador 3D articulado do integrante da banda no estilo PlayStation 2 modernizado.
 * Suporta rotação completa 360° (frente, perfil e costas), proporções anatômicas por modelo,
 * penteados volumétricos, jaquetas, calças, calçados, tatuagens, acessórios, expressões faciais,
 * skins completas e instrumento customizado sincronizado com animações de palco.
 */
fun drawRockerCharacter3D(
    drawScope: DrawScope,
    stats: PlayerStats,
    instrument: Instrument,
    centerX: Float,
    baseY: Float,
    scale: Float,
    yawDeg: Float = 16f,
    pitchDeg: Float = 4f,
    animTimeSec: Float = 0f,
    isStarPowerActive: Boolean = false,
    combo: Int = 0,
    studioPreview: Boolean = false
) {
    val studio = stats.studioProfile
    val rocker = stats.customRocker
    val yawNorm = ((yawDeg % 360f) + 360f) % 360f
    val facingFront = yawNorm <= 95f || yawNorm >= 265f

    // Resolvendo paleta conforme Skin completa ou customização manual
    val (topColor, pantsColor, shoesColor, accColor, hairColor) = when (studio.fullOutfitSkinId) {
        "outfit_skin_inferno" -> listOf(
            Color(0xFF7B1113), Color(0xFF1A1014), Color(0xFF2B120C), Color(0xFFFF6D00), Color(rocker.hairColorHex)
        )
        "outfit_skin_cyber" -> listOf(
            Color(0xFF0E294B), Color(0xFF161336), Color(0xFF00E5FF), Color(0xFF00E5FF), Color(0xFF00E5FF)
        )
        "outfit_skin_ps2_legend" -> listOf(
            Color(0xFF1F1C26), Color(0xFF233246), Color(0xFF121016), Color(0xFFFFD700), Color(rocker.hairColorHex)
        )
        else -> listOf(
            Color(studio.topColorHex),
            Color(studio.pantsColorHex),
            Color(studio.shoesColorHex),
            Color(studio.accessoryColorHex),
            Color(rocker.hairColorHex)
        )
    }

    val skinColor = Color(studio.skinToneHex)
    val shoulderFactor = when {
        studio.rockerBodyModel.contains("Heavy") -> 1.14f
        studio.rockerBodyModel.contains("Punk") -> 0.94f
        studio.rockerBodyModel.contains("Cyber") -> 1.04f
        else -> 1.0f
    }

    // Movimento de palco sincronizado (Headbang + batida rítmica + energia do combo)
    val energyMult = if (isStarPowerActive) 1.55f else if (combo >= 15) 1.25f else 1.0f
    // Movimento mais perceptível no estúdio para avaliar a aparência do personagem.
    val motionMult = if (studioPreview) 2.2f else 1.0f
    val grooveBounce = sin(animTimeSec * 6.5f) * 0.045f * energyMult * motionMult
    val strumSwing = sin(animTimeSec * 12.0f) * 0.06f * energyMult * motionMult
    val headNod = sin(animTimeSec * 6.5f + 0.4f) * 0.03f * energyMult * motionMult

    val hipY = -0.82f + grooveBounce * 0.4f
    val shoulderY = -1.42f + grooveBounce
    val headY = -1.72f + grooveBounce + headNod

    fun pt(x: Float, y: Float, z: Float): Offset =
        projectVec3(Vec3(x, y, z), yawDeg, pitchDeg, centerX, baseY, scale).first

    fun zOf(x: Float, y: Float, z: Float): Float =
        projectVec3(Vec3(x, y, z), yawDeg, pitchDeg, centerX, baseY, scale).second

    // Sombra realista no palco
    val shadowCenter = pt(0f, 0f, 0f)
    drawScope.drawOval(
        color = if (isStarPowerActive) Color(0x5500E5FF) else Color(0x77000000),
        topLeft = Offset(shadowCenter.x - scale * 0.48f, shadowCenter.y - scale * 0.09f),
        size = Size(scale * 0.96f, scale * 0.18f)
    )

    // 1. PERNAS & BOTAS ARTICULADAS (com joelho flexionado em postura de show)
    val isShorts = rocker.pantsClothing.contains("Bermuda")
    val isPlaid = rocker.pantsClothing.contains("Xadrez")

    fun drawLeg(side: Float) {
        val hip = pt(side * 0.14f, hipY, 0f)
        val knee = pt(side * 0.19f, -0.42f, -0.05f)
        val ankle = pt(side * 0.22f, -0.07f, 0.02f)

        // Coxa
        drawScope.drawLine(color = pantsColor, start = hip, end = knee, strokeWidth = scale * 0.17f)
        // Canela (pele se bermuda, calça caso contrário)
        drawScope.drawLine(
            color = if (isShorts) skinColor else pantsColor,
            start = knee,
            end = ankle,
            strokeWidth = scale * 0.15f
        )
        if (isPlaid && !isShorts) {
            drawScope.drawLine(color = Color(0xFFFF1744).copy(alpha = 0.55f), start = hip, end = knee, strokeWidth = 3f)
            drawScope.drawLine(color = Color(0xFFFF1744).copy(alpha = 0.55f), start = knee, end = ankle, strokeWidth = 3f)
        }
        // Calçado / Coturno detalhado
        val toe = pt(side * 0.24f, -0.02f, -0.14f)
        drawScope.drawLine(color = shoesColor, start = ankle, end = toe, strokeWidth = scale * 0.14f)
        drawScope.drawCircle(color = shoesColor, radius = scale * 0.075f, center = ankle)
        drawScope.drawCircle(color = Color.DarkGray, radius = scale * 0.068f, center = toe)
        // Fivela/Cadarço metálico
        drawScope.drawCircle(color = accColor.copy(alpha = 0.7f), radius = scale * 0.024f, center = Offset((ankle.x + toe.x) * 0.5f, ankle.y))
    }

    // Desenha pernas na ordem Z correta
    if (zOf(-0.18f, hipY, 0f) > zOf(0.18f, hipY, 0f)) {
        drawLeg(-1f); drawLeg(1f)
    } else {
        drawLeg(1f); drawLeg(-1f)
    }

    // 2. TRONCO / JAQUETA 3D COM LARGURA DE OMBRO & PROFUNDIDADE
    val torsoFront = listOf(
        pt(-0.24f * shoulderFactor, shoulderY, -0.11f),
        pt(0.24f * shoulderFactor, shoulderY, -0.11f),
        pt(0.18f, hipY, -0.10f),
        pt(-0.18f, hipY, -0.10f)
    )
    val torsoBack = listOf(
        pt(-0.24f * shoulderFactor, shoulderY, 0.11f),
        pt(0.24f * shoulderFactor, shoulderY, 0.11f),
        pt(0.18f, hipY, 0.10f),
        pt(-0.18f, hipY, 0.10f)
    )

    // Lados do tronco (profundidade 3D)
    for (edge in listOf(0 to 3, 1 to 2)) {
        val (a, b) = edge
        val sidePath = Path().apply {
            moveTo(torsoFront[a].x, torsoFront[a].y)
            lineTo(torsoBack[a].x, torsoBack[a].y)
            lineTo(torsoBack[b].x, torsoBack[b].y)
            lineTo(torsoFront[b].x, torsoFront[b].y)
            close()
        }
        drawScope.drawPath(sidePath, color = topColor.copy(alpha = 0.8f), style = Fill)
    }

    val chestPts = if (facingFront) torsoFront else torsoBack
    val chestPath = Path().apply {
        chestPts.forEachIndexed { idx, off -> if (idx == 0) moveTo(off.x, off.y) else lineTo(off.x, off.y) }
        close()
    }
    drawScope.drawPath(chestPath, color = topColor, style = Fill)
    drawScope.drawPath(chestPath, color = Color.Black.copy(alpha = 0.45f), style = Stroke(width = 2f))

    // Detalhes da roupa conforme opção equipada
    if (facingFront) {
        if (rocker.topClothing.contains("Jaqueta")) {
            val zTop = pt(0f, shoulderY + 0.04f, -0.12f)
            val zBot = pt(0f, hipY, -0.11f)
            drawScope.drawLine(color = Color(0xFFD0D5DD), start = zTop, end = zBot, strokeWidth = 3f)
            // Rebites nos ombros
            drawScope.drawCircle(color = accColor, radius = scale * 0.025f, center = pt(-0.17f, shoulderY + 0.09f, -0.12f))
            drawScope.drawCircle(color = accColor, radius = scale * 0.025f, center = pt(0.17f, shoulderY + 0.09f, -0.12f))
        } else if (rocker.topClothing.contains("Colete")) {
            val emblem = pt(-0.08f, shoulderY + 0.18f, -0.12f)
            drawScope.drawRect(color = accColor, topLeft = Offset(emblem.x - 6f, emblem.y - 6f), size = Size(12f, 12f))
        } else {
            val emblem = pt(0f, shoulderY + 0.22f, -0.12f)
            drawScope.drawCircle(color = accColor.copy(alpha = 0.8f), radius = scale * 0.065f, center = emblem)
        }
        // Cinto metálico
        drawScope.drawLine(
            color = Color(0xFF0E0C12),
            start = pt(-0.19f, hipY + 0.02f, -0.115f),
            end = pt(0.19f, hipY + 0.02f, -0.115f),
            strokeWidth = scale * 0.055f
        )
        drawScope.drawCircle(color = accColor, radius = scale * 0.032f, center = pt(0f, hipY + 0.02f, -0.12f))
    } else {
        // Estampa traseira da jaqueta / colete quando visto de costas!
        val backLogo = pt(0f, shoulderY + 0.24f, 0.12f)
        drawScope.drawCircle(color = accColor.copy(alpha = 0.75f), radius = scale * 0.10f, center = backLogo, style = Stroke(width = 3f))
        drawScope.drawCircle(color = hairColor.copy(alpha = 0.7f), radius = scale * 0.045f, center = backLogo)
    }

    // Braço de fundo antes do instrumento se olhando de frente
    val isTankOrTee = rocker.topClothing.contains("Camiseta") || rocker.topClothing.contains("Regata")
    val hasTattoos = !rocker.bodyTattoos.contains("Sem Tatuagens")
    fun drawArm(side: Float, handTarget: Vec3) {
        val shoulder = pt(side * 0.25f * shoulderFactor, shoulderY + 0.05f, 0f)
        val elbow = pt(side * 0.34f * shoulderFactor, shoulderY + 0.30f, -0.06f)
        val hand = pt(handTarget.x, handTarget.y, handTarget.z)

        val upperColor = if (rocker.topClothing.contains("Regata")) skinColor else topColor
        val lowerColor = if (isTankOrTee) skinColor else topColor

        drawScope.drawLine(color = upperColor, start = shoulder, end = elbow, strokeWidth = scale * 0.12f)
        drawScope.drawLine(color = lowerColor, start = elbow, end = hand, strokeWidth = scale * 0.10f)

        // Tatuagens no antebraço exposto
        if (hasTattoos && isTankOrTee) {
            val midForearm = Offset((elbow.x + hand.x) * 0.5f, (elbow.y + hand.y) * 0.5f)
            val tatColor = if (rocker.bodyTattoos.contains("Raio") || rocker.bodyTattoos.contains("Chamas")) {
                Color(0xFFFF3D00)
            } else {
                Color(0xFF1F2B38)
            }
            drawScope.drawCircle(color = tatColor, radius = scale * 0.036f, center = midForearm)
        }

        // Munhequeira c/ espinhos ou bracelete
        if (rocker.accessories.contains("Munhequeiras") || rocker.accessories.contains("Bracelete")) {
            val wrist = Offset(elbow.x * 0.22f + hand.x * 0.78f, elbow.y * 0.22f + hand.y * 0.78f)
            drawScope.drawCircle(color = Color.Black, radius = scale * 0.052f, center = wrist)
            drawScope.drawCircle(color = accColor, radius = scale * 0.052f, center = wrist, style = Stroke(width = 2f))
        }
        drawScope.drawCircle(color = skinColor, radius = scale * 0.055f, center = hand)
    }

    // Rasgos estilosos em calça jeans rasgada
    if (rocker.pantsClothing.contains("Rasgada") && facingFront) {
        listOf(-0.18f, 0.18f).forEach { sx ->
            val kneeRip = pt(sx, -0.44f, -0.09f)
            drawScope.drawLine(
                color = skinColor.copy(alpha = 0.85f),
                start = Offset(kneeRip.x - scale * 0.045f, kneeRip.y),
                end = Offset(kneeRip.x + scale * 0.045f, kneeRip.y),
                strokeWidth = (scale * 0.032f).coerceAtLeast(2.5f)
            )
        }
    }

    // 3. CABEÇA 3D + PESCOÇO + EXPRESSÃO FACIAL + CABELO VOLUMÉTRICO + ÓCULOS
    val neckBase = pt(0f, shoulderY, 0f)
    val headCenter = pt(0f, headY, 0f)
    drawScope.drawLine(color = skinColor, start = neckBase, end = headCenter, strokeWidth = scale * 0.11f)

    // Cabelo traseiro (atrás da cabeça quando de frente, na frente da camada quando de costas)
    fun drawHair() {
        when {
            rocker.hairStyle.contains("Moicano") -> {
                for (i in 0..4) {
                    val f = i / 4f
                    val zSpike = -0.09f + f * 0.22f
                    val tip = pt(0f, headY - 0.30f + f * 0.05f, zSpike)
                    val b1 = pt(-0.04f, headY - 0.11f, zSpike - 0.03f)
                    val b2 = pt(0.04f, headY - 0.11f, zSpike + 0.03f)
                    val spike = Path().apply {
                        moveTo(b1.x, b1.y); lineTo(tip.x, tip.y); lineTo(b2.x, b2.y); close()
                    }
                    drawScope.drawPath(spike, color = hairColor, style = Fill)
                }
            }
            rocker.hairStyle.contains("Metalhead") || rocker.hairStyle.contains("Dreads") -> {
                val strands = if (rocker.hairStyle.contains("Dreads")) 7 else 5
                for (s in -strands / 2..strands / 2) {
                    val xOff = s * 0.055f
                    val topH = pt(xOff, headY - 0.16f, 0.03f)
                    val botH = pt(xOff * 1.35f, headY + 0.24f + headNod, 0.10f)
                    drawScope.drawLine(
                        color = hairColor,
                        start = topH,
                        end = botH,
                        strokeWidth = scale * if (rocker.hairStyle.contains("Dreads")) 0.055f else 0.075f
                    )
                }
            }
            else -> {
                // Glam Rock Desfiado / Cyber Undercut
                val crown = pt(0f, headY - 0.14f, -0.02f)
                drawScope.drawOval(
                    color = hairColor,
                    topLeft = Offset(crown.x - scale * 0.18f, crown.y - scale * 0.12f),
                    size = Size(scale * 0.36f, scale * 0.22f)
                )
            }
        }
    }

    if (facingFront) drawHair()

    val headShapeScale = when (rocker.artStyle) {
        CharacterArtStyle.STYLIZED_REALISTIC -> 1.0f
        CharacterArtStyle.CARTOON -> 1.12f
        CharacterArtStyle.ANIME -> 1.04f
    }
    val headR = scale * 0.17f * headShapeScale
    val faceColor = if (rocker.bodyTattoos.contains("Gótica")) Color(0xFFECEAE6) else skinColor
    when (rocker.artStyle) {
        CharacterArtStyle.STYLIZED_REALISTIC ->
            drawScope.drawCircle(color = faceColor, radius = headR, center = headCenter)
        CharacterArtStyle.CARTOON ->
            drawScope.drawRoundRect(
                color = faceColor,
                topLeft = Offset(headCenter.x - headR * 0.94f, headCenter.y - headR),
                size = Size(headR * 1.88f, headR * 2.0f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(headR * 0.72f, headR * 0.72f)
            )
        CharacterArtStyle.ANIME ->
            drawScope.drawOval(
                color = faceColor,
                topLeft = Offset(headCenter.x - headR * 0.88f, headCenter.y - headR * 1.06f),
                size = Size(headR * 1.76f, headR * 2.12f)
            )
    }
    when (rocker.artStyle) {
        CharacterArtStyle.STYLIZED_REALISTIC ->
            drawScope.drawCircle(color = Color.Black.copy(alpha = 0.28f), radius = headR, center = headCenter, style = Stroke(width = 1.5f))
        CharacterArtStyle.CARTOON ->
            drawScope.drawRoundRect(
                color = Color.Black.copy(alpha = 0.32f),
                topLeft = Offset(headCenter.x - headR * 0.94f, headCenter.y - headR),
                size = Size(headR * 1.88f, headR * 2.0f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(headR * 0.72f, headR * 0.72f),
                style = Stroke(width = 2.4f)
            )
        CharacterArtStyle.ANIME ->
            drawScope.drawOval(
                color = Color.Black.copy(alpha = 0.28f),
                topLeft = Offset(headCenter.x - headR * 0.88f, headCenter.y - headR * 1.06f),
                size = Size(headR * 1.76f, headR * 2.12f),
                style = Stroke(width = 1.6f)
            )
    }

    if (facingFront) {
        val eyeSpread = if (rocker.artStyle == CharacterArtStyle.ANIME) 1.16f else 1.0f
        val eyeL = pt(-0.055f * eyeSpread, headY - 0.02f, -0.15f)
        val eyeR = pt(0.055f * eyeSpread, headY - 0.02f, -0.15f)
        val hasSunglasses = rocker.accessories.contains("Óculos")

        if (isGothicFace) {
            // Maquiagem corpsepaint rock clássico ao redor dos olhos
            drawScope.drawOval(
                color = Color(0xFF141118),
                topLeft = Offset(eyeL.x - headR * 0.26f, eyeL.y - headR * 0.22f),
                size = Size(headR * 0.44f, headR * 0.44f)
            )
            drawScope.drawOval(
                color = Color(0xFF141118),
                topLeft = Offset(eyeR.x - headR * 0.18f, eyeR.y - headR * 0.22f),
                size = Size(headR * 0.44f, headR * 0.44f)
            )
        }

        if (hasSunglasses) {
            val lensColor = if (rocker.accessories.contains("Neon")) Color(0xFF00E5FF) else Color(0xFF101018)
            drawScope.drawRoundRect(
                color = lensColor,
                topLeft = Offset(eyeL.x - headR * 0.34f, eyeL.y - headR * 0.20f),
                size = Size(headR * 0.56f, headR * 0.36f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawScope.drawRoundRect(
                color = lensColor,
                topLeft = Offset(eyeR.x - headR * 0.22f, eyeR.y - headR * 0.20f),
                size = Size(headR * 0.56f, headR * 0.36f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawScope.drawLine(color = accColor, start = eyeL, end = eyeR, strokeWidth = 2.5f)
        } else if (rocker.artStyle == CharacterArtStyle.ANIME || rocker.artStyle == CharacterArtStyle.CARTOON) {
            val eyeWidth = headR * if (rocker.artStyle == CharacterArtStyle.ANIME) 0.48f else 0.38f
            val eyeHeight = headR * if (rocker.artStyle == CharacterArtStyle.ANIME) 0.62f else 0.42f
            listOf(eyeL, eyeR).forEach { eye ->
                drawScope.drawOval(
                    color = Color.White,
                    topLeft = Offset(eye.x - eyeWidth * 0.5f, eye.y - eyeHeight * 0.5f),
                    size = Size(eyeWidth, eyeHeight)
                )
                drawScope.drawCircle(
                    color = if (rocker.artStyle == CharacterArtStyle.ANIME) Color(0xFF176B9A) else Color(0xFF18141E),
                    radius = eyeWidth * 0.28f,
                    center = eye
                )
                if (rocker.artStyle == CharacterArtStyle.ANIME) {
                    drawScope.drawCircle(color = Color.White, radius = eyeWidth * 0.10f, center = Offset(eye.x - eyeWidth * 0.08f, eye.y - eyeHeight * 0.13f))
                }
            }
        } else {
            drawScope.drawCircle(color = if (isGothicFace) Color.White else Color(0xFF18141E), radius = headR * 0.12f, center = eyeL)
            drawScope.drawCircle(color = if (isGothicFace) Color.White else Color(0xFF18141E), radius = headR * 0.12f, center = eyeR)
        }

        // Expressão facial viva
        val mouthPos = pt(0f, headY + 0.075f, -0.15f)
        val effExpr = if (isStarPowerActive || combo >= 25) StagePoseExpression.SHOUTING_ROCK else studio.stageExpression
        when (effExpr) {
            StagePoseExpression.SHOUTING_ROCK -> {
                drawScope.drawOval(
                    color = Color(0xFF3E0A16),
                    topLeft = Offset(mouthPos.x - headR * 0.26f, mouthPos.y - headR * 0.16f),
                    size = Size(headR * 0.52f, headR * 0.34f)
                )
            }
            StagePoseExpression.SMIRK_LEGEND -> {
                drawScope.drawLine(
                    color = Color(0xFF3A1F1D),
                    start = Offset(mouthPos.x - headR * 0.22f, mouthPos.y),
                    end = Offset(mouthPos.x + headR * 0.26f, mouthPos.y - headR * 0.07f),
                    strokeWidth = 2.5f
                )
            }
            else -> {
                drawScope.drawLine(
                    color = Color(0xFF3A1F1D),
                    start = Offset(mouthPos.x - headR * 0.20f, mouthPos.y),
                    end = Offset(mouthPos.x + headR * 0.20f, mouthPos.y),
                    strokeWidth = 2.5f
                )
            }
        }
    } else {
        drawHair()
    }

    // 4. BRAÇOS E INSTRUMENTO CUSTOMIZADO EMPUNHADO EM 3D
    val activeInstConfig = when (instrument) {
        Instrument.GUITAR, Instrument.ACOUSTIC_GUITAR -> studio.guitarConfig
        Instrument.BASS -> studio.bassConfig
        Instrument.DRUMS -> studio.drumsConfig
        Instrument.VOCALS -> studio.vocalConfig
    }

    when (instrument) {
        Instrument.GUITAR, Instrument.ACOUSTIC_GUITAR, Instrument.BASS -> {
            val frettingHand = Vec3(-0.34f, shoulderY + 0.18f, -0.26f)
            val strummingHand = Vec3(0.14f, hipY - 0.12f + strumSwing, -0.26f)
            drawArm(-1f, frettingHand)
            // Instrumento posicionado na frente do torso se frente, ou atrás/sobreposto conforme yaw
            val instCenter = pt(0.02f, hipY - 0.22f, -0.22f)
            drawInstrument3D(
                drawScope = drawScope,
                instrument = instrument,
                config = activeInstConfig,
                centerX = instCenter.x,
                centerY = instCenter.y,
                scale = scale * 0.52f,
                yawDeg = yawDeg + 18f,
                pitchDeg = pitchDeg - 12f,
                pulsePhase = animTimeSec
            )
            drawArm(1f, strummingHand)
        }
        Instrument.DRUMS -> {
            drawArm(-1f, Vec3(-0.26f, hipY - 0.12f + strumSwing, -0.24f))
            drawArm(1f, Vec3(0.26f, hipY - 0.12f - strumSwing, -0.24f))
            val drumPos = pt(0f, hipY + 0.10f, -0.35f)
            drawInstrument3D(
                drawScope = drawScope,
                instrument = Instrument.DRUMS,
                config = activeInstConfig,
                centerX = drumPos.x,
                centerY = drumPos.y,
                scale = scale * 0.48f,
                yawDeg = yawDeg,
                pitchDeg = pitchDeg,
                pulsePhase = animTimeSec
            )
        }
        Instrument.VOCALS -> {
            drawArm(-1f, Vec3(-0.28f, hipY + 0.04f, -0.06f))
            drawArm(1f, Vec3(0.08f, headY + 0.18f + strumSwing * 0.5f, -0.24f))
            val micPos = pt(0.12f, hipY - 0.05f, -0.26f)
            drawInstrument3D(
                drawScope = drawScope,
                instrument = Instrument.VOCALS,
                config = activeInstConfig,
                centerX = micPos.x,
                centerY = micPos.y,
                scale = scale * 0.55f,
                yawDeg = yawDeg,
                pitchDeg = pitchDeg,
                pulsePhase = animTimeSec
            )
        }
    }
}

/**
 * Desenha integrantes de apoio da banda no palco (baterista no praticável elevado + baixista/guitarrista base).
 */
fun drawStageBandCompanions(
    drawScope: DrawScope,
    width: Float,
    height: Float,
    timeSec: Float,
    isStarPowerActive: Boolean,
    primaryInstrument: Instrument,
    stats: PlayerStats
) {
    // 1. Baterista elevado ao fundo à direita da esteira
    if (primaryInstrument != Instrument.DRUMS) {
        val riserX = width * 0.70f
        val riserY = height * 0.58f
        val riserW = width * 0.16f
        val riserH = height * 0.07f

        // Praticável da bateria
        drawScope.drawRect(
            color = Color(0xFF1A1424),
            topLeft = Offset(riserX - riserW * 0.5f, riserY),
            size = Size(riserW, riserH)
        )
        drawScope.drawRect(
            color = if (isStarPowerActive) Color(0xFF00E5FF) else Color(0xFFFF9100).copy(alpha = 0.6f),
            topLeft = Offset(riserX - riserW * 0.5f, riserY),
            size = Size(riserW, riserH),
            style = Stroke(width = 2f)
        )

        val drumScale = (height * 0.19f).coerceIn(55f, 125f)
        val drummerStats = stats.copy(
            customRocker = CustomRockerAppearance(
                hairStyle = "Moicano Punk Clássico",
                hairColorHex = 0xFFFFD700L,
                topClothing = "Regata Rasgada"
            ),
            studioProfile = stats.studioProfile.copy(
                fullOutfitSkinId = "outfit_skin_none",
                topColorHex = 0xFF282032L,
                rockerBodyModel = "Punk Esguio"
            )
        )
        drawRockerCharacter3D(
            drawScope = drawScope,
            stats = drummerStats,
            instrument = Instrument.DRUMS,
            centerX = riserX,
            baseY = riserY + 4f,
            scale = drumScale,
            yawDeg = -18f + sin(timeSec * 2.1f) * 6f,
            pitchDeg = 4f,
            animTimeSec = timeSec,
            isStarPowerActive = isStarPowerActive,
            combo = 15
        )
    }

    // 2. Baixista / Guitarrista de apoio do lado direito do palco
    val sideStats = stats.copy(
        customRocker = CustomRockerAppearance(
            hairStyle = "Glam Rock Desfiado",
            hairColorHex = 0xFFFF2A55L,
            topClothing = "Jaqueta de Couro c/ Rebites",
            accessories = "Óculos Escuros Aviador"
        ),
        studioProfile = stats.studioProfile.copy(
            fullOutfitSkinId = "outfit_skin_none",
            topColorHex = 0xFF1A1822L,
            pantsColorHex = 0xFF1C2738L
        )
    )
    val companionInst = if (primaryInstrument == Instrument.BASS) Instrument.GUITAR else Instrument.BASS
    val companionScale = (height * 0.25f).coerceIn(72f, 165f)
    drawRockerCharacter3D(
        drawScope = drawScope,
        stats = sideStats,
        instrument = companionInst,
        centerX = width * 0.80f,
        baseY = height * 0.78f,
        scale = companionScale,
        yawDeg = -24f + sin(timeSec * 1.6f) * 8f,
        pitchDeg = 4f,
        animTimeSec = timeSec + 0.7f,
        isStarPowerActive = isStarPowerActive,
        combo = 12
    )
}

private fun drawBackstageMirrorStudioBackdrop(
    drawScope: DrawScope,
    width: Float,
    height: Float,
    accent: Color
) {
    drawScope.drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF1D1128), Color(0xFF2B173B), Color(0xFF120B1A))
        ),
        size = Size(width, height)
    )
    // Parede de tijolos & moldura iluminada estilo camarim rock
    val rowH = (height / 8f).coerceAtLeast(18f)
    for (r in 0..8) {
        drawScope.drawLine(Color.Black.copy(alpha = 0.26f), Offset(0f, r * rowH), Offset(width, r * rowH), strokeWidth = 1.4f)
    }
    // Halo de luz atrás do músico
    drawScope.drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.26f), Color.Transparent),
            center = Offset(width * 0.5f, height * 0.48f),
            radius = minOf(width, height) * 0.45f
        ),
        radius = minOf(width, height) * 0.45f,
        center = Offset(width * 0.5f, height * 0.48f)
    )
}

@Composable
fun InteractiveCharacter3DViewer(
    stats: PlayerStats,
    instrument: Instrument,
    modifier: Modifier = Modifier
) {
    var yawDeg by remember { mutableFloatStateOf(18f) }
    var pitchDeg by remember { mutableFloatStateOf(5f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var previewTimeSec by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startTime = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameTime ->
                previewTimeSec = (frameTime - startTime) / 1_000_000_000f
            }
        }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF120B1A), RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        yawDeg = (yawDeg + dragAmount.x * 0.6f) % 360f
                        pitchDeg = (pitchDeg - dragAmount.y * 0.22f).coerceIn(-18f, 24f)
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(0.75f, 1.55f)
                    }
                }
                .testTag("character_3d_viewer")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawBackstageMirrorStudioBackdrop(
                    drawScope = this,
                    width = size.width,
                    height = size.height,
                    accent = Color(stats.studioProfile.accessoryColorHex)
                )
                val charScale = minOf(size.width * 0.72f, size.height * 0.44f) * zoom
                drawRockerCharacter3D(
                    drawScope = this,
                    stats = stats,
                    instrument = instrument,
                    centerX = size.width * 0.5f,
                    baseY = size.height * 0.90f,
                    scale = charScale,
                    yawDeg = yawDeg,
                    pitchDeg = pitchDeg,
                    animTimeSec = previewTimeSec,
                    studioPreview = true
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
            listOf("Frente" to 16f, "Lado" to 90f, "Costas" to 180f).forEach { (label, angle) ->
                TextButton(
                    onClick = { yawDeg = angle; pitchDeg = 5f },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text(label, color = RockAccentGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Slider(
                value = zoom,
                onValueChange = { zoom = it },
                valueRange = 0.75f..1.55f,
                modifier = Modifier.weight(1f).height(24.dp).testTag("character_3d_zoom_slider"),
                colors = SliderDefaults.colors(thumbColor = RockAccentCyan, activeTrackColor = RockAccentCyan)
            )
        }
    }
}
