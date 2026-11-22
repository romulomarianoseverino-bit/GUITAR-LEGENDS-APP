package com.example.guitarlegends.engine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

/**
 * Tela de diagnóstico acessível somente na variante debug.
 * Não altera a navegação nem a tela de personalização de produção.
 */
class Character3DSceneProofActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Character3DSceneProof(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
