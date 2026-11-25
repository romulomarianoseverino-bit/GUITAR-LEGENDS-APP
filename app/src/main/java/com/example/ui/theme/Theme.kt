package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = ArenaGold,
    onPrimary = Color(0xFF1C1300),
    secondary = ArenaCyan,
    onSecondary = Color(0xFF002026),
    tertiary = ArenaCrimson,
    onTertiary = Color.White,
    background = ArenaBgDeep,
    onBackground = ArenaTextLight,
    surface = ArenaSurface,
    onSurface = ArenaTextLight,
    surfaceVariant = ArenaSurfaceVariant,
    onSurfaceVariant = ArenaTextMuted
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFFB28900),
    onPrimary = Color.White,
    secondary = Color(0xFF008394),
    onSecondary = Color.White,
    tertiary = ArenaCrimson,
    onTertiary = Color.White,
    background = Color(0xFFF8F5FF),
    onBackground = ArenaBgDeep,
    surface = Color.White,
    onSurface = ArenaBgDeep
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
