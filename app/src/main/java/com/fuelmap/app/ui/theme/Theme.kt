package com.fuelmap.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Брендовая сине-голубая палитра + насыщенные tonal-поверхности (Material 3 Expressive).
private val LightColors = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF1F6587),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC6E7FF),
    onSecondaryContainer = Color(0xFF001E2E),
    tertiary = Color(0xFFE06C00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC2),
    onTertiaryContainer = Color(0xFF2E1500),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF42474E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3FA),
    surfaceContainer = Color(0xFFECEEF4),
    surfaceContainerHigh = Color(0xFFE6E8EF),
    surfaceContainerHighest = Color(0xFFE1E2E9),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7CF),
    error = Color(0xFFBA1A1A)
)

// Премиальная тёмная палитра (как в референсах): глубокий сине-чёрный фон,
// электрик-синий акцент, «стеклянные» tonal-поверхности.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF5AA2FF),
    onPrimary = Color(0xFF00264D),
    primaryContainer = Color(0xFF1C4E86),
    onPrimaryContainer = Color(0xFFD6E7FF),
    secondary = Color(0xFF53D2FF),
    onSecondary = Color(0xFF00344A),
    secondaryContainer = Color(0xFF134E63),
    onSecondaryContainer = Color(0xFFBFECFF),
    tertiary = Color(0xFFFFC34D),
    onTertiary = Color(0xFF3A2A00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFE6B0),
    background = Color(0xFF090C13),
    onBackground = Color(0xFFE7EBF3),
    surface = Color(0xFF090C13),
    onSurface = Color(0xFFE7EBF3),
    surfaceVariant = Color(0xFF222B39),
    onSurfaceVariant = Color(0xFFAAB3C5),
    surfaceContainerLowest = Color(0xFF05080D),
    surfaceContainerLow = Color(0xFF0F141D),
    surfaceContainer = Color(0xFF141A24),
    surfaceContainerHigh = Color(0xFF1C2330),
    surfaceContainerHighest = Color(0xFF252E3D),
    outline = Color(0xFF3A4557),
    outlineVariant = Color(0xFF222B39),
    error = Color(0xFFFFB4AB),
    inverseSurface = Color(0xFFE7EBF3),
    inverseOnSurface = Color(0xFF11151D)
)

// Крупные «выразительные» скругления.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun FuelMapTheme(
    darkTheme: Boolean = true,        // премиальный тёмный вид по умолчанию (как в референсах)
    dynamicColor: Boolean = false,    // фиксированный фирменный акцент, без подмены цветами обоев
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
