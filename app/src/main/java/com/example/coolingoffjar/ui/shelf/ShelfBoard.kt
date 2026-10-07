package com.example.coolingoffjar.ui.shelf

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.Jar
import com.example.coolingoffjar.ui.util.formatDate

/**
 * The shelf: one piece of artwork, with real objects laid over it. Objects are not part of the
 * picture, so they can be added, removed and tapped independently. The clock and gacha machine are
 * ordinary objects with an action; there is no navigation chrome.
 */
@Composable
fun ShelfBoard(
    onOpenShop: () -> Unit,
    onOpenSettings: () -> Unit,
    memoryJars: List<Jar>,
    onMemoryJar: (Jar) -> Unit,
    modifier: Modifier = Modifier,
    skin: ShelfSkin = LightBrownShelf,
    objects: List<ShelfObject> = PreviewShelfObjects,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = maxWidth
        val unit = width / skin.widthPx.toFloat() // dp per artwork pixel
        Box(Modifier.fillMaxWidth().height(unit * skin.heightPx.toFloat())) {
            Image(
                painter = painterResource(skin.image),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            for (obj in objects) {
                val onClick = when (obj.action) {
                    ShelfAction.SHOP -> onOpenShop
                    ShelfAction.SETTINGS -> onOpenSettings
                    ShelfAction.NONE -> null
                }
                val label = when (obj.action) {
                    ShelfAction.SHOP -> stringResource(R.string.shelf_open_shop)
                    ShelfAction.SETTINGS -> stringResource(R.string.shelf_open_settings)
                    ShelfAction.NONE -> null
                }
                ShelfItem(
                    image = obj.image,
                    skin = skin,
                    unit = unit,
                    shelfWidth = width,
                    tier = obj.tier,
                    x = obj.x,
                    heightPx = obj.heightPx,
                    onClick = onClick,
                    label = label,
                )
            }
            memoryJars.take(MemoryJarSlots.size).forEachIndexed { i, jar ->
                MemoryJarItem(skin, unit, width, MemoryJarSlots[i], jar, onClick = { onMemoryJar(jar) })
            }
        }
    }
}

/** One object standing on a board: sized from the artwork's pixel units, feet on the board's stand line. */
@Composable
private fun ShelfItem(
    image: Int,
    skin: ShelfSkin,
    unit: Dp,
    shelfWidth: Dp,
    tier: Int,
    x: Float,
    heightPx: Float,
    onClick: (() -> Unit)?,
    label: String?,
) {
    val painter = painterResource(image)
    val ratio = painter.intrinsicSize.let { it.width / it.height }
    val h = unit * heightPx
    val w = h * ratio
    val feetY = unit * skin.standLines[tier].toFloat()

    Box(
        Modifier
            .offset(x = shelfWidth * x - w / 2, y = feetY - h)
            .size(w, h)
            .groundShadow()
            .then(if (onClick != null && label != null) Modifier.pressable(label, onClick) else Modifier),
    ) {
        Image(
            painter = painter,
            contentDescription = null, // the clickable wrapper carries the description for interactive objects
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

/** A completed jar: the empty jar with a pile of coins in it. Tap to see it (and spend its freebie). */
@Composable
private fun MemoryJarItem(skin: ShelfSkin, unit: Dp, shelfWidth: Dp, x: Float, jar: Jar, onClick: () -> Unit) {
    val jarPainter = painterResource(R.drawable.item_jar_empty)
    val pilePainter = painterResource(R.drawable.item_coin_pile)
    val jarRatio = jarPainter.intrinsicSize.let { it.width / it.height }
    val pileRatio = pilePainter.intrinsicSize.let { it.width / it.height }
    val h = unit * MemoryJarHeightPx
    val w = h * jarRatio
    val feetY = unit * skin.standLines[MemoryJarTier].toFloat()
    val description = stringResource(R.string.shelf_jar_description, jar.completedAt?.let { formatDate(it) }.orEmpty())
    val pileW = w * 0.74f

    Box(
        Modifier
            .offset(x = shelfWidth * x - w / 2, y = feetY - h)
            .size(w, h)
            .groundShadow()
            .pressable(description, onClick),
    ) {
        Image(painter = jarPainter, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Image(
            painter = pilePainter,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -(h * 0.07f))
                .size(pileW, pileW / pileRatio),
            contentScale = ContentScale.Fit,
        )
    }
}

/** Soft contact shadow so objects sit on the board instead of floating over it. */
private fun Modifier.groundShadow(): Modifier = drawBehind {
    val shadowW = size.width * 0.86f
    val shadowH = 7.dp.toPx()
    drawOval(
        color = Color(0x335A3A1E),
        topLeft = Offset((size.width - shadowW) / 2f, size.height - shadowH * 0.55f),
        size = Size(shadowW, shadowH),
    )
}

/** Tappable object: 48dp minimum target, a small press squash, no ripple, announced as a button. */
private fun Modifier.pressable(description: String, onClick: () -> Unit): Modifier = composed {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "press")
    this
        .minimumInteractiveComponentSize()
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, 1f)
        }
        .clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = description }
}
