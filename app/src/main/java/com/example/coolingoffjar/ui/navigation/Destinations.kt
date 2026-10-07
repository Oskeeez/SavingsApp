package com.example.coolingoffjar.ui.navigation

/**
 * Top-level destinations. Home is the jar and the shelf in one scrolling room; the Shop and Settings are
 * reached by tapping the gacha machine and the desk clock standing on that shelf, so there is no nav bar.
 * Sheets (add, decision, freebie) are overlays on Home, not routes.
 */
sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object Shop : Destination("shop")
    data object ShopItem : Destination("shop/{itemId}") {
        fun route(itemId: String) = "shop/$itemId"
    }
    data object Settings : Destination("settings")
    data object Storage : Destination("storage")
}
