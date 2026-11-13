package com.example.guitarlegends.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.guitarlegends.model.ControlMode
import com.example.guitarlegends.model.GraphicsQuality
import com.example.guitarlegends.ui.components.*
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel

@Composable
fun MultiplayerScreen(viewModel: GameViewModel) {
    val stats by viewModel.playerStats.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Batalha 1v1, 1 = Banda Cooperativa
    var roomCode by remember { mutableStateOf("ROCK-7782") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg)
    ) {
        TopStatusBar(
            coins = stats.coins,
            stars = stats.unlockedStars,
            onBackClick = { viewModel.navigateBack() },
            title = "Multiplayer: Batalha & Banda"
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Left Mode Selection & Room Settings
            Card(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(listOf(RockAccentRed, RockCardBorder))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "MODALIDADE MULTIPLAYER",
                            color = RockAccentGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tab 0: Batalha 1v1
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTab = 0 }
                                .testTag("multiplayer_tab_battle"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTab == 0) Color(0xFF33164A) else Color(0x22FFFFFF)
                            ),
                            border = if (selectedTab == 0) ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(RockAccentRed, RockAccentGold))
                            ) else null
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "BATALHA 1V1 (COMPETITIVO)", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                Text(text = "Disputa de pontuação e medidor de cabo-de-guerra", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tab 1: Banda Cooperativa
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTab = 1 }
                                .testTag("multiplayer_tab_band"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTab == 1) Color(0xFF1E284E) else Color(0x22FFFFFF)
                            ),
                            border = if (selectedTab == 1) ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(RockAccentCyan, RockAccentGreen))
                            ) else null
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "BANDA ROCK (COOPERATIVO)", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                Text(text = "Guitarra + Baixo + Bateria + Vocal na mesma faixa", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = Color(0x15FFFFFF),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "SALA WI-FI LOCAL / CÓDIGO", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(text = roomCode, color = RockAccentGold, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                Text(text = "Outros aparelhos na mesma rede podem entrar", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                            }
                        }
                    }

                    RockButton(
                        text = if (selectedTab == 0) "INICIAR BATALHA ROCK ▶" else "FORMAR BANDA & TOCAR ▶",
                        onClick = {
                            viewModel.navigateTo(GameScreen.MusicLibrary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "multiplayer_start_button",
                        containerColor = if (selectedTab == 0) RockAccentRed else RockAccentCyan,
                        contentColor = if (selectedTab == 0) Color.White else Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Lobby Status Pane
            Card(
                modifier = Modifier
                    .weight(1.3f)
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
                    if (selectedTab == 0) {
                        // 1v1 Battle View
                        Column {
                            Text(text = "SALÃO DE DUELOS (1V1 ROCK OFF)", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(text = "Quem errar menos notas empurra o Rock Meter do oponente até o nocaute musical!", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                PlayerSlotCard(name = "Você (Player 1)", role = "Guitarra Lead", isReady = true, color = RockAccentRed)
                                PlayerSlotCard(name = "Desafiante (Player 2)", role = "Guitarra Rítmica", isReady = true, color = RockAccentCyan)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(color = Color(0x18FFFFFF), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = "MODO DE DUELO: CABO-DE-GUERRA", color = RockAccentGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(text = "• Dificuldades individuais permitidas (um pode jogar no Fácil e outro no Hard)\n• Multiplicadores de Star Power atacam a esteira do oponente com raios", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, lineHeight = 14.sp)
                                }
                            }
                        }
                    } else {
                        // Co-op Band View
                        Column {
                            Text(text = "FORMAÇÃO DA BANDA COMPLETA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(text = "Todos os 4 instrumentos tocam sincronizados para atingir 5 estrelas coletivas!", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BandSlot(name = "Guitarra", player = "Você", color = RockAccentRed, isReady = true, modifier = Modifier.weight(1f))
                                BandSlot(name = "Baixo", player = "CPU / Slot 2", color = Color(0xFF7C4DFF), isReady = true, modifier = Modifier.weight(1f))
                                BandSlot(name = "Bateria", player = "CPU / Slot 3", color = RockAccentOrange, isReady = true, modifier = Modifier.weight(1f))
                                BandSlot(name = "Vocal", player = "CPU / Slot 4", color = RockAccentCyan, isReady = true, modifier = Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(color = Color(0x18FFFFFF), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Groove Coletivo: Se algum integrante errar notas consecutivas, o restante da banda pode usar Star Power para salvá-lo da vaia do público!",
                                    color = RockAccentGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Sincronização de áudio em tempo real • Compatível com Galaxy A15 5G",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerSlotCard(name: String, role: String, isReady: Boolean, color: Color) {
    Card(
        modifier = Modifier.width(180.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.verticalGradient(listOf(color, Color.Transparent)))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(text = role, color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Surface(color = if (isReady) RockAccentGreen.copy(alpha = 0.2f) else Color.DarkGray, shape = RoundedCornerShape(6.dp)) {
                Text(
                    text = if (isReady) "PRONTO PARA O ROCK" else "AGUARDANDO",
                    color = if (isReady) RockAccentGreen else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun BandSlot(name: String, player: String, color: Color, isReady: Boolean, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0x22FFFFFF),
        shape = RoundedCornerShape(10.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.verticalGradient(listOf(color, Color.Transparent)))
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = name.uppercase(), color = color, fontWeight = FontWeight.Black, fontSize = 10.sp)
            Text(text = player, color = Color.White, fontSize = 10.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "✔ CONECTADO", color = RockAccentGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CustomizationScreen(viewModel: GameViewModel) {
    val stats by viewModel.playerStats.collectAsState()
    var selectedCategory by remember { mutableStateOf(0) } // 0 = Personagens, 1 = Guitarras

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg)
    ) {
        TopStatusBar(
            coins = stats.coins,
            stars = stats.unlockedStars,
            onBackClick = { viewModel.navigateBack() },
            title = "Customização: Loja de Lendas"
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Left Character/Guitar Illustration Preview
            Card(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(listOf(RockAccentGold, RockCardBorder))
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.rocker_character_1794455529519),
                        contentDescription = "Rocker Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xAA0F081D), Color(0xF50F081D))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "SEU ROCKSTAR EQUIPADO",
                            color = RockAccentCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                        Text(
                            text = viewModel.characters.find { it.id == stats.selectedCharacterId }?.name ?: "Axel Shredder",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Guitarra: ${viewModel.guitars.find { it.id == stats.selectedGuitarId }?.name ?: "Flame Caster"}",
                            color = RockAccentGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Items Catalog
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
            ) {
                // Category Switcher
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { selectedCategory = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCategory == 0) RockAccentGold else RockDarkCard,
                            contentColor = if (selectedCategory == 0) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("custom_tab_characters")
                    ) {
                        Text("PERSONAGENS", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { selectedCategory = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCategory == 1) RockAccentCyan else RockDarkCard,
                            contentColor = if (selectedCategory == 1) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("custom_tab_guitars")
                    ) {
                        Text("GUITARRAS & SKINS", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedCategory == 0) {
                        items(viewModel.characters) { char ->
                            val isEquipped = stats.selectedCharacterId == char.id
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("character_item_${char.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isEquipped) Color(0xFF2E174D) else RockDarkCard),
                                border = if (isEquipped) ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(RockAccentGold, RockAccentRed))) else null
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = char.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                        Text(text = "${char.style} • ${char.outfitName}", color = RockAccentGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(text = char.bio, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                    }
                                    if (isEquipped) {
                                        Text(text = "✔ EQUIPADO", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    } else if (char.isUnlocked || stats.coins >= char.priceCoins) {
                                        Button(
                                            onClick = {
                                                if (char.isUnlocked) viewModel.selectCharacter(char.id)
                                                else viewModel.unlockCharacter(char.id, char.priceCoins)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (char.isUnlocked) RockAccentCyan else RockAccentGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(if (char.isUnlocked) "EQUIPAR" else "DESBLOQUEAR (${char.priceCoins} LC)", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                        }
                                    } else {
                                        Text(text = "🪙 ${char.priceCoins} LC", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        items(viewModel.guitars) { guitar ->
                            val isEquipped = stats.selectedGuitarId == guitar.id
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("guitar_item_${guitar.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isEquipped) Color(0xFF2E174D) else RockDarkCard),
                                border = if (isEquipped) ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(RockAccentCyan, RockAccentGold))) else null
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = guitar.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                        Text(text = guitar.type, color = Color(guitar.colorHex), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(text = guitar.description, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                    }
                                    if (isEquipped) {
                                        Text(text = "✔ EQUIPADA", color = RockAccentCyan, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    } else if (guitar.isUnlocked || stats.coins >= guitar.priceCoins) {
                                        Button(
                                            onClick = {
                                                if (guitar.isUnlocked) viewModel.selectGuitar(guitar.id)
                                                else viewModel.unlockGuitar(guitar.id, guitar.priceCoins)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (guitar.isUnlocked) RockAccentCyan else RockAccentGold),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(if (guitar.isUnlocked) "EQUIPAR" else "COMPRAR (${guitar.priceCoins} LC)", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                        }
                                    } else {
                                        Text(text = "🪙 ${guitar.priceCoins} LC", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsScreen(viewModel: GameViewModel) {
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
            title = "Estatísticas & Hall da Fama"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.verticalGradient(listOf(RockAccentGold, RockCardBorder)))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = "HISTÓRICO DE CARREIRA", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 14.sp)

                    StatRow("Pontuação Total Acumulada", "${stats.totalScore} pts")
                    StatRow("Total de Notas Acertadas", "${stats.notesHitTotal}")
                    StatRow("Maior Sequência Geral (Combo)", "${stats.maxStreakOverall}x")
                    StatRow("Shows Completados com Sucesso", "${stats.songsCompleted}")
                    StatRow("Estrelas de Turnê Conquistadas", "⭐ ${stats.unlockedStars}")
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.verticalGradient(listOf(RockAccentCyan, RockCardBorder)))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = "CLASSIFICAÇÃO DE LENDA", color = RockAccentCyan, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(color = RockAccentCyan.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp)) {
                            Text(
                                text = "RANK ATUAL: LENDA DO ROCK TIER IV",
                                color = RockAccentCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Continue completando apresentações no Modo Carreira para desbloquear o Estádio Mundial e guitarras exclusivas.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    RockButton(
                        text = "VOLTAR AO MENU",
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "stats_back_button",
                        containerColor = RockAccentCyan,
                        contentColor = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun SettingsScreen(viewModel: GameViewModel) {
    val stats by viewModel.playerStats.collectAsState()
    var latency by remember { mutableStateOf(stats.audioLatencyOffsetMs.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg)
    ) {
        TopStatusBar(
            coins = stats.coins,
            stars = stats.unlockedStars,
            onBackClick = { viewModel.navigateBack() },
            title = "Configurações & Calibração"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Audio Latency & Calibration
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.verticalGradient(listOf(RockAccentCyan, RockCardBorder)))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = "CALIBRAÇÃO DE LATÊNCIA DE ÁUDIO", color = RockAccentCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text(text = "Ajuste o atraso entre o som do fone/alto-falante e o toque na esteira.", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(text = "Offset: ${latency.toInt()} ms", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Slider(
                            value = latency,
                            onValueChange = {
                                latency = it
                                viewModel.setLatencyOffset(it.toLong())
                            },
                            valueRange = -100f..150f,
                            colors = SliderDefaults.colors(thumbColor = RockAccentCyan, activeTrackColor = RockAccentCyan),
                            modifier = Modifier.testTag("latency_slider")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "MODO DE CONTROLES", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.setControlMode(ControlMode.DIRECT_TOUCH) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (stats.controlMode == ControlMode.DIRECT_TOUCH) RockAccentGreen else Color(0x33FFFFFF)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("ctrl_direct_touch")
                            ) {
                                Text("TOQUE DIRETO", color = if (stats.controlMode == ControlMode.DIRECT_TOUCH) Color.Black else Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
                            Button(
                                onClick = { viewModel.setControlMode(ControlMode.FRET_AND_STRUM) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (stats.controlMode == ControlMode.FRET_AND_STRUM) RockAccentGold else Color(0x33FFFFFF)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("ctrl_fret_strum")
                            ) {
                                Text("TRASTE + PALHETA", color = if (stats.controlMode == ControlMode.FRET_AND_STRUM) Color.Black else Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
                        }
                    }

                    Text(text = "Galaxy A15 5G (Dimensity 6100+ • Mali-G57 MC2)", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Graphics & Adaptive Settings
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.verticalGradient(listOf(RockAccentGold, RockCardBorder)))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = "TAXA DE QUADROS & GRÁFICOS", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text(text = "Otimizado para tela Super AMOLED 90Hz do Samsung Galaxy A15.", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        GraphicsQuality.values().forEach { gq ->
                            val isSel = stats.graphicsQuality == gq
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.setGraphicsQuality(gq) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isSel) Color(0xFF2F1B4E) else Color(0x22FFFFFF))
                            ) {
                                Text(
                                    text = "${gq.label} ${if (isSel) "✔" else ""}",
                                    color = if (isSel) RockAccentGold else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    RockButton(
                        text = "SALVAR E VOLTAR",
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_save_button",
                        containerColor = RockAccentGreen,
                        contentColor = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen(viewModel: GameViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.stage_background_1794455515273),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xCC0F081D), Color(0xFA0F081D))))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = RockAccentRed,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "PLAYSTATION 2 RHYTHM ERA",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "GUITAR LEGENDS",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 38.sp,
                letterSpacing = 2.sp
            )
            Text(
                text = "Carregando amplificadores e palco...",
                color = RockAccentGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(color = RockAccentCyan, modifier = Modifier.size(28.dp))
        }
    }
}
