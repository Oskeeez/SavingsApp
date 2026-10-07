package com.example.coolingoffjar.domain

import kotlin.math.hypot

/**
 * The maths behind dragging a thing to a new spot, in artwork pixels, kept pure so it can be tested: where an item's
 * centre sits when it stands in a slot, and which slot a dropped item should settle into.
 */
object ShelfDrag {
    /** A drop farther than this (artwork px) from every spot is cancelled and the item slides back. */
    const val MAX_DISTANCE = 190f

    /** Centre of [item] when it stands in [slot]: (x, y) in artwork px. */
    fun anchor(item: CatalogItem, slot: SlotRef, tiers: Int): Pair<Float, Float> {
        val x = ShelfLayout.slotXs(slot.tier).getOrElse(slot.slot) { 0.5f } * ShelfGeometry.ART_WIDTH
        val y = if (slot.isWall) {
            ShelfGeometry.wallCenterY(ShelfLayout.wallRow(slot.tier)).toFloat()
        } else {
            ShelfGeometry.standLine(slot.tier, tiers) - ShelfLayout.heightFor(item, slot.tier, tiers) / 2f
        }
        return x to y
    }

    /**
     * Where a dropped [itemId] settles, given its centre at ([centerX], [centerY]): the nearest free spot on its own
     * surface (or the spot it came from), or null if it was dropped too far from anywhere.
     */
    fun dropTarget(owned: List<OwnedItem>, itemId: String, tiers: Int, centerX: Float, centerY: Float): SlotRef? {
        val item = ShelfCatalog.find(itemId) ?: return null
        val resolved = ShelfLayout.resolve(owned)
        val me = resolved.firstOrNull { it.itemId == itemId } ?: return null
        val others = resolved.filter { it.itemId != itemId }.map { it.slotRef }.toSet()
        val candidates = ShelfLayout.freeSlots(others, tiers, item.surface) + me.slotRef
        val best = candidates.minByOrNull { slot ->
            val (ax, ay) = anchor(item, slot, tiers)
            hypot(ax - centerX, ay - centerY)
        } ?: return null
        val (bx, by) = anchor(item, best, tiers)
        return if (hypot(bx - centerX, by - centerY) <= MAX_DISTANCE) best else null
    }
}
