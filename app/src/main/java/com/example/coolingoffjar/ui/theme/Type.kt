@file:OptIn(ExperimentalTextApi::class)

package com.example.coolingoffjar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.coolingoffjar.R

/**
 * Nunito is bundled as a variable font in res/font (no downloadable fonts, no network).
 * Variable axes need API 26+, which matches our minSdk.
 */
private fun nunito(weight: FontWeight) = Font(
    resId = R.font.nunito_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Nunito = FontFamily(
    nunito(FontWeight.ExtraLight),
    nunito(FontWeight.Light),
    nunito(FontWeight.Normal),
    nunito(FontWeight.Medium),
    nunito(FontWeight.SemiBold),
)

// Large, light headings; generous line heights. All sizes are sp so large font settings scale.
val JarTypography = Typography(
    displayLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraLight, fontSize = 52.sp, lineHeight = 60.sp),
    displayMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraLight, fontSize = 40.sp, lineHeight = 48.sp),
    headlineLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Light, fontSize = 34.sp, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Light, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Light, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 26.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp),
)
