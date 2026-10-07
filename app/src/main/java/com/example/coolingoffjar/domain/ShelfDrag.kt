package com.example.coolingoffjar.domain

import kotlin.math.hypot

/**
 * The maths behind dragging a thing to a new spot, in scene pixels, kept pure so it can be tested: where an item's
 * centre sits when it stands in a slot, and which slot a dropped item should settle into.
 */
object ShelfDrag {
    /** A drop farther than this (scene px) from every free spot is cancelled and the item slides back. */
    const val MAX_DISTANCE = 120f

    /** Centre of [item] when it stands in [slot]: (x, y) in scene px. */
    fun anchor(item: CatalogItem, slot: SlotRef): Pair<Float, Float> =
        ShelfGeometry.slotCenterX(slot.tier, slot.slot) to ShelfGeometry.standLine(slot.tier) - ShelfLayout.heightFor(item, slot.tier) / 2f

    /**
     * Where a dropped bookcase item settles, given its centre at ([centerX], [centerY]): the nearest free slot (or the
     * one it came from), or null if it was dropped too far from anywhere.
     */
    fun dropTarget(owned: List<OwnedItem>, itemId: String, centerX: Float, centerY: Float): SlotRef? {
        val item = ShelfCatalog.find(itemId) ?: return null
        if (item.surface != ShelfSurface.SHELF) return null
        val resolved = ShelfLayout.resolve(owned)
        val me = resolved.firstOrNull { it.itemId == itemId } ?: return null
        val others = ShelfLayout.taken(resolved.filter { it.itemId != itemId })
        val candidates = ShelfLayout.freeSlots(others)
        val best = candidates.minByOrNull { slot ->
            val (ax, ay) = anchor(item, slot)
            hypot(ax - centerX, ay - centerY)
        } ?: return null
        val (bx, by) = anchor(item, best)
        return if (hypot(bx - centerX, by - centerY) <= MAX_DISTANCE) best else null
    }

    /** How close (scene px) a dropped thing's centre must be to a storage box's centre to go into it. */
    const val STORAGE_RADIUS = 46f

    /**
     * If something let go with its centre at ([centerX], [centerY]) is over a displayed storage box, the id of that box.
     * Boxes, the jar and the other always-there objects cannot be put away; notes and shelf items can.
     */
    fun storageTarget(owned: List<OwnedItem>, itemId: String, centerX: Float, centerY: Float): String? {
        val item = ShelfCatalog.find(itemId) ?: return null
        if (item.category == ShelfCategory.CORE || itemId in ShelfLayout.STORAGE_BOXES || item.surface == ShelfSurface.DECOR) return null
        return ShelfLayout.resolve(owned)
            .filter { it.itemId in ShelfLayout.STORAGE_BOXES }
            .firstOrNull { box ->
                val entry = ShelfCatalog.find(box.itemId) ?: return@firstOrNull false
                val (bx, by) = anchor(entry, box.slotRef)
                hypot(bx - centerX, by - centerY) <= STORAGE_RADIUS
            }?.itemId
    }

    /** Centre of a displayed storage box (scene px), for drawing the "drop here" hint. */
    fun boxCenter(owned: List<OwnedItem>, boxId: String): Pair<Float, Float>? {
        val box = ShelfLayout.resolve(owned).firstOrNull { it.itemId == boxId } ?: return null
        return ShelfCatalog.find(boxId)?.let { anchor(it, box.slotRef) }
    }
}
