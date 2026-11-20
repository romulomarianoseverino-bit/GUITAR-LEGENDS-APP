package com.example.guitarlegends.engine

import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.sceneview.ModelNode
import io.github.sceneview.SceneView
import io.github.sceneview.rememberModelInstance
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.InflaterInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Primeiro bloco de validação do pipeline 3D real.
 *
 * O GLB é um modelo temporário de bloqueio visual: malha tridimensional com
 * vértices e materiais, ainda sem rig, animações ou ligação ao perfil cosmético.
 * Não substitui o personagem final nem o editor 3D completo.
 */
@Composable
fun RealCharacter3DPrototype(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var modelLocation by remember { mutableStateOf<String?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        try {
            val modelFile = withContext(Dispatchers.IO) {
                val target = File(context.cacheDir, "guitar_legends_rocker_blockout.glb")
                val validExisting = target.exists() && target.length() > 12L &&
                    target.inputStream().use { input ->
                        input.read() == 0x67 && input.read() == 0x6C &&
                            input.read() == 0x54 && input.read() == 0x46
                    }

                if (!validExisting) {
                    val encoded = context.assets
                        .open("models/rocker_blockout.glb.zlib.b64")
                        .bufferedReader()
                        .use { it.readText() }
                    val compressed = Base64.decode(encoded, Base64.DEFAULT)
                    val temporary = File(context.cacheDir, "rocker_blockout.glb.tmp")
                    InflaterInputStream(ByteArrayInputStream(compressed)).use { input ->
                        temporary.outputStream().use { output -> input.copyTo(output) }
                    }
                    val validGlb = temporary.inputStream().use { input ->
                        input.read() == 0x67 && input.read() == 0x6C &&
                            input.read() == 0x54 && input.read() == 0x46
                    }
                    check(validGlb) { "O arquivo do modelo 3D não é um GLB válido." }
                    if (target.exists()) target.delete()
                    check(temporary.renameTo(target)) { "Não foi possível preparar o modelo 3D." }
                }
                target
            }
            modelLocation = Uri.fromFile(modelFile).toString()
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF100B19)),
        contentAlignment = Alignment.Center
    ) {
        val location = modelLocation
        if (location != null) {
            SceneView(modifier = Modifier.fillMaxSize()) {
                rememberModelInstance(modelLoader, fileLocation = location)?.let { instance ->
                    ModelNode(
                        modelInstance = instance,
                        scaleToUnits = 1.85f,
                        autoAnimate = false
                    )
                }
            }
        } else {
            Text(
                text = when {
                    loadFailed -> "Não foi possível carregar o modelo 3D de teste."
                    else -> "Preparando o primeiro modelo 3D real…"
                },
                color = if (loadFailed) Color(0xFFFF8A80) else Color(0xFF80DEEA),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
