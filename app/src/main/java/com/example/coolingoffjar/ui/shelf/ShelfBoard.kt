package com.example.coolingoffjar.ui.shelf

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.Jar
import com.example.coolingoffjar.domain.OwnedItem
import com.example.coolingoffjar.domain.ShelfCatalog
import com.example.coolingoffjar.domain.ShelfGeometry
import com.example.coolingoffjar.domain.ShelfLayout
import com.example.coolingoffjar.domain.JarArt
import com.example.coolingoffjar.ui.jar.AssetJar
import com.example.coolingoffjar.ui.jar.JarAspect
import com.example.coolingoffjar.ui.jar.jarStateRes
import com.example.coolingoffjar.ui.theme.JarTheme
import com.example.coolingoffjar.ui.util.formatDate
import kotlin.math.roundToInt

/**
 * The shelf. One piece of artwork (repeated tier by tier as it fills up) with real objects standing on it.
 * The jar is the biggest thing on it; the gacha machine opens the Shop and the clock opens Settings. Those
 * three always have the same place. Everything the user has bought stands in the slot it was given.
 */
@Composable
fun ShelfBoard(
    owned: List<OwnedItem>,
    jarFilled: Int,
    notBuysPerJar: Int,
    readyCount: Int,
    jarGlow: Float,
    memoryJars: List<Jar>,
    onOpenJar: (Rect?) -> Unit,
    onOpenShop: () -> Unit,
    onOpenSettings: () -> Unit,
    onMemoryJar: (Jar) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiers = remember(owned) { ShelfLayout.tiersNeeded(owned) }
    var jarCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = maxWidth
        val unit = width / ShelfGeometry.ART_WIDTH.toFloat() // dp per artwork pixel
        val ctx = Placement(unit, width, tiers)

        Box(Modifier.fillMaxWidth().height(unit * ShelfGeometry.composedHeight(tiers).toFloat())) {
            ShelfBackdrop(tiers, Modifier.fillMaxSize())

            // The permanent objects.
            Standing(ctx, ShelfLayout.CLOCK.tier, ShelfLayout.slotXs(ShelfLayout.CLOCK.tier)[ShelfLayout.CLOCK.slot], ClockHeightPx, R.drawable.item_desk_clock,
                label = stringResource(R.string.shelf_open_settings), onClick = onOpenSettings)
            Standing(ctx, ShelfLayout.GACHA.tier, ShelfLayout.slotXs(ShelfLayout.GACHA.tier)[ShelfLayout.GACHA.slot], GachaHeightPx, R.drawable.item_gacha_machine,
                label = stringResource(R.string.shelf_open_shop), onClick = onOpenShop)

            // The jar: biggest thing here, and the way into the jar scene.
            val jarDescription =
                if (readyCount > 0) {
                    pluralStringResource(R.plurals.jar_open_description_ready, readyCount, jarFilled, notBuysPerJar, readyCount)
                } else {
                    stringResource(R.string.jar_open_description, jarFilled, notBuysPerJar)
                }
            Standing(
                ctx, ShelfLayout.JAR.tier, ShelfLayout.slotXs(ShelfLayout.JAR.tier)[ShelfLayout.JAR.slot], JarHeightPx,
                aspect = JarAspect,
                label = jarDescription,
                onClick = { onOpenJar(jarCoords?.takeIf { it.isAttached }?.boundsInRoot()) },
                extra = Modifier.onGloballyPositioned { jarCoords = it },
            ) {
                AssetJar(
                    filledCount = jarFilled,
                    notBuysPerJar = notBuysPerJar,
                    contentDescription = "",
                    modifier = Modifier.fillMaxSize(),
                    glow = jarGlow,
                )
                if (readyCount > 0) ReadyBadge(readyCount, Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp))
            }

            // Memories: completed jars.
            memoryJars.take(ShelfLayout.MEMORY_JARS.size).forEachIndexed { i, jar ->
                val slot = ShelfLayout.MEMORY_JARS[i]
                val description = stringResource(R.string.shelf_jar_description, jar.completedAt?.let { formatDate(it) }.orEmpty())
                Standing(ctx, slot.tier, ShelfLayout.slotXs(slot.tier)[slot.slot], MemoryJarHeightPx, aspect = JarAspect, label = description, onClick = { onMemoryJar(jar) }) {
                    Image(painterResource(jarStateRes(JarArt.FULL)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            }

            // Everything the user has bought.
            for (item in owned) {
                val entry = ShelfCatalog.find(item.itemId) ?: continue
                val xs = ShelfLayout.slotXs(item.tier)
                val x = xs.getOrNull(item.slot) ?: continue
                Standing(ctx, item.tier, x, ShelfLayout.heightFor(entry, item.tier, tiers), artRes(entry.artKey), aspect = entry.aspect)
            }
        }
    }
}

private class Placement(val unit: Dp, val shelfWidth: Dp, val tiers: Int)

/** The bookcase picture, drawn in slices so extra tiers can be repeated with no seams. */
@Composable
private fun ShelfBackdrop(tiers: Int, modifier: Modifier) {
    val art = ImageBitmap.imageResource(R.drawable.shelf_light_brown)
    Canvas(modifier) {
        val unitPx = size.width / ShelfGeometry.ART_WIDTH
        val dstWidth = size.width.roundToInt()
        for (s in ShelfGeometry.slices(tiers)) {
            val top = (s.dstTop * unitPx).roundToInt()
            val bottom = ((s.dstTop + s.srcHeight) * unitPx).roundToInt()
            drawImage(
                art,
                srcOffset = IntOffset(0, s.srcTop),
                srcSize = IntSize(art.width, s.srcHeight),
                dstOffset = IntOffset(0, top),
                dstSize = IntSize(dstWidth, bottom - top),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}

/** An illustration standing on a board. */
@Composable
private fun BoxScope.Standing(
    ctx: Placement,
    tier: Int,
    x: Float,
    heightPx: Float,
    @androidx.annotation.DrawableRes image: Int,
    aspect: Float? = null,
    label: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val painter = painterResource(image)
    val ratio = aspect ?: painter.intrinsicSize.let { it.width / it.height }
    Standing(ctx, tier, x, heightPx, aspect = ratio, label = label, onClick = onClick) {
        Image(painter, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

/** Anything standing on a board: sized in artwork pixels, feet on the tier's stand line, with a contact shadow. */
@Composable
private fun BoxScope.Standing(
    ctx: Placement,
    tier: Int,
    x: Float,
    heightPx: Float,
    aspect: Float,
    label: String? = null,
    onClick: (() -> Unit)? = null,
    extra: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val h = ctx.unit * heightPx
    val w = h * aspect
    val feetY = ctx.unit * ShelfGeometry.standLine(tier, ctx.tiers).toFloat()
    Box(
        Modifier
            .align(Alignment.TopStart)
            .offset(x = ctx.shelfWidth * x - w / 2, y = feetY - h)
            .size(w, h)
            .groundShadow()
            .then(extra)
            .then(if (onClick != null && label != null) Modifier.pressable(label, onClick) else Modifier),
        content = content,
    )
}

/** Gold count badge: "something is ready for your decision". Sits on the jar. */
@Composable
private fun ReadyBadge(count: Int, modifier: Modifier = Modifier) {
    val palette = JarTheme.palette
    Box(
        modifier
            .size(28.dp)
            .background(palette.gold, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = palette.onGold)
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
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "press")
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
