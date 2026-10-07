package com.example.coolingoffjar.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.coolingoffjar.ui.home.HomeScreen
import com.example.coolingoffjar.ui.settings.SettingsScreen
import com.example.coolingoffjar.ui.shop.ShopScreen

@Composable
fun CoolingOffJarNavHost() {
    val navController = rememberNavController()
    // Gentle cross-fades only; the room is the star, not the transitions.
    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) },
    ) {
        composable(Destination.Home.route) {
            HomeScreen(
                onOpenShop = { navController.navigate(Destination.Shop.route) },
                onOpenSettings = { navController.navigate(Destination.Settings.route) },
            )
        }
        composable(Destination.Shop.route) {
            ShopScreen(onBack = { navController.popBackStack() })
        }
        composable(Destination.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
