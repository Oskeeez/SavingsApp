package com.example.coolingoffjar.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colours from the "Cooling-Off Jar UI Design System" board:
 *   Cream #FAF6EC, Beige #EADFCC, Sage #A7BB9A, Pale Blue #D9E7EB,
 *   Ochre Gold #E0B15E, Warm Brown #8B7355, Charcoal #4B4239, Soft Stone #C9BBA7.
 * Two are nudged for readable text: the secondary text brown is a shade deeper (#7A6347, 5.3:1 on cream),
 * and the primary button green is the board's sage taken darker (#62735A) so cream text on it passes 4.5:1.
 */
@Immutable
data class JarPalette(
    val background: Color,
    /** Card / panel fill (the board's beige-cream cards). */
    val surface: Color,
    val surfaceAlpha: Float,
    val text: Color,
    val textSecondary: Color,
    val gold: Color,
    val goldShadow: Color,
    val goldHighlight: Color,
    /** Text/icon colour to use on top of [gold]. */
    val onGold: Color,
    val sage: Color,
    /** Primary button fill. */
    val sageDeep: Color,
    val onSage: Color,
    val beige: Color,
    val stone: Color,
    val paleBlue: Color,
    /** Soft peach used by the "Freebie available" badge. */
    val peach: Color,
    val glassEdge: Color,
    val glassRim: Color,
    val glassTint: Color,
    val isDark: Boolean,
)

val LightJarPalette = JarPalette(
    background = Color(0xFFFAF6EC),
    surface = Color(0xFFF3E7D3),
    surfaceAlpha = 0.85f,
    text = Color(0xFF4B4239),
    textSecondary = Color(0xFF7A6347),
    gold = Color(0xFFE0B15E),
    goldShadow = Color(0xFFB98A3C),
    goldHighlight = Color(0xFFF3D590),
    onGold = Color(0xFF4B4239),
    sage = Color(0xFFA7BB9A),
    sageDeep = Color(0xFF62735A),
    onSage = Color(0xFFFAF6EC),
    beige = Color(0xFFEADFCC),
    stone = Color(0xFFC9BBA7),
    paleBlue = Color(0xFFD9E7EB),
    peach = Color(0xFFF5DEBE),
    glassEdge = Color(0xFFD9E7EB),
    glassRim = Color.White.copy(alpha = 0.6f),
    glassTint = Color.White.copy(alpha = 0.18f),
    isDark = false,
)

// A candle-lit evening version of the same palette.
val DarkJarPalette = JarPalette(
    background = Color(0xFF211D18),
    surface = Color(0xFF2E2820),
    surfaceAlpha = 1f,
    text = Color(0xFFF3EBDD),
    textSecondary = Color(0xFFC9BBA7),
    gold = Color(0xFFE6BC6A),
    goldShadow = Color(0xFFD9A12E),
    goldHighlight = Color(0xFFF7DDA0),
    onGold = Color(0xFF2B2418),
    sage = Color(0xFF8FA382),
    sageDeep = Color(0xFF8FA382),
    onSage = Color(0xFF1F2A1C),
    beige = Color(0xFF3A3228),
    stone = Color(0xFF6E6354),
    paleBlue = Color(0xFF3B4A4F),
    peach = Color(0xFF5A4630),
    glassEdge = Color(0xFF7FA0AC),
    glassRim = Color.White.copy(alpha = 0.35f),
    glassTint = Color.White.copy(alpha = 0.10f),
    isDark = true,
)
