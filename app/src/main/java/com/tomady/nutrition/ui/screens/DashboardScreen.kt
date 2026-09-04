package com.tomady.nutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.tomady.nutrition.ui.components.HeroStat
import com.tomady.nutrition.ui.components.MacroProgressRow
import com.tomady.nutrition.ui.components.SectionCard
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
    val remainingCalories = (t.calories - consumedCalories).coerceAtLeast(0)
    val consumedPct = if (t.calories > 0) consumedCalories.toFloat() / t.calories.toFloat() else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Text(
                "Bonjour,",
                style = MaterialTheme.typography.bodyMedium,
                color = TomadyColors.muted
            )
            Text(
                "${profile?.displayName ?: "…"} 👋",
                style = MaterialTheme.typography.headlineSmall,
                color = TomadyColors.ink,
                modifier = Modifier.padding(top = 2.dp, bottom = 26.dp)
            )
            HeroStat(
                value = "$remainingCalories",
                caption = "kcal restantes sur ${t.calories}",
                progress = consumedPct,
                modifier = Modifier.padding(bottom = 26.dp)
            )
            SectionCard {
                MacroProgressRow("Protéines", consumedProtein, t.proteinG, TomadyColors.green)
                Box(modifier = Modifier.padding(top = 16.dp)) {
                    MacroProgressRow("Glucides", consumedCarbs, t.carbsG, TomadyColors.amber)
                }
                Box(modifier = Modifier.padding(top = 16.dp)) {
                    MacroProgressRow("Lipides", consumedFat, t.fatG, TomadyColors.coral)
                }
            }
        }
        if (summary == null) {
            item {
                Text(
                    "Aucun repas enregistré aujourd'hui — direction l'onglet Journal pour en ajouter un.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TomadyColors.muted,
                    modifier = Modifier.padding(top = 26.dp)
                )
            }
        } else {
            item {
                Text(
                    "Répartition par repas",
                    style = MaterialTheme.typography.titleSmall,
                    color = TomadyColors.ink,
                    modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
                )
            }
            items(summary!!.meals) { meal ->
                SectionCard(modifier = Modifier.padding(bottom = 11.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            meal.mealType,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TomadyColors.ink
                        )
                        Text(
                            "${meal.totalCalories.toInt()} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = TomadyColors.muted
                        )
                    }
                    Text(
                        "P ${meal.totalProteinG.toInt()} · G ${meal.totalCarbsG.toInt()} · L ${meal.totalFatG.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TomadyColors.muted,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}
