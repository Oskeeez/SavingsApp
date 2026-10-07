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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.coolingoffjar.domain.RoomLook
import com.example.coolingoffjar.domain.ShelfCatalog
import com.example.coolingoffjar.domain.ShelfGeometry
import com.example.coolingoffjar.domain.ShelfLayout
import com.example.coolingoffjar.domain.ShelfLighting
import com.example.coolingoffjar.domain.ShelfSurface
import kotlin.math.roundToInt

/** An item standing on a [ShelfBanner]: [x] is its centre as a fraction of the width. */
data class BannerItem(val artKey: String, val x: Float, val scale: Float = 1f)

/**
 * A single board of the bookcase with a few things on it, in front of the user's own wall: the strip of shelf that
 * pokes above the Shop and sits behind the item in the item detail. Uses the same artwork and lighting as the room.
 */
@Composable
fun ShelfBanner(items: List<BannerItem>, look: RoomLook = RoomLook.DEFAULT, modifier: Modifier = Modifier) {
    val wall = ImageBitmap.imageResource(artRes(look.wall))
    val shelf = ImageBitmap.imageResource(artRes(look.shelf))
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = maxWidth
        val unit = width / ShelfGeometry.SHELF_ART_WIDTH.toFloat() // dp per native shelf pixel
        val stand = STAND - SRC_TOP
        Box(Modifier.fillMaxWidth().height(unit * SRC_HEIGHT.toFloat())) {
            Canvas(Modifier.fillMaxSize()) {
                val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())
                // The wall behind: same proportions as the strip of shelf.
                val wallRows = (wall.width * SRC_HEIGHT / ShelfGeometry.SHELF_ART_WIDTH.toFloat()).roundToInt()
                drawImage(wall, srcOffset = IntOffset(0, 80), srcSize = IntSize(wall.width, wallRows), dstSize = dst, filterQuality = FilterQuality.Medium)
                drawImage(shelf, srcOffset = IntOffset(0, SRC_TOP), srcSize = IntSize(shelf.width, SRC_HEIGHT), dstSize = dst, filterQuality = FilterQuality.Medium)
            }
            for (b in items) {
                val item = ShelfCatalog.find(b.artKey) ?: continue
                val wallItem = item.surface == ShelfSurface.WALL
                // Banner pixels are native shelf pixels; items are sized in scene pixels.
                val sceneH = ShelfLayout.heightFor(item, 1) * b.scale * if (wallItem) 0.8f else 1f
                val h = unit * (sceneH / ShelfGeometry.SHELF_SCALE)
                val w = h * item.aspect
                val y = if (wallItem) unit * (stand / 2f) - h / 2 else unit * stand.toFloat() - h
                val lighting = remember(item.id, b.x) { ShelfLighting.forObject(item, 1, 0.3f + 0.4f * b.x) }
                val filter = remember(lighting) { lightingFilter(lighting) }
                Box(
                    Modifier
                        .offset(x = width * b.x - w / 2, y = y)
                        .size(w, h)
                        .then(if (wallItem) Modifier else Modifier.contactShadow(lighting, unit / ShelfGeometry.SHELF_SCALE)),
                ) {
                    Image(
                        painterResource(artRes(item.artKey)), null,
                        Modifier.fillMaxSize().lit(filter),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }
}

// The slice of the shelf artwork used (native px): the open space above the second board, and the board itself.
private const val SRC_TOP = 96
private const val SRC_HEIGHT = 112
private const val STAND = 178
