package com.example.coolingoffjar.domain

/**
 * The heading written on the wall. It can be dragged anywhere, renamed or removed. [x]/[y] are the top-left corner as
 * fractions of the scene (null = the default spot at the top left of what is on screen).
 */
data class RoomText(
    val title: String = DEFAULT_TITLE,
    val body: String = DEFAULT_BODY,
    val x: Float? = null,
    val y: Float? = null,
    val visible: Boolean = true,
) {
    companion object {
        const val DEFAULT_TITLE = "My shelf"
        const val DEFAULT_BODY = "A collection of good decisions. Each item is a reminder of your patience."
        const val MAX_TITLE = 40
        const val MAX_BODY = 140

        /** Tidies what the user typed: trimmed and limited; an empty title falls back to the default. */
        fun clean(title: String, body: String) =
            title.trim().take(MAX_TITLE).ifEmpty { DEFAULT_TITLE } to body.trim().take(MAX_BODY)
    }
}
