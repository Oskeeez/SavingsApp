package com.example.coolingoffjar.domain

import kotlin.math.max
import kotlin.math.min

/** A place on the bookcase: [tier] 0 is the top of the unit, 4 the bottom shelf; [slot] 0..3 runs left to right. */
data class SlotRef(val tier: Int, val slot: Int)

/** A point in the scene, as fractions of its width and height (so it survives any screen size). */
data class ScenePoint(val x: Float, val y: Float)

/**
 * Something the user has: bought items, plus the jar, clock, gacha machine and memory jars that are always there.
 * Things on the bookcase use [tier]/[slot]; things on the wall use the free position [x]/[y]; walls, floors and
 * shelves use neither.
 */
data class OwnedItem(
    val itemId: String,
    val tier: Int,
    val slot: Int,
    val purchasedAt: Long,
    val x: Float? = null,
    val y: Float? = null,
    /** Put away in a storage box: still owned, but not shown in the room. */
    val stored: Boolean = false,
) {
    val slotRef: SlotRef get() = SlotRef(tier, slot)
}

/**
 * Where things can stand. The bookcase has five levels with four slots each. Everything that stands on it can be
 * dragged to any free slot, the jar and the gacha machine included. Notes are pinned to the wall wherever the user
 * likes: they have no slots.
 */
object ShelfLayout {
    /** Where the always-there objects start out. */
    val DEFAULT_SLOTS: Map<String, SlotRef> = mapOf(
        "clock" to SlotRef(0, 0),
        "jar" to SlotRef(1, 1),
        "gacha" to SlotRef(1, 2),
        "memory_1" to SlotRef(2, 3),
        "memory_2" to SlotRef(3, 3),
    )

    /** Where successive wall items first appear (scene fractions), until the user moves them. */
    private val WALL_DEFAULTS = listOf(
        ScenePoint(0.84f, 0.45f), ScenePoint(0.66f, 0.45f), ScenePoint(0.48f, 0.45f),
        ScenePoint(0.30f, 0.45f), ScenePoint(0.12f, 0.45f), ScenePoint(0.84f, 0.38f),
    )

    /** The storage boxes: drop something on one to put it away, tap one to open the storage. */
    val STORAGE_BOXES = setOf("storage_box_green", "storage_boxes_cream")

    /** Fractions of the scene width at which a level's slots are centred. */
    fun slotXs(tier: Int): List<Float> =
        (0 until ShelfGeometry.SLOTS_PER_LEVEL).map { ShelfGeometry.slotCenterX(tier, it) / ShelfGeometry.SCENE_WIDTH }

    fun isValid(slot: SlotRef): Boolean =
        slot.tier in 0 until ShelfGeometry.LEVELS && slot.slot in 0 until ShelfGeometry.SLOTS_PER_LEVEL

    private fun allSlots(): List<SlotRef> =
        (0 until ShelfGeometry.LEVELS).flatMap { t -> (0 until ShelfGeometry.SLOTS_PER_LEVEL).map { SlotRef(t, it) } }

    /** First free slot, scanning top to bottom, left to right; null when the bookcase is full. */
    fun nextFreeSlot(occupied: Set<SlotRef>): SlotRef? = allSlots().firstOrNull { it !in occupied }

    /** Every slot not in [occupied]. */
    fun freeSlots(occupied: Set<SlotRef>): List<SlotRef> = allSlots().filter { it !in occupied }

    private fun surfaceOf(itemId: String): ShelfSurface = ShelfCatalog.find(itemId)?.surface ?: ShelfSurface.SHELF

    private fun isCore(itemId: String) = ShelfCatalog.find(itemId)?.category == ShelfCategory.CORE

    /** The size of a wall item (scene px). */
    fun wallSize(item: CatalogItem): Pair<Float, Float> {
        val h = min(MAX_NOTE_HEIGHT, MAX_NOTE_WIDTH / item.aspect)
        return h * item.aspect to h
    }

