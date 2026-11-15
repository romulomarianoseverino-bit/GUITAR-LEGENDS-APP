package com.example.guitarlegends.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.guitarlegends.model.*
import com.example.guitarlegends.ui.components.*
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel

@Composable
fun MultiplayerScreen(viewModel: GameViewModel) {
    BackHandler {
        viewModel.navigateBack()
    }
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
                .fillMaxSize()
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
                            viewModel.setCompetitiveMode(selectedTab == 0)
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
    BackHandler {
        viewModel.navigateBack()
    }
    val stats by viewModel.playerStats.collectAsState()
    var selectedCategory by remember { mutableStateOf(0) } // 0 = Criador & Foto, 1 = Roupas, 2 = Instrumentos, 3 = Esteiras
    var instrumentTab by remember { mutableStateOf(Instrument.GUITAR) }

    // Zero-permission native Android Photo Picker (Google Play compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateCustomRockerPhoto(uri.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg)
    ) {
        TopStatusBar(
            coins = stats.coins,
            stars = stats.unlockedStars,
            onBackClick = { viewModel.navigateBack() },
            title = "Estúdio de Criação & Customização Lendária"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Left Character / Instrument Preview Pane
            Card(
                modifier = Modifier
                    .weight(0.85f)
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
                                    listOf(Color.Transparent, Color(0xAA0F081D), Color(0xFA0F081D))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Style Badge
                        Surface(
                            color = Color(stats.customRocker.artStyle.badgeColorHex),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stats.customRocker.artStyle.displayName.uppercase(),
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Bottom Equipped Rocker Info & Photo Button
                        Column {
                            Text(
                                text = "ROCKSTAR ATUAL",
                                color = RockAccentCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                            Text(
                                text = if (stats.selectedCharacterId == "custom_rocker") "Meu Rocker Personalizado"
                                else viewModel.characters.find { it.id == stats.selectedCharacterId }?.name ?: "Axel Shredder",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "Instrumento: ${viewModel.guitars.find { it.id == stats.selectedGuitarId }?.name ?: "Flame Caster"}",
                                color = RockAccentGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Esteira: ${viewModel.highwayThemes.find { it.id == stats.selectedHighwayThemeId }?.title ?: "PS2 Clássico"}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 9.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth().testTag("pick_photo_rocker_button")
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CRIAR COM FOTO DO CELULAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Right Customization Center with 4 Tabs
            Column(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight()
            ) {
                // Main Category Switcher (4 Tabs)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf("PERSONAGENS & FOTO", "ROUPAS & VISUAL", "INSTRUMENTOS", "ESTEIRAS PS2")
                    tabs.forEachIndexed { index, label ->
                        Button(
                            onClick = { selectedCategory = index },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedCategory == index) RockAccentGold else RockDarkCard,
                                contentColor = if (selectedCategory == index) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("custom_tab_$index")
                        ) {
                            Text(label, fontWeight = FontWeight.Black, fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedCategory) {
                        0 -> {
                            // TAB 0: CRIAÇÃO POR FOTOGRAFIA & PERSONAGENS
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                                    shape = RoundedCornerShape(14.dp),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = Brush.horizontalGradient(listOf(RockAccentCyan, RockCardBorder))
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "1. MODALIDADE DE CRIAÇÃO POR FOTO",
                                            color = RockAccentCyan,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            for (method in CharacterCreationMethod.values()) {
                                                val isSelected = stats.customRocker.creationMethod == method
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { viewModel.updateCustomRockerMethod(method) },
                                                    color = if (isSelected) RockAccentCyan.copy(alpha = 0.25f) else Color(0x18FFFFFF),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(RockAccentCyan, Color.White))) else null
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Text(method.label, color = if (isSelected) RockAccentCyan else Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                                        Text(method.description, color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, lineHeight = 11.sp)
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "2. ESCOLHA O ESTILO VISUAL DE INTERPRETAÇÃO 3D",
                                            color = RockAccentGold,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            for (artStyle in CharacterArtStyle.values()) {
                                                val isSelected = stats.customRocker.artStyle == artStyle
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { viewModel.updateCustomRockerStyle(artStyle) },
                                                    color = if (isSelected) Color(artStyle.badgeColorHex).copy(alpha = 0.25f) else Color(0x18FFFFFF),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color(artStyle.badgeColorHex), Color.White))) else null
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(artStyle.displayName, color = Color(artStyle.badgeColorHex), fontWeight = FontWeight.Black, fontSize = 10.sp, textAlign = TextAlign.Center)
                                                        Text(artStyle.eraDescription, color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, textAlign = TextAlign.Center, lineHeight = 11.sp)
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = { viewModel.equipCharacter("custom_rocker") },
                                            colors = ButtonDefaults.buttonColors(containerColor = RockAccentGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("equip_custom_rocker_button")
                                        ) {
                                            Text("EQUIPAR MEU ROCKER CRIADO", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            item {
                                Text("OU ESCOLHA INTEGRANTES CLÁSSICOS", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                            }

                            items(viewModel.characters) { char ->
                                val isEquipped = stats.selectedCharacterId == char.id
                                val isUnlocked = viewModel.isCosmeticUnlocked(char.id, char.isUnlocked)
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("character_item_${char.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isEquipped) Color(0xFF2E174D) else RockDarkCard),
                                    border = if (isEquipped) ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(RockAccentGold, RockAccentRed))) else null
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = char.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                            Text(text = "${char.style} • ${char.outfitName}", color = RockAccentGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Text(text = char.bio, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                        }
                                        if (isEquipped) {
                                            Text(text = "✔ EQUIPADO", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        } else if (isUnlocked || stats.coins >= char.priceCoins) {
                                            Button(
                                                onClick = {
                                                    if (isUnlocked) viewModel.equipCharacter(char.id)
                                                    else viewModel.unlockCharacter(char.id, char.priceCoins)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (isUnlocked) RockAccentCyan else RockAccentGold),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    if (isUnlocked) "EQUIPAR" else "DESBLOQUEAR (${char.priceCoins} LC)",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        } else {
                                            Text(text = "🪙 ${char.priceCoins} LC", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // TAB 1: ROUPAS & ACESSÓRIOS
                            item {
                                val current = stats.customRocker
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("GUARDA-ROUPA & ACESSÓRIOS DO ROCK", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 12.sp)

                                        OutfitRow("Cabelo", current.hairStyle, listOf("Moicano Punk Clássico", "Metalhead Longo", "Glam Rock Desfiado", "Dreads Rocker")) {
                                            viewModel.updateCustomRockerAppearance(it, current.topClothing, current.pantsClothing, current.shoesClothing, current.accessories, current.bodyTattoos)
                                        }
                                        OutfitRow("Jaqueta / Tronco", current.topClothing, listOf("Jaqueta de Couro c/ Rebites", "Colete Jeans c/ Patches", "Camiseta Band Vintage", "Regata Rasgada")) {
                                            viewModel.updateCustomRockerAppearance(current.hairStyle, it, current.pantsClothing, current.shoesClothing, current.accessories, current.bodyTattoos)
                                        }
                                        OutfitRow("Calça", current.pantsClothing, listOf("Calça Jeans Rasgada", "Calça de Couro Preto", "Calça Xadrez Punk", "Bermuda Cargo Metal")) {
                                            viewModel.updateCustomRockerAppearance(current.hairStyle, current.topClothing, it, current.shoesClothing, current.accessories, current.bodyTattoos)
                                        }
                                        OutfitRow("Calçado", current.shoesClothing, listOf("Coturnos de Couro Pesado", "Tênis Skate Cano Alto", "Botas de Motociclista", "Coturnos Militares")) {
                                            viewModel.updateCustomRockerAppearance(current.hairStyle, current.topClothing, current.pantsClothing, it, current.accessories, current.bodyTattoos)
                                        }
                                        OutfitRow("Acessórios", current.accessories, listOf("Munhequeiras c/ Espinhos & Óculos Aviador", "Correntes & Piercings", "Faixa de Cabeça & Bracelete", "Óculos Neon Holográficos")) {
                                            viewModel.updateCustomRockerAppearance(current.hairStyle, current.topClothing, current.pantsClothing, current.shoesClothing, it, current.bodyTattoos)
                                        }
                                        OutfitRow("Tatuagens & Arte", current.bodyTattoos, listOf("Tatuagem Tribal e Chamas", "Caveiras nos Braços", "Maquiagem Gótica Clássica", "Sem Tatuagens")) {
                                            viewModel.updateCustomRockerAppearance(current.hairStyle, current.topClothing, current.pantsClothing, current.shoesClothing, current.accessories, it)
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // TAB 2: INSTRUMENTOS DA BANDA (GUITARRA, BAIXO, BATERIA, VOCAL)
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (inst in Instrument.values()) {
                                        val isSelected = instrumentTab == inst
                                        Button(
                                            onClick = { instrumentTab = inst },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) RockAccentCyan else Color(0x22FFFFFF),
                                                contentColor = if (isSelected) Color.Black else Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(inst.displayName.split(" ")[0].uppercase(), fontWeight = FontWeight.Black, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }

                            val gearList = when (instrumentTab) {
                                Instrument.GUITAR -> viewModel.guitars.map {
                                    InstrumentGear(it.id, Instrument.GUITAR, it.name, it.type, it.colorHex, it.priceCoins, it.isUnlocked, it.description)
                                }
                                Instrument.BASS -> viewModel.basses
                                Instrument.DRUMS -> viewModel.drums
                                Instrument.VOCALS -> viewModel.vocalMics
                            }

                            val equippedId = when (instrumentTab) {
                                Instrument.GUITAR -> stats.selectedGuitarId
                                Instrument.BASS -> stats.selectedBassId
                                Instrument.DRUMS -> stats.selectedDrumsId
                                Instrument.VOCALS -> stats.selectedVocalMicId
                            }

                            items(gearList) { gear ->
                                val isEquipped = equippedId == gear.id
                                val unlocked = viewModel.isCosmeticUnlocked(gear.id, gear.isUnlocked)
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("gear_item_${gear.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isEquipped) Color(0xFF2E174D) else RockDarkCard),
                                    border = if (isEquipped) ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(RockAccentCyan, RockAccentGold))) else null
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = gear.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                            Text(text = gear.modelStyle, color = Color(gear.colorHex), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Text(text = gear.description, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                        }
                                        if (isEquipped) {
                                            Text(text = "✔ EQUIPADO", color = RockAccentCyan, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        } else if (unlocked || stats.coins >= gear.priceCoins) {
                                            Button(
                                                onClick = {
                                                    if (unlocked) viewModel.selectGear(gear.instrumentType, gear.id)
                                                    else viewModel.unlockGear(gear.instrumentType, gear.id, gear.priceCoins)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (unlocked) RockAccentCyan else RockAccentGold),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(if (unlocked) "EQUIPAR" else "COMPRAR (${gear.priceCoins} LC)", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                            }
                                        } else {
                                            Text(text = "🪙 ${gear.priceCoins} LC", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // TAB 3: ESTEIRAS DE NOTAS (HIGHWAY THEMES)
                            items(viewModel.highwayThemes) { theme ->
                                val isEquipped = stats.selectedHighwayThemeId == theme.id
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("theme_item_${theme.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isEquipped) Color(0xFF2E174D) else RockDarkCard),
                                    border = if (isEquipped) ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(RockAccentGold, RockAccentCyan))) else null
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = Color(theme.primaryColorHex),
                                                    shape = CircleShape,
                                                    modifier = Modifier.size(12.dp)
                                                ) {}
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = theme.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                            }
                                            Text(text = theme.description, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                                        }
                                        if (isEquipped) {
                                            Text(text = "✔ ATIVA NO PALCO", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        } else {
                                            Button(
                                                onClick = { viewModel.equipHighwayTheme(theme.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("EQUIPAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
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
    }
}

@Composable
fun OutfitRow(label: String, selectedOption: String, options: List<String>, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label.uppercase(), color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.chunked(2).forEach { rowOpts ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowOpts.forEach { opt ->
                        val isSelected = selectedOption == opt
                        Button(
                            onClick = { onSelect(opt) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) RockAccentCyan.copy(alpha = 0.3f) else Color(0x2AFFFFFF)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isSelected) "✔ $opt" else opt,
                                color = if (isSelected) RockAccentCyan else Color.White,
                                fontSize = 8.sp,
                                maxLines = 1
                            )
                        }
                    }
                    if (rowOpts.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsScreen(viewModel: GameViewModel) {
    BackHandler {
        viewModel.navigateBack()
    }
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
    BackHandler {
        viewModel.navigateBack()
    }
    val stats by viewModel.playerStats.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val releases by viewModel.releasesCatalog.collectAsState()
    var latency by remember { mutableStateOf(stats.audioLatencyOffsetMs.toFloat()) }
    var showChangelogModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg)
    ) {
        TopStatusBar(
            coins = stats.coins,
            stars = stats.unlockedStars,
            onBackClick = { viewModel.navigateBack() },
            title = "Configurações, Calibração & Versão"
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

                        Spacer(modifier = Modifier.height(10.dp))

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

                        Spacer(modifier = Modifier.height(8.dp))

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

                    Text(
                        text = "Esquema Local v${stats.schemaVersion} • Otimizado para Android",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Graphics & Version Updates Panel
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.verticalGradient(listOf(RockAccentGold, RockCardBorder)))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = "GRÁFICOS & ATUALIZAÇÕES", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        GraphicsQuality.values().forEach { gq ->
                            val isSel = stats.graphicsQuality == gq
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { viewModel.setGraphicsQuality(gq) },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isSel) Color(0xFF2F1B4E) else Color(0x22FFFFFF))
                            ) {
                                Text(
                                    text = "${gq.label} ${if (isSel) "✔" else ""}",
                                    color = if (isSel) RockAccentGold else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Painel de Versão & Atualizações
                        Surface(
                            color = Color(0x18FFFFFF),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "VERSÃO INSTALADA: v${viewModel.updateManager.installedVersionName} (Build ${viewModel.updateManager.installedVersionCode})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                                val statusLabel = when (val st = updateStatus) {
                                    is com.example.guitarlegends.data.UpdateCheckStatus.Idle -> "Toque para verificar novas versões."
                                    is com.example.guitarlegends.data.UpdateCheckStatus.Checking -> "Verificando servidor oficial..."
                                    is com.example.guitarlegends.data.UpdateCheckStatus.UpToDate -> "✔ Atualizado (${st.installedVersion})"
                                    is com.example.guitarlegends.data.UpdateCheckStatus.UpdateAvailable ->
                                        "🔔 Nova versão ${st.latestRelease.versionName} disponível!"
                                    is com.example.guitarlegends.data.UpdateCheckStatus.Error -> "⚠ ${st.message}"
                                }
                                Text(
                                    text = statusLabel,
                                    color = if (updateStatus is com.example.guitarlegends.data.UpdateCheckStatus.UpdateAvailable) RockAccentGold else RockAccentCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { viewModel.checkForUpdates(silent = false) },
                                        colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).testTag("settings_check_updates_button")
                                    ) {
                                        Text("VERIFICAR ATUALIZAÇÕES", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 8.sp, maxLines = 1)
                                    }
                                    Button(
                                        onClick = { showChangelogModal = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("settings_changelog_button")
                                    ) {
                                        Text("NOTAS DE VERSÃO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 8.sp, maxLines = 1)
                                    }
                                }
                                if (updateStatus is com.example.guitarlegends.data.UpdateCheckStatus.UpdateAvailable) {
                                    Button(
                                        onClick = { viewModel.launchOfficialUpdate() },
                                        colors = ButtonDefaults.buttonColors(containerColor = RockAccentGold),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("settings_launch_update_button")
                                    ) {
                                        Text("ABRIR CANAL OFICIAL DE ATUALIZAÇÃO ▶", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 8.sp)
                                    }
                                }
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

    if (showChangelogModal) {
        AlertDialog(
            onDismissRequest = { showChangelogModal = false },
            containerColor = RockDarkCard,
            title = {
                Text("Histórico de Versões", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 15.sp)
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(releases) { rel ->
                        Surface(color = Color(0x18FFFFFF), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "v${rel.versionName} (Build ${rel.versionCode}) • ${rel.releaseDateLabel}",
                                    color = RockAccentCyan,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                                Text(text = rel.releaseTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                if (rel.changelog.isNotBlank()) {
                                    Text(text = rel.changelog, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
                                }
                                if (rel.bugFixes.isNotBlank()) {
                                    Text(text = "Correções:\n${rel.bugFixes}", color = RockAccentGreen, fontSize = 9.sp)
                                }
                                if (rel.performanceNotes.isNotBlank()) {
                                    Text(text = "Desempenho:\n${rel.performanceNotes}", color = RockAccentGold, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showChangelogModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan)
                ) {
                    Text("FECHAR", color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        )
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
                    text = "ERA CLÁSSICA DO ROCK RÍTMICO",
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
