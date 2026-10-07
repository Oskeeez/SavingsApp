package com.example.coolingoffjar.domain

import java.util.Random
import kotlin.math.abs
import kotlin.math.roundToInt

/** One coin resting in the jar. [x],[y] are fractions of the jar picture's width and height. */
data class JarCoinSlot(val x: Float, val y: Float, val rotationDeg: Float, val scale: Float)

/**
 * Resting positions for coins inside the jar illustration: a hex-style pile built from the bottom up, with
 * a little seeded jitter so it is the same on every launch. Visible coins = round(F * S), so the jar is
 * exactly full when k = N and fills smoothly in between.
 */
object JarCoins {
    // Interior of the jar picture (fractions of its width / height).
    private const val LEFT = 0.17f
    private const val RIGHT = 0.83f
    private const val TOP = 0.34f
    private const val BOTTOM = 0.93f
    /** Aspect of the jar picture (width / height), so coin spacing is the same on both axes. */
    private const val JAR_ASPECT = 154f / 211f

    /** Coin diameter as a fraction of the jar's width. */
    const val COIN_DIAMETER = 0.24f

    const val SEED = 20_251_007L

    val slots: List<JarCoinSlot> by lazy { generate(SEED) }

    fun visibleCoinCount(fill: Float, slotCount: Int = slots.size): Int =
        (fill.coerceIn(0f, 1f) * slotCount).roundToInt()

    fun generate(seed: Long): List<JarCoinSlot> {
        val random = Random(seed)
        val d = COIN_DIAMETER
        val dh = d * JAR_ASPECT // a coin's diameter as a fraction of the jar's height
        val rowStep = dh * 0.55f
        val colStep = d * 0.70f
        val jitter = d * 0.06f
        val halfWidthForCoin = (RIGHT - LEFT) / 2f - d / 2f - jitter
        val result = ArrayList<JarCoinSlot>()
        var y = BOTTOM - dh / 2f - jitter * JAR_ASPECT
        var row = 0
        while (y >= TOP) {
            val shift = if (row % 2 == 0) 0f else 0.5f
            val offsets = ArrayList<Float>()
            val reach = (halfWidthForCoin / colStep).toInt() + 1
            for (k in -reach..reach) {
                val off = (k + shift) * colStep
                if (abs(off) <= halfWidthForCoin) offsets += off
            }
            offsets.sortWith(compareBy<Float> { abs(it) }.thenBy { it })
            for (off in offsets) {
                result += JarCoinSlot(
                    x = 0.5f + off + (random.nextFloat() - 0.5f) * 2 * jitter,
                    y = y + (random.nextFloat() - 0.5f) * 2 * jitter * JAR_ASPECT,
                    rotationDeg = random.nextFloat() * 50f - 25f,
                    scale = 0.94f + random.nextFloat() * 0.12f,
                )
            }
            row++
            y -= rowStep
        }
        return result
    }
}
