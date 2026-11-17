package com.example.guitarlegends.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
        viewModel.stopGamepadKeyCapture()
        viewModel.navigateBack()
    }
    val stats by viewModel.playerStats.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val releases by viewModel.releasesCatalog.collectAsState()
    val controllerInfo by viewModel.controllerInfo.collectAsState()
    val capturedKeyCode by viewModel.lastCapturedKeyCode.collectAsState()

    val audio = stats.studioProfile.audioSettings
    val ctrl = stats.studioProfile.controllerSettings

    var latency by remember(stats.audioLatencyOffsetMs) { mutableStateOf(stats.audioLatencyOffsetMs.toFloat()) }
    var showChangelogModal by remember { mutableStateOf(false) }
    var captureTarget by remember { mutableStateOf<String?>(null) } // "lane_0".."lane_4", "pause", "star_power", "strum"

    LaunchedEffect(capturedKeyCode, captureTarget) {
        val code = capturedKeyCode
        val target = captureTarget
        if (code != null && target != null) {
            when {
                target.startsWith("lane_") -> {
                    val idx = target.removePrefix("lane_").toIntOrNull()
                    if (idx != null) viewModel.assignControllerLaneKey(idx, code)
                }
                else -> viewModel.assignControllerActionKey(target, code)
            }
            captureTarget = null
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
            onBackClick = {
                viewModel.stopGamepadKeyCapture()
                viewModel.navigateBack()
            },
            title = "Configurações: Áudio, Controle PS4, Calibração & Versão"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Painel Esquerdo: Mixer Completo de Áudio + Latência + Modo de Controle + Conta
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.verticalGradient(listOf(RockAccentCyan, RockCardBorder)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "MIXER DE ÁUDIO E VOLUME", color = RockAccentCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { viewModel.toggleMuteAudio() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (audio.isMuted) RockAccentRed else Color(0x33FFFFFF)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp).testTag("audio_mute_button")
                            ) {
                                Text(
                                    text = if (audio.isMuted) "MUDO ON" else "SILENCIAR",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp
                                )
                            }
                            Button(
                                onClick = { viewModel.previewAudioMixer() },
                                colors = ButtonDefaults.buttonColors(containerColor = RockAccentGold),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp).testTag("audio_preview_button")
                            ) {
                                Text("TESTAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 8.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.resetAudioSettings() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp).testTag("audio_reset_button")
                            ) {
                                Text("PADRÃO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                            }
                        }
                    }

                    VolumeMixerSlider("Volume Geral (Master)", audio.masterVolume, "audio_slider_master") {
                        viewModel.updateAudioSettings(audio.copy(masterVolume = it))
                    }
                    VolumeMixerSlider("Volume das Músicas", audio.musicVolume, "audio_slider_music") {
                        viewModel.updateAudioSettings(audio.copy(musicVolume = it))
                    }
                    VolumeMixerSlider("Efeitos de Gameplay (Notas/Baqueta)", audio.gameplaySfxVolume, "audio_slider_gameplay_sfx") {
                        viewModel.updateAudioSettings(audio.copy(gameplaySfxVolume = it))
                    }
                    VolumeMixerSlider("Efeitos de Interface & Menus", audio.uiSfxVolume, "audio_slider_ui_sfx") {
                        viewModel.updateAudioSettings(audio.copy(uiSfxVolume = it))
                    }
                    VolumeMixerSlider("Ambiente, Público & Palco", audio.stageFxVolume, "audio_slider_stage_fx") {
                        viewModel.updateAudioSettings(audio.copy(stageFxVolume = it))
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    Text(text = "CALIBRAÇÃO DE LATÊNCIA: ${latency.toInt()} ms", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
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

                    Text(text = "MODO DE CONTROLE NA TELA", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.setControlMode(ControlMode.DIRECT_TOUCH) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (stats.controlMode == ControlMode.DIRECT_TOUCH) RockAccentGreen else Color(0x33FFFFFF)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("ctrl_direct_touch")
                        ) {
                            Text("TOQUE DIRETO", color = if (stats.controlMode == ControlMode.DIRECT_TOUCH) Color.Black else Color.White, fontWeight = FontWeight.Black, fontSize = 9.sp)
                        }
                        Button(
                            onClick = { viewModel.setControlMode(ControlMode.FRET_AND_STRUM) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (stats.controlMode == ControlMode.FRET_AND_STRUM) RockAccentGold else Color(0x33FFFFFF)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("ctrl_fret_strum")
                        ) {
                            Text("TRASTE + PALHETA", color = if (stats.controlMode == ControlMode.FRET_AND_STRUM) Color.Black else Color.White, fontWeight = FontWeight.Black, fontSize = 9.sp)
                        }
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(GameScreen.Profile) },
                        shape = RoundedCornerShape(10.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(RockAccentCyan, RockAccentGold))
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("settings_my_account_button")
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = RockAccentCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("MINHA CONTA (VISITANTE / GOOGLE)", color = RockAccentCyan, fontWeight = FontWeight.Black, fontSize = 9.sp)
                    }
                }
            }

            // Painel Direito: Controle Bluetooth / DualShock 4 + Gráficos + Atualizações
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.verticalGradient(listOf(RockAccentGold, RockCardBorder)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                    // Seção Controle Bluetooth / PS4
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "CONTROLE BLUETOOTH / DUALSHOCK 4", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Switch(
                            checked = ctrl.enabled,
                            onCheckedChange = { viewModel.updateControllerSettings(ctrl.copy(enabled = it)) },
                            modifier = Modifier.testTag("controller_enable_switch")
                        )
                    }

                    Surface(
                        color = if (controllerInfo.isConnected) RockAccentGreen.copy(alpha = 0.2f) else Color(0x18FFFFFF),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("controller_status_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = controllerInfo.statusMessage,
                                color = if (controllerInfo.isConnected) RockAccentGreen else Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { viewModel.refreshGamepadConnection() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("ATUALIZAR", color = RockAccentCyan, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.applyDualShock4DefaultPreset() },
                            colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(30.dp).testTag("preset_ps4_button")
                        ) {
                            Text("PADRÃO PS4 (L2/L1/R1/R2/✕)", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 8.sp, maxLines = 1)
                        }
                        Button(
                            onClick = { viewModel.applyFaceButtonsControllerPreset() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(30.dp).testTag("preset_face_button")
                        ) {
                            Text("BOTÕES (□/△/○/✕/R1)", color = Color.White, fontWeight = FontWeight.Black, fontSize = 8.sp, maxLines = 1)
                        }
                    }

                    if (captureTarget != null) {
                        Surface(
                            color = RockAccentGold.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Pressione o botão desejado no seu controle agora...",
                                    color = RockAccentGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = {
                                    captureTarget = null
                                    viewModel.stopGamepadKeyCapture()
                                }) {
                                    Text("CANCELAR", color = Color.White, fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    // Mapeamento das 5 Pistas (tocam notas e acordes simultâneos)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val laneColors = listOf(Color(0xFF00E676), Color(0xFFFF1744), Color(0xFFFFEA00), Color(0xFF2979FF), Color(0xFFFF9100))
                        for (lane in 0..4) {
                            val code = ctrl.laneKeyCodes.getOrElse(lane) { ControllerSettings.DEFAULT_PS4_LANE_KEYCODES[lane] }
                            val capturing = captureTarget == "lane_$lane"
                            Surface(
                                color = if (capturing) RockAccentGold else Color(0x22FFFFFF),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        captureTarget = "lane_$lane"
                                        viewModel.startGamepadKeyCapture()
                                    }
                                    .testTag("controller_remap_lane_$lane")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(modifier = Modifier.size(8.dp).background(laneColors[lane], CircleShape))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (capturing) "..." else ControllerSettings.keyCodeDisplayName(code),
                                        color = if (capturing) Color.Black else Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 8.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Botões Auxiliares (Pausa, Star Power, Palheta)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            Triple("pause", "PAUSE", ctrl.pauseKeyCode),
                            Triple("star_power", "STAR POWER", ctrl.starPowerKeyCode),
                            Triple("strum", "PALHETA", ctrl.strumKeyCode)
                        ).forEach { (actionId, title, code) ->
                            val capturing = captureTarget == actionId
                            OutlinedButton(
                                onClick = {
                                    captureTarget = actionId
                                    viewModel.startGamepadKeyCapture()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(34.dp).testTag("controller_remap_$actionId")
                            ) {
                                Text(
                                    text = if (capturing) "$title: ..." else "$title: ${ControllerSettings.keyCodeDisplayName(code)}",
                                    color = if (capturing) RockAccentGold else Color.White,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    Text(text = "GRÁFICOS & ATUALIZAÇÕES", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    GraphicsQuality.values().forEach { gq ->
                        val isSel = stats.graphicsQuality == gq
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.setGraphicsQuality(gq) },
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

                    // Painel de Versão & Atualizações
                    Surface(color = Color(0x18FFFFFF), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
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
                        onClick = {
                            viewModel.stopGamepadKeyCapture()
                            viewModel.navigateBack()
                        },
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
    val authState by viewModel.authUiState.collectAsState()
    val stats by viewModel.playerStats.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RockPurpleBg),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.stage_background_1794455515273),
            contentDescription = "Palco Principal do Guitar Legends",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0x990F081D), Color(0xEE0F081D))))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .widthIn(max = 520.dp)
                .padding(horizontal = 24.dp)
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
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "GUITAR LEGENDS",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 40.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = when (authState) {
                    is com.example.guitarlegends.viewmodel.AuthUiState.Authenticated ->
                        "Bem-vindo de volta, ${stats.nickname.ifBlank { "Lenda" }}!"
                    else ->
                        "Acesso imediato no Modo Visitante • Conta na nuvem opcional"
                },
                color = RockAccentGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))
            RockButton(
                text = "ENTRAR NO JOGO",
                onClick = { viewModel.enterGame() },
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(54.dp),
                testTag = "enter_game_button",
                icon = Icons.Default.PlayArrow,
                containerColor = RockAccentGold,
                contentColor = Color.Black
            )
        }
    }
}

@Composable
private fun VolumeMixerSlider(
    label: String,
    value: Float,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = "${(value * 100).toInt()}%", color = RockAccentGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = RockAccentCyan, activeTrackColor = RockAccentCyan),
            modifier = Modifier.height(26.dp).testTag(testTag)
        )
    }
}
