package com.example.guitarlegends.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guitarlegends.model.AudioClassification
import com.example.guitarlegends.model.BpmConfidence
import com.example.guitarlegends.model.Track
import com.example.guitarlegends.ui.components.*
import com.example.guitarlegends.viewmodel.GameScreen
import com.example.guitarlegends.viewmodel.GameViewModel

@Composable
fun MusicLibraryScreen(viewModel: GameViewModel) {
    BackHandler {
        viewModel.stopPreview()
        viewModel.navigateBack()
    }

    val stats by viewModel.playerStats.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    val selected by viewModel.selectedTrackFlow.collectAsState()
    val isImporting by viewModel.isImportingAudio.collectAsState()
    val libraryError by viewModel.libraryErrorMessage.collectAsState()
    val playbackError by viewModel.playbackErrorMessage.collectAsState()
    val isPreviewPlaying by viewModel.isPreviewPlaying.collectAsState()
    val isPreviewPaused by viewModel.isPreviewPaused.collectAsState()
    val previewTrackId by viewModel.previewTrackId.collectAsState()
    val previewPosMs by viewModel.previewPositionMs.collectAsState()
    val previewDurMs by viewModel.previewDurationMs.collectAsState()

    var trackToDelete by remember { mutableStateOf<Track?>(null) }
    var trackToCalibrate by remember { mutableStateOf<Track?>(null) }
    var bpmSlider by remember { mutableStateOf(120f) }
    var syncSlider by remember { mutableStateOf(0f) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importAudioFile(uri)
        }
    }

    // Modal de Calibração Manual de BPM & Sincronia
    if (trackToCalibrate != null) {
        val target = trackToCalibrate!!
        AlertDialog(
            onDismissRequest = { trackToCalibrate = null },
            title = {
                Text("Calibrar BPM & Sincronia", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Faixa: ${target.title}\nConfiança detectada: ${target.bpmConfidence.label}",
                        color = RockAccentCyan,
                        fontSize = 11.sp
                    )
                    Text("BPM Efetivo: ${bpmSlider.toInt()} BPM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Slider(
                        value = bpmSlider,
                        onValueChange = { bpmSlider = it },
                        valueRange = 60f..220f,
                        colors = SliderDefaults.colors(thumbColor = RockAccentGold, activeTrackColor = RockAccentGold)
                    )
                    Text("Deslocamento de Sincronia: ${syncSlider.toInt()} ms", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Slider(
                        value = syncSlider,
                        onValueChange = { syncSlider = it },
                        valueRange = -400f..400f,
                        colors = SliderDefaults.colors(thumbColor = RockAccentCyan, activeTrackColor = RockAccentCyan)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.calibrateTrack(target.id, bpmSlider.toDouble(), syncSlider.toLong())
                        trackToCalibrate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentGreen)
                ) {
                    Text("APLICAR AJUSTE", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { trackToCalibrate = null }) {
                    Text("CANCELAR", color = Color.White, fontSize = 11.sp)
                }
            },
            containerColor = RockDarkCard
        )
    }

    // Modal de Confirmação para Remoção Segura
    if (trackToDelete != null) {
        val target = trackToDelete!!
        AlertDialog(
            onDismissRequest = { trackToDelete = null },
            title = {
                Text("Remover Música da Biblioteca?", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
            },
            text = {
                Text(
                    text = "A cópia privada de '${target.title}' armazenada pelo Guitar Legends será removida.\n\nO arquivo original no seu aparelho permanecerá intacto.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteImportedTrack(target)
                        trackToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("REMOVER DA BIBLIOTECA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { trackToDelete = null }) {
                    Text("CANCELAR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            containerColor = RockDarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal de Erro de Decodificação/Reprodução (sem fallback enganoso!)
    if (playbackError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPlaybackError() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RockAccentRed, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Falha na Reprodução do Áudio", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            },
            text = {
                Text(
                    text = playbackError.orEmpty(),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearPlaybackError()
                        viewModel.togglePreview(selected)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan)
                ) {
                    Text("TENTAR NOVAMENTE", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearPlaybackError() }) {
                    Text("ESCOLHER OUTRO ARQUIVO", color = RockAccentGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            containerColor = RockDarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal de Aviso de Importação
    if (libraryError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearLibraryError() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RockAccentRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aviso da Biblioteca", fontWeight = FontWeight.Black, color = Color.White, fontSize = 15.sp)
                }
            },
            text = {
                Text(
                    text = libraryError.orEmpty(),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearLibraryError() },
                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ENTENDI", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            },
            containerColor = RockDarkCard,
            shape = RoundedCornerShape(16.dp)
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
            onBackClick = {
                viewModel.stopPreview()
                viewModel.navigateBack()
            },
            title = "Biblioteca Musical — Faixas & Áudios"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Lista de Músicas
            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MÚSICAS DISPONÍVEIS (${tracks.size})",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { audioPickerLauncher.launch("audio/*") },
                        enabled = !isImporting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RockAccentCyan,
                            disabledContainerColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("import_audio_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isImporting) "ANALISANDO PCM..." else "IMPORTAR DO CELULAR",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }

                if (isImporting) {
                    Surface(
                        color = Color(0x3300E5FF),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RockAccentCyan, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Validando formato, decodificando PCM e detectando batidas...",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tracks, key = { it.id }) { track ->
                        val isSelected = track.id == selected.id
                        val isTrackPreviewing = (isPreviewPlaying || isPreviewPaused) && previewTrackId == track.id

                        TrackItemCard(
                            track = track,
                            isSelected = isSelected,
                            isPreviewPlaying = isTrackPreviewing,
                            onSelect = { viewModel.selectTrack(track) },
                            onDelete = if (!track.isBuiltIn) {
                                { trackToDelete = track }
                            } else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Painel Direito: Detalhes, Prévia Contínua & Calibração
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
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = when (selected.audioClassification) {
                                    AudioClassification.BUILT_IN_SYNTH -> RockAccentRed
                                    AudioClassification.SHORT_CLIP -> RockAccentGold
                                    AudioClassification.UNVERIFIED_STREAM -> RockAccentOrange
                                    AudioClassification.MUSIC_FULL -> RockAccentCyan
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = selected.audioClassification.badgeLabel,
                                    color = if (selected.isBuiltIn) Color.White else Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            if (!selected.isBuiltIn) {
                                Row {
                                    IconButton(
                                        onClick = {
                                            bpmSlider = selected.effectiveBpm.toFloat()
                                            syncSlider = selected.syncOffsetMs.toFloat()
                                            trackToCalibrate = selected
                                        },
                                        modifier = Modifier.size(28.dp).testTag("calibrate_track_button")
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = "Calibrar BPM", tint = RockAccentCyan, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { trackToDelete = selected },
                                        modifier = Modifier.size(28.dp).testTag("delete_selected_track_button")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remover", tint = RockAccentRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(text = selected.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1)
                        Text(text = selected.artist, color = RockAccentCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetaBadge(label = "BPM (${selected.bpmConfidence.label.take(6)})", value = "${selected.effectiveBpm.toInt()}")
                            MetaBadge(label = "DURAÇÃO", value = formatTime(selected.durationMs))
                            MetaBadge(label = "PICOS", value = if (selected.onsetTimesMs.isNotEmpty()) "${selected.onsetTimesMs.size}" else "Grade")
                        }

                        if (!selected.isBuiltIn && selected.bpmConfidence == BpmConfidence.LOW_UNRELIABLE) {
                            Text(
                                text = "⚠ Pulso rítmico incerto: toque no ícone 🎛️ acima para calibrar BPM manualmente.",
                                color = RockAccentGold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Controles Completos de Prévia (Tocar / Pausar / Retomar / Parar)
                        val isThisPlaying = isPreviewPlaying && previewTrackId == selected.id
                        val isThisPaused = isPreviewPaused && previewTrackId == selected.id

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { viewModel.togglePreview(selected) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isThisPlaying -> RockAccentGold
                                        isThisPaused -> RockAccentCyan
                                        else -> Color(0x33FFFFFF)
                                    }
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("toggle_preview_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isThisPlaying || isThisPaused) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        isThisPlaying -> "PAUSAR PRÉVIA"
                                        isThisPaused -> "RETOMAR PRÉVIA"
                                        else -> "OUVIR PRÉVIA"
                                    },
                                    color = if (isThisPlaying || isThisPaused) Color.Black else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }

                            if (isThisPlaying || isThisPaused) {
                                Button(
                                    onClick = { viewModel.stopPreview() },
                                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentRed),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("stop_preview_button")
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Parar Prévia", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        if (isThisPlaying || isThisPaused) {
                            val totalDur = previewDurMs.coerceAtLeast(selected.durationMs).coerceAtLeast(1L)
                            val frac = (previewPosMs.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { frac },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = RockAccentGold,
                                trackColor = Color(0x33FFFFFF)
                            )
                            Text(
                                text = "${formatTime(previewPosMs)} / ${formatTime(totalDur)} ${if (isThisPaused) "(Pausado)" else ""}",
                                color = RockAccentGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    RockButton(
                        text = "ESCOLHER INSTRUMENTO ▶",
                        onClick = {
                            viewModel.stopPreview()
                            viewModel.navigateTo(GameScreen.InstrumentSelect)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "proceed_to_instrument_button",
                        containerColor = RockAccentGreen,
                        contentColor = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun TrackItemCard(
    track: Track,
    isSelected: Boolean,
    isPreviewPlaying: Boolean = false,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("track_item_${track.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2C194D) else RockDarkCard
        ),
        border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(listOf(RockAccentGold, RockAccentRed))
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = if (isSelected) RockAccentGold else Color(0x33FFFFFF),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPreviewPlaying) Icons.Default.VolumeUp
                            else if (track.isBuiltIn) Icons.Default.MusicNote
                            else Icons.Default.SdStorage,
                            contentDescription = null,
                            tint = if (isSelected) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    val shortTag = if (track.isShortAudio) " • Clipe Curto" else ""
                    Text(
                        text = "${track.artist} • ${formatTime(track.durationMs)} • ${track.effectiveBpm.toInt()} BPM$shortTag",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    Text(
                        text = "ESCOLHIDA",
                        color = RockAccentGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_track_${track.id}")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Remover da Biblioteca", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MetaBadge(label: String, value: String) {
    Surface(color = Color(0x22FFFFFF), shape = RoundedCornerShape(8.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

fun formatTime(durationMs: Long): String {
    val totalSec = durationMs / 1000L
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return String.format("%d:%02d", min, sec)
}
