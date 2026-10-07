package com.example.coolingoffjar.ui.shelf

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import com.example.coolingoffjar.domain.CatalogItem
import com.example.coolingoffjar.domain.ObjectLighting
import com.example.coolingoffjar.domain.ShelfGeometry
import com.example.coolingoffjar.domain.ShelfLighting

/** Warm shadow colour: slightly amber rather than grey, to match the hand-painted look. */
private val ShadowColor = Color(0xFF3A2010)

/**
 * The colour treatment for a level's light: brightness and a gentle warm tint. Original artwork is never altered; this
 * is applied while drawing.
 */
fun lightingFilter(l: ObjectLighting): ColorFilter {
    val r = l.brightness * (1f + l.warmth * 0.7f)
    val g = l.brightness * (1f + l.warmth * 0.1f)
    val b = l.brightness * (1f - l.warmth * 1.3f)
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                r, 0f, 0f, 0f, 0f,
                0f, g, 0f, 0f, 0f,
                0f, 0f, b, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}

/** Draws this content through [filter] (a colour treatment), leaving room around it so glows are not clipped. */
fun Modifier.lit(filter: ColorFilter): Modifier = drawWithContent {
    val paint = Paint().apply { colorFilter = filter }
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(-size.width * 0.5f, -size.height * 0.5f, size.width * 1.5f, size.height * 1.5f), paint)
        drawContent()
        canvas.restore()
    }
}

/**
 * A soft shadow that starts at the contact point and falls down and to the right, away from the window light. Drawn
 * in a few soft layers (no blur, so it works on every Android version) and generated from the object's own size.
 */
fun Modifier.contactShadow(l: ObjectLighting, unit: Dp): Modifier = drawBehind {
    val u = unit.toPx()
    fun ellipse(cx: Float, cy: Float, width: Float, thickness: Float, alpha: Float) {
        val radius = width / 2f
        val inner = 0.12f + 0.5f * (1f - l.shadowBlur)
        val brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to ShadowColor.copy(alpha = alpha),
                inner to ShadowColor.copy(alpha = alpha * 0.7f),
                1f to Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = radius,
        )
        withTransform({ scale(1f, thickness / width, pivot = Offset(cx, cy)) }) {
            drawCircle(brush, radius = radius, center = Offset(cx, cy))
        }
    }
    val width = size.width * l.shadowWidthScale
    val thickness = l.shadowThickness * u
    val baseY = size.height
    // The wide, soft part, thrown toward the lower right.
    ellipse(size.width / 2f + l.shadowOffsetX * u, baseY + l.shadowOffsetY * u - thickness * 0.2f, width * 1.05f, thickness, l.shadowOpacity)
    // A tight, darker core right at the contact point so the object clearly sits on the board.
    ellipse(size.width / 2f + l.shadowOffsetX * u * 0.35f, baseY + l.shadowOffsetY * u * 0.3f - thickness * 0.1f, width * 0.78f, thickness * 0.55f, (l.shadowOpacity * 1.25f).coerceAtMost(0.5f))
}

/**
 * Anything standing on the bookcase. It is positioned against its level's baseline, takes that level's lighting and
 * gets its own contact shadow, so moving it to another level changes how it looks with no other code. [content] draws
 * the plain, untouched artwork; [modifier] carries interaction (clicks, dragging).
 */
@Composable
fun BoxScope.ShelfObject(
    item: CatalogItem,
    tier: Int,
    centerX: Float,
    heightPx: Float,
    unit: Dp,
    modifier: Modifier = Modifier,
    aspect: Float = item.aspect,
    content: @Composable BoxScope.() -> Unit,
) {
    val lighting = remember(item.id, tier, centerX) { ShelfLighting.forObject(item, tier, centerX / ShelfGeometry.SCENE_WIDTH) }
    val filter = remember(lighting) { lightingFilter(lighting) }
    val h = unit * heightPx
    val w = h * aspect
    val feetY = unit * ShelfGeometry.standLine(tier)
    Box(
        Modifier
            .align(Alignment.TopStart)
            .offset(x = unit * centerX - w / 2, y = feetY - h)
            .size(w, h)
            .then(modifier)
            .contactShadow(lighting, unit),
    ) {
        Box(Modifier.fillMaxSize().lit(filter), content = content)
    }
}
