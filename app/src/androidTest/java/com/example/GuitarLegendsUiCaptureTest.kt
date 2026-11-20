package com.example

import android.graphics.Bitmap
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end UI smoke checks on a real Android emulator.
 *
 * Captures are written to the app's external files directory so CI can pull
 * them from the emulator and publish them as a workflow artifact.
 */
@RunWith(AndroidJUnit4::class)
class GuitarLegendsUiCaptureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun openCustomizationAndCaptureRealScreen() {
        awaitTag("menu_customization")
        composeRule.onNodeWithTag("menu_customization").performClick()
        awaitTag("preview_mode_0")
        awaitTag("custom_tab_0")
        captureScreen("01-personalizacao.png")
    }

    @Test
    fun startGameplayAndCaptureRealHighway() {
        awaitTag("menu_quick_play")
        composeRule.onNodeWithTag("menu_quick_play").performClick()

        // The built-in first track is selected by default; continue to instrument selection.
        awaitTag("proceed_to_instrument_button")
        composeRule.onNodeWithTag("proceed_to_instrument_button").performClick()

        awaitTag("instrument_guitar")
        composeRule.onNodeWithTag("instrument_guitar").performClick()

        awaitTag("start_concert_button")
        composeRule.onNodeWithTag("start_concert_button").performClick()

        // Wait for the actual gameplay controls, not just the launch button.
        awaitTag("fret_touch_controls", timeoutMillis = 90_000)
        awaitTag("fret_touch_button_0")
        captureScreen("02-partida-pista.png")
    }

    private fun awaitTag(tag: String, timeoutMillis: Long = 45_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasTestTag(tag)
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(tag).assertExists()
    }

    private fun captureScreen(fileName: String) {
        composeRule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap: Bitmap = instrumentation.uiAutomation.takeScreenshot()
            ?: throw AssertionError("O Android não retornou uma captura de tela para $fileName")

        val context = instrumentation.targetContext
        val outputDir = File(context.getExternalFilesDir(null), "guitar-legends-captures")
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw AssertionError("Não foi possível criar a pasta de capturas: ${outputDir.absolutePath}")
        }
        val output = File(outputDir, fileName)
        output.outputStream().use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                throw AssertionError("Falha ao salvar a captura: ${output.absolutePath}")
            }
        }
        bitmap.recycle()
        println("CAPTURE_SAVED: ${output.absolutePath}")
    }
}
