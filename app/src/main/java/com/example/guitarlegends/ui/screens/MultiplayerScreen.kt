package com.example.guitarlegends.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guitarlegends.data.GoogleCredentialResult
import com.example.guitarlegends.data.GoogleSignInHelper
import com.example.guitarlegends.model.*
import com.example.guitarlegends.ui.components.*
import com.example.guitarlegends.viewmodel.AuthUiState
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel
import kotlinx.coroutines.launch

@Composable
fun MultiplayerScreen(viewModel: GameViewModel) {
    BackHandler {
        viewModel.navigateBack()
    }

    val stats by viewModel.playerStats.collectAsState()
    val authState by viewModel.authUiState.collectAsState()
    val room by viewModel.multiplayerRoom.collectAsState()
    val participants by viewModel.multiplayerParticipants.collectAsState()
    val selectedMode by viewModel.multiplayerMode.collectAsState()
    val isBusy by viewModel.isMultiplayerBusy.collectAsState()
    val mpError by viewModel.multiplayerError.collectAsState()
    val connectionWarning by viewModel.multiplayerConnectionWarning.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    val selectedTrack by viewModel.selectedTrackFlow.collectAsState()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var joinCodeInput by remember { mutableStateOf("") }
    var inviteCopiedNotice by remember { mutableStateOf(false) }

    val selfUid = viewModel.currentMultiplayerUid()
    val isSignedIn = selfUid != null && (authState is AuthUiState.Authenticated || authState is AuthUiState.NeedsNickname)
    val isHost = room != null && selfUid != null && room!!.hostUid == selfUid
    val selfParticipant = participants.find { it.userId == selfUid }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg)
    ) {
        TopStatusBar(
            coins = stats.coins,
            stars = stats.unlockedStars,
            onBackClick = { viewModel.navigateBack() },
            title = if (room != null) "Sala Online • ${room!!.roomCode}" else "Multiplayer: Batalha & Banda Online"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Painel Esquerdo: Modalidade, Criar/Entrar na Sala ou Gerenciar Convite/Saída
            Card(
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(listOf(RockAccentRed, RockCardBorder))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "MODALIDADE MULTIPLAYER",
                            color = RockAccentGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ModeSelectorChip(
                                title = "BATALHA 1V1",
                                subtitle = "2 jogadores",
                                isSelected = selectedMode == MultiplayerMode.BATTLE_1V1,
                                accent = RockAccentRed,
                                testTag = "multiplayer_tab_battle",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setMultiplayerMode(MultiplayerMode.BATTLE_1V1) }
                            )
                            ModeSelectorChip(
                                title = "BANDA CO-OP",
                                subtitle = "Até 4 músicos",
                                isSelected = selectedMode == MultiplayerMode.BAND_COOP,
                                accent = RockAccentCyan,
                                testTag = "multiplayer_tab_band",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setMultiplayerMode(MultiplayerMode.BAND_COOP) }
                            )
                        }

                        if (room == null) {
                            if (!isSignedIn) {
                                Surface(
                                    color = RockAccentGold.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = Brush.horizontalGradient(listOf(RockAccentGold, Color.Transparent))
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "MODO VISITANTE • SALAS ONLINE REQUEREM LOGIN",
                                            color = RockAccentGold,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp
                                        )
                                        Text(
                                            text = "Conecte sua conta Google para criar salas reais, convidar amigos por código e sincronizar pontuações ao vivo.",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp
                                        )
                                        RockButton(
                                            text = "ENTRAR COM GOOGLE",
                                            onClick = {
                                                scope.launch {
                                                    viewModel.clearMultiplayerError()
                                                    when (val cred = GoogleSignInHelper.requestGoogleSignIn(context)) {
                                                        is GoogleCredentialResult.Success -> {
                                                            viewModel.signInWithGoogleIdToken(cred.idToken, mergeGuestProgress = true)
                                                        }
                                                        is GoogleCredentialResult.Cancelled -> Unit
                                                        is GoogleCredentialResult.Error -> {
                                                            viewModel.setAuthError(cred.message)
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            testTag = "multiplayer_google_signin_button",
                                            icon = Icons.Default.AccountCircle,
                                            containerColor = RockAccentCyan,
                                            contentColor = Color.Black
                                        )
                                    }
                                }
                            } else {
                                // Usuário autenticado: Criar sala ou Entrar por código
                                RockButton(
                                    text = if (isBusy) "CRIANDO SALA..." else "CRIAR SALA ONLINE",
                                    onClick = { viewModel.createMultiplayerRoom(selectedMode) },
                                    enabled = !isBusy,
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "multiplayer_create_room_button",
                                    icon = Icons.Default.AddCircle,
                                    containerColor = RockAccentGreen,
                                    contentColor = Color.Black
                                )

                                Surface(
                                    color = Color(0x16FFFFFF),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "ENTRAR EM SALA EXISTENTE",
                                            color = RockAccentCyan,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp
                                        )
                                        OutlinedTextField(
                                            value = joinCodeInput,
                                            onValueChange = { joinCodeInput = it.uppercase().take(24) },
                                            label = { Text("Código (ex: ROCK-4829)", fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("multiplayer_join_code_input"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = RockAccentCyan,
                                                unfocusedBorderColor = RockCardBorder,
                                                focusedLabelColor = RockAccentCyan,
                                                unfocusedLabelColor = Color.LightGray
                                            )
                                        )
                                        Button(
                                            onClick = { viewModel.joinMultiplayerRoom(joinCodeInput) },
                                            enabled = joinCodeInput.trim().length >= 4 && !isBusy,
                                            colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(42.dp)
                                                .testTag("multiplayer_join_room_button")
                                        ) {
                                            Text("ENTRAR NA SALA", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Dentro de uma sala ativa
                            val activeRoom = room!!
                            Surface(
                                color = Color(0x2000E5FF),
                                shape = RoundedCornerShape(12.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(listOf(RockAccentCyan, RockAccentGold))
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("multiplayer_room_code_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = activeRoom.roomCode,
                                            color = RockAccentGold,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp
                                        )
                                        Surface(
                                            color = when (activeRoom.statusEnum) {
                                                RoomStatus.WAITING -> RockAccentGreen.copy(alpha = 0.25f)
                                                RoomStatus.PLAYING -> RockAccentCyan.copy(alpha = 0.25f)
                                                RoomStatus.FINISHED -> RockAccentGold.copy(alpha = 0.25f)
                                                RoomStatus.CLOSED -> RockAccentRed.copy(alpha = 0.25f)
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = activeRoom.statusEnum.label.uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Anfitrião: ${activeRoom.hostNickname} • Vagas: ${participants.size}/${activeRoom.maxPlayers}",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 9.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(activeRoom.roomCode))
                                                inviteCopiedNotice = true
                                                val shareText = "Bora tocar no Guitar Legends! Entre na minha sala (${activeRoom.modeEnum.displayName}) com o código: ${activeRoom.roomCode}"
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                }
                                                runCatching {
                                                    context.startActivity(Intent.createChooser(intent, "Convidar jogadores"))
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .testTag("multiplayer_invite_button")
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Convidar", tint = RockAccentCyan, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (inviteCopiedNotice) "COPIADO!" else "CONVIDAR",
                                                color = RockAccentCyan,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp
                                            )
                                        }

                                        Button(
                                            onClick = { viewModel.leaveMultiplayerRoom() },
                                            colors = ButtonDefaults.buttonColors(containerColor = RockAccentRed.copy(alpha = 0.85f)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(0.9f)
                                                .height(36.dp)
                                                .testTag("multiplayer_leave_room_button")
                                        ) {
                                            Text("SAIR DA SALA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (room == null) {
                        RockButton(
                            text = if (selectedMode == MultiplayerMode.BATTLE_1V1) "DESAFIO LOCAL 1V1 ▶" else "TREINO DE BANDA LOCAL ▶",
                            onClick = { viewModel.startMultiplayerMatch() },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "multiplayer_start_button",
                            containerColor = if (selectedMode == MultiplayerMode.BATTLE_1V1) RockAccentRed else RockAccentCyan,
                            contentColor = if (selectedMode == MultiplayerMode.BATTLE_1V1) Color.White else Color.Black
                        )
                    } else if (isHost) {
                        RockButton(
                            text = if (isBusy) "INICIANDO..." else "INICIAR PARTIDA ONLINE ▶",
                            onClick = { viewModel.startMultiplayerMatch() },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "multiplayer_start_button",
                            containerColor = RockAccentGreen,
                            contentColor = Color.Black
                        )
                    } else {
                        val readyNow = selfParticipant?.ready ?: false
                        RockButton(
                            text = if (readyNow) "ESTOU PRONTO ✔" else "CONFIRMAR PRONTO",
                            onClick = { viewModel.toggleMultiplayerReady() },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "multiplayer_ready_button",
                            containerColor = if (readyNow) RockAccentGreen else RockAccentGold,
                            contentColor = Color.Black
                        )
                    }
                }
            }

            // Painel Direito: Participantes Conectados em Tempo Real, Instrumento/Faixa e Reconexão
            Card(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(listOf(RockAccentCyan, RockCardBorder))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!mpError.isNullOrBlank()) {
                        Surface(
                            color = RockAccentRed.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("multiplayer_error_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = mpError.orEmpty(),
                                    color = RockAccentRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearMultiplayerError() }) {
                                    Text("OK", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    if (!connectionWarning.isNullOrBlank()) {
                        Surface(
                            color = RockAccentGold.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("multiplayer_connection_warning")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = connectionWarning.orEmpty(),
                                    color = RockAccentGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (room != null) {
                                    Button(
                                        onClick = { viewModel.reconnectMultiplayerRoom() },
                                        colors = ButtonDefaults.buttonColors(containerColor = RockAccentGold),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("multiplayer_reconnect_button")
                                    ) {
                                        Text("RECONECTAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 8.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (room != null) {
                        val activeRoom = room!!
                        // Barra de Faixa Sincronizada + Loadout do Jogador
                        Surface(
                            color = Color(0x18FFFFFF),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🎵 ${activeRoom.trackTitle}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isHost && activeRoom.statusEnum == RoomStatus.WAITING && tracks.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                val idx = tracks.indexOfFirst { it.id == selectedTrack.id }
                                                val next = tracks[(idx + 1).mod(tracks.size)]
                                                viewModel.updateMultiplayerTrack(next)
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp).testTag("multiplayer_cycle_track_button")
                                        ) {
                                            Text("TROCAR MÚSICA ▶", color = RockAccentCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }

                                // Seleção rápida de Instrumento + Dificuldade sincronizada com a sala
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(Instrument.values().toList()) { inst ->
                                        val sel = viewModel.selectedInstrument == inst
                                        FilterChip(
                                            selected = sel,
                                            onClick = { viewModel.updateMultiplayerLoadout(instrument = inst) },
                                            label = { Text(inst.displayName.substringBefore(" "), fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = RockAccentCyan.copy(alpha = 0.25f),
                                                selectedLabelColor = RockAccentCyan,
                                                labelColor = Color.White.copy(alpha = 0.8f)
                                            )
                                        )
                                    }
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(Difficulty.values().toList()) { diff ->
                                        val sel = viewModel.selectedDifficulty == diff
                                        FilterChip(
                                            selected = sel,
                                            onClick = { viewModel.updateMultiplayerLoadout(difficulty = diff) },
                                            label = { Text(diff.displayName, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = RockAccentGold.copy(alpha = 0.25f),
                                                selectedLabelColor = RockAccentGold,
                                                labelColor = Color.White.copy(alpha = 0.8f)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "MÚSICOS NA SALA (${participants.size}/${activeRoom.maxPlayers})",
                            color = RockAccentCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(participants, key = { it.userId }) { p ->
                                LiveParticipantCard(
                                    participant = p,
                                    isHost = p.userId == activeRoom.hostUid,
                                    isSelf = p.userId == selfUid
                                )
                            }
                            val emptySlots = (activeRoom.maxPlayers - participants.size).coerceAtLeast(0)
                            items(emptySlots) { idx ->
                                Surface(
                                    color = Color(0x0EFFFFFF),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Vaga Aberta #${participants.size + idx + 1} • Compartilhe o código ${activeRoom.roomCode}",
                                        color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Prévia detalhada antes de criar ou entrar em uma sala
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (selectedMode == MultiplayerMode.BATTLE_1V1) "SALÃO DE DUELOS ONLINE (1V1)" else "BANDA COOPERATIVA ONLINE (ATÉ 4)",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (selectedMode == MultiplayerMode.BATTLE_1V1) {
                                    "Crie uma sala para gerar um código único ou informe o código de um amigo para disputar pontuação, combo e Rock Meter em tempo real."
                                } else {
                                    "Cada integrante escolhe Guitarra, Baixo, Bateria ou Vocal e todos sobem ao palco sincronizados na mesma faixa."
                                },
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            Surface(
                                color = Color(0x18FFFFFF),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("SEU PERFIL PARA O PALCO ONLINE", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                    Text(
                                        text = "• Jogador: ${stats.nickname.ifBlank { "Rocker" }} • Instrumento padrão: ${viewModel.selectedInstrument.displayName}\n• Dificuldade: ${viewModel.selectedDifficulty.displayName} • Detecção automática de desconexão e reconexão ativa.",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
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
private fun ModeSelectorChip(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .heightIn(min = 52.dp)
            .testTag(testTag)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accent.copy(alpha = 0.22f) else Color(0x1AFFFFFF)
        ),
        border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(listOf(accent, RockAccentGold))
        ) else null
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
            Text(text = subtitle, color = if (isSelected) accent else Color.White.copy(alpha = 0.6f), fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun LiveParticipantCard(
    participant: MultiplayerParticipantCloud,
    isHost: Boolean,
    isSelf: Boolean
) {
    val isStale = participant.isStale(System.currentTimeMillis())
    val statusColor = when {
        isStale -> RockAccentRed
        participant.connectionStateEnum == ParticipantConnectionState.RECONNECTING -> RockAccentGold
        else -> RockAccentGreen
    }
    val statusText = if (isStale) "DESCONECTADO" else participant.connectionStateEnum.label.uppercase()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("participant_card_${participant.userId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelf) Color(0x282979FF) else Color(0x20FFFFFF)
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(listOf(statusColor.copy(alpha = 0.7f), RockCardBorder))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = participant.nickname + if (isSelf) " (Você)" else "",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                    if (isHost) {
                        Surface(color = RockAccentGold.copy(alpha = 0.25f), shape = RoundedCornerShape(4.dp)) {
                            Text("HOST", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 7.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
                Text(
                    text = "${participant.instrumentEnum.displayName} • ${participant.difficultyEnum.displayName} • ${participant.score} pts (${participant.combo}x)",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 9.sp
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Surface(color = statusColor.copy(alpha = 0.22f), shape = RoundedCornerShape(5.dp)) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = if (participant.ready) RockAccentGreen.copy(alpha = 0.2f) else Color(0x26FFFFFF),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Text(
                        text = if (participant.ready || isHost) "PRONTO" else "AGUARDANDO",
                        color = if (participant.ready || isHost) RockAccentGreen else Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
