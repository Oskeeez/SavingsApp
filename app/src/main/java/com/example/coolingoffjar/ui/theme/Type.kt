package com.example.coolingoffjar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.coolingoffjar.R

/**
 * Mali: the soft, handwritten rounded face the design-system boards use for "Take your time." and "My Shelf".
 * Bundled in res/font (no downloadable fonts, no network).
 */
val Mali = FontFamily(
    Font(R.font.mali_regular, FontWeight.Light),
    Font(R.font.mali_regular, FontWeight.Normal),
    Font(R.font.mali_medium, FontWeight.Medium),
    Font(R.font.mali_semibold, FontWeight.SemiBold),
    Font(R.font.mali_semibold, FontWeight.Bold),
)

// Headings are the heavier cuts, as on the boards; body text stays regular. All sizes are sp so large fonts scale.
val JarTypography = Typography(
    displayLarge = TextStyle(fontFamily = Mali, fontWeight = FontWeight.SemiBold, fontSize = 48.sp, lineHeight = 58.sp),
    displayMedium = TextStyle(fontFamily = Mali, fontWeight = FontWeight.SemiBold, fontSize = 38.sp, lineHeight = 46.sp),
    headlineLarge = TextStyle(fontFamily = Mali, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Mali, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Mali, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Mali, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Mali, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = Mali, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = Mali, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = Mali, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Mali, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Mali, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = Mali, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = Mali, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
)
