package com.tomady.nutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.tomady.nutrition.data.local.diet.entity.Profile
import com.tomady.nutrition.service.diet.DailySummary
import com.tomady.nutrition.service.diet.DailyTargets
import com.tomady.nutrition.ui.CURRENT_USER_ID
import com.tomady.nutrition.ui.components.MacroProgressRow
import com.tomady.nutrition.ui.components.SectionCard
import com.tomady.nutrition.ui.components.TomadyTopBar
import com.tomady.nutrition.ui.ensureDefaultProfile
import com.tomady.nutrition.ui.rememberTomadyApp
import com.tomady.nutrition.ui.theme.TomadyColors
import java.time.LocalDate

@Composable
fun DashboardScreen() {
    val app = rememberTomadyApp()
    var profile by remember { mutableStateOf<Profile?>(null) }
    var targets by remember { mutableStateOf<DailyTargets?>(null) }
    var summary by remember { mutableStateOf<DailySummary?>(null) }
    var loading by remember { mutableStateOf(true) }

    val today = remember { LocalDate.now().toString() }

    LaunchedEffect(Unit) {
        val resolvedProfile = ensureDefaultProfile(app)
        profile = resolvedProfile

        val latestBio = app.dietService.getBioRecord(CURRENT_USER_ID, today)
        targets = app.dietService.computeDailyTargets(resolvedProfile, latestBio)
            ?: DailyTargets(calories = 2000, proteinG = 120, carbsG = 220, fatG = 65)

        summary = app.dietService.getDailySummary(CURRENT_USER_ID, today)
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TomadyTopBar(title = "Bonjour, ${profile?.displayName ?: "…"} 👋")

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TomadyColors.green)
            }
            return
        }

        val t = targets ?: DailyTargets(2000, 120, 220, 65)
        val consumedCalories = summary?.totalCalories?.toInt() ?: 0
        val consumedProtein = summary?.totalProteinG?.toInt() ?: 0
        val consumedCarbs = summary?.totalCarbsG?.toInt() ?: 0
        val consumedFat = summary?.totalFatG?.toInt() ?: 0

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionCard {
                    Text("Calories aujourd'hui", style = MaterialTheme.typography.bodySmall, color = TomadyColors.muted)
                    Text(
                        "$consumedCalories kcal",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TomadyColors.ink
                    )
                    Text(
                        "Objectif : ${t.calories} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = TomadyColors.muted
                    )
                }
            }
            item {
                SectionCard {
                    Text(
                        "Macronutriments",
                        style = MaterialTheme.typography.titleSmall,
                        color = TomadyColors.ink,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    MacroProgressRow("Protéines", consumedProtein, t.proteinG, TomadyColors.green)
                    Box(modifier = Modifier.padding(top = 12.dp)) {
                        MacroProgressRow("Glucides", consumedCarbs, t.carbsG, TomadyColors.amber)
                    }
                    Box(modifier = Modifier.padding(top = 12.dp)) {
                        MacroProgressRow("Lipides", consumedFat, t.fatG, TomadyColors.blue)
                    }
                }
            }
            if (summary == null) {
                item {
                    Text(
                        "Aucun repas enregistré aujourd'hui — direction l'onglet Journal pour en ajouter un.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TomadyColors.muted
                    )
                }
            } else {
                item {
                    Text(
                        "Répartition par repas",
                        style = MaterialTheme.typography.titleSmall,
                        color = TomadyColors.ink
                    )
                }
                items(summary!!.meals) { meal ->
                    SectionCard {
                        Text(meal.mealType, style = MaterialTheme.typography.titleSmall, color = TomadyColors.ink)
                        Text(
                            "${meal.totalCalories.toInt()} kcal · P${meal.totalProteinG.toInt()} · G${meal.totalCarbsG.toInt()} · L${meal.totalFatG.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TomadyColors.muted
                        )
                    }
                }
            }
        }
    }
}
