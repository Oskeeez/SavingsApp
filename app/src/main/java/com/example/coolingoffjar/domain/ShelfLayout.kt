package com.example.coolingoffjar.domain

import kotlin.math.min

data class SlotRef(val tier: Int, val slot: Int)

/** An item the user has bought, and where it stands. */
data class OwnedItem(val itemId: String, val tier: Int, val slot: Int, val purchasedAt: Long) {
    val slotRef: SlotRef get() = SlotRef(tier, slot)
}

/**
 * Where things can stand. Each tier has a few slots along its board. A handful are reserved forever: the desk clock
 * (Settings) on the top row, the jar and the gacha machine (Shop) on the second row, and two for completed-jar
 * memories. Everything else is free for the user to fill, wherever they choose.
 */
object ShelfLayout {
    val CLOCK = SlotRef(0, 0)
    val JAR = SlotRef(1, 0)
    val GACHA = SlotRef(1, 1)
    val MEMORY_JARS = listOf(SlotRef(2, 2), SlotRef(3, 2))

    private val reserved: Set<SlotRef> = setOf(CLOCK, JAR, GACHA) + MEMORY_JARS

    /** Horizontal centres of a tier's slots, as fractions of the shelf width. The second row has room for two big things. */
    fun slotXs(tier: Int): List<Float> = if (tier == JAR.tier) listOf(0.30f, 0.70f) else listOf(0.22f, 0.50f, 0.78f)

    fun isReserved(slot: SlotRef): Boolean = slot in reserved

    fun isValid(slot: SlotRef): Boolean = slot.tier >= 0 && slot.slot in slotXs(slot.tier).indices

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

    /** Every slot the user could still put something in, across the first [tiers] tiers. */
    fun freeSlots(occupied: Set<SlotRef>, tiers: Int): List<SlotRef> =
        (0 until tiers).flatMap { tier -> slotXs(tier).indices.map { SlotRef(tier, it) } }
            .filter { it !in reserved && it !in occupied }

    /**
     * Where each owned item actually stands. A saved position is kept whenever it is a real, unreserved, unshared slot
     * (earlier purchases win a clash). Anything else, such as an item bought under an older layout whose spot is now
     * taken by the jar, is moved to the next free slot, so nothing is ever hidden or overlapping.
     */
    fun resolve(owned: List<OwnedItem>): List<OwnedItem> {
        val ordered = owned.sortedWith(compareBy({ it.purchasedAt }, { it.itemId }))
        val kept = HashSet<SlotRef>()
        val ok = ordered.map { item ->
            val slot = item.slotRef
            val good = isValid(slot) && slot !in reserved && kept.add(slot)
            good
        }
        val taken = HashSet(kept)
        return ordered.mapIndexed { i, item ->
            if (ok[i]) item else nextFreeSlot(taken).also { taken += it }.let { item.copy(tier = it.tier, slot = it.slot) }
        }
    }

    /** How many tiers the shelf needs to show everything. */
    fun tiersNeeded(resolved: List<OwnedItem>): Int =
        maxOf(ShelfGeometry.MIN_TIERS, (resolved.maxOfOrNull { it.tier } ?: 0) + 1)

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
