package com.example.coolingoffjar.domain

import kotlin.math.roundToInt

/**
 * The jar artwork has six pictures: empty, 1, 2, 3, 4 and 5 coins. This picks the one to show for a jar that holds
 * [filled] of [perJar] coins, whatever [perJar] is:
 *   - nothing in it is always the empty picture;
 *   - the full picture is shown exactly when the jar is full (k >= N), never before;
 *   - in between it moves up through the 1-4 coin pictures in proportion to F = k / N.
 */
object JarArt {
    const val STATES = 6
    const val EMPTY = 0
    const val FULL = STATES - 1

    fun stateFor(filled: Int, perJar: Int): Int = when {
        filled <= 0 -> EMPTY
        filled >= perJar -> FULL
        else -> ((filled.toFloat() / perJar) * FULL).roundToInt().coerceIn(1, FULL - 1)
    }
}
