package com.example.coolingoffjar.domain

import kotlin.math.min

/**
 * A place for something to stand. On the shelf [tier] is 0 for the top of the unit, 1.. for the compartments below.
 * On the wall above the shelf the tier is negative: -1 is the upper row of notes, -2 the lower row.
 */
data class SlotRef(val tier: Int, val slot: Int) {
    val isWall: Boolean get() = tier < 0
}

/** An item the user has bought, and where it stands. */
data class OwnedItem(val itemId: String, val tier: Int, val slot: Int, val purchasedAt: Long) {
    val slotRef: SlotRef get() = SlotRef(tier, slot)
}

/**
 * Where things can stand. Each shelf row has four slots along its board; the wall has two rows of three note spots.
 * A handful of slots are reserved forever: the desk clock (Settings) on the top row, the jar and the gacha machine
 * (Shop) on the second row, and two for completed-jar memories. Everything else is the user's to arrange.
 */
object ShelfLayout {
    val CLOCK = SlotRef(0, 0)
    val JAR = SlotRef(1, 1)
    val GACHA = SlotRef(1, 2)
    val MEMORY_JARS = listOf(SlotRef(2, 3), SlotRef(3, 3))

    private val reserved: Set<SlotRef> = setOf(CLOCK, JAR, GACHA) + MEMORY_JARS

    val WALL_TIERS = listOf(-1, -2)

    /** Horizontal centres of a row's slots, as fractions of the shelf width. */
    fun slotXs(tier: Int): List<Float> =
        if (tier < 0) listOf(0.22f, 0.50f, 0.78f) else listOf(0.212f, 0.405f, 0.597f, 0.789f)

    /** 0 for the upper wall row, 1 for the lower. */
    fun wallRow(tier: Int): Int = -tier - 1

    fun isReserved(slot: SlotRef): Boolean = slot in reserved

    private fun isValidFor(slot: SlotRef, surface: ShelfSurface): Boolean = when (surface) {
        ShelfSurface.SHELF -> slot.tier >= 0 && slot.slot in slotXs(slot.tier).indices
        ShelfSurface.WALL -> slot.tier in WALL_TIERS && slot.slot in slotXs(slot.tier).indices
    }

    /** True for any real slot, on the shelf or the wall. */
    fun isValid(slot: SlotRef): Boolean = isValidFor(slot, ShelfSurface.SHELF) || isValidFor(slot, ShelfSurface.WALL)

    /** True if [slot] is a real, unreserved spot on [surface] that is not in [taken]. */
    fun isUsable(slot: SlotRef, surface: ShelfSurface, taken: Set<SlotRef>): Boolean =
        isValidFor(slot, surface) && slot !in reserved && slot !in taken

    /** First free, unreserved shelf slot, scanning top to bottom, left to right. Never runs out: rows are added. */
    fun nextFreeSlot(occupied: Set<SlotRef>): SlotRef {
        var tier = 0
        while (true) {
            for (slot in slotXs(tier).indices) {
                val ref = SlotRef(tier, slot)
                if (ref !in reserved && ref !in occupied) return ref
            }
            tier++
        }
    }

    /** First free wall spot (upper row first), or null if the wall is full. */
    fun nextFreeWallSlot(occupied: Set<SlotRef>): SlotRef? =
        WALL_TIERS.flatMap { tier -> slotXs(tier).indices.map { SlotRef(tier, it) } }.firstOrNull { it !in occupied }

    fun nextFreeSlot(occupied: Set<SlotRef>, surface: ShelfSurface): SlotRef? = when (surface) {
        ShelfSurface.SHELF -> nextFreeSlot(occupied)
        ShelfSurface.WALL -> nextFreeWallSlot(occupied)
    }

    /** Every spot the user could still use on [surface] (for the shelf: across the first [tiers] rows). */
    fun freeSlots(occupied: Set<SlotRef>, tiers: Int, surface: ShelfSurface = ShelfSurface.SHELF): List<SlotRef> {
        val rows = if (surface == ShelfSurface.WALL) WALL_TIERS else (0 until tiers).toList()
        return rows.flatMap { tier -> slotXs(tier).indices.map { SlotRef(tier, it) } }
            .filter { isUsable(it, surface, occupied) }
    }

    private fun surfaceOf(itemId: String): ShelfSurface = ShelfCatalog.find(itemId)?.surface ?: ShelfSurface.SHELF

    /**
     * Where each owned item actually stands. A saved position is kept whenever it is a real, unreserved, unshared spot
     * on the item's own surface (earlier purchases win a clash). Anything else, such as an item bought under an older
     * layout whose spot is now taken by the jar, is moved to the next free spot, so nothing is hidden or overlapping.
     */
    fun resolve(owned: List<OwnedItem>): List<OwnedItem> {
        val ordered = owned.sortedWith(compareBy({ it.purchasedAt }, { it.itemId }))
        val kept = HashSet<SlotRef>()
        val ok = ordered.map { item ->
            val slot = item.slotRef
            isValidFor(slot, surfaceOf(item.itemId)) && slot !in reserved && kept.add(slot)
        }
        val taken = HashSet(kept)
        return ordered.mapIndexed { i, item ->
            if (ok[i]) {
                item
            } else {
                val next = nextFreeSlot(taken, surfaceOf(item.itemId))
                if (next == null) item else { taken += next; item.copy(tier = next.tier, slot = next.slot) }
            }
        }
    }

    /** Can [itemId] be moved to [target]? It must stay on its own surface, on a real, unreserved, empty spot. */
    fun canMove(owned: List<OwnedItem>, itemId: String, target: SlotRef): Boolean {
        val resolved = ResolvedShelf(owned)
        val me = resolved.byId[itemId] ?: return false
        if (target == me.slotRef) return true
        val others = resolved.items.filter { it.itemId != itemId }.map { it.slotRef }.toSet()
        return isUsable(target, surfaceOf(itemId), others)
    }

    private class ResolvedShelf(owned: List<OwnedItem>) {
        val items = resolve(owned)
        val byId = items.associateBy { it.itemId }
    }

    /** How many shelf rows are needed to show everything (wall notes do not add rows). */
    fun tiersNeeded(resolved: List<OwnedItem>): Int =
        maxOf(ShelfGeometry.MIN_TIERS, (resolved.filter { !it.slotRef.isWall }.maxOfOrNull { it.tier } ?: 0) + 1)

    /**
     * Height in artwork px for an item: as big as fits its spot, so wide things (cat, camera) come out low and tall
     * things (plants, lamps) come out tall. Notes are sized for the wall.
     */
    fun heightFor(item: CatalogItem, tier: Int, tiers: Int): Float {
        if (item.surface == ShelfSurface.WALL) return min(MAX_NOTE_HEIGHT, MAX_NOTE_WIDTH / item.aspect)
        val maxByHeight = min(ShelfGeometry.compartmentHeight(tier, tiers) - 8f, MAX_ITEM_HEIGHT)
        val maxByWidth = MAX_ITEM_WIDTH / item.aspect
        return min(maxByHeight, maxByWidth)
    }

    const val MAX_ITEM_WIDTH = 100f
    private const val MAX_ITEM_HEIGHT = 105f
    const val MAX_NOTE_WIDTH = 150f
    private const val MAX_NOTE_HEIGHT = 120f
}
