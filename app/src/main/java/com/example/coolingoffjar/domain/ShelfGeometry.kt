package com.example.coolingoffjar.domain

/**
 * The shelf artwork ("Empty Wooden Bookshelf in Warm Sunlight", cropped to the unit) is 562 x 1400 px: empty wall at
 * the top, the bookcase, then a strip of floor. To give the shelf as many rows as it needs it is drawn in three kinds
 * of slice, cut just under a board so the joins are invisible:
 *   - TOP:    the wall, and the top of the unit (things can stand on top of it)
 *   - MIDDLE: one compartment plus its board, repeated once per extra row
 *   - BOTTOM: the last compartment, the base and the floor
 * All numbers are artwork pixels.
 */
object ShelfGeometry {
    const val ART_WIDTH = 562
    const val ART_HEIGHT = 1400

    private const val TOP_END = 562 // just under the top board
    private const val MID_START = 712 // just under the second board
    private const val MID_END = 866 // just under the third board
    private const val BOTTOM_START = 1030 // just under the fourth board

    private const val TOP_STAND = 546 // feet line on the top board
    private const val MID_STAND_IN_UNIT = 128 // feet line in a middle slice (measured from its top)
    private const val BOTTOM_STAND_IN_SLICE = 145 // feet line on the base (measured from the slice top)
    private const val TOP_HEADROOM = 96 // how tall things on top of the unit may be (notes hang on the wall above)

    const val MIN_TIERS = 5
    const val BOTTOM_HEIGHT = ART_HEIGHT - BOTTOM_START
    private const val MID_HEIGHT = MID_END - MID_START

    /** Centres of the two rows of wall notes above the shelf. Row 0 is the upper one. */
    private val WALL_ROW_CENTERS = intArrayOf(270, 400)
    const val WALL_ROWS = 2

    /** A strip of the artwork: [srcTop]/[srcHeight] in the image, drawn at [dstTop] in the composed shelf. */
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

    /** Usable height of the space above [tier]'s board. */
    fun compartmentHeight(tier: Int, tiers: Int): Int {
        val t = tiers.coerceAtLeast(2)
        return when {
            tier <= 0 -> TOP_HEADROOM
            tier >= t - 1 -> BOTTOM_STAND_IN_SLICE
            else -> MID_STAND_IN_UNIT
        }
    }

    /** Vertical centre of a wall row (0 = upper). */
    fun wallCenterY(wallRow: Int): Int = WALL_ROW_CENTERS[wallRow.coerceIn(0, WALL_ROWS - 1)]
}
