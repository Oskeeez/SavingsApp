package com.example.coolingoffjar.domain

import java.util.Random
import kotlin.math.abs
import kotlin.math.roundToInt

/** One resting coin, in jar width units (see [JarGeometry]). [squashY] < 1 draws a tilted ellipse. */
data class CoinSlot(val x: Float, val y: Float, val rotationDeg: Float, val squashY: Float)

/**
 * Precomputed resting positions: hex-style packing with a small seeded jitter, ordered bottom to top
 * (centre-out within each row) so the first `n` slots always look like a naturally filling jar.
 * Visible coins = round(F * S), so the jar reaches the top exactly when k = N.
 */
object CoinLayout {
    /** Coin diameter is about 10% of the jar body width. */
    const val COIN_DIAMETER = 0.10f * JarGeometry.BODY_WIDTH

    /** Fixed seed: the pile looks the same on every launch. */
    const val SEED = 20_251_007L

    private const val TOP_CENTER_Y = 0.33f
    private const val ROW_STEP = 0.80f // fraction of diameter between rows (overlapping, hex-like)
    private const val COL_STEP = 0.92f // fraction of diameter between coins in a row
    private const val JITTER = 0.10f // fraction of diameter
    private const val TILTED_SHARE = 0.28f

    val slots: List<CoinSlot> by lazy { generate(SEED) }

    /** How many of the [slotCount] resting coins to show for fill level [fill] (0..1). */
    fun visibleCoinCount(fill: Float, slotCount: Int = slots.size): Int =
        (fill.coerceIn(0f, 1f) * slotCount).roundToInt()

    fun generate(seed: Long): List<CoinSlot> {
        val d = COIN_DIAMETER
        val r = d / 2
        val rowStep = d * ROW_STEP
        val colStep = d * COL_STEP
        val jitter = d * JITTER
        val random = Random(seed)
        val result = ArrayList<CoinSlot>()

        var row = 0
        var y = JarGeometry.INNER_BOTTOM - r - jitter // jitter must never push a coin through the glass floor
        while (y >= TOP_CENTER_Y) {
            // Test the width a little below the centre: that is where the coin is widest against the glass.
            val limit = JarGeometry.interiorHalfWidth((y + r * 0.8f).coerceAtMost(JarGeometry.INNER_BOTTOM - 0.001f)) -
                r * 0.9f - jitter
            if (limit > 0f) {
                val shift = if (row % 2 == 0) 0f else 0.5f
                val offsets = ArrayList<Float>()
                var k = -(limit / colStep).toInt() - 1
                while (k <= (limit / colStep).toInt() + 1) {
                    val off = (k + shift) * colStep
                    if (abs(off) <= limit) offsets += off
                    k++
                }
                offsets.sortWith(compareBy<Float> { abs(it) }.thenBy { it })
                for (off in offsets) {
                    val tilted = random.nextFloat() < TILTED_SHARE
                    result += CoinSlot(
                        x = 0.5f + off + (random.nextFloat() - 0.5f) * 2 * jitter,
                        y = y + (random.nextFloat() - 0.5f) * 2 * jitter,
                        rotationDeg = random.nextFloat() * 180f - 90f,
                        squashY = if (tilted) 0.55f + random.nextFloat() * 0.3f else 1f,
                    )
                }
            }
            row++
            y -= rowStep
        }
        return result
    }
}
