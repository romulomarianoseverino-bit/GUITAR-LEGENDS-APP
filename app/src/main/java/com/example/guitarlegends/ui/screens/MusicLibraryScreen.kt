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
import com.example.guitarlegends.model.Playlist
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
    val playlists by viewModel.playlists.collectAsState()
    val selected by viewModel.selectedTrackFlow.collectAsState()
    val isImporting by viewModel.isImportingAudio.collectAsState()
    val libraryError by viewModel.libraryErrorMessage.collectAsState()
    val playbackError by viewModel.playbackErrorMessage.collectAsState()
    val isPreviewPlaying by viewModel.isPreviewPlaying.collectAsState()
    val isPreviewPaused by viewModel.isPreviewPaused.collectAsState()
    val previewTrackId by viewModel.previewTrackId.collectAsState()
    val previewPosMs by viewModel.previewPositionMs.collectAsState()
    val previewDurMs by viewModel.previewDurationMs.collectAsState()
    val playlistStatusMessage by viewModel.playlistStatusMessage.collectAsState()
    val playlistStatusIsError by viewModel.playlistStatusIsError.collectAsState()

    var showPlaylistsTab by remember { mutableStateOf(false) }
    var openedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showFolderAddTrackDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var renameBuffer by remember { mutableStateOf("") }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    var trackToDelete by remember { mutableStateOf<Track?>(null) }
    var trackToCalibrate by remember { mutableStateOf<Track?>(null) }
    var bpmSlider by remember { mutableStateOf(120f) }
    var syncSlider by remember { mutableStateOf(0f) }

    LaunchedEffect(playlists) {
        if (openedPlaylistId != null && playlists.none { it.id == openedPlaylistId }) {
            openedPlaylistId = null
            showFolderAddTrackDialog = false
        }
    }

    if (showPlaylistsTab && openedPlaylistId != null) {
        BackHandler {
            showFolderAddTrackDialog = false
            openedPlaylistId = null
            viewModel.clearPlaylistStatusMessage()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importAudioFile(uri)
        }
    }

    // Modal de Renomear Playlist
    if (playlistToRename != null) {
        val targetPl = playlistToRename!!
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            title = { Text("Renomear Playlist", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp) },
            text = {
                OutlinedTextField(
                    value = renameBuffer,
                    onValueChange = { renameBuffer = it.take(40) },
                    singleLine = true,
                    label = { Text("Novo nome da playlist", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RockAccentGold,
                        unfocusedBorderColor = RockCardBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renamePlaylist(targetPl.id, renameBuffer)
                        playlistToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentGold)
                ) {
                    Text("SALVAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text("CANCELAR", color = Color.White, fontSize = 11.sp)
                }
            },
            containerColor = RockDarkCard
        )
    }

    // Modal de Adicionar Faixas dentro de uma Pasta de Playlist
    val openedPlaylistForModal = playlists.find { it.id == openedPlaylistId }
    if (showFolderAddTrackDialog && openedPlaylistForModal != null) {
        AlertDialog(
            onDismissRequest = { showFolderAddTrackDialog = false },
            title = {
                Text(
                    text = "Adicionar músicas em '${openedPlaylistForModal.name}'",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!playlistStatusMessage.isNullOrBlank()) {
                        Surface(
                            color = if (playlistStatusIsError) RockAccentRed.copy(alpha = 0.25f) else RockAccentGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("folder_add_dialog_banner")
                        ) {
                            Text(
                                text = playlistStatusMessage.orEmpty(),
                                color = if (playlistStatusIsError) RockAccentRed else RockAccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .testTag("folder_add_track_dialog_list"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(tracks, key = { it.id }) { catalogTrack ->
                            val alreadyInFolder = openedPlaylistForModal.trackIds.contains(catalogTrack.id)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                    Text(
                                        text = catalogTrack.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${catalogTrack.artist} • ${formatTime(catalogTrack.durationMs)}",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.addTrackToPlaylist(openedPlaylistForModal.id, catalogTrack.id)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (alreadyInFolder) Color(0xFF474056) else RockAccentGold
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("folder_add_track_${catalogTrack.id}")
                                ) {
                                    Text(
                                        text = if (alreadyInFolder) "JÁ NA PASTA" else "ADICIONAR",
                                        color = if (alreadyInFolder) Color.White else Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderAddTrackDialog = false }) {
                    Text("CONCLUIR", color = RockAccentCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            containerColor = RockDarkCard
        )
    }

    // Modal de Adicionar Faixa Selecionada à Playlist
    if (showAddToPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            title = {
                Text("Adicionar '${selected.title}' à Playlist", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!playlistStatusMessage.isNullOrBlank()) {
                        Surface(
                            color = if (playlistStatusIsError) RockAccentRed.copy(alpha = 0.25f) else RockAccentGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("add_to_playlist_dialog_banner")
                        ) {
                            Text(
                                text = playlistStatusMessage.orEmpty(),
                                color = if (playlistStatusIsError) RockAccentRed else RockAccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    if (playlists.isEmpty()) {
                        Text(
                            text = "Nenhuma playlist criada ainda. Digite um nome abaixo para criar e adicionar esta faixa.",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it.take(40) },
                            singleLine = true,
                            label = { Text("Nome da nova playlist", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = RockAccentCyan,
                                unfocusedBorderColor = RockCardBorder
                            )
                        )
                        Button(
                            onClick = {
                                val created = viewModel.createPlaylist(newPlaylistName)
                                viewModel.addTrackToPlaylist(created.id, selected.id)
                                newPlaylistName = ""
                                showAddToPlaylistDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CRIAR & ADICIONAR FAIXA", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    } else {
                        playlists.forEach { pl ->
                            val alreadyIn = pl.trackIds.contains(selected.id)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pl.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                    Text("${pl.trackIds.size} faixa(s)", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                }
                                Button(
                                    onClick = {
                                        val added = viewModel.addTrackToPlaylist(pl.id, selected.id)
                                        if (added) {
                                            showAddToPlaylistDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (alreadyIn) Color(0xFF474056) else RockAccentGold
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("dialog_add_to_playlist_${pl.id}")
                                ) {
                                    Text(
                                        text = if (alreadyIn) "NA PASTA" else "ADICIONAR",
                                        color = if (alreadyIn) Color.White else Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylistDialog = false }) {
                    Text("FECHAR", color = RockAccentCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            containerColor = RockDarkCard
        )
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
            // Lista de Músicas ou Playlists Personalizadas
            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showPlaylistsTab = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!showPlaylistsTab) RockAccentGold else Color(0x22FFFFFF),
                            contentColor = if (!showPlaylistsTab) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(32.dp).testTag("library_tab_tracks")
                    ) {
                        Text("FAIXAS (${tracks.size})", fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                    Button(
                        onClick = { showPlaylistsTab = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showPlaylistsTab) RockAccentGold else Color(0x22FFFFFF),
                            contentColor = if (showPlaylistsTab) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(32.dp).testTag("library_tab_playlists")
                    ) {
                        Text("PLAYLISTS (${playlists.size})", fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }

                if (!showPlaylistsTab) {
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
                } else {
                    if (!playlistStatusMessage.isNullOrBlank()) {
                        Surface(
                            color = if (playlistStatusIsError) RockAccentRed.copy(alpha = 0.25f) else RockAccentGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(
                                    listOf(if (playlistStatusIsError) RockAccentRed else RockAccentGreen, RockCardBorder)
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .testTag("playlist_status_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = playlistStatusMessage.orEmpty(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearPlaylistStatusMessage() },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Fechar aviso", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    val openedPlaylist = playlists.find { it.id == openedPlaylistId }
                    if (openedPlaylist != null) {
                        val resolvedTracks = openedPlaylist.trackIds.mapNotNull { tid -> tracks.find { it.id == tid } }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("playlist_folder_detail_${openedPlaylist.id}"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        openedPlaylistId = null
                                        showFolderAddTrackDialog = false
                                        viewModel.clearPlaylistStatusMessage()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp).testTag("playlist_back_to_folders_button")
                                ) {
                                    Text("← VOLTAR ÀS PASTAS", color = RockAccentCyan, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            renameBuffer = openedPlaylist.name
                                            playlistToRename = openedPlaylist
                                        },
                                        modifier = Modifier.size(32.dp).testTag("rename_playlist_${openedPlaylist.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Renomear Pasta", tint = RockAccentGold, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deletePlaylist(openedPlaylist.id)
                                            openedPlaylistId = null
                                        },
                                        modifier = Modifier.size(32.dp).testTag("delete_playlist_${openedPlaylist.id}")
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir Pasta", tint = RockAccentRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Surface(
                                color = Color(0x28FFD54F),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = RockAccentGold, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(openedPlaylist.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
                                            Text(
                                                text = "${resolvedTracks.size} música(s) nesta pasta",
                                                color = RockAccentCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { showFolderAddTrackDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp).testTag("folder_open_add_tracks_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("ADICIONAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.addTrackToPlaylist(openedPlaylist.id, selected.id) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.fillMaxWidth().height(32.dp).testTag("add_selected_track_to_playlist_${openedPlaylist.id}")
                            ) {
                                Text(
                                    text = "+ ADICIONAR '${selected.title.uppercase()}' A ESTA PASTA",
                                    color = RockAccentGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            if (resolvedTracks.isEmpty()) {
                                Surface(
                                    color = RockDarkCard,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Pasta vazia. Toque em ADICIONAR acima para incluir músicas sem misturar com o catálogo geral.",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("folder_tracks_list"),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(resolvedTracks.size, key = { idx -> resolvedTracks[idx].id }) { idx ->
                                        val t = resolvedTracks[idx]
                                        val isTrackSelected = t.id == selected.id
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.selectTrack(t) }
                                                .testTag("folder_track_${t.id}"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isTrackSelected) Color(0xFF2C194D) else RockDarkCard
                                            ),
                                            border = if (isTrackSelected) ButtonDefaults.outlinedButtonBorder.copy(
                                                brush = Brush.horizontalGradient(listOf(RockAccentGold, RockAccentCyan))
                                            ) else null
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = "#${idx + 1}",
                                                        color = RockAccentGold,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(t.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                                        Text(
                                                            text = "${t.artist} • ${formatTime(t.durationMs)} • ${t.effectiveBpm.toInt()} BPM",
                                                            color = Color.White.copy(alpha = 0.65f),
                                                            fontSize = 9.sp,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { viewModel.removeTrackFromPlaylist(openedPlaylist.id, t.id) },
                                                    modifier = Modifier.size(30.dp).testTag("remove_track_${openedPlaylist.id}_${t.id}")
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remover da pasta", tint = RockAccentRed, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.startPlaylistPlayback(openedPlaylist.id, shuffle = false) },
                                    enabled = resolvedTracks.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(34.dp).testTag("play_playlist_seq_${openedPlaylist.id}")
                                ) {
                                    Text("SEQUÊNCIA ▶", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                }
                                Button(
                                    onClick = { viewModel.startPlaylistPlayback(openedPlaylist.id, shuffle = true) },
                                    enabled = resolvedTracks.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(34.dp).testTag("play_playlist_shuffle_${openedPlaylist.id}")
                                ) {
                                    Text("ALEATÓRIO 🔀", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it.take(40) },
                                placeholder = { Text("Nome da nova pasta / playlist...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("playlist_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = RockAccentGold,
                                    unfocusedBorderColor = RockCardBorder
                                )
                            )
                            Button(
                                onClick = {
                                    viewModel.createPlaylist(newPlaylistName)
                                    newPlaylistName = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp).testTag("create_playlist_button")
                            ) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CRIAR PASTA", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
                        }

                        if (playlists.isEmpty()) {
                            Surface(
                                color = RockDarkCard,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Crie pastas de playlists personalizadas para organizar seus shows e tocar em sequência ou aleatório.",
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().testTag("playlist_folders_list"),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(playlists, key = { it.id }) { pl ->
                                    val resolvedTracks = pl.trackIds.mapNotNull { tid -> tracks.find { it.id == tid } }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.clearPlaylistStatusMessage()
                                                openedPlaylistId = pl.id
                                            }
                                            .testTag("playlist_folder_${pl.id}"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                                        border = ButtonDefaults.outlinedButtonBorder.copy(
                                            brush = Brush.horizontalGradient(listOf(RockAccentGold.copy(alpha = 0.55f), RockCardBorder))
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        color = Color(0x33FFD54F),
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier.size(38.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.Folder,
                                                                contentDescription = "Pasta ${pl.name}",
                                                                tint = RockAccentGold,
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(pl.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
                                                        Text(
                                                            text = "${resolvedTracks.size} faixa(s) • Toque para abrir a pasta",
                                                            color = RockAccentCyan,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = {
                                                            renameBuffer = pl.name
                                                            playlistToRename = pl
                                                        },
                                                        modifier = Modifier.size(30.dp).testTag("rename_playlist_${pl.id}")
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Renomear", tint = RockAccentGold, modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.deletePlaylist(pl.id) },
                                                        modifier = Modifier.size(30.dp).testTag("delete_playlist_${pl.id}")
                                                    ) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir Playlist", tint = RockAccentRed, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Button(
                                                    onClick = { viewModel.startPlaylistPlayback(pl.id, shuffle = false) },
                                                    enabled = resolvedTracks.isNotEmpty(),
                                                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentGreen),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f).height(32.dp).testTag("play_playlist_seq_${pl.id}")
                                                ) {
                                                    Text("SEQUÊNCIA ▶", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                                }
                                                Button(
                                                    onClick = { viewModel.startPlaylistPlayback(pl.id, shuffle = true) },
                                                    enabled = resolvedTracks.isNotEmpty(),
                                                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f).height(32.dp).testTag("play_playlist_shuffle_${pl.id}")
                                                ) {
                                                    Text("ALEATÓRIO 🔀", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp)
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

                            Button(
                                onClick = { showAddToPlaylistDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFD54F)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_add_to_playlist_dialog")
                            ) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = "Adicionar à Playlist", tint = RockAccentGold, modifier = Modifier.size(16.dp))
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
