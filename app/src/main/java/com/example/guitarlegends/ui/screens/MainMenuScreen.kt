package com.example.guitarlegends.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.guitarlegends.data.UpdateCheckStatus
import com.example.guitarlegends.ui.components.*
import com.example.guitarlegends.viewmodel.AuthUiState
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel

@Composable
fun MainMenuScreen(viewModel: GameViewModel) {
    val stats by viewModel.playerStats.collectAsState()
    val authState by viewModel.authUiState.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val controllerInfo by viewModel.controllerInfo.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg)
    ) {
        TopStatusBar(
            coins = stats.coins,
            stars = stats.unlockedStars,
            title = when (val auth = authState) {
                is AuthUiState.Authenticated -> "Guitar Legends • ${auth.nickname}"
                else -> if (stats.nickname.isNotBlank()) "Guitar Legends • ${stats.nickname} (Visitante)" else "Guitar Legends • Visitante"
            }
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Painel Esquerdo: Atmosfera do Palco
            Card(
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(listOf(RockAccentRed, RockAccentGold))
                ),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.stage_background_1794455515273),
                        contentDescription = "Palco do Show",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xCC0F081D), Color(0xFA0F081D))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Surface(
                            color = RockAccentRed,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "ERA CLÁSSICA DO ROCK DE PALCO",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "GUITAR LEGENDS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 23.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Toque suas músicas em 60-90 FPS horizontal no modo visitante ou com conta na nuvem.",
                            color = Color(0xFFD4C8EA),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = if (authState is AuthUiState.Authenticated) RockAccentGreen.copy(alpha = 0.22f) else RockAccentGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clickable { viewModel.navigateTo(GameScreen.Profile) }
                                .testTag("hero_account_pill")
                        ) {
                            Text(
                                text = when (val auth = authState) {
                                    is AuthUiState.Authenticated -> "☁ CONTA CONECTADA: ${auth.nickname}"
                                    is AuthUiState.NeedsNickname -> "⚙ FINALIZAR NICKNAME EM MINHA CONTA"
                                    else -> "🎸 MODO VISITANTE • PROGRESSO LOCAL ATIVO"
                                },
                                color = if (authState is AuthUiState.Authenticated) RockAccentGreen else RockAccentGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (controllerInfo.isConnected) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = RockAccentCyan.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable { viewModel.navigateTo(GameScreen.Settings) }
                                    .testTag("menu_controller_badge")
                            ) {
                                Text(
                                    text = "🎮 ${controllerInfo.kind.label.uppercase()} CONECTADO",
                                    color = RockAccentCyan,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (updateStatus is UpdateCheckStatus.UpdateAvailable) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = RockAccentCyan,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { viewModel.navigateTo(GameScreen.Settings) }
                            ) {
                                Text(
                                    text = "⬆ NOVA ATUALIZAÇÃO DISPONÍVEL!",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Painel Direito: Destaques Fixos (Partida Rápida + Multiplayer) & Grade Compacta 2x3
            val activeRoom by viewModel.multiplayerRoom.collectAsState()
            Column(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Linha Hero Fixa: Partida Rápida + MULTIPLAYER (sempre 100% visível sem rolagem)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeaturedMenuCard(
                        title = "Partida Rápida",
                        subtitle = "Escolha e toque agora",
                        icon = Icons.Default.PlayArrow,
                        accentColor = RockAccentGreen,
                        badgeText = "SOLO",
                        testTag = "menu_quick_play",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.setCompetitiveMode(false)
                            viewModel.navigateTo(GameScreen.MusicLibrary)
                        }
                    )
                    FeaturedMenuCard(
                        title = "Multiplayer",
                        subtitle = if (activeRoom != null) "Sala ${activeRoom!!.roomCode} ativa" else "Salas Online 1v1 & Banda",
                        icon = Icons.Default.Groups,
                        accentColor = RockAccentRed,
                        badgeText = if (activeRoom != null) "AO VIVO" else "ONLINE",
                        testTag = "menu_multiplayer",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(GameScreen.CompetitiveMultiplayer) }
                    )
                }

                // Grade 2x3 Compacta (cabe perfeitamente em telas landscape 360dp+ sem cortar itens)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MenuCard(
                            title = "Modo Carreira",
                            subtitle = "Desbloqueie palcos",
                            icon = Icons.Default.EmojiEvents,
                            accentColor = RockAccentGold,
                            testTag = "menu_career",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = {
                                viewModel.setCompetitiveMode(false)
                                viewModel.navigateTo(GameScreen.Career)
                            }
                        )
                        MenuCard(
                            title = "Biblioteca Pessoal",
                            subtitle = "Use MP3/WAV/OGG/FLAC",
                            icon = Icons.Default.LibraryMusic,
                            accentColor = RockAccentCyan,
                            testTag = "menu_music_library",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { viewModel.navigateTo(GameScreen.MusicLibrary) }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MenuCard(
                            title = "Recompensas Diárias",
                            subtitle = "Sequência (${stats.rewardStreak}d)",
                            icon = Icons.Default.CardGiftcard,
                            accentColor = Color(0xFFFFD700),
                            testTag = "menu_daily_rewards",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { viewModel.navigateTo(GameScreen.DailyRewards) }
                        )
                        MenuCard(
                            title = "Customização",
                            subtitle = "Rocker, Skins & Esteiras",
                            icon = Icons.Default.Palette,
                            accentColor = RockAccentOrange,
                            testTag = "menu_customization",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { viewModel.navigateTo(GameScreen.Customization) }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MenuCard(
                            title = "Minha Conta",
                            subtitle = when (val auth = authState) {
                                is AuthUiState.Authenticated -> "${auth.nickname} • Nuvem Ativa"
                                is AuthUiState.NeedsNickname -> "Finalizar Nickname"
                                else -> "Visitante • Login Opcional"
                            },
                            icon = Icons.Default.AccountCircle,
                            accentColor = Color(0xFF80D8FF),
                            testTag = "menu_profile",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { viewModel.navigateTo(GameScreen.Profile) }
                        )
                        MenuCard(
                            title = "Configurações & Versão",
                            subtitle = "Mixer, PS4, Latência & Versões",
                            icon = Icons.Default.Settings,
                            accentColor = Color(0xFFB388FF),
                            testTag = "menu_settings",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { viewModel.navigateTo(GameScreen.Settings) }
                        )
                    }
                }

                // Barra de estatísticas rápidas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RockDarkCard, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⭐ Shows: ${stats.songsCompleted} | Maior Combo: ${stats.maxStreakOverall}x | 🏆 Vitórias 1v1: ${stats.competitiveWins}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "VER RECORDES ▶",
                        color = RockAccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(GameScreen.Statistics) }
                            .testTag("menu_stats_link")
                    )
                }
            }
        }
    }
}

@Composable
fun FeaturedMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    badgeText: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(58.dp)
            .testTag(testTag)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.16f)
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(listOf(accentColor, RockAccentGold))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accentColor,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.Black,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = title.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Surface(
                        color = accentColor.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = accentColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 7.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag(testTag)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RockDarkCard),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.78f), RockCardBorder))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 8.sp,
                    maxLines = 1
                )
            }
        }
    }
}
