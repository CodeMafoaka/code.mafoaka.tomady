package com.tomady.nutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomady.nutrition.data.local.diet.entity.DishHistory
import com.tomady.nutrition.service.diet.NutritionSummary
import com.tomady.nutrition.ui.CURRENT_USER_ID
import com.tomady.nutrition.ui.components.SectionCard
import com.tomady.nutrition.ui.components.TomadyTopBar
import com.tomady.nutrition.ui.rememberTomadyApp
import com.tomady.nutrition.ui.theme.TomadyColors
import java.time.LocalDate

@Composable
fun JournalScreen(onAddMeal: () -> Unit, refreshKey: Int = 0) {
    val app = rememberTomadyApp()
    val today = remember { LocalDate.now().toString() }

    var entries by remember { mutableStateOf<List<DishHistory>>(emptyList()) }
    var nutritionByDishId by remember { mutableStateOf<Map<String, NutritionSummary>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(refreshKey) {
        loading = true
        val history = app.dietService.getDishHistoryByDate(CURRENT_USER_ID, today)
        val nutritionMap = mutableMapOf<String, NutritionSummary>()
        for (entry in history) {
            val dishId = entry.dishId ?: continue
            app.dietService.computeDishNutrition(dishId)?.let { nutritionMap[dishId] = it }
        }
        entries = history
        nutritionByDishId = nutritionMap
        loading = false
    }

    Scaffold(
        topBar = { TomadyTopBar(title = "Journal du jour") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMeal, containerColor = TomadyColors.green) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un repas", tint = TomadyColors.white)
            }
        }
    ) { padding ->
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TomadyColors.green)
            }
        } else if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aucun repas enregistré aujourd'hui.\nAppuyez sur + pour en ajouter un.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TomadyColors.muted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    val nutrition = entry.dishId?.let { nutritionByDishId[it] }
                    SectionCard {
                        Text(
                            entry.notes ?: entry.mealType ?: "Repas",
                            style = MaterialTheme.typography.titleSmall,
                            color = TomadyColors.ink
                        )
                        Text(
                            entry.mealType ?: "Repas",
                            style = MaterialTheme.typography.bodySmall,
                            color = TomadyColors.muted
                        )
                        if (nutrition != null) {
                            Text(
                                "${nutrition.totalCalories.toInt()} kcal · P${nutrition.totalProteinG.toInt()} · G${nutrition.totalCarbsG.toInt()} · L${nutrition.totalFatG.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TomadyColors.inkSoft,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
