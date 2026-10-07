package com.example.coolingoffjar.ui.shelf

import androidx.annotation.DrawableRes
import com.example.coolingoffjar.R

/**
 * Geometry of one shelf artwork, in artwork pixels (the PNG is [widthPx] x [heightPx]).
 * Objects are positioned in these units and scaled to the screen, so any shelf colour with the same
 * layout can be dropped in. [standLines] are the y of each board's top surface, top to bottom: an
 * object on tier `i` has its feet on `standLines[i]`.
 */
class ShelfSkin(
    @DrawableRes val image: Int,
    val widthPx: Int,
    val heightPx: Int,
    val standLines: List<Int>,
)

val LightBrownShelf = ShelfSkin(
    image = R.drawable.shelf_light_brown,
    widthPx = 302,
    heightPx = 1024,
    standLines = listOf(180, 316, 460, 610, 778),
)

/** Colour of the wooden floor along the bottom edge of the artwork, used to fill under the gesture bar. */
const val ShelfFloorColor = 0xFFAE7242

enum class ShelfAction { NONE, SHOP, SETTINGS }

/**
 * Something standing on the shelf. [x] is the horizontal centre as a fraction (0..1) of the artwork
 * width; [heightPx] is the object's height in artwork pixels (its width follows the image's aspect).
 */
data class ShelfObject(
    val id: String,
    @DrawableRes val image: Int,
    val tier: Int,
    val x: Float,
    val heightPx: Float,
    val action: ShelfAction = ShelfAction.NONE,
)

/**
 * Preview arrangement used while the shelf look is being assessed. The clock (Settings) and the
 * gacha machine (Shop) are the two permanent, reserved objects on the first tier; the rest is sample
 * decoration that the purchase system will replace.
 */
val PreviewShelfObjects: List<ShelfObject> = listOf(
    // Tier 0: the two functional objects, always here.
    ShelfObject("clock", R.drawable.item_desk_clock, 0, 0.24f, 62f, ShelfAction.SETTINGS),
    ShelfObject("gacha", R.drawable.item_gacha_machine, 0, 0.54f, 118f, ShelfAction.SHOP),
    ShelfObject("flowers", R.drawable.item_flower_vase, 0, 0.80f, 84f),
    // Tier 1
    ShelfObject("cat", R.drawable.item_cat_calico, 1, 0.30f, 50f),
    ShelfObject("lamp", R.drawable.item_mushroom_lamp, 1, 0.72f, 92f),
    // Tier 2
    ShelfObject("monstera", R.drawable.item_monstera, 2, 0.30f, 112f),
    ShelfObject("camera", R.drawable.item_film_camera, 2, 0.66f, 62f),
    // Tier 3
    ShelfObject("globe", R.drawable.item_world_globe, 3, 0.25f, 104f),
    ShelfObject("books", R.drawable.item_books_stack, 3, 0.56f, 48f),
    ShelfObject("rabbit", R.drawable.item_rabbit, 3, 0.82f, 66f),
    // Tier 4
    ShelfObject("pothos", R.drawable.item_pothos_potted, 4, 0.25f, 84f),
)

/** Tier and x-centres where completed jars ("memories") stand. Newest first. */
const val MemoryJarTier = 4
val MemoryJarSlots = listOf(0.56f, 0.80f)
const val MemoryJarHeightPx = 78f
