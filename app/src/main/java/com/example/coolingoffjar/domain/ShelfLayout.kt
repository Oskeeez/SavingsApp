package com.example.coolingoffjar.domain

import kotlin.math.min

data class SlotRef(val tier: Int, val slot: Int)

/** An item the user has bought, and where it stands. Position is stored so the shelf never reshuffles. */
data class OwnedItem(val itemId: String, val tier: Int, val slot: Int, val purchasedAt: Long) {
    val slotRef: SlotRef get() = SlotRef(tier, slot)
}

/**
 * Where things can stand. Each tier has a few slots along its board. A handful are reserved forever: the
 * jar, the gacha machine (Shop) and the desk clock (Settings) always have the same place, whatever is
 * bought, and two more are kept for completed-jar memories.
 */
object ShelfLayout {
    val JAR = SlotRef(0, 0)
    val GACHA = SlotRef(0, 1)
    val CLOCK = SlotRef(1, 0)
    val MEMORY_JARS = listOf(SlotRef(2, 2), SlotRef(3, 2))

    private val reserved: Set<SlotRef> = setOf(JAR, GACHA, CLOCK) + MEMORY_JARS

    /** Horizontal centres of a tier's slots, as fractions of the shelf width. */
    fun slotXs(tier: Int): List<Float> = if (tier == 0) listOf(0.30f, 0.70f) else listOf(0.22f, 0.50f, 0.78f)

    fun isReserved(slot: SlotRef): Boolean = slot in reserved

    /** First free, unreserved slot, scanning the shelf top to bottom, left to right. Never runs out: tiers are added. */
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

    /** How many tiers the shelf needs to show everything. */
    fun tiersNeeded(owned: List<OwnedItem>): Int =
        maxOf(ShelfGeometry.MIN_TIERS, (owned.maxOfOrNull { it.tier } ?: 0) + 1)

    /**
     * Height in artwork px for an item standing in [tier]: as big as fits a slot's width and the compartment's
     * height, so wide things (cat, camera) come out low and tall things (plants, lamps) come out tall.
     */
    fun heightFor(item: CatalogItem, tier: Int, tiers: Int): Float {
        val maxByHeight = min(ShelfGeometry.compartmentHeight(tier, tiers) - 8f, MAX_ITEM_HEIGHT)
        val maxByWidth = MAX_ITEM_WIDTH / item.aspect
        return min(maxByHeight, maxByWidth)
    }

    private const val MAX_ITEM_WIDTH = 92f
    private const val MAX_ITEM_HEIGHT = 105f
}
