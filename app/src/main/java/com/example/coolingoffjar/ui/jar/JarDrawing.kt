package com.example.coolingoffjar.ui.jar

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.example.coolingoffjar.domain.CoinLayout
import com.example.coolingoffjar.domain.CoinSlot
import com.example.coolingoffjar.domain.JarGeometry
import com.example.coolingoffjar.ui.theme.JarPalette

/*
 * All jar drawing happens in "width units" (see JarGeometry): callers wrap these functions in
 * `withTransform { scale(size.width, size.width, Offset.Zero) }`, so every number here is a fraction of
 * the jar width and the same code serves the big Home jar and the mini jars on the Shelf.
 *
 * Draw order (back to front): contact shadow, glass back, coins, glass front, lid.
 */

/** The glass silhouette: wide body, softly rounded shoulders, short neck. */
fun jarBodyPath(): Path = Path().apply {
    with(JarGeometry) {
        moveTo(NECK_LEFT, LID_BOTTOM)
        lineTo(NECK_LEFT, NECK_BOTTOM)
        cubicTo(NECK_LEFT, 0.31f, BODY_LEFT, 0.30f, BODY_LEFT, SHOULDER_BOTTOM)
        lineTo(BODY_LEFT, BODY_STRAIGHT_BOTTOM)
        quadraticTo(BODY_LEFT, BODY_BOTTOM, BODY_LEFT + BODY_CORNER, BODY_BOTTOM)
        lineTo(BODY_RIGHT - BODY_CORNER, BODY_BOTTOM)
        quadraticTo(BODY_RIGHT, BODY_BOTTOM, BODY_RIGHT, BODY_STRAIGHT_BOTTOM)
        lineTo(BODY_RIGHT, SHOULDER_BOTTOM)
        cubicTo(BODY_RIGHT, 0.30f, NECK_RIGHT, 0.31f, NECK_RIGHT, NECK_BOTTOM)
        lineTo(NECK_RIGHT, LID_BOTTOM)
        close()
    }
}

/** Brushes and colours built once per theme so per-frame drawing does not allocate shaders. */
class JarPaints(val palette: JarPalette) {
    // Coin gradient is defined for a unit circle (centre 0,0 radius 1) and reused for every coin.
    val coinFace = Brush.radialGradient(
        0.00f to palette.goldHighlight,
        0.55f to palette.gold,
        1.00f to palette.goldShadow,
        center = Offset(-0.30f, -0.30f),
        radius = 1.45f,
    )
    val coinRim = lerp(palette.goldShadow, Color.Black, 0.30f)
    val coinInnerRing = palette.goldShadow.copy(alpha = 0.60f)
    val coinGlint = palette.goldHighlight.copy(alpha = 0.90f)

    private val lidTop = if (palette.isDark) Color(0xFFB9B3A8) else Color(0xFFE4DFD6)
    private val lidMid = if (palette.isDark) Color(0xFF8C867B) else Color(0xFFBDB6AA)
    private val lidLow = if (palette.isDark) Color(0xFF5F5A52) else Color(0xFF8E877B)
    val lidBrush = Brush.verticalGradient(
        0.0f to lidTop, 0.45f to lidMid, 1.0f to lidLow,
        startY = 0f, endY = JarGeometry.LID_BOTTOM,
    )
    val lidDark = lidLow.copy(alpha = 0.75f)
    val lidLight = Color.White.copy(alpha = if (palette.isDark) 0.35f else 0.65f)

    val glassBack = Brush.horizontalGradient(
        0.00f to palette.glassEdge.copy(alpha = palette.glassEdge.alpha * 0.40f),
        0.14f to palette.glassEdge.copy(alpha = 0f),
        0.86f to palette.glassEdge.copy(alpha = 0f),
        1.00f to palette.glassEdge.copy(alpha = palette.glassEdge.alpha * 0.40f),
        startX = JarGeometry.BODY_LEFT, endX = JarGeometry.BODY_RIGHT,
    )
    val bottomShadow = Brush.verticalGradient(
        0f to palette.text.copy(alpha = 0f),
        1f to palette.text.copy(alpha = if (palette.isDark) 0.35f else 0.14f),
        startY = 0.93f, endY = JarGeometry.BODY_BOTTOM,
    )
    val stripeLeft = Brush.verticalGradient(
        0.00f to palette.glassRim.copy(alpha = 0f),
        0.18f to palette.glassRim,
        0.80f to palette.glassRim.copy(alpha = palette.glassRim.alpha * 0.75f),
        1.00f to palette.glassRim.copy(alpha = 0f),
        startY = 0.46f, endY = 1.02f,
    )
    val stripeRight = Brush.verticalGradient(
        0.00f to palette.glassRim.copy(alpha = 0f),
        0.25f to palette.glassRim.copy(alpha = palette.glassRim.alpha * 0.70f),
        0.80f to palette.glassRim.copy(alpha = palette.glassRim.alpha * 0.55f),
        1.00f to palette.glassRim.copy(alpha = 0f),
        startY = 0.50f, endY = 0.98f,
    )
    val baseReflection = Brush.horizontalGradient(
        0.0f to palette.glassRim.copy(alpha = 0f),
        0.5f to palette.glassRim.copy(alpha = palette.glassRim.alpha * 0.55f),
        1.0f to palette.glassRim.copy(alpha = 0f),
        startX = 0.22f, endX = 0.78f,
    )
}

