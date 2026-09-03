package com.tomady.nutrition.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.tomady.nutrition.ui.screens.LogMealScreen
import com.tomady.nutrition.ui.screens.ProfileScreen
import com.tomady.nutrition.ui.theme.ThemeManager
import com.tomady.nutrition.ui.theme.TomadyColors
import com.tomady.nutrition.ui.theme.TomadyTheme
import com.tomady.nutrition.config.ConfigManager

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
    val app = rememberTomadyApp()
    var bootstrapped by remember { mutableStateOf(false) }

    // Profile/DishHistory both have a hard ForeignKey onto User — create the
    // User row (and default Profile) once here, before any screen mounts, so
    // no screen can race ahead and try to write against a User row that
    // doesn't exist yet (that throws an uncaught SQLiteConstraintException
    // that crashes the whole app).
    LaunchedEffect(Unit) {
        val context = app.applicationContext
        val savedTheme = ConfigManager(context).get().ui.theme
        TomadyColors.applyTheme(ThemeManager.load(context, savedTheme))
        ensureDefaultProfile(app)
        bootstrapped = true
    }

    if (!bootstrapped) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TomadyColors.green)
        }
        return
    }

    val navController: NavHostController = rememberNavController()
    var journalRefreshKey by remember { mutableStateOf(0) }

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
                composable("journal") {
                    JournalScreen(
                        onAddMeal = { navController.navigate("logMeal") },
                        refreshKey = journalRefreshKey
                    )
                }
                composable("logMeal") {
                    LogMealScreen(onDone = {
                        journalRefreshKey++
                        navController.popBackStack()
                    })
                }
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
