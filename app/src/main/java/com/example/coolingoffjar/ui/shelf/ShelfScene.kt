package com.example.coolingoffjar.ui.shelf

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.Jar
import com.example.coolingoffjar.domain.JarArt
import com.example.coolingoffjar.domain.OwnedItem
import com.example.coolingoffjar.domain.RoomLook
import com.example.coolingoffjar.domain.SceneCamera
import com.example.coolingoffjar.domain.ScenePoint
import com.example.coolingoffjar.domain.ShelfCatalog
import com.example.coolingoffjar.domain.ShelfDrag
import com.example.coolingoffjar.domain.ShelfGeometry
import com.example.coolingoffjar.domain.ShelfLayout
import com.example.coolingoffjar.domain.ShelfSurface
import com.example.coolingoffjar.domain.SlotRef
import com.example.coolingoffjar.domain.ZoomTarget
import com.example.coolingoffjar.ui.jar.AssetJar
import com.example.coolingoffjar.ui.jar.JarAspect
import com.example.coolingoffjar.ui.jar.jarStateRes
import com.example.coolingoffjar.ui.theme.JarTheme
import com.example.coolingoffjar.ui.util.formatDate

/**
 * The room: a wall, a floor and the bookcase, drawn as one fixed picture that is scaled to cover the screen (the edge
 * of the picture is never shown). The jar, the gacha machine (Shop), the clock (Settings), finished jars and
 * everything the user has bought stand on the bookcase; notes hang anywhere on the wall. Press and hold anything to
 * pick it up and drop it somewhere else. A camera zooms smoothly in on one object ([zoom] 0..1 toward [zoomTarget]).
 */
