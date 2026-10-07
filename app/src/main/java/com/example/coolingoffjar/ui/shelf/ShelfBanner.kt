package com.example.coolingoffjar.ui.shelf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.coolingoffjar.domain.ShelfCatalog
import com.example.coolingoffjar.domain.ShelfGeometry
import com.example.coolingoffjar.domain.ShelfLayout
import kotlin.math.roundToInt

/** An item standing on a [ShelfBanner]: [x] is its centre as a fraction of the width. */
data class BannerItem(val artKey: String, val x: Float, val scale: Float = 1f)

/**
 * A single board of the shelf with a few things on it: the strip of shelf that pokes above the Shop and
 * sits behind the item in the item detail. It uses the same artwork as the main shelf.
 */
@Composable
fun ShelfBanner(items: List<BannerItem>, modifier: Modifier = Modifier) {
    val art = ImageBitmap.imageResource(ShelfArtwork)
    val tiers = ShelfGeometry.MIN_TIERS
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val unit = maxWidth / ShelfGeometry.ART_WIDTH.toFloat()
        val stand = ShelfGeometry.standLine(0, tiers) - SRC_TOP
        Box(Modifier.fillMaxWidth().height(unit * SRC_HEIGHT.toFloat())) {
            Canvas(Modifier.fillMaxSize()) {
                drawImage(
                    art,
                    srcOffset = IntOffset(0, SRC_TOP),
                    srcSize = IntSize(art.width, SRC_HEIGHT),
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.Medium,
                )
            }
            for (b in items) {
                val item = ShelfCatalog.find(b.artKey) ?: continue
                val h = unit * (ShelfLayout.heightFor(item, 1, tiers) * b.scale)
                val w = h * item.aspect
                Image(
                    painterResource(artRes(item.artKey)), null,
                    Modifier.offset(x = maxWidth * b.x - w / 2, y = unit * stand.toFloat() - h).size(w, h),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

// The slice of artwork used: from just under the top frame down to the first board's front edge.
private const val SRC_TOP = 58
private const val SRC_HEIGHT = 140