fun DrawScope.drawContactShadow(paints: JarPaints) {
    drawOval(
        color = paints.palette.text.copy(alpha = if (paints.palette.isDark) 0.35f else 0.10f),
        topLeft = Offset(0.12f, 1.205f),
        size = Size(0.76f, 0.040f),
    )
}

fun DrawScope.drawGlassBack(paints: JarPaints, body: Path) {
    drawPath(body, paints.palette.glassTint)
    drawPath(body, paints.glassBack)
}

/** One coin at [center] with radius [radius] (width units), tilted into an ellipse when [squashY] < 1. */
fun DrawScope.drawCoin(paints: JarPaints, center: Offset, radius: Float, rotationDeg: Float = 0f, squashY: Float = 1f) {
    withTransform({
        translate(center.x, center.y)
        rotate(rotationDeg, Offset.Zero)
        scale(radius, radius * squashY, Offset.Zero)
    }) {
        // Everything below is in unit-circle coordinates, so widths are fractions of the radius.
        drawCircle(paints.coinFace, radius = 1f, center = Offset.Zero)
        drawCircle(paints.coinRim, radius = 0.95f, center = Offset.Zero, style = Stroke(width = 0.10f))
        drawCircle(paints.coinInnerRing, radius = 0.64f, center = Offset.Zero, style = Stroke(width = 0.07f))
        drawCircle(paints.coinGlint, radius = 0.07f, center = Offset(-0.38f, -0.40f))
    }
}

fun DrawScope.drawRestingCoins(paints: JarPaints, count: Int) {
    val slots = CoinLayout.slots
    val radius = CoinLayout.COIN_DIAMETER / 2
    for (i in 0 until count.coerceIn(0, slots.size)) {
        drawCoinSlot(paints, slots[i], radius)
    }
}

fun DrawScope.drawCoinSlot(paints: JarPaints, slot: CoinSlot, radius: Float = CoinLayout.COIN_DIAMETER / 2) {
    drawCoin(paints, Offset(slot.x, slot.y), radius, slot.rotationDeg, slot.squashY)
}

/** Glass in front of the coins: thickness shading, inner shadow, base reflection, highlight stripes, rim. */
fun DrawScope.drawGlassFront(paints: JarPaints, body: Path) {
    val palette = paints.palette
    clipPath(body) {
        drawPath(body, paints.glassBack) // edge shading again, now over the coins
        drawRect(paints.bottomShadow, Offset(0f, 0.93f), Size(1f, 0.30f))
        drawOval(paints.baseReflection, Offset(0.22f, 1.115f), Size(0.56f, 0.050f))

        // Bright stripe on the upper left of the body, thinner one on the right edge.
        drawRoundRect(paints.stripeLeft, Offset(0.105f, 0.46f), Size(0.050f, 0.56f), CornerRadius(0.025f))
        drawRoundRect(paints.stripeRight, Offset(0.895f, 0.50f), Size(0.022f, 0.48f), CornerRadius(0.011f))
        // Small glint on the shoulder.
        drawLine(
            palette.glassRim, Offset(0.17f, 0.335f), Offset(0.215f, 0.285f),
            strokeWidth = 0.020f, cap = StrokeCap.Round,
        )
    }
    drawPath(body, palette.glassEdge, style = Stroke(width = 0.012f))
    drawPath(body, palette.glassRim.copy(alpha = palette.glassRim.alpha * 0.8f), style = Stroke(width = 0.004f))
}

/** Metal-style lid band with three subtle ridges. */
fun DrawScope.drawLid(paints: JarPaints) {
    with(JarGeometry) {
        val width = LID_RIGHT - LID_LEFT
        drawRoundRect(paints.lidBrush, Offset(LID_LEFT, 0f), Size(width, LID_BOTTOM), CornerRadius(0.022f))
        for (y in floatArrayOf(0.040f, 0.068f, 0.096f)) {
            drawLine(paints.lidDark, Offset(LID_LEFT + 0.012f, y), Offset(LID_RIGHT - 0.012f, y), strokeWidth = 0.007f)
            drawLine(paints.lidLight, Offset(LID_LEFT + 0.012f, y + 0.007f), Offset(LID_RIGHT - 0.012f, y + 0.007f), strokeWidth = 0.004f)
        }
        // Top specular edge.
        drawLine(
            paints.lidLight, Offset(LID_LEFT + 0.02f, 0.012f), Offset(LID_RIGHT - 0.02f, 0.012f),
            strokeWidth = 0.007f, cap = StrokeCap.Round,
        )
    }
}

/** Convenience: draw the whole jar at [restingCoins] coins. Callers apply the width-unit scale first. */
fun DrawScope.drawJar(paints: JarPaints, body: Path, restingCoins: Int) {
    drawContactShadow(paints)
    drawGlassBack(paints, body)
    clipPath(body) { drawRestingCoins(paints, restingCoins) }
    drawGlassFront(paints, body)
    drawLid(paints)
}
