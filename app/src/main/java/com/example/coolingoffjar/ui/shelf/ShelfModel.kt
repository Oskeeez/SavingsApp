package com.example.coolingoffjar.ui.shelf

import androidx.annotation.DrawableRes
import com.example.coolingoffjar.R

/** Shelf artwork. All colour variants share the same layout; only the one drawn here is used for now. */
@DrawableRes
const val ShelfArtwork: Int = R.drawable.shelf_light_brown

/** Colour of the wooden floor along the bottom edge of the artwork, used to fill under the gesture bar. */
const val ShelfFloorColor = 0xFFAE7242

/** Heights (artwork px) of the permanent objects. The jar is deliberately the biggest thing on the shelf. */
const val JarHeightPx = 128f
const val GachaHeightPx = 118f
const val ClockHeightPx = 60f
const val MemoryJarHeightPx = 78f
