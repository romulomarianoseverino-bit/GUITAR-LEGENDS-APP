package com.example.guitarlegends.engine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.math.Float3
import io.github.sceneview.math.Position

/**
 * Prova isolada do pipeline 3D real com Filament/SceneView.
 *
 * Usa geometria 3D nativa como bloqueio inicial de personagem. Ainda não é
 * o modelo artístico final, não contém rig esquelético nem animações de clipes.
 * Não substituir o renderizador de produção antes da validação em dispositivo.
 */
@Composable
fun Character3DSceneProof(modifier: Modifier = Modifier) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)

    // SceneView 4.18.0 recebe a cor Compose diretamente; a sobrecarga com
    // parâmetro "unlit" pertence a versões posteriores da API.
    val skin = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFFE2B58F))
    }
    val jacket = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF252B3A))
    }
    val pants = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF171B25))
    }
    val hair = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF24170F))
    }
    val accent = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFFFFB300))
    }

    SceneView(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        cameraManipulator = rememberCameraManipulator()
    ) {
        // Cabeça e cabelo
        SphereNode(
            radius = 0.145f,
            materialInstance = skin,
            position = Position(0f, 0.77f, 0f)
        )
        SphereNode(
            radius = 0.15f,
            materialInstance = hair,
            position = Position(0f, 0.88f, -0.025f)
        )

        // Tronco e braços
        CubeNode(
            size = Float3(0.42f, 0.52f, 0.22f),
            materialInstance = jacket,
            position = Position(0f, 0.31f, 0f)
        )
        CylinderNode(
            radius = 0.055f,
            height = 0.43f,
            materialInstance = jacket,
            position = Position(-0.27f, 0.31f, 0f)
        )
        CylinderNode(
            radius = 0.055f,
            height = 0.43f,
            materialInstance = jacket,
            position = Position(0.27f, 0.31f, 0f)
        )

        // Pernas
        CylinderNode(
            radius = 0.075f,
            height = 0.48f,
            materialInstance = pants,
            position = Position(-0.12f, -0.18f, 0f)
        )
        CylinderNode(
            radius = 0.075f,
            height = 0.48f,
            materialInstance = pants,
            position = Position(0.12f, -0.18f, 0f)
        )

        // Detalhe visual de palco no peito
        CubeNode(
            size = Float3(0.10f, 0.10f, 0.035f),
            materialInstance = accent,
            position = Position(0f, 0.37f, 0.13f)
        )
    }
}
