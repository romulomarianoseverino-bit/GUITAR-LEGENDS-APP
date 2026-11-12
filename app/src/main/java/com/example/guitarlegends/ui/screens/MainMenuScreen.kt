package com.example.guitarlegends.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.guitarlegends.ui.components.*
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel

@Composable
fun MainMenuScreen(viewModel: GameViewModel) {
    val stats by viewModel.playerStats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg)
    ) {
        TopStatusBar(
            coins = stats.coins,
            stars = stats.unlockedStars,
            title = "Guitar Legends"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Left Hero Banner (Rock concert atmosphere)
            Card(
                modifier = Modifier
                    .weight(0.9f)
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
                        contentDescription = "Concert Stage",
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
                                text = "ROCK PS2 ERA REBORN",
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
                            fontSize = 24.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Toque com suas próprias músicas em 60-90 FPS horizontal.",
                            color = Color(0xFFD4C8EA),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Main Menu Actions Grid
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        MenuCard(
                            title = "Partida Rápida",
                            subtitle = "Escolha e toque",
                            icon = Icons.Default.PlayArrow,
                            accentColor = RockAccentGreen,
                            testTag = "menu_quick_play",
                            onClick = { viewModel.navigateTo(GameScreen.MusicLibrary) }
                        )
                    }
                    item {
                        MenuCard(
                            title = "Modo Carreira",
                            subtitle = "Desbloqueie palcos",
                            icon = Icons.Default.EmojiEvents,
                            accentColor = RockAccentGold,
                            testTag = "menu_career",
                            onClick = { viewModel.navigateTo(GameScreen.Career) }
                        )
                    }
                    item {
                        MenuCard(
                            title = "Biblioteca Pessoal",
                            subtitle = "Use seus MP3/Áudios",
                            icon = Icons.Default.LibraryMusic,
                            accentColor = RockAccentCyan,
                            testTag = "menu_music_library",
                            onClick = { viewModel.navigateTo(GameScreen.MusicLibrary) }
                        )
                    }
                    item {
                        MenuCard(
                            title = "Multiplayer",
                            subtitle = "Batalha 1v1 & Banda",
                            icon = Icons.Default.Groups,
                            accentColor = RockAccentRed,
                            testTag = "menu_multiplayer",
                            onClick = { viewModel.navigateTo(GameScreen.CompetitiveMultiplayer) }
                        )
                    }
                    item {
                        MenuCard(
                            title = "Customização",
                            subtitle = "Personagens & Guitarras",
                            icon = Icons.Default.Palette,
                            accentColor = RockAccentOrange,
                            testTag = "menu_customization",
                            onClick = { viewModel.navigateTo(GameScreen.Customization) }
                        )
                    }
                    item {
                        MenuCard(
                            title = "Configurações",
                            subtitle = "Latência & Controles",
                            icon = Icons.Default.Settings,
                            accentColor = Color(0xFFB388FF),
                            testTag = "menu_settings",
                            onClick = { viewModel.navigateTo(GameScreen.Settings) }
                        )
                    }
                }

                // Quick stats strip at the bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(RockDarkCard, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⭐ Músicas Concluídas: ${stats.songsCompleted} | Maior Combo: ${stats.maxStreakOverall}x",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "VER RECORDES ▶",
                        color = RockAccentCyan,
                        fontSize = 11.sp,
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
fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .testTag(testTag)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RockDarkCard),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.8f), RockCardBorder))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}
