package com.example.coolingoffjar.ui.components

import androidx.annotation.DrawableRes
import com.example.coolingoffjar.R
import com.example.coolingoffjar.domain.WantIcons

/** Artwork for a want's icon key (the pastel stationery set). Unknown keys get the default sprig. */
@DrawableRes
fun wantIconRes(key: String): Int = when (WantIcons.normalize(key)) {
    "headphones" -> R.drawable.want_icon_headphones
    "laptop" -> R.drawable.want_icon_laptop
    "gamepad" -> R.drawable.want_icon_gamepad
    "sneaker" -> R.drawable.want_icon_sneaker
    "journal" -> R.drawable.want_icon_journal
    "tshirt" -> R.drawable.want_icon_tshirt
    "camera" -> R.drawable.want_icon_camera
    "mug" -> R.drawable.want_icon_mug
    "plane" -> R.drawable.want_icon_plane
    "house" -> R.drawable.want_icon_house
    "plant" -> R.drawable.want_icon_plant
    "film_camera" -> R.drawable.want_icon_film_camera
    "snow_globe" -> R.drawable.want_icon_snow_globe
    "globe" -> R.drawable.want_icon_globe
    "lamp" -> R.drawable.want_icon_lamp
    "cat" -> R.drawable.want_icon_cat
    "rabbit" -> R.drawable.want_icon_rabbit
    "frame" -> R.drawable.want_icon_frame
    "books" -> R.drawable.want_icon_books
    "diffuser" -> R.drawable.want_icon_diffuser
    "tickets" -> R.drawable.want_icon_tickets
    "box" -> R.drawable.want_icon_box
    "backpack" -> R.drawable.want_icon_backpack
    else -> R.drawable.want_icon_sprig
}

/** Human name for screen readers. */
fun wantIconLabel(key: String): String = WantIcons.normalize(key).replace('_', ' ')
