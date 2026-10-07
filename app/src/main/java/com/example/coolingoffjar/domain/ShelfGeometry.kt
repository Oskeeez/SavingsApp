package com.example.coolingoffjar.domain

/**
 * The room is one fixed picture, the "scene", 664 x 1786 px: a wall on top (with 600 px of spare wall above the part
 * that fills the screen at rest), then a floor from [WALL_HEIGHT] down. The
 * five-level bookcase stands on the floor, drawn on top at [SHELF_SCALE]. Everything (objects, notes, the camera when
 * it zooms) is positioned in scene pixels, so nothing ever depends on the screen size and the picture's edge is never
 * shown: the home screen scales the scene to cover the screen and crops the overflow.
 *
 * Numbers marked "native" are pixels of the supplied shelf cut-out (491 x 659).
 */
object ShelfGeometry {
    const val SCENE_WIDTH = 664

    /** Extra wall above the original picture, so the room can be scrolled up to see higher on the wall. */
    const val WALL_EXTRA = 600

    /** The part of the scene that fills the screen at rest (the original picture); the extra wall lies above it. */
    const val VIEW_HEIGHT = 1186
    const val SCENE_HEIGHT = VIEW_HEIGHT + WALL_EXTRA

    /** Where the wall ends and the skirting board / floor begin. */
    const val WALL_HEIGHT = 764 + WALL_EXTRA

    const val LEVELS = 5
    const val SLOTS_PER_LEVEL = 4

    const val SHELF_ART_WIDTH = 491
    const val SHELF_ART_HEIGHT = 659
    const val SHELF_SCALE = 0.92f
    const val SHELF_LEFT = (SCENE_WIDTH - SHELF_ART_WIDTH * SHELF_SCALE) / 2f
    const val SHELF_BOTTOM = WALL_HEIGHT + 107.64f // puts the back edge of the bottom board (native y 542) exactly on the skirting line (WALL_HEIGHT)
    const val SHELF_TOP = SHELF_BOTTOM - SHELF_ART_HEIGHT * SHELF_SCALE

    /** Where things' feet go on each board: a little in from the front edge of the board's top face (native y). */
    private val STAND_NATIVE = intArrayOf(42, 178, 313, 445, 590)

    /** Underside of the board above each level (native y); the top level is open to the wall. */
    private val CEILING_NATIVE = intArrayOf(-1, 64, 204, 341, 480)

    /** Open width between the uprights at each level (native x). */
    private val LEFT_NATIVE = intArrayOf(44, 61, 61, 62, 67)
    private val RIGHT_NATIVE = intArrayOf(448, 429, 429, 429, 429)

    /** How tall things standing on the top of the bookcase may be (there is only wall above it). */
    private const val TOP_HEADROOM = 100f

    private fun sceneY(native: Int): Float = SHELF_TOP + native * SHELF_SCALE
    private fun sceneX(native: Float): Float = SHELF_LEFT + native * SHELF_SCALE

    /** y (scene px) of the board surface things on [tier] stand on. */
    fun standLine(tier: Int): Float = sceneY(STAND_NATIVE[tier.coerceIn(0, LEVELS - 1)])

    /** Free height (scene px) between a level's board and whatever is above it. */
    fun compartmentHeight(tier: Int): Float {
        val t = tier.coerceIn(0, LEVELS - 1)
        return if (t == 0) TOP_HEADROOM else (STAND_NATIVE[t] - CEILING_NATIVE[t]) * SHELF_SCALE
    }

    /** x (scene px) of the left and right ends of the usable width of [tier]'s board. */
    fun levelLeft(tier: Int): Float = sceneX(LEFT_NATIVE[tier.coerceIn(0, LEVELS - 1)].toFloat())
    fun levelRight(tier: Int): Float = sceneX(RIGHT_NATIVE[tier.coerceIn(0, LEVELS - 1)].toFloat())

    /** Width of one slot on [tier]. */
    fun slotWidth(tier: Int): Float = (levelRight(tier) - levelLeft(tier)) / SLOTS_PER_LEVEL

    /** Centre x (scene px) of slot [slot] on [tier]. */
    fun slotCenterX(tier: Int, slot: Int): Float = levelLeft(tier) + slotWidth(tier) * (slot + 0.5f)
}