@Composable
fun ShelfScene(
    owned: List<OwnedItem>,
    look: RoomLook,
    jarFilled: Int,
    notBuysPerJar: Int,
    readyCount: Int,
    jarGlow: Float,
    memoryJars: List<Jar>,
    onObjectTap: (String) -> Unit,
    onOpenShop: () -> Unit,
    onOpenSettings: () -> Unit,
    onMemoryJar: (Jar) -> Unit,
    modifier: Modifier = Modifier,
    onMoveItem: ((String, SlotRef) -> Unit)? = null,
    onMoveNote: ((String, ScenePoint) -> Unit)? = null,
    placement: PlacementMode? = null,
    showHeader: Boolean = true,
    zoom: () -> Float = { 0f },
    zoomTarget: ZoomTarget? = null,
) {
    // Drops that have been sent but not yet saved are shown straight away so nothing flicks back.
    var pendingSlot by remember { mutableStateOf<Pair<String, SlotRef>?>(null) }
    var pendingNote by remember { mutableStateOf<Pair<String, ScenePoint>?>(null) }
    LaunchedEffect(owned) { pendingSlot = null; pendingNote = null }
    val placed = remember(owned, pendingSlot, pendingNote) {
        ShelfLayout.resolve(owned).map { item ->
            val s = pendingSlot
            val n = pendingNote
            when {
                s != null && s.first == item.itemId -> item.copy(tier = s.second.tier, slot = s.second.slot)
                n != null && n.first == item.itemId -> item.copy(x = n.second.x, y = n.second.y)
                else -> item
            }
        }
    }
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val palette = JarTheme.palette
    val placingEntry = placement?.let { ShelfCatalog.find(it.artKey) }

    BoxWithConstraints(modifier.clipToBounds().background(palette.background)) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val k0 = SceneCamera.coverScale(screenW, screenH) // screen px per scene px
        val unit = with(density) { k0.toDp() } // dp per scene px
        val sceneW = unit * ShelfGeometry.SCENE_WIDTH
        val sceneH = unit * ShelfGeometry.SCENE_HEIGHT
        val latestPlaced by rememberUpdatedState(placed)
        val latestMoveItem by rememberUpdatedState(onMoveItem)
        val latestMoveNote by rememberUpdatedState(onMoveNote)
        val latestK0 by rememberUpdatedState(k0)

        // Where a dragged note is let go (kept on the wall), and where a dragged bookcase item would settle.
        fun noteDropPoint(item: OwnedItem, entry: com.example.coolingoffjar.domain.CatalogItem): ScenePoint = ShelfLayout.clampWall(
            entry,
            ScenePoint(
                ((item.x ?: 0.5f) * ShelfGeometry.SCENE_WIDTH + dragOffset.x / latestK0) / ShelfGeometry.SCENE_WIDTH,
                ((item.y ?: 0.2f) * ShelfGeometry.SCENE_HEIGHT + dragOffset.y / latestK0) / ShelfGeometry.SCENE_HEIGHT,
            ),
        )
        fun slotDropTarget(item: OwnedItem, entry: com.example.coolingoffjar.domain.CatalogItem): SlotRef? {
            val (ax, ay) = ShelfDrag.anchor(entry, item.slotRef)
            return ShelfDrag.dropTarget(latestPlaced, item.itemId, ax + dragOffset.x / latestK0, ay + dragOffset.y / latestK0)
        }

        Box(
            Modifier
                .align(Alignment.Center)
                .requiredSize(sceneW, sceneH)
                .graphicsLayer {
                    val f = SceneCamera.frame(zoom(), zoomTarget, screenW, screenH)
                    val s = f.scale / k0
                    scaleX = s
                    scaleY = s
                    translationX = (ShelfGeometry.SCENE_WIDTH / 2f - f.centerX) * f.scale
                    translationY = (ShelfGeometry.SCENE_HEIGHT / 2f - f.centerY) * f.scale
                },
        ) {
            // ---- The room: wall, floor, and the bookcase with its soft shadow.
            SceneImage(artRes(look.wall), unit, 0f, 0f, ShelfGeometry.SCENE_WIDTH.toFloat(), ShelfGeometry.WALL_HEIGHT.toFloat())
            SceneImage(
                artRes(look.floor), unit, 0f, ShelfGeometry.WALL_HEIGHT.toFloat(),
                ShelfGeometry.SCENE_WIDTH.toFloat(), (ShelfGeometry.SCENE_HEIGHT - ShelfGeometry.WALL_HEIGHT).toFloat(),
            )
            val s = ShelfGeometry.SHELF_SCALE
            SceneImage(
                R.drawable.shelf_shadow, unit,
                ShelfGeometry.SHELF_LEFT - 40f * s + 10f, ShelfGeometry.SHELF_TOP - 40f * s + 8f, 571f * s, 739f * s,
            )
            SceneImage(
                artRes(look.shelf), unit, ShelfGeometry.SHELF_LEFT, ShelfGeometry.SHELF_TOP,
                ShelfGeometry.SHELF_ART_WIDTH * s, ShelfGeometry.SHELF_ART_HEIGHT * s,
            )

            // ---- Things standing on the bookcase (the jar, clock and gacha machine among them).
            for (item in placed) {
                val entry = ShelfCatalog.find(item.itemId) ?: continue
                if (entry.surface != ShelfSurface.SHELF) continue
                val dragging = dragId == item.itemId
                val target = if (dragging) slotDropTarget(item, entry) else null
                val interaction = if (onMoveItem == null || placement != null) Modifier else dragModifier(
                    key = item.itemId,
                    dragging = dragging,
                    translation = if (dragging) dragOffset else Offset.Zero,
                    onStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dragOffset = Offset.Zero
                        dragId = item.itemId
                    },
                    onDelta = { dragOffset += it },
                    onEnd = {
                        val current = latestPlaced.firstOrNull { it.itemId == item.itemId }
                        val drop = if (current != null) slotDropTarget(current, entry) else null
                        if (current != null && drop != null && drop != current.slotRef) {
                            pendingSlot = item.itemId to drop
                            latestMoveItem?.invoke(item.itemId, drop)
                        }
                        dragId = null
                        dragOffset = Offset.Zero
                    },
                )
                val tap = placement == null
                val h = ShelfLayout.heightFor(entry, item.tier)
                val x = ShelfGeometry.slotCenterX(item.tier, item.slot)
                when (item.itemId) {
                    "clock" -> ShelfObject(entry, item.tier, x, h, unit, interaction.pressableIf(tap, stringResource(R.string.shelf_open_settings), onOpenSettings)) {
                        Image(painterResource(R.drawable.item_desk_clock), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                    "gacha" -> ShelfObject(entry, item.tier, x, h, unit, interaction.pressableIf(tap, stringResource(R.string.shelf_open_shop), onOpenShop)) {
                        Image(painterResource(R.drawable.item_gacha_machine), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                    "jar" -> {
                        val description =
                            if (readyCount > 0) pluralStringResource(R.plurals.jar_open_description_ready, readyCount, jarFilled, notBuysPerJar, readyCount)
                            else stringResource(R.string.jar_open_description, jarFilled, notBuysPerJar)
                        ShelfObject(entry, item.tier, x, h, unit, interaction.pressableIf(tap, description) { onObjectTap("jar") }, aspect = JarAspect) {
                            AssetJar(
                                filledCount = jarFilled,
                                notBuysPerJar = notBuysPerJar,
                                contentDescription = "",
                                modifier = Modifier.fillMaxSize(),
                                glow = jarGlow,
                            )
                            if (readyCount > 0) ReadyBadge(readyCount, Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp))
                        }
                    }
                    "memory_1", "memory_2" -> {
                        val jar = memoryJars.getOrNull(item.itemId.removePrefix("memory_").toInt() - 1)
                        if (jar != null) {
                            val description = stringResource(R.string.shelf_jar_description, jar.completedAt?.let { formatDate(it) }.orEmpty())
                            ShelfObject(entry, item.tier, x, h, unit, interaction.pressableIf(tap, description) { onMemoryJar(jar) }, aspect = JarAspect) {
                                Image(painterResource(jarStateRes(JarArt.FULL)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            }
                        }
                    }
                    else -> ShelfObject(entry, item.tier, x, h, unit, interaction.pressableIf(tap, entry.name) { onObjectTap(item.itemId) }) {
                        Image(painterResource(artRes(entry.artKey)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                }

                // Where it would land if let go now.
                if (dragging && target != null && target != item.slotRef) {
                    SpotMarkerAt(unit, ShelfGeometry.slotCenterX(target.tier, target.slot), ShelfGeometry.standLine(target.tier) - 22f, 44f)
                }
            }

            // ---- Notes on the wall: exactly where the user put them.
            for (item in placed) {
                val entry = ShelfCatalog.find(item.itemId) ?: continue
                if (entry.surface != ShelfSurface.WALL) continue
                val (w, h) = ShelfLayout.wallSize(entry)
                val dragging = dragId == item.itemId
                val point = if (dragging) noteDropPoint(item, entry) else ScenePoint(item.x ?: 0.5f, item.y ?: 0.2f)
                val cx = point.x * ShelfGeometry.SCENE_WIDTH
                val cy = point.y * ShelfGeometry.SCENE_HEIGHT
                val interaction = if (onMoveNote == null || placement != null) Modifier else dragModifier(
                    key = item.itemId,
                    dragging = dragging,
                    translation = Offset.Zero, // the note itself moves (clamped to the wall), not just its picture
                    onStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dragOffset = Offset.Zero
                        dragId = item.itemId
                    },
                    onDelta = { dragOffset += it },
                    onEnd = {
                        val current = latestPlaced.firstOrNull { it.itemId == item.itemId }
                        if (current != null) {
                            val drop = noteDropPoint(current, entry)
                            pendingNote = item.itemId to drop
                            latestMoveNote?.invoke(item.itemId, drop)
                        }
                        dragId = null
                        dragOffset = Offset.Zero
                    },
                )
                WallNote(
                    entry.artKey, unit, cx, cy, w, h,
                    interaction.pressableIf(placement == null, entry.name) { onObjectTap(item.itemId) },
                )
            }

            // ---- Choosing where something new goes.
            if (placement != null && placingEntry != null) {
                if (placingEntry.surface == ShelfSurface.SHELF) {
                    val taken = ShelfLayout.taken(placed)
                    for (slot in ShelfLayout.freeSlots(taken)) {
                        val x = ShelfGeometry.slotCenterX(slot.tier, slot.slot)
                        val label = stringResource(R.string.place_spot_description, slot.tier + 1, slot.slot + 1)
                        if (slot == placement.selected) {
                            val h = ShelfLayout.heightFor(placingEntry, slot.tier)
                            ShelfObject(
                                placingEntry, slot.tier, x, h, unit,
                                Modifier.alpha(0.94f).pressable(label) { placement.onSelect(slot) },
                            ) {
                                Image(painterResource(artRes(placingEntry.artKey)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            }
                        } else {
                            SpotMarkerAt(unit, x, ShelfGeometry.standLine(slot.tier) - 22f, 44f, label) { placement.onSelect(slot) }
                        }
                    }
                } else {
                    // A note goes wherever it is tapped or dragged to on the wall.
                    val (w, h) = ShelfLayout.wallSize(placingEntry)
                    Box(
                        Modifier
                            .offset(0.dp, 0.dp)
                            .size(sceneW, unit * ShelfGeometry.WALL_HEIGHT)
                            .pointerInput(placingEntry.id) {
                                fun set(p: Offset) {
                                    placement.onPoint(
                                        ShelfLayout.clampWall(
                                            placingEntry,
                                            ScenePoint(p.x / k0 / ShelfGeometry.SCENE_WIDTH, p.y / k0 / ShelfGeometry.SCENE_HEIGHT),
                                        ),
                                    )
                                }
                                detectTapGestures { set(it) }
                            }
                            .pointerInput(placingEntry.id) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    placement.onPoint(
                                        ShelfLayout.clampWall(
                                            placingEntry,
                                            ScenePoint(
                                                change.position.x / k0 / ShelfGeometry.SCENE_WIDTH,
                                                change.position.y / k0 / ShelfGeometry.SCENE_HEIGHT,
                                            ),
                                        ),
                                    )
                                }
                            }
                            .semantics { contentDescription = "Tap the wall to hang the note where you like" },
                    )
                    val p = placement.point ?: ScenePoint(0.5f, 0.22f)
                    val c = ShelfLayout.clampWall(placingEntry, p)
                    WallNote(placingEntry.artKey, unit, c.x * ShelfGeometry.SCENE_WIDTH, c.y * ShelfGeometry.SCENE_HEIGHT, w, h, Modifier.alpha(0.94f))
                }
            }

            // ---- The heading, on the wall above the bookcase.
            if (showHeader) {
                val visLeft = ((ShelfGeometry.SCENE_WIDTH - screenW / k0) / 2f).coerceAtLeast(0f)
                val visTop = ((ShelfGeometry.SCENE_HEIGHT - screenH / k0) / 2f).coerceAtLeast(0f)
                val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                Column(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(x = unit * visLeft + 22.dp, y = unit * visTop + statusBar + 14.dp)
                        .width(with(density) { (screenW * 0.72f).toDp() }),
                ) {
                    Text(
                        stringResource(R.string.shelf_header_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.text,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.shelf_header_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/** Which spot the user is choosing for [artKey], and what happens when they choose. */
class PlacementMode(
    val artKey: String,
    /** The chosen bookcase slot (for bookcase items). */
    val selected: SlotRef?,
    /** The chosen wall point (for notes). */
    val point: ScenePoint?,
    val onSelect: (SlotRef) -> Unit,
    val onPoint: (ScenePoint) -> Unit,
)

/** A picture placed by scene pixels. */
@Composable
private fun BoxScope.SceneImage(res: Int, unit: Dp, x: Float, y: Float, w: Float, h: Float) {
    Image(
        painterResource(res), null,
        Modifier.align(Alignment.TopStart).offset(unit * x, unit * y).size(unit * w, unit * h),
        contentScale = ContentScale.FillBounds,
    )
}

/** A note hanging on the wall, centred on ([cx], [cy]) in scene px, with a soft little shadow behind it. */
@Composable
private fun BoxScope.WallNote(artKey: String, unit: Dp, cx: Float, cy: Float, w: Float, h: Float, modifier: Modifier) {
    Box(
        Modifier
            .align(Alignment.TopStart)
            .offset(unit * (cx - w / 2f), unit * (cy - h / 2f))
            .size(unit * w, unit * h)
            .then(modifier),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .offset(unit * 2.5f, unit * 3f)
                .alpha(0.14f)
                .background(androidx.compose.ui.graphics.Color(0xFF4A2C14), androidx.compose.foundation.shape.RoundedCornerShape(unit * 4f)),
        )
        Image(painterResource(artRes(artKey)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

/** Press-and-hold-to-drag. [translation] moves the picture while it is held (pass zero to move it another way). */
private fun dragModifier(
    key: String,
    dragging: Boolean,
    translation: Offset,
    onStart: () -> Unit,
    onDelta: (Offset) -> Unit,
    onEnd: () -> Unit,
): Modifier = Modifier
    .zIndex(if (dragging) 10f else 0f)
    .graphicsLayer {
        if (dragging) {
            translationX = translation.x
            translationY = translation.y
            alpha = 0.92f
        }
    }
    .pointerInput(key) {
        detectDragGesturesAfterLongPress(
            onDragStart = { onStart() },
            onDrag = { change, amount ->
                change.consume()
                onDelta(amount)
            },
            onDragEnd = { onEnd() },
            onDragCancel = { onEnd() },
        )
    }

private fun Modifier.pressableIf(enabled: Boolean, description: String, onClick: () -> Unit): Modifier =
    if (enabled) pressable(description, onClick) else this

/** A dashed ring with a plus: "you could put something here". Centred on ([cx], [cy]) in scene px. */
@Composable
private fun BoxScope.SpotMarkerAt(unit: Dp, cx: Float, cy: Float, size: Float, label: String? = null, onClick: (() -> Unit)? = null) {
    Box(
        Modifier
            .align(Alignment.TopStart)
            .offset(unit * (cx - size / 2f), unit * (cy - size / 2f))
            .size(unit * size, unit * size)
            .then(if (onClick != null && label != null) Modifier.pressable(label, onClick) else Modifier),
    ) { SpotMarker() }
}

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
