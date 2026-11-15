package com.example.guitarlegends.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
    val authState by viewModel.authUiState.collectAsState()
    val authError by viewModel.authErrorMessage.collectAsState()
    val isBusy by viewModel.isAuthBusy.collectAsState()
    val nickAvail by viewModel.nicknameAvailability.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nicknameInput by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Painel Esquerdo: Identidade do Palco
        Card(
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = RockDarkCard),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.verticalGradient(listOf(RockAccentGold, RockAccentRed))
            )
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
                                listOf(Color(0x550F081D), Color(0xCC0F081D), Color(0xFA0F081D))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Surface(color = RockAccentRed, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "EDIÇÃO LENDÁRIA • CONTA & NUVEM",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "GUITAR LEGENDS",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Nickname exclusivo reservado para sua lenda\n• Personagens, skins, moedas e carreira salvos na nuvem\n• Calendário de recompensas diárias sincronizado",
                        color = Color(0xFFD9CCF2),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Painel Direito: Login Google / Cadastro de Nickname
        Card(
            modifier = Modifier
                .weight(1.1f)
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                when (val state = authState) {
                    is AuthUiState.NeedsNickname -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "ESCOLHA SEU NICKNAME EXCLUSIVO",
                                color = RockAccentGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Conta conectada: ${state.email}\nDefina seu nome público único de palco (3 a 24 caracteres: letras, números, _ ou -).",
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            OutlinedTextField(
                                value = nicknameInput,
                                onValueChange = { nicknameInput = it.take(24) },
                                label = { Text("Nickname Exclusivo", fontSize = 12.sp) },
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
                                    onClick = { viewModel.checkNicknameAvailability(nicknameInput) },
                                    enabled = nicknameInput.trim().length >= 3 && !isBusy,
                                    modifier = Modifier.testTag("check_nickname_button")
                                ) {
                                    Text("VERIFICAR DISPONIBILIDADE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RockAccentCyan)
                                }
                                if (nickAvail == true) {
                                    Text("✔ DISPONÍVEL!", color = RockAccentGreen, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                } else if (nickAvail == false) {
                                    Text("✖ INDISPONÍVEL", color = RockAccentRed, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }

                            if (!authError.isNullOrBlank()) {
                                Surface(color = RockAccentRed.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = authError.orEmpty(),
                                        color = RockAccentRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.signOut() },
                                enabled = !isBusy,
                                modifier = Modifier.weight(0.45f)
                            ) {
                                Text("SAIR DA CONTA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            RockButton(
                                text = if (isBusy) "RESERVANDO..." else "CONFIRMAR E ENTRAR",
                                onClick = { viewModel.submitNickname(nicknameInput) },
                                enabled = nicknameInput.trim().length >= 3 && !isBusy,
                                modifier = Modifier.weight(0.95f),
                                testTag = "confirm_nickname_button",
                                containerColor = RockAccentGreen,
                                contentColor = Color.Black
                            )
                        }
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "ACESSO À CONTA DE JOGADOR",
                                color = RockAccentCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Entre com sua conta Google verificada para criar seu perfil exclusivo, salvar sua progressão na nuvem e sincronizar recompensas diárias entre dispositivos.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            Surface(
                                color = Color(0x22FFFFFF),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("SEGURANÇA & RECUPERAÇÃO", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    Text(
                                        text = "• Autenticação segura via Credential Manager do Android\n• Recuperação automática de conta e progresso pelo seu e-mail\n• Proteção contra acessos não autorizados e perda de dados",
                                        color = Color.White.copy(alpha = 0.78f),
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            if (!authError.isNullOrBlank()) {
                                Surface(color = RockAccentRed.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = authError.orEmpty(),
                                        color = RockAccentRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RockButton(
                                text = if (isBusy) "CONECTANDO..." else "ENTRAR COM CONTA GOOGLE",
                                onClick = {
                                    scope.launch {
                                        viewModel.setAuthError(null)
                                        when (val cred = GoogleSignInHelper.requestGoogleSignIn(context)) {
                                            is GoogleCredentialResult.Success -> {
                                                viewModel.signInWithGoogleIdToken(cred.idToken)
                                            }
                                            is GoogleCredentialResult.Cancelled -> {
                                                viewModel.setAuthError("Login cancelado. Toque novamente para entrar.")
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: GameViewModel) {
    BackHandler { viewModel.navigateBack() }

    val stats by viewModel.playerStats.collectAsState()
    val authState by viewModel.authUiState.collectAsState()
    val cloudStatus by viewModel.cloudSyncStatus.collectAsState()
    val rewardHistory by viewModel.rewardHistory.collectAsState()
    val progressionHistory by viewModel.progressionHistory.collectAsState()
    val authError by viewModel.authErrorMessage.collectAsState()
    val isBusy by viewModel.isAuthBusy.collectAsState()

    var showEditNicknameDialog by remember { mutableStateOf(false) }
    var editedNickname by remember { mutableStateOf(stats.nickname) }

    if (showEditNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNicknameDialog = false },
            title = { Text("Alterar Nickname Exclusivo", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Seu nickname anterior será liberado somente se o novo estiver disponível.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = editedNickname,
                        onValueChange = { editedNickname = it.take(24) },
                        label = { Text("Novo Nickname") },
                        singleLine = true,
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
                        viewModel.submitNickname(editedNickname)
                        showEditNicknameDialog = false
                    },
                    enabled = editedNickname.trim().length >= 3 && !isBusy,
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
            title = "Perfil da Lenda & Nuvem"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cartão Esquerdo: Identidade, E-mail, Sincronização e Sair
            Card(
                modifier = Modifier
                    .weight(0.95f)
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
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = RockAccentGold, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "IDENTIDADE VERIFICADA",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            TextButton(onClick = {
                                editedNickname = stats.nickname
                                showEditNicknameDialog = true
                            }) {
                                Text("EDITAR NICK", color = RockAccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = stats.nickname.ifBlank { "Rocker sem Nickname" },
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        val emailLabel = (authState as? AuthUiState.Authenticated)?.email ?: stats.email
                        Text(
                            text = "E-mail: ${emailLabel.ifBlank { "Não conectado" }}",
                            color = RockAccentCyan,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Nuvem: $cloudStatus • Revisão #${stats.syncRevision}",
                            color = RockAccentGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(color = Color(0x18FFFFFF), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("RESUMO SINCRONIZADO", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                Text(
                                    text = "• Shows concluídos: ${stats.songsCompleted} | Vitórias Duelo: ${stats.competitiveWins}/${stats.competitiveMatches}\n• Itens cosméticos desbloqueados: ${stats.unlockedCosmetics.size}\n• Sequência diária atual: ${stats.rewardStreak} dias",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

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
                            Text("ENCERRAR SESSÃO", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Painel Direito: Conquistas, Histórico de Progressão e Recompensas
            Card(
                modifier = Modifier
                    .weight(1.25f)
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
                        Text("CONQUISTAS DESBLOQUEADAS (${stats.achievementsUnlocked.size}/${AchievementCatalog.all.size})", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
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
                        Text("HISTÓRICO DE PROGRESSÃO (${progressionHistory.size})", color = RockAccentCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    if (progressionHistory.isEmpty()) {
                        item {
                            Text("Nenhuma apresentação registrada na nuvem ainda.", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
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
                        Text("HISTÓRICO DE RECOMPENSAS DIÁRIAS (${rewardHistory.size})", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
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
