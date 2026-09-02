package com.tomady.nutrition.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import com.tomady.nutrition.ui.components.TOMADY_TABS
import com.tomady.nutrition.ui.components.TomadyBottomBar
import com.tomady.nutrition.ui.screens.AssistantScreen
import com.tomady.nutrition.ui.screens.CatalogueScreen
import com.tomady.nutrition.ui.screens.DashboardScreen
import com.tomady.nutrition.ui.screens.FoodDetailScreen
import com.tomady.nutrition.ui.screens.JournalScreen
import com.tomady.nutrition.ui.screens.ProfileScreen
import com.tomady.nutrition.ui.theme.TomadyColors
import com.tomady.nutrition.ui.theme.TomadyTheme

/**
 * Entry point for the native Tomady UI. Lives in the same process as the
 * backend services (Room DB, DietAPIService, GemmaAndroidService, ...) and
 * calls them directly — no HTTP round-trip needed, unlike the separate React
 * Native mobile app which talks to this app's REST API instead.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TomadyTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = TomadyColors.canvas) {
                    TomadyApp()
                }
            }
        }
    }
}

private const val ROUTE_FOOD_DETAIL = "foodDetail/{foodId}"

@Composable
fun TomadyApp() {
    val navController: NavHostController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            // Hide the bottom bar on the food detail sub-screen.
            if (TOMADY_TABS.any { it.route == currentRoute }) {
                TomadyBottomBar(currentRoute = currentRoute) { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("dashboard") { DashboardScreen() }
                composable("journal") { JournalScreen() }
                composable("catalogue") {
                    CatalogueScreen(onOpenFood = { foodId ->
                        navController.navigate("foodDetail/$foodId")
                    })
                }
                composable("assistant") { AssistantScreen() }
                composable("profile") { ProfileScreen() }
                composable(ROUTE_FOOD_DETAIL) { backStackEntry ->
                    val foodId = backStackEntry.arguments?.getString("foodId")?.toLongOrNull() ?: return@composable
                    FoodDetailScreen(foodId = foodId, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
