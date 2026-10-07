package com.example.coolingoffjar.ui.shelf

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import com.example.coolingoffjar.domain.ShelfDrag
import com.example.coolingoffjar.domain.ShelfSurface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.coolingoffjar.domain.SlotRef
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
 * three always have the same place. Everything the user has bought stands in the slot it was given, and can be
 * picked up (press and hold) and dropped somewhere else. Notes hang on the wall above the boards.
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
    placement: PlacementMode? = null,
    showHeader: Boolean = true,
    onMoveItem: ((String, SlotRef) -> Unit)? = null,
) {
    // A drop that has been sent but not yet saved: shown straight away so the item does not flick back.
    var pendingMove by remember { mutableStateOf<Pair<String, SlotRef>?>(null) }
    LaunchedEffect(owned) { pendingMove = null }
    // Where everything really stands (old saved positions that clash with the jar are tidied up).
    val placed = remember(owned, pendingMove) {
        val resolved = ShelfLayout.resolve(owned)
        val pending = pendingMove
        if (pending == null) resolved
        else resolved.map { if (it.itemId == pending.first) it.copy(tier = pending.second.tier, slot = pending.second.slot) else it }
    }
    val placingEntry = placement?.let { ShelfCatalog.find(it.artKey) }
    // While choosing a spot for a shelf item, one extra empty row is offered at the bottom so there is always somewhere new.
    val extraRow = placement != null && placingEntry?.surface != ShelfSurface.WALL
    val tiers = remember(placed, extraRow) { ShelfLayout.tiersNeeded(placed) + if (extraRow) 1 else 0 }
    var jarCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (showHeader) ShelfHeader()
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = maxWidth
            val unit = width / ShelfGeometry.ART_WIDTH.toFloat() // dp per artwork pixel
            val unitPx = with(density) { unit.toPx() }
            val ctx = Placement(unit, width, tiers)
            val latestPlaced by rememberUpdatedState(placed)
            val latestTiers by rememberUpdatedState(tiers)
            val latestUnitPx by rememberUpdatedState(unitPx)
            val latestMove by rememberUpdatedState(onMoveItem)

            // Where the dragged thing would land if let go now (null = nowhere: it slides back).
            fun targetFor(id: String): SlotRef? {
                val entry = ShelfCatalog.find(id) ?: return null
                val me = latestPlaced.firstOrNull { it.itemId == id } ?: return null
                val (ax, ay) = ShelfDrag.anchor(entry, me.slotRef, latestTiers)
                return ShelfDrag.dropTarget(
                    latestPlaced, id, latestTiers,
                    ax + dragOffset.x / latestUnitPx, ay + dragOffset.y / latestUnitPx,
                )
            }

            Box(Modifier.fillMaxWidth().height(unit * ShelfGeometry.composedHeight(tiers).toFloat())) {
                ShelfBackdrop(tiers, Modifier.fillMaxSize())

                // The permanent objects.
                Standing(ctx, ShelfLayout.CLOCK.tier, ShelfLayout.slotXs(ShelfLayout.CLOCK.tier)[ShelfLayout.CLOCK.slot], ClockHeightPx, R.drawable.item_desk_clock,
                    label = stringResource(R.string.shelf_open_settings), onClick = onOpenSettings.takeIf { placement == null })
                Standing(ctx, ShelfLayout.GACHA.tier, ShelfLayout.slotXs(ShelfLayout.GACHA.tier)[ShelfLayout.GACHA.slot], GachaHeightPx, R.drawable.item_gacha_machine,
                    label = stringResource(R.string.shelf_open_shop), onClick = onOpenShop.takeIf { placement == null })

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
                    onClick = { onOpenJar(jarCoords?.takeIf { it.isAttached }?.boundsInRoot()) }.takeIf { placement == null },
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
                    Standing(ctx, slot.tier, ShelfLayout.slotXs(slot.tier)[slot.slot], MemoryJarHeightPx, aspect = JarAspect, label = description, onClick = { onMemoryJar(jar) }.takeIf { placement == null }) {
                        Image(painterResource(jarStateRes(JarArt.FULL)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                }

                // Where a dragged thing would land.
                val draggingId = dragId
                if (draggingId != null) {
                    val target = targetFor(draggingId)
                    val entry = ShelfCatalog.find(draggingId)
                    val mine = placed.firstOrNull { it.itemId == draggingId }
                    if (target != null && entry != null && target != mine?.slotRef) {
                        Standing(ctx, target.tier, ShelfLayout.slotXs(target.tier)[target.slot], 44f, aspect = 1f, wall = target.isWall) { SpotMarker() }
                    }
                }

                // Everything the user has bought.
                for (item in placed) {
                    val entry = ShelfCatalog.find(item.itemId) ?: continue
                    val x = ShelfLayout.slotXs(item.tier).getOrNull(item.slot) ?: continue
                    val dragging = dragId == item.itemId
                    val dragModifier =
                        if (onMoveItem == null || placement != null) Modifier
                        else Modifier
                            .zIndex(if (dragging) 10f else 0f)
                            .graphicsLayer {
                                if (dragging) {
                                    translationX = dragOffset.x
                                    translationY = dragOffset.y
                                    alpha = 0.92f
                                }
                            }
                            .pointerInput(item.itemId) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        dragOffset = Offset.Zero
                                        dragId = item.itemId
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount
                                    },
                                    onDragEnd = {
                                        val id = dragId
                                        val target = if (id != null) targetFor(id) else null
                                        val mine = latestPlaced.firstOrNull { it.itemId == id }
                                        if (id != null && target != null && target != mine?.slotRef) {
                                            pendingMove = id to target
                                            latestMove?.invoke(id, target)
                                        }
                                        dragId = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        dragId = null
                                        dragOffset = Offset.Zero
                                    },
                                )
                            }
                    Standing(
                        ctx, item.tier, x, ShelfLayout.heightFor(entry, item.tier, tiers), artRes(entry.artKey),
                        aspect = entry.aspect, wall = item.slotRef.isWall, extra = dragModifier,
                    )
                }

                // Choosing where a new thing goes: every empty spot is a tappable marker; the chosen one shows the item.
                if (placement != null && placingEntry != null) {
                    val entry = placingEntry
                    val taken = placed.map { it.slotRef }.toSet()
                    for (slot in ShelfLayout.freeSlots(taken, tiers, entry.surface)) {
                        val x = ShelfLayout.slotXs(slot.tier)[slot.slot]
                        val label =
                            if (slot.isWall) stringResource(R.string.place_wall_spot_description, ShelfLayout.wallRow(slot.tier) + 1, slot.slot + 1)
                            else stringResource(R.string.place_spot_description, slot.tier + 1, slot.slot + 1)
                        if (slot == placement.selected) {
                            Standing(
                                ctx, slot.tier, x, ShelfLayout.heightFor(entry, slot.tier, tiers), aspect = entry.aspect, wall = slot.isWall,
                                label = label, onClick = { placement.onSelect(slot) },
                                extra = Modifier.alpha(0.92f),
                            ) {
                                Image(painterResource(artRes(entry.artKey)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            }
                        } else {
                            Standing(ctx, slot.tier, x, 44f, aspect = 1f, wall = slot.isWall, label = label, onClick = { placement.onSelect(slot) }) {
                                SpotMarker()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "My shelf" and its one-line promise, centred above the bookcase. */
@Composable
private fun ShelfHeader() {
    val palette = JarTheme.palette
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(top = 8.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.shelf_header_title),
            style = MaterialTheme.typography.headlineMedium,
            color = palette.text,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.shelf_header_body),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private class Placement(val unit: Dp, val shelfWidth: Dp, val tiers: Int)

/** The bookcase picture, drawn in slices so extra tiers can be repeated with no seams. */
@Composable
private fun ShelfBackdrop(tiers: Int, modifier: Modifier) {
    val art = ImageBitmap.imageResource(R.drawable.shelf_sunlit)
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

/** An illustration standing on a board (or hanging on the wall). */
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
    wall: Boolean = false,
    extra: Modifier = Modifier,
) {
    val painter = painterResource(image)
    val ratio = aspect ?: painter.intrinsicSize.let { it.width / it.height }
    Standing(ctx, tier, x, heightPx, aspect = ratio, label = label, onClick = onClick, wall = wall, extra = extra) {
        Image(painter, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

/**
 * Anything standing on a board: sized in artwork pixels, feet on the tier's stand line, with a contact shadow.
 * A [wall] item is centred on its wall row instead and has no shadow.
 */
@Composable
private fun BoxScope.Standing(
    ctx: Placement,
    tier: Int,
    x: Float,
    heightPx: Float,
    aspect: Float,
    label: String? = null,
    onClick: (() -> Unit)? = null,
    wall: Boolean = false,
    extra: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val h = ctx.unit * heightPx
    val w = h * aspect
    val top =
        if (wall) ctx.unit * ShelfGeometry.wallCenterY(ShelfLayout.wallRow(tier)).toFloat() - h / 2
        else ctx.unit * ShelfGeometry.standLine(tier, ctx.tiers).toFloat() - h
    Box(
        Modifier
            .align(Alignment.TopStart)
            .offset(x = ctx.shelfWidth * x - w / 2, y = top)
            .size(w, h)
            .then(if (wall) Modifier else Modifier.groundShadow())
            .then(extra)
            .then(if (onClick != null && label != null) Modifier.pressable(label, onClick) else Modifier),
        content = content,
    )
}

/** Which spot the user is choosing for [artKey], and what happens when they tap one. */
class PlacementMode(val artKey: String, val selected: SlotRef?, val onSelect: (SlotRef) -> Unit)

/** A dashed ring with a plus: "you could put something here". */
@Composable
private fun SpotMarker() {
    val palette = JarTheme.palette
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = palette.sageDeep.copy(alpha = 0.85f),
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
            )
        }
        Text("+", style = MaterialTheme.typography.titleLarge, color = palette.sageDeep)
    }
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
