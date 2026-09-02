package com.tomady.nutrition.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** One tab in the main bottom navigation bar. */
data class TomadyTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val TOMADY_TABS = listOf(
    TomadyTab("dashboard", "Accueil", Icons.Filled.Home),
    TomadyTab("journal", "Journal", Icons.Filled.MenuBook),
    TomadyTab("catalogue", "Aliments", Icons.Filled.Restaurant),
    TomadyTab("assistant", "Assistant", Icons.Filled.SmartToy),
    TomadyTab("profile", "Profil", Icons.Filled.Person),
)

@Composable
fun TomadyBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        TOMADY_TABS.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}
