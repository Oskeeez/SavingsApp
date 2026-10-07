package com.example.coolingoffjar.domain

/**
 * How light falls on the five levels of the bookcase, read off the shelf artwork (warm sun coming in from the upper
 * left, so every shadow falls a little down and to the right). Objects stay plain transparent pictures; when one
 * stands on a level it picks up that level's lighting here, so moving it to another level changes its look on its own.
 *
 * The aim is "that belongs in the room", not realism: the differences between levels are deliberately small.
 * Measured on the artwork: the board tops get steadily darker and a touch warmer toward the floor (about 141, 120,
 * 118, 115 and 108 luminance), and the left of every board is a few percent brighter than the right.
 */
data class ZoneLighting(
    /** Multiplies the object's brightness (1 = as drawn). */
    val brightness: Float,
    /** Tints the object toward warm light: 0 = none, 0.05 = a gentle amber. */
    val warmth: Float,
    /** Strength of the contact shadow, 0..1. */
    val shadowOpacity: Float,
    /** Shadow offset (scene px) along the light direction: right and down. */
    val shadowOffsetX: Float,
    val shadowOffsetY: Float,
    /** Edge softness of the shadow, 0 (crisp) .. 1 (very soft). */
    val shadowBlur: Float,
)

/** What an object on a level actually gets, after position and the object's own size and type are taken into account. */
data class ObjectLighting(
    val brightness: Float,
    val warmth: Float,
    val shadowOpacity: Float,
    /** Shadow width as a multiple of the object's width. */
    val shadowWidthScale: Float,
    /** Shadow thickness (scene px). */
    val shadowThickness: Float,
    val shadowOffsetX: Float,
    val shadowOffsetY: Float,
    val shadowBlur: Float,
)

/** How an object's shadow differs from the standard one: how wide, how thick, how strong. */
data class ShadowProfile(val width: Float, val thickness: Float, val strength: Float)

object ShelfLighting {
    /** One zone per level, top to bottom. */
    val ZONES: List<ZoneLighting> = listOf(
        ZoneLighting(brightness = 1.040f, warmth = 0.015f, shadowOpacity = 0.24f, shadowOffsetX = 6.0f, shadowOffsetY = 2.2f, shadowBlur = 0.75f),
        ZoneLighting(brightness = 1.000f, warmth = 0.025f, shadowOpacity = 0.28f, shadowOffsetX = 6.5f, shadowOffsetY = 2.4f, shadowBlur = 0.70f),
        ZoneLighting(brightness = 0.970f, warmth = 0.032f, shadowOpacity = 0.31f, shadowOffsetX = 6.5f, shadowOffsetY = 2.6f, shadowBlur = 0.65f),
        ZoneLighting(brightness = 0.945f, warmth = 0.040f, shadowOpacity = 0.34f, shadowOffsetX = 7.0f, shadowOffsetY = 2.8f, shadowBlur = 0.60f),
        ZoneLighting(brightness = 0.920f, warmth = 0.048f, shadowOpacity = 0.36f, shadowOffsetX = 7.0f, shadowOffsetY = 3.0f, shadowBlur = 0.55f),
    )

    private val STANDARD = ShadowProfile(width = 0.95f, thickness = 7f, strength = 1f)

    private val BY_ID = mapOf(
        "clock" to ShadowProfile(0.85f, 4.5f, 0.9f), // small, tight
        "film_camera" to ShadowProfile(0.90f, 5f, 1.0f), // short, right underneath
        "gacha" to ShadowProfile(1.08f, 10f, 1.2f), // big, firmly grounded
        "jar" to ShadowProfile(1.0f, 8f, 1.1f),
        "memory" to ShadowProfile(1.0f, 6f, 1.0f),
        "cat_calico" to ShadowProfile(1.08f, 8f, 0.95f), // soft and broad under the body
        "wooden_tray" to ShadowProfile(0.95f, 4f, 0.9f),
    )

    private val BY_CATEGORY = mapOf(
        ShelfCategory.PLANTS to ShadowProfile(1.05f, 8f, 0.9f), // wider and softer
        ShelfCategory.DECOR to ShadowProfile(0.92f, 6f, 1.0f),
        ShelfCategory.PETS to ShadowProfile(1.05f, 7f, 0.95f),
        ShelfCategory.LIGHTING to ShadowProfile(0.95f, 6f, 0.95f),
        ShelfCategory.ART to ShadowProfile(0.9f, 4f, 0.9f),
    )

    private val TIGHT = setOf("books_stack", "books_standing", "storage_box_green", "storage_boxes_cream") // sit flat on the board

    fun profileFor(item: CatalogItem): ShadowProfile = when {
        item.id in TIGHT -> ShadowProfile(0.9f, 3.5f, 1.0f)
        item.id in BY_ID -> BY_ID.getValue(item.id)
        item.artKey in BY_ID -> BY_ID.getValue(item.artKey)
        else -> BY_CATEGORY[item.category] ?: STANDARD
    }

    /**
     * The lighting for [item] standing on [tier] with its centre [xFraction] of the way across the scene.
     * Across a level the light fades very slightly from left (sunnier, warmer) to right (a little less direct).
     */
    fun forObject(item: CatalogItem, tier: Int, xFraction: Float): ObjectLighting {
        val zone = ZONES[tier.coerceIn(0, ZONES.size - 1)]
        val profile = profileFor(item)
        // -1 at the left end of the bookcase, +1 at the right.
        val side = ((xFraction * ShelfGeometry.SCENE_WIDTH - ShelfGeometry.SHELF_LEFT) /
            (ShelfGeometry.SHELF_ART_WIDTH * ShelfGeometry.SHELF_SCALE) * 2f - 1f).coerceIn(-1f, 1f)
        return ObjectLighting(
            brightness = zone.brightness * (1f - 0.028f * side),
            warmth = zone.warmth * (1f - 0.18f * side),
            shadowOpacity = (zone.shadowOpacity * profile.strength * (1f + 0.06f * side)).coerceIn(0f, 0.5f),
            shadowWidthScale = profile.width,
            shadowThickness = profile.thickness,
            shadowOffsetX = zone.shadowOffsetX,
            shadowOffsetY = zone.shadowOffsetY,
            shadowBlur = zone.shadowBlur,
        )
    }
}
