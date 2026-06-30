package com.fuelmap.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.fuelmap.app.R

private fun manrope(weight: Int, fontWeight: FontWeight) =
    Font(
        resId = R.font.manrope,
        weight = fontWeight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight))
    )

val Manrope = FontFamily(
    manrope(400, FontWeight.Normal),
    manrope(500, FontWeight.Medium),
    manrope(600, FontWeight.SemiBold),
    manrope(700, FontWeight.Bold),
    manrope(800, FontWeight.ExtraBold)
)

val AppTypography: Typography = Typography().let { d ->
    fun TextStyle.m(weight: FontWeight = this.fontWeight ?: FontWeight.Normal): TextStyle =
        copy(fontFamily = Manrope, fontWeight = weight)
    d.copy(
        displayLarge = d.displayLarge.m(FontWeight.ExtraBold),
        displayMedium = d.displayMedium.m(FontWeight.ExtraBold),
        displaySmall = d.displaySmall.m(FontWeight.Bold),
        headlineLarge = d.headlineLarge.m(FontWeight.Bold),
        headlineMedium = d.headlineMedium.m(FontWeight.Bold),
        headlineSmall = d.headlineSmall.m(FontWeight.Bold),
        titleLarge = d.titleLarge.m(FontWeight.Bold),
        titleMedium = d.titleMedium.m(FontWeight.SemiBold),
        titleSmall = d.titleSmall.m(FontWeight.SemiBold),
        bodyLarge = d.bodyLarge.m(FontWeight.Normal),
        bodyMedium = d.bodyMedium.m(FontWeight.Normal),
        bodySmall = d.bodySmall.m(FontWeight.Normal),
        labelLarge = d.labelLarge.m(FontWeight.SemiBold),
        labelMedium = d.labelMedium.m(FontWeight.Medium),
        labelSmall = d.labelSmall.m(FontWeight.Medium)
    )
}
