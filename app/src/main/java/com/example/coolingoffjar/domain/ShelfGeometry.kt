package com.example.coolingoffjar.domain

/**
 * The bookcase artwork is 302 x 1024 px. To give the shelf as many tiers as it needs it is drawn in three
 * kinds of slice, cut at the lower edge of a board so the joins are invisible:
 *   - TOP:    the frame, the first compartment and its board
 *   - MIDDLE: one compartment plus its board, repeated once per extra tier
 *   - BOTTOM: the last compartment, the plinth and the floor
 * All numbers are artwork pixels.
 */
object ShelfGeometry {
    const val ART_WIDTH = 302
    const val ART_HEIGHT = 1024

    private const val TOP_END = 196
    private const val MID_START = 327
    private const val MID_END = 470
    private const val BOTTOM_START = 628

    private const val TOP_STAND = 180 // y of the first board's top surface, in the TOP slice
    private const val MID_STAND_IN_UNIT = 133 // 460 - 327
    private const val BOTTOM_STAND_IN_SLICE = 150 // 778 - 628
    private const val TOP_COMPARTMENT_TOP = 42 // inside the top frame

    const val MIN_TIERS = 5
    const val BOTTOM_HEIGHT = ART_HEIGHT - BOTTOM_START
    private const val MID_HEIGHT = MID_END - MID_START

    /** A strip of the artwork: [srcTop]/[srcHeight] in the PNG, drawn at [dstTop] in the composed shelf. */
    data class Slice(val srcTop: Int, val srcHeight: Int, val dstTop: Int)

    fun slices(tiers: Int): List<Slice> {
        val t = tiers.coerceAtLeast(2)
        val result = ArrayList<Slice>()
        var y = 0
        result += Slice(0, TOP_END, y); y += TOP_END
        repeat(t - 2) { result += Slice(MID_START, MID_HEIGHT, y); y += MID_HEIGHT }
        result += Slice(BOTTOM_START, BOTTOM_HEIGHT, y)
        return result
    }

    fun composedHeight(tiers: Int): Int = slices(tiers).last().let { it.dstTop + it.srcHeight }

    /** y of the board surface that things on [tier] stand on (feet line). */
    fun standLine(tier: Int, tiers: Int): Int {
        val t = tiers.coerceAtLeast(2)
        return when {
            tier <= 0 -> TOP_STAND
            tier >= t - 1 -> TOP_END + (t - 2) * MID_HEIGHT + BOTTOM_STAND_IN_SLICE
            else -> TOP_END + (tier - 1) * MID_HEIGHT + MID_STAND_IN_UNIT
        }
    }

    /** Usable height of the compartment above [tier]'s board. */
    fun compartmentHeight(tier: Int, tiers: Int): Int {
        val t = tiers.coerceAtLeast(2)
        return when {
            tier <= 0 -> TOP_STAND - TOP_COMPARTMENT_TOP
            tier >= t - 1 -> BOTTOM_STAND_IN_SLICE
            else -> MID_STAND_IN_UNIT
        }
    }
}
