package com.example.coolingoffjar.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** App-specific palette on top of the Material 3 scheme: gold + glass tokens the jar canvas needs. */
@Immutable
data class JarPalette(
    val background: Color,
    val surface: Color,
    val surfaceAlpha: Float,
    val text: Color,
    val textSecondary: Color,
    val gold: Color,
    val goldShadow: Color,
    val goldHighlight: Color,
    val glassTint: Color,
    val glassRim: Color,
    val glassEdge: Color,
    /** Text/icon colour to use on top of [gold]. */
    val onGold: Color,
    val isDark: Boolean,
)

val LightJarPalette = JarPalette(
    background = Color(0xFFFAF6EE),
    surface = Color(0xFFFFFFFF),
    surfaceAlpha = 0.70f,
    text = Color(0xFF2B2A2E),
    textSecondary = Color(0xFF6F6B73),
    gold = Color(0xFFE8B84A),
    goldShadow = Color(0xFFC98F1E),
    goldHighlight = Color(0xFFFFE9A8),
    glassTint = Color.White.copy(alpha = 0.18f),
    glassRim = Color.White.copy(alpha = 0.60f),
    glassEdge = Color(0xFFCFE3EA),
    onGold = Color(0xFF2B2A2E),
    isDark = false,
)

val DarkJarPalette = JarPalette(
    background = Color(0xFF1B1A1F),
    surface = Color(0xFF25242B),
    surfaceAlpha = 1f,
    text = Color(0xFFF2EEE6),
    textSecondary = Color(0xFFB0ABB5),
    // Same golds, slightly brighter against the dark background.
    gold = Color(0xFFF0C45A),
    goldShadow = Color(0xFFD9A12E),
    goldHighlight = Color(0xFFFFEDB5),
    glassTint = Color.White.copy(alpha = 0.10f),
    glassRim = Color.White.copy(alpha = 0.35f),
    glassEdge = Color(0xFF7FA0AC),
    onGold = Color(0xFF2B2A2E),
    isDark = true,
)
