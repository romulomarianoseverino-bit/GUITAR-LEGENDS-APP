package com.example.guitarlegends.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

/**
 * Prova isolada do pipeline 3D real com Filament/SceneView.
 *
 * A cena carrega a malha GLB contínua gerada em
 * app/src/main/assets/models/continuous_human_base.glb. Não monta mais o
 * corpo com SphereNode/Ellipsoid. A malha atual é uma base anatômica neutra:
 * ainda não contém roupa detalhada, guitarra, rig esquelético ou animações.
 *
 * Esta tela permanece isolada para validação visual; não integra a produção.
 */
@Composable
fun Character3DSceneProof(modifier: Modifier = Modifier) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)

    SceneView(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        cameraManipulator = rememberCameraManipulator()
    ) {
        // Caminho relativo a app/src/main/assets/. O helper trata o carregamento
        // assíncrono e recompõe a cena quando a instância GLB estiver pronta.
        rememberModelInstance(
            modelLoader = modelLoader,
            assetFileLocation = "models/continuous_human_base.glb"
        )?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.72f,
                autoAnimate = false
            )
        }
    }
}
