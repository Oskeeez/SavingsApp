package com.example.coolingoffjar.domain

/** The icons a want can wear. The key is what is stored; the UI maps it to the artwork. */
object WantIcons {
    const val DEFAULT = "sprig"

    /** In the order they appear in the picker. */
    val all: List<String> = listOf(
        "headphones", "laptop", "gamepad", "sneaker", "journal", "tshirt",
        "camera", "sprig", "mug", "plane", "house", "plant",
        "film_camera", "snow_globe", "globe", "lamp", "cat", "rabbit",
        "frame", "books", "diffuser", "tickets", "box", "backpack",
    )

    /** Anything unknown (an old row, a future icon) falls back to the default. */
    fun normalize(key: String?): String = if (key != null && key in all) key else DEFAULT
}

/**
 * The progress artwork shows five dots, whatever the jar size. This picks how many are filled (0..5):
 * none for an empty jar, all five exactly when the jar is full, and 1..4 in proportion in between,
 * so the first coin always shows and the last dot only lights when the jar is complete.
 */
object ProgressArt {
    const val STATES = 6
    const val FULL = STATES - 1

    fun stateFor(filled: Int, perJar: Int): Int = when {
        filled <= 0 -> 0
        filled >= perJar -> FULL
        else -> ((filled.toLong() * FULL) / perJar).toInt().coerceIn(1, FULL - 1)
    }
}
