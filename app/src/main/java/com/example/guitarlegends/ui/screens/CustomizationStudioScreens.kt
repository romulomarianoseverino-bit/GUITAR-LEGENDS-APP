package com.example.guitarlegends.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.atan2
import kotlin.math.hypot
import com.example.guitarlegends.data.HighwayImageManager
import com.example.guitarlegends.engine.InteractiveCharacter3DViewer
import com.example.guitarlegends.engine.InteractiveInstrument3DViewer
import com.example.guitarlegends.engine.drawHighwayStudioPreview
import com.example.guitarlegends.model.*
import com.example.guitarlegends.ui.components.*
import com.example.guitarlegends.viewmodel.GameViewModel

@Composable
fun CustomizationScreen(viewModel: GameViewModel) {
    BackHandler {
        viewModel.clearStudioMessages()
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val stats by viewModel.playerStats.collectAsState()
    val studioActionMessage by viewModel.studioActionMessage.collectAsState()
    val highwayUploadError by viewModel.highwayUploadError.collectAsState()

    // 0 = Personagem, 1 = Instrumento, 2 = Esteira, 3 = Loja & Desafios, 4 = Presets
    var selectedCategory by remember { mutableIntStateOf(0) }
    // 0 = Boneco 3D, 1 = Instrumento 3D, 2 = Esteira
    var previewMode by remember { mutableIntStateOf(0) }
    var instrumentTab by remember { mutableStateOf(Instrument.GUITAR) }
    var previewingShopItem by remember { mutableStateOf<CosmeticShopItem?>(null) }
    var shopFilter by remember { mutableStateOf("Todos") }
    var presetNameInput by remember { mutableStateOf("") }

    // Sincroniza modo de prévia automaticamente quando o usuário muda para uma aba específica
    LaunchedEffect(selectedCategory) {
        when (selectedCategory) {
            0 -> previewMode = 0
            1 -> previewMode = 1
            2 -> previewMode = 2
        }
    }

    val effectiveStats = remember(stats, previewingShopItem) {
        val item = previewingShopItem
        if (item != null) StudioCatalog.applyCosmeticLook(stats, item.id) else stats
    }

    val activeHighwayCfg = effectiveStats.studioProfile.highwayConfig
    val activeHighwayTheme = viewModel.highwayThemes.find { it.id == effectiveStats.selectedHighwayThemeId }
        ?: HighwayTheme.PS2_WOOD_STEEL

    val cachedHighwayBitmap = remember(activeHighwayCfg.selectedStampId, activeHighwayCfg.customImagePath) {
        HighwayImageManager.loadHighwayOverlayBitmap(
            context = context,
            stampId = activeHighwayCfg.selectedStampId,
            customImagePath = activeHighwayCfg.customImagePath
        )
    }

    // Zero-permission native Android Photo Picker (Google Play compliant)
    val rockerPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateCustomRockerPhoto(uri.toString())
        }
    }

    val highwayPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importCustomHighwayImage(uri)
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
                viewModel.clearStudioMessages()
                viewModel.navigateBack()
            },
            title = "Estúdio 3D • Customização, Loja & Presets"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Painel Esquerdo: Pré-visualizador Interativo 360° (Personagem / Instrumento / Esteira)
            Card(
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RockDarkCard),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(listOf(RockAccentGold, RockCardBorder))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Alternador do Modo de Pré-visualização 3D
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val previewLabels = listOf("Boneco 360°", "Instrumento 360°", "Esteira")
                        previewLabels.forEachIndexed { idx, label ->
                            Button(
                                onClick = { previewMode = idx },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (previewMode == idx) RockAccentCyan else Color(0x22FFFFFF),
                                    contentColor = if (previewMode == idx) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .testTag("preview_mode_$idx")
                            ) {
                                Text(label, fontWeight = FontWeight.Black, fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    }

                    // Área 3D / Preview
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (previewMode) {
                            0 -> InteractiveCharacter3DViewer(
                                stats = effectiveStats,
                                instrument = instrumentTab,
                                modifier = Modifier.fillMaxSize()
                            )
                            1 -> InteractiveInstrument3DViewer(
                                instrument = instrumentTab,
                                config = viewModel.getInstrumentConfig(effectiveStats, instrumentTab),
                                modifier = Modifier.fillMaxSize()
                            )
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x660B0617), RoundedCornerShape(14.dp))
                                        .testTag("highway_studio_preview")
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                                        drawHighwayStudioPreview(
                                            drawScope = this,
                                            width = size.width,
                                            height = size.height,
                                            highwayTheme = activeHighwayTheme,
                                            overlayBitmap = cachedHighwayBitmap,
                                            overlayOpacity = activeHighwayCfg.stampOpacity
                                        )
                                    }
                                    Surface(
                                        color = Color(0xAA000000),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                    ) {
                                        Text(
                                            text = "PERSPECTIVA CLÁSSICA FIXA • ${activeHighwayTheme.title.uppercase()}",
                                            color = RockAccentGold,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Banner se estiver pré-visualizando um item da loja
                    previewingShopItem?.let { item ->
                        Surface(
                            color = RockAccentGold.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "PRÉVIA ATIVA: ${item.title}",
                                        color = RockAccentGold,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Item ainda não comprado (${item.priceCoins} LC)",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 8.sp
                                    )
                                }
                                TextButton(
                                    onClick = { previewingShopItem = null },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("LIMPAR", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    // Resumo rápido equipado
                    Surface(
                        color = Color(0x18FFFFFF),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "${effectiveStats.studioProfile.rockerBodyModel} • ${effectiveStats.studioProfile.stageExpression.label}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "Acabamento: ${viewModel.getInstrumentConfig(effectiveStats, instrumentTab).finish.label} • Moedas: ${stats.coins} LC",
                                color = RockAccentCyan,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Painel Direito: 5 Abas Completas do Estúdio
            Column(
                modifier = Modifier
                    .weight(1.38f)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val tabs = listOf("PERSONAGEM", "INSTRUMENTO", "ESTEIRA", "LOJA & DESAFIOS", "PRESETS")
                    tabs.forEachIndexed { index, label ->
                        Button(
                            onClick = {
                                viewModel.playUiClick()
                                selectedCategory = index
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedCategory == index) RockAccentGold else RockDarkCard,
                                contentColor = if (selectedCategory == index) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("custom_tab_$index")
                        ) {
                            Text(label, fontWeight = FontWeight.Black, fontSize = 8.sp, maxLines = 1)
                        }
                    }
                }

                if (!studioActionMessage.isNullOrBlank()) {
                    Surface(
                        color = RockAccentGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = studioActionMessage.orEmpty(),
                                color = RockAccentGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "✕",
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { viewModel.clearStudioMessages() }
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedCategory) {
                        0 -> {
                            // ABA 0: PERSONAGEM 3D COMPLETO + FOTO + GUARDA-ROUPA + SKINS
                            item {
                                StudioSectionCard(title = "BIOTIPO, TOM DE PELE & EXPRESSÃO NO PALCO", accent = RockAccentGold) {
                                    Text("MODELO CORPORAL 3D", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    OutfitGridOptions(
                                        selected = stats.studioProfile.rockerBodyModel,
                                        options = StudioCatalog.bodyModels
                                    ) { model ->
                                        viewModel.updateRockerBodyAndSkin(model, stats.studioProfile.skinToneHex)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("TOM DE PELE", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    ColorPaletteRow(
                                        colors = StudioCatalog.skinTones,
                                        selectedHex = stats.studioProfile.skinToneHex
                                    ) { tone ->
                                        viewModel.updateRockerBodyAndSkin(stats.studioProfile.rockerBodyModel, tone)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("EXPRESSÃO FACIAL NO SHOW", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        StagePoseExpression.values().forEach { expr ->
                                            val sel = stats.studioProfile.stageExpression == expr
                                            Button(
                                                onClick = { viewModel.updateRockerExpression(expr) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (sel) RockAccentCyan else Color(0x22FFFFFF),
                                                    contentColor = if (sel) Color.Black else Color.White
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f).height(30.dp)
                                            ) {
                                                Text(expr.label, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                val current = stats.customRocker
                                val sp = stats.studioProfile
                                StudioSectionCard(title = "PENTEADO, ROUPAS, CALÇADOS, ACESSÓRIOS & CORES", accent = RockAccentCyan) {
                                    OutfitRow("Penteado 3D", current.hairStyle, StudioCatalog.hairStyles) { opt ->
                                        viewModel.updateCustomRockerAppearance(opt, current.topClothing, current.pantsClothing, current.shoesClothing, current.accessories, current.bodyTattoos)
                                    }
                                    Text("COR DO CABELO", color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    ColorPaletteRow(StudioCatalog.paletteColors, current.hairColorHex) { hex ->
                                        viewModel.updateRockerStudioColors(hex, sp.topColorHex, sp.pantsColorHex, sp.shoesColorHex, sp.accessoryColorHex)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutfitRow("Jaqueta / Peça Superior", current.topClothing, StudioCatalog.topClothings) { opt ->
                                        viewModel.updateCustomRockerAppearance(current.hairStyle, opt, current.pantsClothing, current.shoesClothing, current.accessories, current.bodyTattoos)
                                    }
                                    Text("COR DA JAQUETA / CAMISA", color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    ColorPaletteRow(StudioCatalog.paletteColors, sp.topColorHex) { hex ->
                                        viewModel.updateRockerStudioColors(current.hairColorHex, hex, sp.pantsColorHex, sp.shoesColorHex, sp.accessoryColorHex)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutfitRow("Calça / Bermuda", current.pantsClothing, StudioCatalog.pantsClothings) { opt ->
                                        viewModel.updateCustomRockerAppearance(current.hairStyle, current.topClothing, opt, current.shoesClothing, current.accessories, current.bodyTattoos)
                                    }
                                    Text("COR DA CALÇA", color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    ColorPaletteRow(StudioCatalog.paletteColors, sp.pantsColorHex) { hex ->
                                        viewModel.updateRockerStudioColors(current.hairColorHex, sp.topColorHex, hex, sp.shoesColorHex, sp.accessoryColorHex)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutfitRow("Calçados", current.shoesClothing, StudioCatalog.shoeStyles) { opt ->
                                        viewModel.updateCustomRockerAppearance(current.hairStyle, current.topClothing, current.pantsClothing, opt, current.accessories, current.bodyTattoos)
                                    }
                                    Text("COR DO CALÇADO", color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    ColorPaletteRow(StudioCatalog.paletteColors, sp.shoesColorHex) { hex ->
                                        viewModel.updateRockerStudioColors(current.hairColorHex, sp.topColorHex, sp.pantsColorHex, hex, sp.accessoryColorHex)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutfitRow("Acessórios (Óculos, Correntes & Braceletes)", current.accessories, StudioCatalog.accessoryStyles) { opt ->
                                        viewModel.updateCustomRockerAppearance(current.hairStyle, current.topClothing, current.pantsClothing, current.shoesClothing, opt, current.bodyTattoos)
                                    }
                                    OutfitRow("Tatuagens & Arte Corporal", current.bodyTattoos, StudioCatalog.tattooStyles) { opt ->
                                        viewModel.updateCustomRockerAppearance(current.hairStyle, current.topClothing, current.pantsClothing, current.shoesClothing, current.accessories, opt)
                                    }
                                }
                            }

                            item {
                                StudioSectionCard(title = "SKINS COMPLETAS DE TRAJE", accent = RockAccentGold) {
                                    StudioCatalog.outfitSkins.forEach { skin ->
                                        val isEquipped = stats.studioProfile.fullOutfitSkinId == skin.id
                                        val unlocked = viewModel.isCosmeticUnlocked(skin.id, skin.unlockedByDefault)
                                        CompactShopRow(
                                            title = skin.title,
                                            subtitle = skin.subtitle,
                                            accent = Color(skin.colorHex),
                                            isEquipped = isEquipped,
                                            isUnlocked = unlocked,
                                            priceCoins = skin.priceCoins,
                                            canAfford = stats.coins >= skin.priceCoins,
                                            onPreview = { previewingShopItem = skin },
                                            onAction = {
                                                previewingShopItem = null
                                                if (unlocked) viewModel.equipOutfitSkin(skin.id)
                                                else viewModel.unlockAndEquipCosmetic(skin)
                                            }
                                        )
                                    }
                                }
                            }

                            item {
                                StudioSectionCard(title = "CRIAÇÃO POR FOTO & INTEGRANTES CLÁSSICOS", accent = RockAccentCyan) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = {
                                                rockerPhotoLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("pick_photo_rocker_button")
                                        ) {
                                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("FOTO DO CELULAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.equipCharacter("custom_rocker") },
                                            colors = ButtonDefaults.buttonColors(containerColor = RockAccentGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("equip_custom_rocker_button")
                                        ) {
                                            Text("EQUIPAR MEU ROCKER", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CharacterArtStyle.values().forEach { art ->
                                            val sel = stats.customRocker.artStyle == art
                                            Button(
                                                onClick = { viewModel.updateCustomRockerStyle(art) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (sel) Color(art.badgeColorHex).copy(alpha = 0.25f) else Color(0x18FFFFFF)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(art.displayName, color = Color(art.badgeColorHex), fontWeight = FontWeight.Black, fontSize = 8.sp, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // ABA 1: INSTRUMENTOS 3D (MODELOS, CORES, ACABAMENTO, ESTAMPAS, ADESIVOS & SKINS)
                            item {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (inst in Instrument.values()) {
                                        val isSel = instrumentTab == inst
                                        Button(
                                            onClick = { instrumentTab = inst },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSel) RockAccentCyan else Color(0x22FFFFFF),
                                                contentColor = if (isSel) Color.Black else Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f).testTag("inst_studio_tab_${inst.name}")
                                        ) {
                                            Text(inst.displayName.split(" ")[0].uppercase(), fontWeight = FontWeight.Black, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }

                            item {
                                val cfg = viewModel.getInstrumentConfig(stats, instrumentTab)
                                val models = StudioCatalog.instrumentModelsByType[instrumentTab].orEmpty()
                                StudioSectionCard(title = "ESTÚDIO 3D: ${instrumentTab.displayName.uppercase()}", accent = RockAccentCyan) {
                                    Text("MODELO ESTRUTURAL 3D", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        models.forEach { (modelId, modelLabel) ->
                                            val sel = cfg.modelStyleId == modelId
                                            Button(
                                                onClick = { viewModel.updateInstrumentCustomConfig(instrumentTab, cfg.copy(modelStyleId = modelId)) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (sel) RockAccentGold else Color(0x22FFFFFF),
                                                    contentColor = if (sel) Color.Black else Color.White
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f).height(32.dp)
                                            ) {
                                                Text(modelLabel, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("ACABAMENTO DE PINTURA", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        items(InstrumentFinish.values().toList()) { finish ->
                                            val sel = cfg.finish == finish
                                            Button(
                                                onClick = { viewModel.updateInstrumentCustomConfig(instrumentTab, cfg.copy(finish = finish)) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (sel) RockAccentCyan else Color(0x22FFFFFF),
                                                    contentColor = if (sel) Color.Black else Color.White
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(finish.label, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("COR PRINCIPAL DO CORPO", color = Color.White.copy(alpha = 0.75f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    ColorPaletteRow(StudioCatalog.paletteColors, cfg.primaryColorHex) { hex ->
                                        viewModel.updateInstrumentCustomConfig(instrumentTab, cfg.copy(primaryColorHex = hex, fullSkinId = "skin_none"))
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("COR SECUNDÁRIA / ESCUDO", color = Color.White.copy(alpha = 0.75f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    ColorPaletteRow(StudioCatalog.paletteColors, cfg.secondaryColorHex) { hex ->
                                        viewModel.updateInstrumentCustomConfig(instrumentTab, cfg.copy(secondaryColorHex = hex, fullSkinId = "skin_none"))
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("COR DAS FERRAGENS / CAPTADORES", color = Color.White.copy(alpha = 0.75f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    ColorPaletteRow(StudioCatalog.paletteColors, cfg.hardwareColorHex) { hex ->
                                        viewModel.updateInstrumentCustomConfig(instrumentTab, cfg.copy(hardwareColorHex = hex, fullSkinId = "skin_none"))
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("ESTAMPA NO CORPO", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        items(StudioCatalog.instrumentStamps) { (id, label) ->
                                            val sel = cfg.stampId == id
                                            Button(
                                                onClick = { viewModel.updateInstrumentCustomConfig(instrumentTab, cfg.copy(stampId = id)) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (sel) RockAccentGold else Color(0x22FFFFFF),
                                                    contentColor = if (sel) Color.Black else Color.White
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("ADESIVO DE PALCO", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        items(StudioCatalog.instrumentStickers) { (id, label) ->
                                            val sel = cfg.stickerId == id
                                            Button(
                                                onClick = { viewModel.updateInstrumentCustomConfig(instrumentTab, cfg.copy(stickerId = id)) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (sel) RockAccentGreen else Color(0x22FFFFFF),
                                                    contentColor = if (sel) Color.Black else Color.White
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                val cfg = viewModel.getInstrumentConfig(stats, instrumentTab)
                                StudioSectionCard(title = "SKINS COMPLETAS DO INSTRUMENTO", accent = RockAccentGold) {
                                    StudioCatalog.instrumentSkins.forEach { skin ->
                                        val isEquipped = cfg.fullSkinId == skin.id
                                        val unlocked = viewModel.isCosmeticUnlocked(skin.id, skin.unlockedByDefault)
                                        CompactShopRow(
                                            title = skin.title,
                                            subtitle = skin.subtitle,
                                            accent = Color(skin.colorHex),
                                            isEquipped = isEquipped,
                                            isUnlocked = unlocked,
                                            priceCoins = skin.priceCoins,
                                            canAfford = stats.coins >= skin.priceCoins,
                                            onPreview = { previewingShopItem = skin },
                                            onAction = {
                                                previewingShopItem = null
                                                if (unlocked) {
                                                    viewModel.updateInstrumentCustomConfig(instrumentTab, cfg.copy(fullSkinId = skin.id))
                                                } else {
                                                    viewModel.unlockAndEquipCosmetic(skin)
                                                }
                                            }
                                        )
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
                                val isEq = equippedId == gear.id
                                val unl = viewModel.isCosmeticUnlocked(gear.id, gear.isUnlocked)
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("gear_item_${gear.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isEq) Color(0xFF2E174D) else RockDarkCard)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(gear.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                            Text(gear.modelStyle, color = Color(gear.colorHex), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        if (isEq) {
                                            Text("✔ EQUIPADO", color = RockAccentCyan, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                        } else {
                                            Button(
                                                onClick = {
                                                    if (unl) viewModel.selectGear(gear.instrumentType, gear.id)
                                                    else viewModel.unlockGear(gear.instrumentType, gear.id, gear.priceCoins)
                                                },
                                                enabled = unl || stats.coins >= gear.priceCoins,
                                                colors = ButtonDefaults.buttonColors(containerColor = if (unl) RockAccentCyan else RockAccentGold),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(if (unl) "EQUIPAR" else "${gear.priceCoins} LC", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // ABA 2: ESTEIRA DE NOTAS FIXA + ESTAMPAS + UPLOAD DE IMAGEM SEGURA (SEM SLIDERS DE TAMANHO)
                            item {
                                StudioSectionCard(title = "PERSONALIZAÇÃO DA ESTEIRA (PERSPECTIVA FIXA DE CONSOLE)", accent = RockAccentGold) {
                                    Text(
                                        text = "A largura e inclinação da esteira são fixas para manter precisão nas notas. Personalize tema, estampa ou imagem de fundo abaixo.",
                                        color = Color.White.copy(alpha = 0.72f),
                                        fontSize = 9.sp,
                                        lineHeight = 13.sp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("OPACIDADE DA ESTAMPA / IMAGEM (${(activeHighwayCfg.stampOpacity * 100).toInt()}%)", color = RockAccentCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = activeHighwayCfg.stampOpacity,
                                        onValueChange = { viewModel.updateHighwayStampOpacity(it) },
                                        valueRange = 0.15f..0.90f,
                                        colors = SliderDefaults.colors(thumbColor = RockAccentGold, activeTrackColor = RockAccentGold),
                                        modifier = Modifier.testTag("highway_opacity_slider")
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = {
                                                highwayPhotoLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("upload_highway_image_button")
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ENVIAR IMAGEM PRÓPRIA", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                        }
                                        if (!activeHighwayCfg.customImagePath.isNullOrBlank()) {
                                            OutlinedButton(
                                                onClick = { viewModel.clearCustomHighwayImage() },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("clear_highway_image_button")
                                            ) {
                                                Text("REMOVER IMAGEM", color = RockAccentRed, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                            }
                                        }
                                    }

                                    if (!highwayUploadError.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = highwayUploadError.orEmpty(),
                                            color = RockAccentRed,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            item {
                                StudioSectionCard(title = "GALERIA DE ESTAMPAS EMBUTIDAS DA ESTEIRA", accent = RockAccentCyan) {
                                    StudioCatalog.highwayStamps.forEach { stamp ->
                                        val isEq = activeHighwayCfg.selectedStampId == stamp.id && activeHighwayCfg.customImagePath == null
                                        val unlocked = stamp.unlockedByDefault || stats.unlockedCosmetics.contains(stamp.id)
                                        CompactShopRow(
                                            title = stamp.title,
                                            subtitle = stamp.description,
                                            accent = Color(stamp.accentHex),
                                            isEquipped = isEq,
                                            isUnlocked = unlocked,
                                            priceCoins = stamp.priceCoins,
                                            canAfford = stats.coins >= stamp.priceCoins,
                                            onPreview = {
                                                previewingShopItem = CosmeticShopItem(stamp.id, "Estampas", stamp.title, stamp.description, stamp.priceCoins, stamp.accentHex, stamp.unlockedByDefault)
                                                previewMode = 2
                                            },
                                            onAction = {
                                                previewingShopItem = null
                                                viewModel.unlockHighwayStamp(stamp)
                                            }
                                        )
                                    }
                                }
                            }

                            items(viewModel.highwayThemes) { theme ->
                                val isEquipped = stats.selectedHighwayThemeId == theme.id
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("theme_item_${theme.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isEquipped) Color(0xFF2E174D) else RockDarkCard)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(color = Color(theme.primaryColorHex), shape = CircleShape, modifier = Modifier.size(10.dp)) {}
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(theme.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                            }
                                            Text(theme.description, color = Color.White.copy(alpha = 0.68f), fontSize = 9.sp)
                                        }
                                        if (isEquipped) {
                                            Text("✔ ATIVA", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                        } else {
                                            Button(
                                                onClick = { viewModel.equipHighwayTheme(theme.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("EQUIPAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // ABA 3: LOJA INTERNA COM PRÉ-VISUALIZAÇÃO + DESAFIOS DE MOEDAS
                            item {
                                StudioSectionCard(title = "LOJA DE COSMÉTICOS & ESTILO", accent = RockAccentGold) {
                                    val categories = listOf("Todos", "Roupas & Visual", "Instrumentos", "Skins de Traje", "Skins de Instrumento", "Estampas de Esteira")
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        items(categories) { cat ->
                                            val sel = shopFilter == cat
                                            Button(
                                                onClick = { shopFilter = cat },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (sel) RockAccentGold else Color(0x22FFFFFF),
                                                    contentColor = if (sel) Color.Black else Color.White
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(cat, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val filtered = StudioCatalog.shopCosmetics.filter {
                                        shopFilter == "Todos" || it.category.contains(shopFilter.split(" ")[0], ignoreCase = true)
                                    }
                                    filtered.forEach { item ->
                                        val unlocked = viewModel.isCosmeticUnlocked(item.id, item.unlockedByDefault)
                                        CompactShopRow(
                                            title = "${item.title} (${item.category})",
                                            subtitle = item.subtitle,
                                            accent = Color(item.colorHex),
                                            isEquipped = false,
                                            isUnlocked = unlocked,
                                            priceCoins = item.priceCoins,
                                            canAfford = stats.coins >= item.priceCoins,
                                            itemTagSuffix = item.id,
                                            onPreview = {
                                                previewingShopItem = item
                                                previewMode = when {
                                                    item.category.contains("Esteira") -> 2
                                                    item.category.contains("Instrumento") -> 1
                                                    else -> 0
                                                }
                                            },
                                            onAction = {
                                                previewingShopItem = null
                                                viewModel.unlockAndEquipCosmetic(item)
                                            }
                                        )
                                    }
                                }
                            }

                            item {
                                StudioSectionCard(title = "DESAFIOS PARA GANHAR LEGEND COINS (LC)", accent = RockAccentGreen) {
                                    StudioCatalog.challenges.forEach { challenge ->
                                        val progress = challenge.progressProvider(stats).coerceAtMost(challenge.targetValue)
                                        val claimed = stats.studioProfile.claimedChallenges.contains(challenge.id)
                                        val ready = progress >= challenge.targetValue && !claimed
                                        Surface(
                                            color = Color(0x1AFFFFFF),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(challenge.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                    Text(challenge.description, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                                                    Text(
                                                        text = "Progresso: $progress/${challenge.targetValue} • Recompensa: +${challenge.rewardCoins} LC",
                                                        color = RockAccentGold,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                                when {
                                                    claimed -> Text("✔ RESGATADO", color = RockAccentGreen, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                                    ready -> Button(
                                                        onClick = { viewModel.claimChallengeReward(challenge) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = RockAccentGreen),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.testTag("challenge_claim_${challenge.id}")
                                                    ) {
                                                        Text("RESGATAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                                    }
                                                    else -> Text("${progress}/${challenge.targetValue}", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        4 -> {
                            // ABA 4: PRESETS SALVOS DE APARÊNCIA COMPLETA
                            item {
                                StudioSectionCard(title = "SALVAR NOVO PRESET DE VISUAL COMPLETO", accent = RockAccentCyan) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = presetNameInput,
                                            onValueChange = { presetNameInput = it.take(28) },
                                            label = { Text("Nome do Preset (ex: Metal Arena)", fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("preset_name_input"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = RockAccentCyan,
                                                unfocusedBorderColor = RockCardBorder
                                            )
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.saveAppearancePreset(presetNameInput)
                                                presetNameInput = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RockAccentGreen),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(48.dp).testTag("save_preset_button")
                                        ) {
                                            Text("SALVAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                            val savedPresets = stats.studioProfile.savedPresets
                            if (savedPresets.isEmpty()) {
                                item {
                                    Surface(color = Color(0x18FFFFFF), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Nenhum preset salvo ainda. Monte seu personagem, instrumento e esteira e salve acima!",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                }
                            } else {
                                items(savedPresets) { preset ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("preset_item_${preset.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = RockDarkCard)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(preset.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                                Text(
                                                    text = "${preset.rockerBodyModel} • ${preset.customRocker.hairStyle}",
                                                    color = RockAccentGold,
                                                    fontSize = 9.sp
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = { viewModel.applyAppearancePreset(preset.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = RockAccentCyan),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.testTag("apply_preset_${preset.id}")
                                                ) {
                                                    Text("APLICAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { viewModel.deleteAppearancePreset(preset.id) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.testTag("delete_preset_${preset.id}")
                                                ) {
                                                    Text("EXCLUIR", color = RockAccentRed, fontWeight = FontWeight.Bold, fontSize = 9.sp)
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
}

@Composable
private fun StudioSectionCard(
    title: String,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RockDarkCard),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(listOf(accent.copy(alpha = 0.65f), RockCardBorder))
        )
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = title, color = accent, fontWeight = FontWeight.Black, fontSize = 11.sp)
            content()
        }
    }
}

@Composable
private fun OutfitGridOptions(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { opt ->
            val sel = selected == opt
            Button(
                onClick = { onSelect(opt) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sel) RockAccentGold else Color(0x22FFFFFF),
                    contentColor = if (sel) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).height(30.dp)
            ) {
                Text(opt, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ColorPaletteRow(
    colors: List<Long>,
    selectedHex: Long,
    onSelect: (Long) -> Unit
) {
    var showWheelDialog by remember { mutableStateOf(false) }

    val wheelPalette = remember(colors) {
        if (colors.size >= 10) colors
        else (colors + listOf(
            0xFFFF1744L, 0xFFFF6D00L, 0xFFFFD700L, 0xFF76FF03L,
            0xFF00E676L, 0xFF00E5FFL, 0xFF2979FFL, 0xFF7C4DFFL,
            0xFFE040FBL, 0xFFFF4081L, 0xFFE0E0E0L, 0xFF1F1B26L
        )).distinct()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Roda Cromática clássica compacta para abrir o seletor radial de console
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.75f), CircleShape)
                .clickable { showWheelDialog = true }
                .testTag("color_wheel_button")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val segCount = wheelPalette.size.coerceAtLeast(6)
                val sweep = 360f / segCount
                wheelPalette.take(segCount).forEachIndexed { idx, hex ->
                    drawArc(
                        color = Color(hex),
                        startAngle = idx * sweep - 90f,
                        sweepAngle = sweep + 0.8f,
                        useCenter = true
                    )
                }
                drawCircle(
                    color = Color(0xFF140F1E),
                    radius = size.minDimension * 0.28f
                )
                drawCircle(
                    color = Color(selectedHex),
                    radius = size.minDimension * 0.20f
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(colors) { hex ->
                val sel = hex == selectedHex
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (sel) 2.5.dp else 1.dp,
                            color = if (sel) RockAccentGold else Color.White.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) }
                )
            }
        }
    }

    if (showWheelDialog) {
        ClassicColorWheelDialog(
            colors = wheelPalette,
            selectedHex = selectedHex,
            onSelectColor = { hex ->
                onSelect(hex)
                showWheelDialog = false
            },
            onDismiss = { showWheelDialog = false }
        )
    }
}

@Composable
private fun ClassicColorWheelDialog(
    colors: List<Long>,
    selectedHex: Long,
    onSelectColor: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161024)),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.verticalGradient(listOf(RockAccentGold, RockCardBorder)),
                width = 2.dp
            ),
            modifier = Modifier.testTag("color_wheel_dialog")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ESCOLHER COR (RODA CLÁSSICA)",
                    color = RockAccentGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )

                // Roda segmentada radial interativa
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(196.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(colors) {
                                detectTapGestures { offset ->
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val dx = offset.x - cx
                                    val dy = offset.y - cy
                                    val dist = hypot(dx, dy)
                                    val maxR = size.width / 2f
                                    if (dist in (maxR * 0.28f)..maxR && colors.isNotEmpty()) {
                                        var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                        if (deg < 0f) deg += 360f
                                        val segIdx = ((deg / 360f) * colors.size).toInt().coerceIn(0, colors.lastIndex)
                                        onSelectColor(colors[segIdx])
                                    }
                                }
                            }
                    ) {
                        val segCount = colors.size.coerceAtLeast(1)
                        val sweep = 360f / segCount
                        val pad = 10f
                        val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)

                        // Moldura metálica externa
                        drawCircle(
                            color = Color(0xFF3E354D),
                            radius = size.minDimension * 0.49f
                        )

                        colors.forEachIndexed { idx, hex ->
                            val isSel = hex == selectedHex
                            val startDeg = idx * sweep - 90f
                            drawArc(
                                color = Color(hex),
                                startAngle = startDeg + 1.5f,
                                sweepAngle = (sweep - 3f).coerceAtLeast(2f),
                                useCenter = true,
                                topLeft = Offset(pad, pad),
                                size = arcSize
                            )
                            if (isSel) {
                                drawArc(
                                    color = Color.White,
                                    startAngle = startDeg + 1.5f,
                                    sweepAngle = (sweep - 3f).coerceAtLeast(2f),
                                    useCenter = false,
                                    topLeft = Offset(pad, pad),
                                    size = arcSize,
                                    style = Stroke(width = 5f)
                                )
                            }
                        }

                        // Centro escuro tipo pedal/botão de guitarra com preview da cor atual
                        drawCircle(
                            color = Color(0xFF120D1C),
                            radius = size.minDimension * 0.21f
                        )
                        drawCircle(
                            color = Color(0xFFD5B66B),
                            radius = size.minDimension * 0.21f,
                            style = Stroke(width = 3f)
                        )
                        drawCircle(
                            color = Color(selectedHex),
                            radius = size.minDimension * 0.14f
                        )
                    }
                }

                Text(
                    text = "Toque em qualquer fatia do círculo ou feche abaixo",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 10.sp
                )

                TextButton(onClick = onDismiss) {
                    Text("FECHAR", color = RockAccentCyan, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CompactShopRow(
    title: String,
    subtitle: String,
    accent: Color,
    isEquipped: Boolean,
    isUnlocked: Boolean,
    priceCoins: Int,
    canAfford: Boolean,
    itemTagSuffix: String = title,
    onPreview: () -> Unit,
    onAction: () -> Unit
) {
    Surface(
        color = if (isEquipped) Color(0xFF2E174D) else Color(0x18FFFFFF),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(accent))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                    Text(subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 8.sp, maxLines = 1)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onPreview,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp).testTag("shop_preview_$itemTagSuffix")
                ) {
                    Text("PRÉVIA", color = RockAccentCyan, fontWeight = FontWeight.Black, fontSize = 8.sp)
                }
                if (isEquipped) {
                    Text("✔ ATIVO", color = RockAccentGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
                } else {
                    Button(
                        onClick = onAction,
                        enabled = isUnlocked || canAfford,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isUnlocked) RockAccentCyan else RockAccentGold),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp).testTag("shop_buy_$itemTagSuffix")
                    ) {
                        Text(
                            text = if (isUnlocked) "EQUIPAR" else "${priceCoins} LC",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}
