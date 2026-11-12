package com.example.guitarlegends.ui.screens

import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.guitarlegends.engine.LaneColors
import com.example.guitarlegends.engine.drawHighway
import com.example.guitarlegends.model.ControlMode
import com.example.guitarlegends.model.HitRating
import com.example.guitarlegends.ui.components.*
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel

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
    val isPaused by viewModel.isPaused.collectAsState()
    val stats by viewModel.playerStats.collectAsState()

    val track = viewModel.selectedTrack
    val difficulty = viewModel.selectedDifficulty
    val instrument = viewModel.selectedInstrument
    val numLanes = difficulty.lanesUsed

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

        // Center 3D Perspective Highway Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawHighway(
                drawScope = this,
                canvasWidth = size.width,
                canvasHeight = size.height,
                currentTimeMs = currentPosMs,
                notes = activeNotes,
                difficulty = difficulty,
                activeFretLanes = activeFretLanes,
                isStarPowerActive = isStarPowerActive,
                particleSystem = viewModel.particleSystem
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
        }

        // Top Right Controls (GLAS status badge & Pause button)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        // Left HUD: Score, Streak, Multiplier Amplifier, Star Power
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Score Board
            Surface(
                color = Color(0xCC160F2B),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(listOf(RockAccentGold, Color.Transparent))
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text(text = "PONTOS", color = RockAccentGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(text = "$score", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }

            // Multiplier Amplifier & Combo Streak
            Surface(
                color = if (isStarPowerActive) Color(0xEE004D73) else Color(0xCC1E1338),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(
                        if (isStarPowerActive) listOf(RockAccentCyan, Color.White)
                        else listOf(RockAccentRed, RockAccentGold)
                    )
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Big Multiplier Dial
                    Surface(
                        color = if (isStarPowerActive) RockAccentCyan else RockAccentRed,
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${multiplier}X",
                                color = if (isStarPowerActive) Color.Black else Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isStarPowerActive) "OVERDRIVE!" else "COMBO",
                            color = if (isStarPowerActive) RockAccentCyan else Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$combo NOTAS",
                            color = if (combo > 20) RockAccentGold else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Star Power Activator Button
            val canActivateStarPower = starPowerEnergy >= 0.5f && !isStarPowerActive
            Button(
                onClick = { viewModel.activateStarPower() },
                enabled = canActivateStarPower,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStarPowerActive) Color(0xFF00E5FF) else RockAccentCyan,
                    disabledContainerColor = Color(0x4420173A)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("star_power_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (canActivateStarPower) Color.Black else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isStarPowerActive) "⚡ ATIVO!" else if (canActivateStarPower) "⚡ STAR POWER" else "ENERGIA ${(starPowerEnergy * 100).toInt()}%",
                    color = if (canActivateStarPower || isStarPowerActive) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }
        }

        // Right HUD: Rock Meter Gauge & Hit Rating feedback
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Hit rating popup
            if (lastHitRating != null) {
                Surface(
                    color = Color(lastHitRating!!.colorHex).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = lastHitRating!!.label,
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // PS2 Rock Meter Gauge
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xDD18102B)),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(listOf(RockAccentGold, Color.Transparent))
                ),
                modifier = Modifier.width(80.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ROCK",
                        color = RockAccentGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "METER",
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Vertical Meter Bar
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(110.dp)
                            .background(Color.Black, RoundedCornerShape(11.dp))
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(11.dp))
                    ) {
                        val meterColor = when {
                            rockMeter > 0.7f -> RockAccentGreen
                            rockMeter > 0.35f -> RockAccentGold
                            else -> RockAccentRed
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(rockMeter)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(meterColor, meterColor.copy(alpha = 0.6f))
                                    ),
                                    RoundedCornerShape(11.dp)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
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
                        fontSize = 10.sp
                    )
                }
            }
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
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LANE ${lane + 1}",
                                color = if (isPressed) Color.Black else Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
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
