package com.example.guitarlegends.engine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.filament.MaterialInstance
import io.github.sceneview.SceneScope
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader

/**
 * Prova isolada do pipeline 3D real com Filament/SceneView.
 *
 * Esta etapa refina a silhueta com volumes mais sobrepostos e acrescenta
 * elementos faciais legíveis. Continua sendo um bloqueio anatômico de prova:
 * não possui malha contínua, rig esquelético, topologia facial ou animações.
 * Não integrar à produção antes da validação visual em aparelho.
 */
@Composable
fun Character3DSceneProof(modifier: Modifier = Modifier) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)

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
    val shoes = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF090B12))
    }
    val eyeWhite = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFFF7F3EC))
    }
    val eyeDark = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF241B19))
    }
    val lips = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF925B50))
    }

    SceneView(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        cameraManipulator = rememberCameraManipulator()
    ) {
        // Cabeça e cabelo. O rosto está voltado para a câmera, no eixo -Z.
        Ellipsoid(Position(0f, 0.79f, 0f), Scale(0.135f, 0.17f, 0.12f), skin)
        Ellipsoid(Position(0f, 0.895f, -0.018f), Scale(0.145f, 0.075f, 0.125f), hair)
        Ellipsoid(Position(0f, 0.59f, 0f), Scale(0.06f, 0.115f, 0.06f), skin)

        // Orelhas, com volume e posição lateral visíveis em vista frontal.
        for (side in listOf(-1f, 1f)) {
            Ellipsoid(Position(side * 0.132f, 0.785f, -0.002f), Scale(0.035f, 0.058f, 0.035f), skin)
            Ellipsoid(Position(side * 0.145f, 0.785f, -0.027f), Scale(0.014f, 0.03f, 0.012f), lips)
        }

        // Olhos, íris/pupilas e sobrancelhas.
        for (side in listOf(-1f, 1f)) {
            Ellipsoid(Position(side * 0.052f, 0.815f, -0.105f), Scale(0.027f, 0.018f, 0.018f), eyeWhite)
            Ellipsoid(Position(side * 0.052f, 0.815f, -0.121f), Scale(0.012f, 0.014f, 0.009f), eyeDark)
            Ellipsoid(Position(side * 0.052f, 0.852f, -0.105f), Scale(0.032f, 0.009f, 0.012f), hair)
        }

        // Ponte, ponta e narinas do nariz, projetadas discretamente para a frente.
        Ellipsoid(Position(0f, 0.785f, -0.119f), Scale(0.018f, 0.045f, 0.025f), skin)
        Ellipsoid(Position(0f, 0.759f, -0.139f), Scale(0.025f, 0.018f, 0.022f), skin)
        for (side in listOf(-1f, 1f)) {
            Ellipsoid(Position(side * 0.012f, 0.752f, -0.153f), Scale(0.005f, 0.004f, 0.004f), eyeDark)
        }

        // Boca simples, com lábio superior/inferior em volumes discretos.
        Ellipsoid(Position(0f, 0.715f, -0.121f), Scale(0.034f, 0.008f, 0.012f), lips)
        Ellipsoid(Position(0f, 0.704f, -0.12f), Scale(0.027f, 0.006f, 0.01f), skin)

        // Tronco mais contínuo: sobreposição maior entre tórax, cintura e quadril.
        Ellipsoid(Position(0f, 0.405f, 0f), Scale(0.23f, 0.205f, 0.13f), jacket)
        Ellipsoid(Position(0f, 0.255f, 0f), Scale(0.19f, 0.205f, 0.12f), jacket)
        Ellipsoid(Position(0f, 0.075f, 0f), Scale(0.19f, 0.14f, 0.125f), pants)

        // Braços com volumes sobrepostos para reduzir a leitura de segmentos soltos.
        for (side in listOf(-1f, 1f)) {
            Ellipsoid(Position(side * 0.215f, 0.455f, 0f), Scale(0.11f, 0.12f, 0.11f), jacket)
            Ellipsoid(Position(side * 0.275f, 0.32f, 0f), Scale(0.083f, 0.17f, 0.082f), jacket)
            Ellipsoid(Position(side * 0.295f, 0.18f, 0f), Scale(0.078f, 0.09f, 0.078f), jacket)
            Ellipsoid(Position(side * 0.305f, 0.055f, 0f), Scale(0.068f, 0.15f, 0.068f), skin)
            Ellipsoid(Position(side * 0.305f, -0.085f, -0.005f), Scale(0.06f, 0.065f, 0.055f), skin)
        }

        // Pernas com transições mais graduais entre quadril, coxas e panturrilhas.
        for (side in listOf(-1f, 1f)) {
            Ellipsoid(Position(side * 0.105f, -0.12f, 0f), Scale(0.095f, 0.20f, 0.10f), pants)
            Ellipsoid(Position(side * 0.11f, -0.31f, 0f), Scale(0.083f, 0.095f, 0.087f), pants)
            Ellipsoid(Position(side * 0.115f, -0.49f, 0.005f), Scale(0.076f, 0.19f, 0.08f), pants)
            Ellipsoid(Position(side * 0.12f, -0.655f, -0.025f), Scale(0.078f, 0.09f, 0.092f), pants)
            Ellipsoid(Position(side * 0.12f, -0.735f, -0.065f), Scale(0.098f, 0.058f, 0.15f), shoes)
        }

        // Detalhes discretos da roupa.
        Ellipsoid(Position(0f, 0.34f, -0.13f), Scale(0.025f, 0.12f, 0.012f), accent)
        Ellipsoid(Position(0f, 0.075f, -0.12f), Scale(0.055f, 0.025f, 0.018f), accent)
    }
}

/** Esfera esticada em escala não uniforme para formar volumes arredondados. */
@Composable
private fun SceneScope.Ellipsoid(
    position: Position,
    scale: Scale,
    materialInstance: MaterialInstance
) {
    Node(position = position, scale = scale) {
        SphereNode(radius = 1f, materialInstance = materialInstance)
    }
}