    /** Keeps a note wholly on the wall (and wholly inside the picture). */
    fun clampWall(item: CatalogItem, p: ScenePoint): ScenePoint {
        val (w, h) = wallSize(item)
        val minX = w / 2f / ShelfGeometry.SCENE_WIDTH
        val minY = h / 2f / ShelfGeometry.SCENE_HEIGHT
        val maxY = (ShelfGeometry.WALL_HEIGHT - 6f - h / 2f) / ShelfGeometry.SCENE_HEIGHT
        return ScenePoint(p.x.coerceIn(minX, max(minX, 1f - minX)), p.y.coerceIn(minY, max(minY, maxY)))
    }

    /**
     * Where each item really stands. Things on the bookcase keep a saved slot when it is real and not shared (the
     * always-there objects and earlier purchases win a clash); anything else moves to the next free slot, so nothing is
     * hidden or overlapping. Wall items without a position get a default one. Walls, floors and shelves are not
     * placed, so they are left out, and so are things put away in storage.
     */
    fun resolve(owned: List<OwnedItem>): List<OwnedItem> {
        val ordered = owned
            .filter { surfaceOf(it.itemId) != ShelfSurface.DECOR && !it.stored }
            .sortedWith(compareBy({ if (isCore(it.itemId)) 0 else 1 }, { it.purchasedAt }, { it.itemId }))
        val kept = HashSet<SlotRef>()
        val shelfOk = ordered.associate { item ->
            item.itemId to (surfaceOf(item.itemId) == ShelfSurface.SHELF && isValid(item.slotRef) && kept.add(item.slotRef))
        }
        val taken = HashSet(kept)
        var wallIndex = 0
        return ordered.map { item ->
            val entry = ShelfCatalog.find(item.itemId)
            when {
                entry == null -> item
                entry.surface == ShelfSurface.WALL -> {
                    val base = if (item.x != null && item.y != null) ScenePoint(item.x, item.y)
                    else WALL_DEFAULTS[wallIndex % WALL_DEFAULTS.size]
                    wallIndex++
                    val p = clampWall(entry, base)
                    item.copy(x = p.x, y = p.y)
                }
                shelfOk.getValue(item.itemId) -> item
                else -> {
                    val next = nextFreeSlot(taken)
                    if (next == null) item else { taken += next; item.copy(tier = next.tier, slot = next.slot) }
                }
            }
        }
    }

    /** Can [itemId] stand in [target]? It must be a bookcase item and the slot must be real and empty. */
    fun canMove(owned: List<OwnedItem>, itemId: String, target: SlotRef): Boolean {
        val resolved = resolve(owned)
        val me = resolved.firstOrNull { it.itemId == itemId } ?: return false
        if (surfaceOf(itemId) != ShelfSurface.SHELF || !isValid(target)) return false
        if (target == me.slotRef) return true
        return resolved.none { it.itemId != itemId && surfaceOf(it.itemId) == ShelfSurface.SHELF && it.slotRef == target }
    }

    /** Slots in use by bookcase items (the wall has none). */
    fun taken(resolved: List<OwnedItem>): Set<SlotRef> =
        resolved.filter { surfaceOf(it.itemId) == ShelfSurface.SHELF }.map { it.slotRef }.toSet()

    /**
     * Height in scene px for an item standing on [tier]: as big as fits, so wide things (cat, camera) come out low
     * and tall things (plants, lamps) come out tall. The jar, gacha machine and the like have fixed sizes. Notes are
     * sized for the wall.
     */
    fun heightFor(item: CatalogItem, tier: Int): Float {
        item.fixedHeightPx?.let { return min(it, ShelfGeometry.compartmentHeight(tier) - 2f) }
        if (item.surface == ShelfSurface.WALL) return wallSize(item).second
        val maxByHeight = min(ShelfGeometry.compartmentHeight(tier) - 6f, MAX_ITEM_HEIGHT)
        val maxByWidth = MAX_ITEM_WIDTH / item.aspect
        return min(maxByHeight, maxByWidth)
    }

    const val MAX_ITEM_WIDTH = 74f
    private const val MAX_ITEM_HEIGHT = 80f
    const val MAX_NOTE_WIDTH = 104f
    private const val MAX_NOTE_HEIGHT = 104f
}
