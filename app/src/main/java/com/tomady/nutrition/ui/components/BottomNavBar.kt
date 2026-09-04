package com.tomady.nutrition.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tomady.nutrition.ui.theme.TomadyColors

/** One tab in the main bottom navigation bar. */
data class TomadyTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val TOMADY_TABS = listOf(
    TomadyTab("dashboard", "Accueil", Icons.Outlined.Home),
    TomadyTab("journal", "Journal", Icons.Outlined.MenuBook),
    TomadyTab("catalogue", "Aliments", Icons.Outlined.Restaurant),
    TomadyTab("assistant", "Assistant", Icons.Outlined.SmartToy),
    TomadyTab("profile", "Profil", Icons.Outlined.Person),
)

@Composable
fun TomadyBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(containerColor = TomadyColors.canvas) {
        TOMADY_TABS.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TomadyColors.ink,
                    selectedTextColor = TomadyColors.ink,
                    unselectedIconColor = TomadyColors.muted,
                    unselectedTextColor = TomadyColors.muted,
                    indicatorColor = TomadyColors.card
                )
            )
        }
    }
}
