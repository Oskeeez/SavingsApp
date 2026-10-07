package com.example.coolingoffjar.ui.shelf

import androidx.annotation.DrawableRes
import com.example.coolingoffjar.R

/** Maps a catalogue item's `artKey` to its illustration. One place to change when artwork is swapped. */
@DrawableRes
fun artRes(key: String): Int = when (key) {
    "anthurium" -> R.drawable.item_anthurium
    "monstera" -> R.drawable.item_monstera
    "pothos_potted" -> R.drawable.item_pothos_potted
    "pothos_trailing" -> R.drawable.item_pothos_trailing
    "ivy_hanging" -> R.drawable.item_ivy_hanging
    "bonsai" -> R.drawable.item_bonsai
    "flower_vase" -> R.drawable.item_flower_vase
    "books_stack" -> R.drawable.item_books_stack
    "books_standing" -> R.drawable.item_books_standing
    "wooden_house" -> R.drawable.item_wooden_house
    "world_globe" -> R.drawable.item_world_globe
    "snow_globe" -> R.drawable.item_snow_globe
    "film_camera" -> R.drawable.item_film_camera
    "reed_diffuser" -> R.drawable.item_reed_diffuser
    "storage_box_green" -> R.drawable.item_storage_box_green
    "storage_boxes_cream" -> R.drawable.item_storage_boxes_cream
    "basket" -> R.drawable.item_basket
    "ceramic_vases" -> R.drawable.item_ceramic_vases
    "stacked_stones" -> R.drawable.item_stacked_stones
    "wooden_tray" -> R.drawable.item_wooden_tray
    "mini_mountains" -> R.drawable.item_mini_mountains
    "pen_cup" -> R.drawable.item_pen_cup
    "cat_calico" -> R.drawable.item_cat_calico
    "rabbit" -> R.drawable.item_rabbit
    "bird_figure" -> R.drawable.item_bird_figure
    "mushroom_lamp" -> R.drawable.item_mushroom_lamp
    "moon_lamp" -> R.drawable.item_moon_lamp
    "framed_landscape" -> R.drawable.item_framed_landscape
    "framed_flowers" -> R.drawable.item_framed_flowers
    "framed_night" -> R.drawable.item_framed_night
    else -> error("No artwork for '$key'")
}
