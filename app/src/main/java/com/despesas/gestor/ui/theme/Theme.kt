package com.despesas.gestor.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Paleta limpa e minimalista: verde-azulado como cor principal, neutros suaves.
val Green700 = Color(0xFF0E7C66)
val Green500 = Color(0xFF19A188)
val GreenContainer = Color(0xFFCDEFE6)
val Coral = Color(0xFFE5644E)
val Amber = Color(0xFFE0A64B)

private val LightColors = lightColorScheme(
    primary = Green700,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = Color(0xFF07271F),
    secondary = Green500,
    onSecondary = Color.White,
    background = Color(0xFFF7F9F8),
    onBackground = Color(0xFF1A1C1B),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFEDF1EF),
    onSurfaceVariant = Color(0xFF52605B),
    error = Coral,
    outline = Color(0xFFC3CCC8)
)

private val DarkColors = darkColorScheme(
    primary = Green500,
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF0B5A49),
    onPrimaryContainer = GreenContainer,
    secondary = Green500,
    background = Color(0xFF111413),
    onBackground = Color(0xFFE1E3E1),
    surface = Color(0xFF191D1B),
    onSurface = Color(0xFFE1E3E1),
    surfaceVariant = Color(0xFF2A302D),
    onSurfaceVariant = Color(0xFFBEC9C4),
    error = Coral,
    outline = Color(0xFF48534E)
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp)
)

@Composable
fun GestorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
