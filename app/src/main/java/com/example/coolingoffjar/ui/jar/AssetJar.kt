package com.example.coolingoffjar.ui.jar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.JarCoins
import com.example.coolingoffjar.domain.JarRules
import com.example.coolingoffjar.ui.theme.JarTheme
import kotlin.math.roundToInt

/**
 * The jar, from the supplied artwork: the empty jar picture with sprout coins piled inside it.
 * Visible coins = round(F * S) with F = min(1, k / N), so it is full exactly when k = N.
 * The jar picture is nearly opaque, so the coins are drawn in front of it, then a faint second pass of the
 * glass goes over them so they look like they are inside. [glow] (0..1) adds a soft golden halo behind it.
 * (Coin drop animation comes later; for now the pile simply reflects the count.)
 */
@Composable
fun AssetJar(
    filledCount: Int,
    notBuysPerJar: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    glow: Float = 0f,
) {
    val jar = ImageBitmap.imageResource(R.drawable.item_jar_empty)
    val coin = ImageBitmap.imageResource(R.drawable.item_coin)
    val palette = JarTheme.palette
    val visible = JarCoins.visibleCoinCount(JarRules.fillLevel(filledCount, notBuysPerJar))

    Canvas(
        modifier
            .aspectRatio(jar.width.toFloat() / jar.height)
            .semantics { this.contentDescription = contentDescription },
    ) {
        if (glow > 0.01f) {
            val r = size.width * 0.95f
            val c = Offset(size.width / 2f, size.height * 0.55f)
            drawCircle(
                Brush.radialGradient(
                    0f to palette.gold.copy(alpha = 0.55f * glow),
                    1f to palette.gold.copy(alpha = 0f),
                    center = c,
                    radius = r,
                ),
                radius = r,
                center = c,
            )
        }
        val full = IntSize(size.width.roundToInt(), size.height.roundToInt())
        drawImage(jar, dstSize = full, filterQuality = FilterQuality.Medium)
        drawCoins(coin, visible)
        drawImage(jar, dstSize = full, alpha = 0.22f, filterQuality = FilterQuality.Medium)
    }
}

private fun DrawScope.drawCoins(coin: ImageBitmap, count: Int) {
    val slots = JarCoins.slots
    val ratio = coin.height.toFloat() / coin.width
    for (i in 0 until count.coerceIn(0, slots.size)) {
        val slot = slots[i]
        val w = size.width * JarCoins.COIN_DIAMETER * slot.scale
        val h = w * ratio
        val cx = size.width * slot.x
        val cy = size.height * slot.y
        withTransform({ rotate(slot.rotationDeg, Offset(cx, cy)) }) {
            drawImage(
                coin,
                dstOffset = IntOffset((cx - w / 2f).roundToInt(), (cy - h / 2f).roundToInt()),
                dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}
