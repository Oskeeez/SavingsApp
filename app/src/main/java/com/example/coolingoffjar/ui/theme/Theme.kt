package com.example.coolingoffjar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val LocalJarPalette = staticCompositionLocalOf { LightJarPalette }

/** Convenience accessor: `JarTheme.palette.gold`. */
object JarTheme {
    val palette: JarPalette
        @Composable @ReadOnlyComposable get() = LocalJarPalette.current
}

private fun JarPalette.toColorScheme() =
    if (isDark) {
        darkColorScheme(
            primary = sageDeep,
            onPrimary = onSage,
            tertiary = gold,
            secondary = textSecondary,
            background = background,
            onBackground = text,
            surface = surface,
            onSurface = text,
            onSurfaceVariant = textSecondary,
            surfaceContainerLow = surface,
            surfaceContainer = surface,
            surfaceContainerHigh = surface,
            outline = glassEdge,
            outlineVariant = glassEdge.copy(alpha = 0.4f),
        )
    } else {
        lightColorScheme(
            primary = sageDeep,
            onPrimary = onSage,
            tertiary = gold,
            secondary = textSecondary,
            background = background,
            onBackground = text,
            surface = surface,
            onSurface = text,
            onSurfaceVariant = textSecondary,
            surfaceContainerLow = background,
            surfaceContainer = background,
            surfaceContainerHigh = background,
            inverseSurface = text,
            inverseOnSurface = background,
            outline = glassEdge,
            outlineVariant = glassEdge.copy(alpha = 0.6f),
        )
    }

private val JarShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * Always the cream design-system palette, whatever the phone's dark-mode setting: the room is meant to look warm and
 * sunny. (A dark palette exists for a possible future Appearance option.) Dynamic colour is intentionally not used.
 */
@Composable
fun CoolingOffJarTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkJarPalette else LightJarPalette
    CompositionLocalProvider(LocalJarPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = JarTypography,
            shapes = JarShapes,
            content = content,
        )
    }
}
