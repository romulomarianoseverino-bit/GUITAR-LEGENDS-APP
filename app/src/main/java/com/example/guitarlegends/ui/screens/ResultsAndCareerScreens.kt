package com.example.guitarlegends.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guitarlegends.model.CareerVenue
import com.example.guitarlegends.ui.components.*
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel

@Composable
fun ResultsScreen(viewModel: GameViewModel) {
    val result = viewModel.lastMatchResult
    val stats by viewModel.playerStats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg)
    ) {
        TopStatusBar(
            coins = stats.coins,
            stars = stats.unlockedStars,
            title = "Resultado da Apresentação"
        )

        if (result != null) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Left Pane: Big Stars & Score Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.verticalGradient(listOf(RockAccentGold, RockCardBorder))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = result.track.title.uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${result.instrument.displayName} • ${result.difficulty.displayName}",
                                color = RockAccentCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // 5 Stars display
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (s in 1..5) {
                                val isEarned = s <= result.stars
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isEarned) RockAccentGold else Color(0x33FFFFFF),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Final Score
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "PONTUAÇÃO FINAL", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${result.score}", color = RockAccentGold, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        }

                        // Reward Pill
                        Surface(
                            color = RockAccentCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🪙 +${result.coinsEarned} MOEDAS DE LENDA!",
                                color = RockAccentCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right Pane: Match Breakdown & Action Buttons
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(RockAccentCyan, RockCardBorder))
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "ESTATÍSTICAS DA PARTIDA",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatBox("PRECISÃO", String.format("%.1f%%", result.accuracyPercent))
                                StatBox("MAIOR COMBO", "${result.maxCombo}x")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatBox("NOTAS ACERTADAS", "${result.hitNotes}")
                                StatBox("NOTAS PERDIDAS", "${result.missedNotes}")
                            }

                            if (result.isAdaptiveUsed) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "✔ Sistema Adaptativo GLAS ativo durante esta apresentação",
                                    color = RockAccentGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RockButton(
                            text = "TOCAR NOVAMENTE",
                            onClick = { viewModel.startMatch() },
                            modifier = Modifier.weight(1f),
                            testTag = "results_retry_button",
                            containerColor = RockAccentGreen,
                            contentColor = Color.Black
                        )
                        RockButton(
                            text = "TROCAR MÚSICA",
                            onClick = { viewModel.navigateTo(GameScreen.MusicLibrary) },
                            modifier = Modifier.weight(1f),
                            testTag = "results_change_song_button",
                            containerColor = RockAccentCyan,
                            contentColor = Color.Black
                        )
                        RockButton(
                            text = "MENU PRINCIPAL",
                            onClick = { viewModel.navigateTo(GameScreen.MainMenu) },
                            modifier = Modifier.weight(1f),
                            testTag = "results_menu_button",
                            containerColor = RockDarkCard,
                            contentColor = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Surface(
        color = Color(0x18FFFFFF),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.width(140.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun CareerScreen(viewModel: GameViewModel) {
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
            title = "Modo Carreira: Turnê Mundial"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(viewModel.careerVenues) { venue ->
                val isUnlocked = stats.unlockedStars >= venue.requiredStars
                VenueCard(
                    venue = venue,
                    isUnlocked = isUnlocked,
                    currentStars = stats.unlockedStars,
                    onStart = {
                        // Select first venue track and launch
                        val track = viewModel.tracks.value.find { it.id == venue.trackIds.firstOrNull() }
                            ?: viewModel.tracks.value[0]
                        viewModel.selectTrack(track)
                        viewModel.navigateTo(GameScreen.DifficultySelect)
                    }
                )
            }
        }
    }
}

@Composable
fun VenueCard(
    venue: CareerVenue,
    isUnlocked: Boolean,
    currentStars: Int,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("venue_${venue.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) RockDarkCard else Color(0xFF140D22)
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(
                if (isUnlocked) listOf(RockAccentGold, RockCardBorder)
                else listOf(Color.DarkGray, Color.Transparent)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = venue.name.uppercase(),
                        color = if (isUnlocked) Color.White else Color.Gray,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${venue.location}",
                        color = if (isUnlocked) RockAccentCyan else Color.DarkGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = venue.description,
                    color = Color.White.copy(alpha = if (isUnlocked) 0.7f else 0.4f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "Recompensa: 🪙 ${venue.rewardCoins} Moedas | Estrelas necessárias: ⭐ ${venue.requiredStars}",
                    color = RockAccentGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (isUnlocked) {
                RockButton(
                    text = "TOCAR PALCO ▶",
                    onClick = onStart,
                    testTag = "play_venue_${venue.id}",
                    containerColor = RockAccentGold,
                    contentColor = Color.Black
                )
            } else {
                Surface(
                    color = Color(0x33FF0000),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "BLOQUEADO (${venue.requiredStars - currentStars} ⭐ FALTANDO)",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
