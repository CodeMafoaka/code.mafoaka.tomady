package com.tomady.nutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.tomady.nutrition.TomadyApp
import com.tomady.nutrition.data.local.diet.entity.DishHistory
import com.tomady.nutrition.service.diet.NutritionSummary
import com.tomady.nutrition.ui.CURRENT_USER_ID
import com.tomady.nutrition.ui.components.ListRow
import com.tomady.nutrition.ui.components.SectionCard
import com.tomady.nutrition.ui.components.StatusPill
import com.tomady.nutrition.ui.components.TomadyTopBar
import com.tomady.nutrition.ui.rememberTomadyApp
import com.tomady.nutrition.ui.theme.TomadyColors
import java.time.LocalDate

private data class DayLog(
    val date: String,
    val entries: List<DishHistory>,
    val nutritionByDishId: Map<String, NutritionSummary>
) {
    val totalCalories: Int
        get() = entries.sumOf { entry ->
            val n = entry.dishId?.let { nutritionByDishId[it] } ?: return@sumOf 0
            (n.totalCalories * entry.servings).toInt()
        }
}

private suspend fun loadDay(app: TomadyApp, date: String): DayLog {
    val history = app.dietService.getDishHistoryByDate(CURRENT_USER_ID, date)
    val nutritionMap = mutableMapOf<String, NutritionSummary>()
    for (entry in history) {
        val dishId = entry.dishId ?: continue
        app.dietService.computeDishNutrition(dishId)?.let { nutritionMap[dishId] = it }
    }
    return DayLog(date, history, nutritionMap)
}

@Composable
fun JournalScreen(onAddMeal: () -> Unit, refreshKey: Int = 0) {
    val app = rememberTomadyApp()
    val today = remember { LocalDate.now().toString() }
    val yesterday = remember { LocalDate.now().minusDays(1).toString() }

    var todayLog by remember { mutableStateOf<DayLog?>(null) }
    var yesterdayLog by remember { mutableStateOf<DayLog?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(refreshKey) {
        loading = true
        todayLog = loadDay(app, today)
        yesterdayLog = loadDay(app, yesterday)
        loading = false
    }

    Scaffold(
        topBar = {
            TomadyTopBar(title = "Aujourd'hui") {
                StatusPill(
                    text = "${todayLog?.totalCalories ?: 0} kcal",
                    tint = TomadyColors.ink,
                    tintBackground = TomadyColors.card,
                    showDot = false
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMeal, containerColor = TomadyColors.ink) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un repas", tint = TomadyColors.canvas)
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
        } else {
            val today0 = todayLog
            val yesterday0 = yesterdayLog
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (today0 == null || today0.entries.isEmpty()) {
                    item {
                        Text(
                            "Aucun repas enregistré aujourd'hui.\nAppuyez sur + pour en ajouter un.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TomadyColors.muted,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                } else {
                    item {
                        SectionCard(modifier = Modifier.padding(bottom = 16.dp)) {
                            today0.entries.forEachIndexed { index, entry ->
                                val nutrition = entry.dishId?.let { today0.nutritionByDishId[it] }
                                val calories = nutrition?.let { (it.totalCalories * entry.servings).toInt() }
                                val subtitle = listOfNotNull(
                                    entry.time,
                                    entry.mealType,
                                    entry.servings.takeIf { it != 1.0 }?.let { "$it portion(s)" }
                                ).joinToString(" · ")
                                ListRow(
                                    title = entry.notes ?: entry.mealType ?: "Repas",
                                    subtitle = subtitle.ifBlank { null },
                                    trailingValue = calories?.toString(),
                                    trailingUnit = if (calories != null) "kcal" else null,
                                    showDivider = index > 0
                                )
                            }
                        }
                    }
                }
                if (yesterday0 != null && yesterday0.entries.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hier", style = MaterialTheme.typography.titleSmall, color = TomadyColors.ink)
                            Text(
                                "${yesterday0.totalCalories} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = TomadyColors.muted
                            )
                        }
                    }
                    item {
                        SectionCard {
                            yesterday0.entries.forEachIndexed { index, entry ->
                                val nutrition = entry.dishId?.let { yesterday0.nutritionByDishId[it] }
                                val calories = nutrition?.let { (it.totalCalories * entry.servings).toInt() }
                                ListRow(
                                    title = entry.notes ?: entry.mealType ?: "Repas",
                                    subtitle = entry.time,
                                    trailingValue = calories?.toString(),
                                    trailingUnit = if (calories != null) "kcal" else null,
                                    showDivider = index > 0
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
