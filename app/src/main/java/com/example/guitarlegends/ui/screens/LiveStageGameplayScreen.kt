package com.example.guitarlegends.ui.screens

import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.guitarlegends.engine.LaneColors
import com.example.guitarlegends.engine.drawHighway
import com.example.guitarlegends.model.ControlMode
import com.example.guitarlegends.model.Instrument
import com.example.guitarlegends.ui.components.*
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LiveStageGameplayScreen(viewModel: GameViewModel) {
    BackHandler {
        viewModel.togglePause()
    }

    val currentPosMs by viewModel.currentSongTimeMs.collectAsState()
    val activeNotes by viewModel.activeNotes.collectAsState()
    val score by viewModel.score.collectAsState()
    val combo by viewModel.combo.collectAsState()
    val multiplier by viewModel.multiplier.collectAsState()
    val rockMeter by viewModel.rockMeter.collectAsState()
    val starPowerEnergy by viewModel.starPowerEnergy.collectAsState()
    val isStarPowerActive by viewModel.isStarPowerActive.collectAsState()
    val lastHitRating by viewModel.lastHitRating.collectAsState()
    val activeFretLanes by viewModel.activeFretLanes.collectAsState()
    val sustainedLanes by viewModel.sustainedLanes.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val stats by viewModel.playerStats.collectAsState()
    val controllerInfo by viewModel.controllerInfo.collectAsState()
    val mpRoom by viewModel.multiplayerRoom.collectAsState()
    val mpParticipants by viewModel.multiplayerParticipants.collectAsState()
    val mpWarning by viewModel.multiplayerConnectionWarning.collectAsState()

    val track = viewModel.selectedTrack
    val difficulty = viewModel.selectedDifficulty
    val instrument = viewModel.selectedInstrument
    val numLanes = difficulty.lanesUsed

    val context = androidx.compose.ui.platform.LocalContext.current
    val highwayConfig = stats.studioProfile.highwayConfig
    val highwayOverlayBitmap = remember(highwayConfig.selectedStampId, highwayConfig.customImagePath) {
        com.example.guitarlegends.data.HighwayImageManager.loadHighwayOverlayBitmap(
            context = context,
            stampId = highwayConfig.selectedStampId,
            customImagePath = highwayConfig.customImagePath
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Concert Stage Visual Background
        Image(
            painter = painterResource(id = R.drawable.stage_background_1794455515273),
            contentDescription = "Palco do Show",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark stage spotlight overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x99000000),
                            Color(0x33000000),
                            Color(0xCC000000)
                        )
                    )
                )
        )

        // Center 3D Perspective Highway & Stage Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val selectedTheme = viewModel.highwayThemes.find { it.id == stats.selectedHighwayThemeId }
                ?: com.example.guitarlegends.model.HighwayTheme.PS2_WOOD_STEEL

            drawHighway(
                drawScope = this,
                canvasWidth = size.width,
                canvasHeight = size.height,
                currentTimeMs = currentPosMs,
                notes = activeNotes,
                difficulty = difficulty,
                activeFretLanes = activeFretLanes,
                sustainedLanes = sustainedLanes,
                isStarPowerActive = isStarPowerActive,
                particleSystem = viewModel.particleSystem,
                highwayTheme = selectedTheme,
                rockMeter = rockMeter,
                combo = combo,
                instrument = instrument,
                playerStats = stats,
                highwayOverlayBitmap = highwayOverlayBitmap,
                overlayOpacity = highwayConfig.stampOpacity
            )
        }

        // Top Left Track & Progress Info
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 8.dp)
        ) {
            Surface(
                color = Color(0xCC1A102E),
                shape = RoundedCornerShape(10.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(listOf(RockAccentCyan, RockCardBorder))
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${track.title.uppercase()} • ${instrument.displayName.uppercase()}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${formatTime(currentPosMs)} / ${formatTime(track.durationMs)}",
                        color = RockAccentCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            if (mpRoom != null && mpParticipants.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xCC120B24),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(RockAccentGold, RockCardBorder))
                    ),
                    modifier = Modifier.testTag("gameplay_multiplayer_scoreboard")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "SALA ${mpRoom!!.roomCode} • AO VIVO",
                            color = RockAccentGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp
                        )
                        val selfUid = viewModel.currentMultiplayerUid()
                        mpParticipants.sortedByDescending { if (it.userId == selfUid) score else it.score }
                            .take(4)
                            .forEach { p ->
                                val displayedScore = if (p.userId == selfUid) score else p.score
                                val stale = p.isStale(System.currentTimeMillis())
                                Text(
                                    text = "${if (stale) "⚠ " else ""}${p.nickname}: $displayedScore pts",
                                    color = if (stale) RockAccentRed else if (p.userId == selfUid) RockAccentCyan else Color.White,
                                    fontWeight = if (p.userId == selfUid) FontWeight.Black else FontWeight.SemiBold,
                                    fontSize = 9.sp
                                )
                            }
                        if (!mpWarning.isNullOrBlank()) {
                            Text(
                                text = mpWarning.orEmpty(),
                                color = RockAccentGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }

        // Top Right Controls (Controller badge, GLAS status badge & Pause button)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (controllerInfo.isConnected) {
                Surface(
                    color = Color(0xCC112A3A),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(RockAccentCyan, Color.Transparent))
                    ),
                    modifier = Modifier.padding(end = 8.dp).testTag("gameplay_controller_badge")
                ) {
                    Text(
                        text = "🎮 ${controllerInfo.kind.label}",
                        color = RockAccentCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            if (stats.glassAdaptiveEnabled) {
                Surface(
                    color = Color(0xCC150D25),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(RockAccentGreen, Color.Transparent))
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = viewModel.adaptiveEngine.statusText,
                        color = RockAccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            IconButton(
                onClick = { viewModel.togglePause() },
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xCC1D1333), CircleShape)
                    .border(1.dp, RockAccentGold, CircleShape)
                    .testTag("game_pause_button")
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Pausa",
                    tint = RockAccentGold,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Top Center Vocal Pitch Guide Bar when singing Vocals
        if (instrument == Instrument.VOCALS) {
            VocalMelodyPitchBar(
                currentTimeMs = currentPosMs,
                activeNotes = activeNotes,
                isStarPowerActive = isStarPowerActive,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 42.dp)
            )
        }

        // Left HUD: Classic Console Guitar-Amp Score, Multiplier Dial, Combo Pips & Star Power
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            GuitarAmpScoreCabinet(
                score = score,
                combo = combo,
                multiplier = multiplier,
                isStarPowerActive = isStarPowerActive
            )

            // Star Power Activator Button
            val canActivateStarPower = starPowerEnergy >= 0.5f && !isStarPowerActive
            Button(
                onClick = { viewModel.activateStarPower() },
                enabled = canActivateStarPower,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStarPowerActive) Color(0xFF00E5FF) else RockAccentCyan,
                    disabledContainerColor = Color(0x66161028)
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(
                        if (canActivateStarPower || isStarPowerActive) listOf(Color.White, RockAccentCyan)
                        else listOf(Color(0x4400E5FF), Color(0x22FFFFFF))
                    )
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .width(126.dp)
                    .testTag("star_power_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (canActivateStarPower || isStarPowerActive) Color.Black else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isStarPowerActive) "ATIVO!" else if (canActivateStarPower) "ESTRELAR!" else "SP ${(starPowerEnergy * 100).toInt()}%",
                    color = if (canActivateStarPower || isStarPowerActive) Color.Black else Color(0xFF9BE7FF),
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                )
            }
        }

        // Right HUD: Hit Rating feedback + Classic Semicircular Rock Meter & 5 Star Power Vacuum Tubes
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hit rating popup
            if (lastHitRating != null) {
                Surface(
                    color = Color(lastHitRating!!.colorHex).copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(Color.White, Color.Transparent))
                    )
                ) {
                    Text(
                        text = lastHitRating!!.label,
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            AnalogRockMeterWithTubes(
                rockMeter = rockMeter,
                starPowerEnergy = starPowerEnergy,
                isStarPowerActive = isStarPowerActive
            )
        }

        // Bottom Controls: Fret Strike Touch Buttons (Multi-touch enabled)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.84f)
                .padding(bottom = 12.dp)
                .height(68.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (lane in 0 until numLanes) {
                val color = LaneColors[lane]
                val isPressed = activeFretLanes.contains(lane)

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .testTag("fret_touch_button_$lane")
                        .pointerInteropFilter { motionEvent ->
                            when (motionEvent.actionMasked) {
                                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                                    viewModel.onFretDown(lane)
                                    true
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                                    viewModel.onFretUp(lane)
                                    true
                                }
                                else -> false
                            }
                        },
                    color = if (isPressed) color else color.copy(alpha = 0.25f),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.verticalGradient(
                            listOf(if (isPressed) Color.White else color, color.copy(alpha = 0.5f))
                        ),
                        width = if (isPressed) 4.dp else 2.dp
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    // Botão visual limpo de traste (sem textos 'PISTA 1', 'PISTA 2', etc.)
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    color = if (isPressed) Color.White else color.copy(alpha = 0.55f),
                                    shape = CircleShape
                                )
                                .border(
                                    width = if (isPressed) 3.dp else 2.dp,
                                    color = if (isPressed) color else Color.White.copy(alpha = 0.7f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(
                                        color = if (isPressed) color else Color.White.copy(alpha = 0.85f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            // Optional Strum bar for Fret+Strum mode
            if (stats.controlMode == ControlMode.FRET_AND_STRUM) {
                Button(
                    onClick = { viewModel.onStrumBar() },
                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentGold),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .testTag("strum_bar_button")
                ) {
                    Text("PALHETA ⚡", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }

        // Pause Menu Dialog Overlay
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
                    .clickable { /* absorb taps */ },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(360.dp)
                        .testTag("pause_dialog"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.verticalGradient(listOf(RockAccentGold, RockCardBorder))
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "SHOW EM PAUSA",
                            color = RockAccentGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${track.title} • ${instrument.displayName}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        RockButton(
                            text = "CONTINUAR SHOW",
                            onClick = { viewModel.togglePause() },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "pause_resume_button",
                            containerColor = RockAccentGreen,
                            contentColor = Color.Black
                        )

                        RockButton(
                            text = "REINICIAR MÚSICA",
                            onClick = { viewModel.restartMatch() },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "pause_restart_button",
                            containerColor = RockAccentCyan,
                            contentColor = Color.Black
                        )

                        RockButton(
                            text = "SAIR PARA O MENU",
                            onClick = {
                                viewModel.stop()
                                viewModel.navigateTo(GameScreen.MainMenu)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "pause_exit_button",
                            containerColor = RockAccentRed,
                            contentColor = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Gabinete de Amplificador clássico PS2 (Score em LED verde, Dial X1..X8,
 * coluna de 10 LEDs de combo e odômetro de sequência de notas).
 */
@Composable
private fun GuitarAmpScoreCabinet(
    score: Int,
    combo: Int,
    multiplier: Int,
    isStarPowerActive: Boolean
) {
    val frameColor = if (isStarPowerActive) RockAccentCyan else Color(0xFFD5B66B)
    val dialRingColor = when {
        isStarPowerActive -> RockAccentCyan
        multiplier >= 4 -> Color(0xFFE040FB)
        multiplier == 3 -> Color(0xFFFFB300)
        multiplier == 2 -> Color(0xFF00E676)
        else -> Color(0xFF78909C)
    }

    val litLeds = when {
        isStarPowerActive || multiplier >= 4 -> 10
        combo <= 0 -> 0
        else -> ((combo - 1) % 10) + 1
    }

    Surface(
        color = Color(0xEE141118),
        shape = RoundedCornerShape(10.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.verticalGradient(listOf(frameColor, Color(0xFF4E4330), frameColor)),
            width = 2.dp
        ),
        modifier = Modifier
            .width(126.dp)
            .testTag("amp_score_cabinet")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Painel LED digital verde da pontuação
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF071209), RoundedCornerShape(5.dp))
                    .border(1.dp, Color(0xFF3E4B3C), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = score.toString().padStart(5, '0'),
                    color = if (isStarPowerActive) RockAccentCyan else Color(0xFF8CFF5E),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
            }

            // Corpo do alto-falante + coluna vertical de LEDs de streak + badge X1..X8
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Coluna de 10 pips de combo
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .background(Color(0xFF0B0A10), RoundedCornerShape(4.dp))
                        .padding(horizontal = 3.dp, vertical = 3.dp)
                ) {
                    for (idx in 9 downTo 0) {
                        val isLit = idx < litLeds
                        val pipColor = when {
                            !isLit -> Color(0xFF282430)
                            isStarPowerActive -> RockAccentCyan
                            idx >= 7 -> Color(0xFFFF4081)
                            idx >= 4 -> RockAccentGold
                            else -> Color(0xFF00E676)
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 8.dp, height = 4.dp)
                                .background(pipColor, RoundedCornerShape(2.dp))
                        )
                    }
                }

                // Dial metálico circular com X1 / X2 / X3 / X4 / X8
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Brush.radialGradient(listOf(Color(0xFF2D253A), Color(0xFF0E0B14))),
                            CircleShape
                        )
                        .border(3.dp, dialRingColor, CircleShape)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    ) {
                        Text(
                            text = "x$multiplier",
                            color = dialRingColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            // Odômetro mecânico da sequência de notas (♪ 194)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A090E), RoundedCornerShape(5.dp))
                    .border(1.dp, Color(0xFF4F4759), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "♪",
                    color = if (isStarPowerActive) RockAccentCyan else RockAccentGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
                Text(
                    text = combo.toString().padStart(3, '0'),
                    color = if (combo >= 30) RockAccentGold else Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

/**
 * Medidor Analógico Semicircular "ROCK" com ponteiro móvel e 5 válvulas luminosas
 * ciano de Energia Estrelar no topo, fiel à referência clássica de console.
 */
@Composable
private fun AnalogRockMeterWithTubes(
    rockMeter: Float,
    starPowerEnergy: Float,
    isStarPowerActive: Boolean
) {
    val animatedRock by animateFloatAsState(
        targetValue = rockMeter.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 160),
        label = "rockNeedle"
    )
    val animatedSp by animateFloatAsState(
        targetValue = starPowerEnergy.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 180),
        label = "spTubes"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(114.dp)
            .testTag("analog_rock_meter")
    ) {
        Box(
            modifier = Modifier
                .width(112.dp)
                .height(92.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val dialCenter = Offset(w * 0.5f, h * 0.88f)
                val dialRadius = w * 0.37f

                // 5 Válvulas radialmente montadas no topo do arco (-48°, -24°, 0°, +24°, +48°)
                val tubeAngles = listOf(-48f, -24f, 0f, 24f, 48f)
                tubeAngles.forEachIndexed { idx, deg ->
                    val threshold = (idx + 1) / 5f
                    val isFilled = isStarPowerActive || animatedSp >= threshold - 0.08f
                    val rad = Math.toRadians((deg - 90f).toDouble())
                    val dist = dialRadius + 16f
                    val cx = dialCenter.x + (cos(rad) * dist).toFloat()
                    val cy = dialCenter.y + (sin(rad) * dist).toFloat()

                    rotate(degrees = deg, pivot = Offset(cx, cy)) {
                        // Halo ciano quando energizada
                        if (isFilled) {
                            drawOval(
                                color = Color(0x8800E5FF),
                                topLeft = Offset(cx - 8f, cy - 13f),
                                size = Size(16f, 24f)
                            )
                        }
                        // Vidro da válvula
                        drawRoundRect(
                            color = if (isFilled) Color(0xFF00E5FF) else Color(0xFF1F2733),
                            topLeft = Offset(cx - 5f, cy - 10f),
                            size = Size(10f, 18f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                        )
                        // Brilho interno branco
                        drawRoundRect(
                            color = if (isFilled) Color.White.copy(alpha = 0.85f) else Color(0x33FFFFFF),
                            topLeft = Offset(cx - 2f, cy - 7f),
                            size = Size(3.5f, 11f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                        )
                        // Base metálica da válvula
                        drawRect(
                            color = Color(0xFF6B7280),
                            topLeft = Offset(cx - 5.5f, cy + 5f),
                            size = Size(11f, 4f)
                        )
                    }
                }

                // Fundo semicircular escuro
                drawArc(
                    color = Color(0xEE100D17),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(dialCenter.x - dialRadius, dialCenter.y - dialRadius),
                    size = Size(dialRadius * 2f, dialRadius * 2f)
                )

                // Setores coloridos: Vermelho (esq) • Amarelo/Ouro (centro) • Verde (dir)
                val arcTopLeft = Offset(dialCenter.x - dialRadius * 0.86f, dialCenter.y - dialRadius * 0.86f)
                val arcSize = Size(dialRadius * 1.72f, dialRadius * 1.72f)
                drawArc(
                    color = Color(0xFFE53935),
                    startAngle = 185f,
                    sweepAngle = 54f,
                    useCenter = true,
                    topLeft = arcTopLeft,
                    size = arcSize
                )
                drawArc(
                    color = Color(0xFFFFC107),
                    startAngle = 241f,
                    sweepAngle = 58f,
                    useCenter = true,
                    topLeft = arcTopLeft,
                    size = arcSize
                )
                drawArc(
                    color = Color(0xFF00E676),
                    startAngle = 301f,
                    sweepAngle = 54f,
                    useCenter = true,
                    topLeft = arcTopLeft,
                    size = arcSize
                )

                // Aro cromado / dourado do mostrador
                drawArc(
                    color = if (isStarPowerActive) Color(0xFF00E5FF) else Color(0xFFD6CEB8),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(dialCenter.x - dialRadius, dialCenter.y - dialRadius),
                    size = Size(dialRadius * 2f, dialRadius * 2f),
                    style = Stroke(width = 5f)
                )
                drawLine(
                    color = Color(0xFF9E9686),
                    start = Offset(dialCenter.x - dialRadius - 2f, dialCenter.y),
                    end = Offset(dialCenter.x + dialRadius + 2f, dialCenter.y),
                    strokeWidth = 4f
                )

                // Ponteiro analógico animado (-75° a +75°)
                val needleDeg = -75f + animatedRock * 150f
                val needleRad = Math.toRadians((needleDeg - 90f).toDouble())
                val tipX = dialCenter.x + (cos(needleRad) * (dialRadius * 0.84f)).toFloat()
                val tipY = dialCenter.y + (sin(needleRad) * (dialRadius * 0.84f)).toFloat()

                drawLine(
                    color = Color.Black.copy(alpha = 0.6f),
                    start = Offset(dialCenter.x + 2f, dialCenter.y + 1f),
                    end = Offset(tipX + 2f, tipY + 1f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = dialCenter,
                    end = Offset(tipX, tipY),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color(0xFF2B2638),
                    radius = 7f,
                    center = dialCenter
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5f,
                    center = dialCenter
                )
            }

            Text(
                text = "ROCK",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }

        // Badge de status de vibração da plateia
        Surface(
            color = Color(0xCC120D1D),
            shape = RoundedCornerShape(6.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.horizontalGradient(
                    listOf(
                        when {
                            rockMeter > 0.75f -> RockAccentGreen
                            rockMeter > 0.38f -> RockAccentGold
                            else -> RockAccentRed
                        },
                        Color.Transparent
                    )
                )
            )
        ) {
            Text(
                text = when {
                    rockMeter > 0.8f -> "LENDA!"
                    rockMeter > 0.5f -> "BOM"
                    rockMeter > 0.25f -> "PERIGO"
                    else -> "VAIAS!"
                },
                color = when {
                    rockMeter > 0.8f -> RockAccentGreen
                    rockMeter > 0.5f -> RockAccentGold
                    else -> RockAccentRed
                },
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Barra discreta superior de guia melódico vocal exibida apenas quando o instrumento é VOCAL.
 */
@Composable
private fun VocalMelodyPitchBar(
    currentTimeMs: Long,
    activeNotes: List<com.example.guitarlegends.model.Note>,
    isStarPowerActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xBB100B1C),
        shape = RoundedCornerShape(10.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(
                if (isStarPowerActive) listOf(RockAccentCyan, Color.White)
                else listOf(RockAccentGold, RockAccentCyan)
            )
        ),
        modifier = modifier
            .fillMaxWidth(0.46f)
            .height(26.dp)
            .testTag("vocal_pitch_bar")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val windowMs = 2600f
                val playheadX = w * 0.18f

                // Guias sutis de afinação
                for (row in 1..4) {
                    val y = h * (row / 5f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                activeNotes.forEach { n ->
                    if (!n.isMissed) {
                        val dt = (n.timeMs - currentTimeMs).toFloat()
                        if (dt in -300f..windowMs) {
                            val startX = playheadX + (dt / windowMs) * (w - playheadX)
                            val lenW = ((n.durationMs.coerceAtLeast(180L).toFloat()) / windowMs) * (w - playheadX)
                            val pitchY = h * (0.82f - (n.lane.coerceIn(0, 4) / 4f) * 0.64f)
                            val phraseColor = if (isStarPowerActive || n.isStarPower) Color(0xFF00E5FF) else Color(0xFFFFD54F)
                            drawLine(
                                color = phraseColor,
                                start = Offset(startX.coerceAtLeast(0f), pitchY),
                                end = Offset((startX + lenW).coerceAtMost(w), pitchY),
                                strokeWidth = 5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Agulha vocal
                drawLine(
                    color = Color.White,
                    start = Offset(playheadX, 3f),
                    end = Offset(playheadX, h - 3f),
                    strokeWidth = 2f
                )
            }
        }
    }
}

