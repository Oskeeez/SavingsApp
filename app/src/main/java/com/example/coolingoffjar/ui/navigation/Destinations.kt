package com.example.coolingoffjar.ui.navigation

/** Top-level destinations. Sheets (add, decision, freebie) are overlays on Home, not routes. */
sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object Shelf : Destination("shelf")
    data object Settings : Destination("settings")
}
