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

// Светлая палитра в духе Яндекс Заправок: чистый белый фон, насыщенный зелёный акцент,
// мягкие светло-серые «пилюли» и карточки.
private val LightColors = lightColorScheme(
    primary = Color(0xFF15B34A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDF5E4),
    onPrimaryContainer = Color(0xFF05351A),
    secondary = Color(0xFF00B3A4),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC9F3EE),
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = Color(0xFF1466D8),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD8E6FF),
    onTertiaryContainer = Color(0xFF001B3D),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF16181B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16181B),
    surfaceVariant = Color(0xFFEFF1F3),
    onSurfaceVariant = Color(0xFF6A7075),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFBFC),
    surfaceContainer = Color(0xFFF3F4F6),
    surfaceContainerHigh = Color(0xFFEDEEF1),
    surfaceContainerHighest = Color(0xFFE6E8EB),
    outline = Color(0xFFC6C9CE),
    outlineVariant = Color(0xFFE4E6E9),
    error = Color(0xFFE5484D)
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

// Фирменный зелёный градиент (как кнопка «Подтверждаю» у Яндекс Заправок).
val BrandGradient = listOf(Color(0xFF3AD07D), Color(0xFF12B36A))

// Крупные «выразительные» скругления (пилюли/капсулы, как у Яндекса).
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun FuelMapTheme(
    darkTheme: Boolean = false,       // светлый «яндекс»-вид по умолчанию
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
