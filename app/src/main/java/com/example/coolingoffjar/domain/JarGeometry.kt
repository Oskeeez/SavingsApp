package com.example.coolingoffjar.domain

import kotlin.math.sqrt

/**
 * Shape of the mason jar in "width units": the jar is 1.0 wide and [HEIGHT] tall, y grows downward.
 * Both the Canvas drawing and the coin layout read these numbers, so glass and coins always agree.
 */
object JarGeometry {
    const val ASPECT = 0.8f // width / height
    const val HEIGHT = 1.25f

    const val GLASS = 0.035f // glass thickness; coins live inside it

    const val LID_LEFT = 0.18f
    const val LID_RIGHT = 0.82f
    const val LID_BOTTOM = 0.13f

    const val NECK_LEFT = 0.21f
    const val NECK_RIGHT = 0.79f
    const val NECK_BOTTOM = 0.21f

    const val BODY_LEFT = 0.04f
    const val BODY_RIGHT = 0.96f
    const val BODY_WIDTH = BODY_RIGHT - BODY_LEFT
    const val SHOULDER_BOTTOM = 0.44f
    const val BODY_STRAIGHT_BOTTOM = 1.08f
    const val BODY_BOTTOM = 1.20f
    const val BODY_CORNER = BODY_BOTTOM - BODY_STRAIGHT_BOTTOM

    private const val NECK_HALF = (NECK_RIGHT - NECK_LEFT) / 2 - GLASS
    private const val BODY_HALF = BODY_WIDTH / 2 - GLASS
    const val INNER_BOTTOM = BODY_BOTTOM - GLASS

    /** Half the usable inner width (measured from the vertical centre line x = 0.5) at height [y]. */
    fun interiorHalfWidth(y: Float): Float = when {
        y < NECK_BOTTOM -> NECK_HALF
        y < SHOULDER_BOTTOM -> {
            // Smoothstep: vertical tangents at both ends, like the S-curve of the shoulder path.
            val t = (y - NECK_BOTTOM) / (SHOULDER_BOTTOM - NECK_BOTTOM)
            NECK_HALF + (BODY_HALF - NECK_HALF) * (t * t * (3 - 2 * t))
        }
        y < BODY_STRAIGHT_BOTTOM -> BODY_HALF
        y < INNER_BOTTOM -> {
            val dy = y - BODY_STRAIGHT_BOTTOM
            val r = BODY_CORNER
            (BODY_HALF - (r - sqrt((r * r - dy * dy).coerceAtLeast(0f)))).coerceAtLeast(0f)
        }
        else -> 0f
    }
}
