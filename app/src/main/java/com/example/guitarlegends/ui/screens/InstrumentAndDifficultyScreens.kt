package com.example.guitarlegends.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guitarlegends.model.Difficulty
import com.example.guitarlegends.model.Instrument
import com.example.guitarlegends.ui.components.*
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel

@Composable
fun InstrumentSelectScreen(viewModel: GameViewModel) {
    val stats by viewModel.playerStats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg)
    ) {
        TopStatusBar(
            coins = stats.coins,
            stars = stats.unlockedStars,
            onBackClick = { viewModel.navigateBack() },
            title = "Escolha seu Instrumento"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "MÚSICA: ${viewModel.selectedTrack.title.uppercase()}",
                color = RockAccentCyan,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(Instrument.values()) { instrument ->
                    val isSelected = instrument == viewModel.selectedInstrument
                    InstrumentCard(
                        instrument = instrument,
                        isSelected = isSelected,
                        onSelect = {
                            viewModel.selectInstrument(instrument)
                            viewModel.navigateTo(GameScreen.DifficultySelect)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InstrumentCard(
    instrument: Instrument,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val accentColor = when (instrument) {
        Instrument.GUITAR -> RockAccentRed
        Instrument.BASS -> Color(0xFF7C4DFF)
        Instrument.DRUMS -> RockAccentOrange
        Instrument.VOCALS -> RockAccentCyan
    }

    val icon: ImageVector = when (instrument) {
        Instrument.GUITAR -> Icons.Default.ElectricBolt
        Instrument.BASS -> Icons.Default.GraphicEq
        Instrument.DRUMS -> Icons.Default.MusicNote
        Instrument.VOCALS -> Icons.Default.Mic
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("instrument_${instrument.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2E174B) else RockDarkCard
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(
                listOf(accentColor, if (isSelected) RockAccentGold else RockCardBorder)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = instrument.displayName.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = instrument.subtitle,
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = instrument.description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = Color(0x18FFFFFF),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Foco: ${instrument.primaryLaneFocus}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DifficultySelectScreen(viewModel: GameViewModel) {
    val stats by viewModel.playerStats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg)
    ) {
        TopStatusBar(
            coins = stats.coins,
            stars = stats.unlockedStars,
            onBackClick = { viewModel.navigateBack() },
            title = "Nível de Dificuldade & GLAS"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Difficulties List
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Difficulty.values().forEach { diff ->
                    val isSelected = diff == viewModel.selectedDifficulty
                    val diffColor = when (diff) {
                        Difficulty.EASY -> RockAccentGreen
                        Difficulty.MEDIUM -> RockAccentGold
                        Difficulty.HARD -> RockAccentOrange
                        Difficulty.EXPERT -> RockAccentRed
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectDifficulty(diff) }
                            .testTag("diff_${diff.name.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF2F174F) else RockDarkCard
                        ),
                        border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(diffColor, RockAccentGold))
                        ) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = diff.displayName.uppercase(),
                                        color = diffColor,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = diffColor.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${diff.lanesUsed} BOTÕES",
                                            color = diffColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = diff.description,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            if (isSelected) {
                                Text(
                                    text = "✔ ESCOLHIDO",
                                    color = RockAccentGold,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right pane: GLAS Adaptive System Toggle + Launch Button
            Card(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(listOf(RockAccentCyan, RockCardBorder))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "SISTEMA ADAPTATIVO GLAS",
                                    color = RockAccentCyan,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Guitar Legends Adaptive System",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.sp
                                )
                            }
                            Switch(
                                checked = stats.glassAdaptiveEnabled,
                                onCheckedChange = { viewModel.toggleAdaptiveSystem(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = RockAccentCyan,
                                    checkedTrackColor = RockAccentCyan.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.testTag("glas_adaptive_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (stats.glassAdaptiveEnabled)
                                "ATIVO: Avalia combos, precisão e reação em tempo real para suavizar ou intensificar as janelas sem acelerar ou distorcer a música."
                            else
                                "DESATIVADO: Dificuldade 100% estática conforme o modo selecionado.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            color = Color(0x15FFFFFF),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "RESUMO DO SHOW", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Música: ${viewModel.selectedTrack.title}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Text(text = "Instrumento: ${viewModel.selectedInstrument.displayName}", color = RockAccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Dificuldade: ${viewModel.selectedDifficulty.displayName}", color = RockAccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    RockButton(
                        text = "⚡ ENTRAR NO PALCO!",
                        onClick = { viewModel.startMatch() },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "start_concert_button",
                        containerColor = RockAccentRed,
                        contentColor = Color.White
                    )
                }
            }
        }
    }
}
