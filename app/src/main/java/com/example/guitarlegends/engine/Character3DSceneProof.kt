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
 * Esta etapa substitui o bloqueio de cubos e cilindros por volumes elipsoidais
 * sobrepostos, para testar uma silhueta humana mais orgânica. Ainda não é um
 * personagem final: não possui malha contínua, rig esquelético, roupa detalhada
 * nem animações de clipes. Não integrar à produção antes da validação em aparelho.
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

    SceneView(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        cameraManipulator = rememberCameraManipulator()
    ) {
        // Cabeça, cabelo, pescoço e ombros: formas ovais em vez de blocos.
        Ellipsoid(Position(0f, 0.79f, 0f), Scale(0.135f, 0.17f, 0.12f), skin)
        Ellipsoid(Position(0f, 0.895f, -0.018f), Scale(0.145f, 0.075f, 0.125f), hair)
        Ellipsoid(Position(0f, 0.59f, 0f), Scale(0.055f, 0.105f, 0.055f), skin)

        // Tronco com tórax mais largo, cintura mais estreita e quadril natural.
        Ellipsoid(Position(0f, 0.405f, 0f), Scale(0.225f, 0.19f, 0.125f), jacket)
        Ellipsoid(Position(0f, 0.245f, 0f), Scale(0.175f, 0.19f, 0.115f), jacket)
        Ellipsoid(Position(0f, 0.065f, 0f), Scale(0.185f, 0.125f, 0.12f), pants)

        // Ombros arredondados, braços superiores, cotovelos, antebraços e mãos.
        for (side in listOf(-1f, 1f)) {
            Ellipsoid(Position(side * 0.225f, 0.47f, 0f), Scale(0.105f, 0.105f, 0.105f), jacket)
            Ellipsoid(Position(side * 0.285f, 0.315f, 0f), Scale(0.078f, 0.17f, 0.078f), jacket)
            Ellipsoid(Position(side * 0.305f, 0.16f, 0f), Scale(0.073f, 0.075f, 0.073f), jacket)
            Ellipsoid(Position(side * 0.315f, 0.025f, 0f), Scale(0.062f, 0.145f, 0.062f), jacket)
            Ellipsoid(Position(side * 0.315f, -0.105f, -0.005f), Scale(0.055f, 0.065f, 0.055f), skin)
        }

        // Coxas, joelhos, panturrilhas e botas com volumes graduais.
        for (side in listOf(-1f, 1f)) {
            Ellipsoid(Position(side * 0.105f, -0.145f, 0f), Scale(0.092f, 0.19f, 0.10f), pants)
            Ellipsoid(Position(side * 0.11f, -0.335f, 0f), Scale(0.078f, 0.085f, 0.085f), pants)
            Ellipsoid(Position(side * 0.115f, -0.505f, 0.005f), Scale(0.072f, 0.18f, 0.078f), pants)
            Ellipsoid(Position(side * 0.12f, -0.665f, -0.025f), Scale(0.075f, 0.085f, 0.09f), pants)
            Ellipsoid(Position(side * 0.12f, -0.735f, -0.065f), Scale(0.095f, 0.055f, 0.15f), shoes)
        }

        // Detalhes simples para dar leitura de roupa e escala ao corpo.
        Ellipsoid(Position(0f, 0.34f, -0.13f), Scale(0.025f, 0.12f, 0.012f), accent)
        Ellipsoid(Position(0f, 0.065f, -0.115f), Scale(0.055f, 0.025f, 0.018f), accent)
    }
}

/** Esfera esticada em escala não uniforme para formar volumes anatômicos arredondados. */
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
