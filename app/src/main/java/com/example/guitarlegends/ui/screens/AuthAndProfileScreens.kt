package com.example.guitarlegends.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guitarlegends.data.GoogleCredentialResult
import com.example.guitarlegends.data.GoogleSignInHelper
import com.example.guitarlegends.data.UpdateCheckStatus
import com.example.guitarlegends.model.AchievementCatalog
import com.example.guitarlegends.ui.components.*
import com.example.guitarlegends.viewmodel.AuthUiState
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(viewModel: GameViewModel) {
    ProfileScreen(viewModel = viewModel)
}

@Composable
fun ProfileScreen(viewModel: GameViewModel) {
    BackHandler { viewModel.navigateBack() }

    val stats by viewModel.playerStats.collectAsState()
    val authState by viewModel.authUiState.collectAsState()
    val cloudStatus by viewModel.cloudSyncStatus.collectAsState()
    val hasUnlinkedGuest by viewModel.hasUnlinkedGuestProgress.collectAsState()
    val rewardHistory by viewModel.rewardHistory.collectAsState()
    val progressionHistory by viewModel.progressionHistory.collectAsState()
    val authError by viewModel.authErrorMessage.collectAsState()
    val isBusy by viewModel.isAuthBusy.collectAsState()
    val nickAvail by viewModel.nicknameAvailability.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showEditNicknameDialog by remember { mutableStateOf(false) }
    var editedNickname by remember { mutableStateOf(stats.nickname) }
    var cloudNicknameInput by remember { mutableStateOf(stats.nickname) }
    var mergeGuestOnLink by remember(hasUnlinkedGuest) { mutableStateOf(true) }

    if (showEditNicknameDialog) {
        val isAuthenticated = authState is AuthUiState.Authenticated
        AlertDialog(
            onDismissRequest = { showEditNicknameDialog = false },
            title = {
                Text(
                    text = if (isAuthenticated) "Alterar Nickname Exclusivo" else "Alterar Apelido de Visitante",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isAuthenticated) {
                            "Seu nickname anterior será liberado somente se o novo estiver disponível na nuvem."
                        } else {
                            "Defina seu nome de palco local para jogar no modo visitante."
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = editedNickname,
                        onValueChange = { editedNickname = it.take(24) },
                        label = { Text(if (isAuthenticated) "Novo Nickname" else "Apelido no Palco") },
                        singleLine = true,
                        modifier = Modifier.testTag("edit_nickname_dialog_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RockAccentCyan,
                            unfocusedBorderColor = RockCardBorder
                        )
                    )
                    if (!authError.isNullOrBlank()) {
                        Text(authError.orEmpty(), color = RockAccentRed, fontSize = 10.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isAuthenticated) {
                            viewModel.submitNickname(editedNickname)
                        } else {
                            viewModel.updateGuestNickname(editedNickname)
                        }
                        showEditNicknameDialog = false
                    },
                    enabled = editedNickname.trim().length >= (if (isAuthenticated) 3 else 2) && !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan)
                ) {
                    Text("SALVAR", color = Color.Black, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNicknameDialog = false }) {
                    Text("CANCELAR", color = Color.White)
                }
            },
            containerColor = RockDarkCard
        )
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
            title = "Minha Conta & Perfil"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cartão Esquerdo: Minha Conta (Modo Visitante, Vincular/Entrar com Google, Gerenciar e Sair)
            Card(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(
                        listOf(
                            if (authState is AuthUiState.Authenticated) RockAccentGreen else RockAccentGold,
                            RockCardBorder
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    when (val state = authState) {
                        is AuthUiState.NeedsNickname -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(color = RockAccentGold.copy(alpha = 0.22f), shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = "CRIAR PERFIL NA NUVEM",
                                        color = RockAccentGold,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "ESCOLHA SEU NICKNAME EXCLUSIVO",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Conta conectada: ${state.email}\nSeu progresso local atual será salvo na nuvem ao confirmar (3 a 24 caracteres: letras, números, _ ou -).",
                                    color = Color.White.copy(alpha = 0.78f),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )

                                OutlinedTextField(
                                    value = cloudNicknameInput,
                                    onValueChange = { cloudNicknameInput = it.take(24) },
                                    label = { Text("Nickname Exclusivo", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("nickname_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = RockAccentCyan,
                                        unfocusedBorderColor = RockCardBorder,
                                        focusedLabelColor = RockAccentCyan,
                                        unfocusedLabelColor = Color.LightGray
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.checkNicknameAvailability(cloudNicknameInput) },
                                        enabled = cloudNicknameInput.trim().length >= 3 && !isBusy,
                                        modifier = Modifier.testTag("check_nickname_button")
                                    ) {
                                        Text("VERIFICAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RockAccentCyan)
                                    }
                                    if (nickAvail == true) {
                                        Text("✔ DISPONÍVEL!", color = RockAccentGreen, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                    } else if (nickAvail == false) {
                                        Text("✖ INDISPONÍVEL", color = RockAccentRed, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                    }
                                }

                                if (!authError.isNullOrBlank()) {
                                    Surface(color = RockAccentRed.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = authError.orEmpty(),
                                            color = RockAccentRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.signOut() },
                                    modifier = Modifier.weight(0.45f).testTag("auth_cancel_nickname_button")
                                ) {
                                    Text("VOLTAR", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                RockButton(
                                    text = if (isBusy) "SALVANDO..." else "CONFIRMAR PERFIL",
                                    onClick = { viewModel.submitNickname(cloudNicknameInput) },
                                    enabled = cloudNicknameInput.trim().length >= 3 && !isBusy,
                                    modifier = Modifier.weight(0.95f),
                                    testTag = "confirm_nickname_button",
                                    containerColor = RockAccentGreen,
                                    contentColor = Color.Black
                                )
                            }
                        }

                        is AuthUiState.Authenticated -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(color = RockAccentGreen, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = "CONTA CONECTADA NA NUVEM",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            editedNickname = stats.nickname
                                            showEditNicknameDialog = true
                                        },
                                        modifier = Modifier.testTag("edit_nickname_button")
                                    ) {
                                        Text("EDITAR NICK", color = RockAccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    text = stats.nickname.ifBlank { "Rocker" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = "E-mail: ${state.email.ifBlank { stats.email.ifBlank { "Conta Google" } }}",
                                    color = RockAccentCyan,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Nuvem: $cloudStatus • Revisão #${stats.syncRevision}",
                                    color = RockAccentGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Surface(color = Color(0x18FFFFFF), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("DADOS DA CONTA SINCRONIZADOS", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                        Text(
                                            text = "• Shows concluídos: ${stats.songsCompleted} | Vitórias Duelo: ${stats.competitiveWins}/${stats.competitiveMatches}\n• Itens desbloqueados: ${stats.unlockedCosmetics.size} | Sequência diária: ${stats.rewardStreak}d",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.syncCloudNow() },
                                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("profile_sync_button")
                                ) {
                                    Text("SINCRONIZAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                }
                                Button(
                                    onClick = { viewModel.signOut() },
                                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("profile_sign_out_button")
                                ) {
                                    Text("SAIR DA CONTA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                }
                            }
                        }

                        is AuthUiState.Loading -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(color = RockAccentCyan.copy(alpha = 0.22f), shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = "AUTENTICANDO...",
                                        color = RockAccentCyan,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = RockAccentCyan,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Text(
                                        text = "Verificando conta na nuvem. Você pode cancelar e voltar ao Modo Visitante a qualquer momento.",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.cancelPendingAuth() },
                                    modifier = Modifier.fillMaxWidth().testTag("auth_loading_cancel_button")
                                ) {
                                    Text("CANCELAR E VOLTAR AO MODO VISITANTE", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                }
                            }
                        }

                        else -> {
                            // Modo Visitante (SignedOut)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(color = RockAccentGold, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = "MODO VISITANTE ATIVO",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            editedNickname = stats.nickname
                                            showEditNicknameDialog = true
                                        },
                                        modifier = Modifier.testTag("edit_nickname_button")
                                    ) {
                                        Text("EDITAR APELIDO", color = RockAccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    text = stats.nickname.ifBlank { "Visitante Lendário" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 19.sp
                                )
                                Text(
                                    text = "Status: $cloudStatus",
                                    color = RockAccentGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Você já tem acesso livre ao jogo! Se quiser salvar na nuvem ou recuperar progresso em outro aparelho, entre ou vincule sua conta Google opcionalmente.",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )

                                Surface(color = Color(0x18FFFFFF), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("PROGRESSO LOCAL PRESERVADO", color = RockAccentCyan, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                        Text(
                                            text = "• Shows: ${stats.songsCompleted} | Estrelas: ${stats.unlockedStars} | Moedas: ${stats.coins} LC\n• Itens desbloqueados: ${stats.unlockedCosmetics.size} | Sequência diária: ${stats.rewardStreak}d",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Mesclar progresso local ao entrar/vincular",
                                                color = RockAccentGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Switch(
                                                checked = mergeGuestOnLink,
                                                onCheckedChange = { mergeGuestOnLink = it },
                                                modifier = Modifier.testTag("merge_guest_progress_switch")
                                            )
                                        }
                                    }
                                }

                                if (!authError.isNullOrBlank()) {
                                    Surface(color = RockAccentRed.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = authError.orEmpty(),
                                            color = RockAccentRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                RockButton(
                                    text = if (isBusy) "CONECTANDO..." else "ENTRAR / VINCULAR COM GOOGLE",
                                    onClick = {
                                        scope.launch {
                                            viewModel.setAuthError(null)
                                            when (val cred = GoogleSignInHelper.requestGoogleSignIn(context)) {
                                                is GoogleCredentialResult.Success -> {
                                                    viewModel.signInWithGoogleIdToken(
                                                        idToken = cred.idToken,
                                                        mergeGuestProgress = mergeGuestOnLink
                                                    )
                                                }
                                                is GoogleCredentialResult.Cancelled -> {
                                                    viewModel.setAuthError("Login cancelado. Você continua jogando normalmente como visitante.")
                                                }
                                                is GoogleCredentialResult.Error -> {
                                                    viewModel.setAuthError(cred.message)
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "google_sign_in_button",
                                    icon = Icons.Default.AccountCircle,
                                    containerColor = RockAccentCyan,
                                    contentColor = Color.Black
                                )
                                Text(
                                    text = "Usa sua conta Google (e-mail Google) para criar perfil ou vincular conta existente. Login separado por e-mail/senha requer ativação manual no Console Firebase.",
                                    color = Color.White.copy(alpha = 0.58f),
                                    fontSize = 8.sp,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Painel Direito: Conquistas, Histórico de Progressão e Recompensas (Visitante ou Nuvem)
            Card(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(listOf(RockAccentCyan, RockCardBorder))
                )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "CONQUISTAS DESBLOQUEADAS (${stats.achievementsUnlocked.size}/${AchievementCatalog.all.size})",
                            color = RockAccentGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    items(AchievementCatalog.all) { ach ->
                        val unlocked = stats.achievementsUnlocked.contains(ach.id)
                        Surface(
                            color = if (unlocked) Color(0x2200E676) else Color(0x14FFFFFF),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ach.title, color = if (unlocked) RockAccentGreen else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(ach.description, color = Color.White.copy(alpha = 0.65f), fontSize = 9.sp)
                                }
                                Text(if (unlocked) "✔" else "🔒", fontSize = 12.sp)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "HISTÓRICO DE PROGRESSÃO (${progressionHistory.size})",
                            color = RockAccentCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    if (progressionHistory.isEmpty()) {
                        item {
                            Text("Nenhuma apresentação registrada ainda.", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    } else {
                        items(progressionHistory.take(6)) { entry ->
                            Surface(color = Color(0x16FFFFFF), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${entry.trackTitle} (${entry.instrument})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                        Text("${entry.difficulty} • ${String.format("%.1f%%", entry.accuracyPercent)} • +${entry.coinsEarned} LC", color = Color.White.copy(alpha = 0.65f), fontSize = 9.sp)
                                    }
                                    Text("⭐ ${entry.stars} • ${entry.score}", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "HISTÓRICO DE RECOMPENSAS DIÁRIAS (${rewardHistory.size})",
                            color = RockAccentGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    if (rewardHistory.isEmpty()) {
                        item {
                            Text("Resgate sua primeira recompensa no Calendário Diário!", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    } else {
                        items(rewardHistory.take(6)) { reward ->
                            Surface(color = Color(0x16FFFFFF), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Dia ${reward.dayNumber} (${reward.dayKey}): ${reward.rewardTitle}", color = Color.White, fontSize = 10.sp)
                                    Text("+${reward.coinsGranted} LC", color = RockAccentCyan, fontWeight = FontWeight.Black, fontSize = 10.sp)
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
fun UpdateAvailableDialog(viewModel: GameViewModel) {
    val showDialog by viewModel.showUpdateDialog.collectAsState()
    val status by viewModel.updateStatus.collectAsState()

    if (!showDialog) return
    val available = status as? UpdateCheckStatus.UpdateAvailable ?: return

    AlertDialog(
        onDismissRequest = { viewModel.dismissUpdateDialog() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = RockAccentCyan, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (available.isMandatory) "Atualização Obrigatória Disponível" else "Nova Atualização Disponível",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Versão instalada: ${available.installedVersionName} (Build ${available.installedVersionCode})\nNova versão: ${available.latestRelease.versionName} (Build ${available.latestRelease.versionCode})",
                    color = RockAccentGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = available.latestRelease.releaseTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
                Text(
                    text = "Novidades:\n${available.latestRelease.changelog}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
                if (available.latestRelease.bugFixes.isNotBlank()) {
                    Text(
                        text = "Correções:\n${available.latestRelease.bugFixes}",
                        color = RockAccentCyan,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "Seus dados locais, músicas importadas e progresso na nuvem serão preservados e migrados automaticamente.",
                    color = RockAccentGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.launchOfficialUpdate() },
                colors = ButtonDefaults.buttonColors(containerColor = RockAccentGreen),
                modifier = Modifier.testTag("update_now_button")
            ) {
                Text("ATUALIZAR AGORA", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        },
        dismissButton = if (!available.isMandatory) {
            {
                TextButton(
                    onClick = { viewModel.dismissUpdateDialog() },
                    modifier = Modifier.testTag("update_later_button")
                ) {
                    Text("ATUALIZAR DEPOIS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        } else null,
        containerColor = RockDarkCard,
        shape = RoundedCornerShape(16.dp)
    )
}
