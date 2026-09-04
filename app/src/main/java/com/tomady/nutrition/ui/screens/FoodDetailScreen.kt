package com.tomady.nutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.tomady.nutrition.service.foodb.FoodDetailResult
import com.tomady.nutrition.service.nutrition.FoodMacros
import com.tomady.nutrition.ui.components.HeroStat
import com.tomady.nutrition.ui.components.ListRow
import com.tomady.nutrition.ui.components.MacroProgressRow
import com.tomady.nutrition.ui.components.SectionCard
import com.tomady.nutrition.ui.components.TomadyTopBar
import com.tomady.nutrition.ui.rememberTomadyApp
import com.tomady.nutrition.ui.theme.TomadyColors
import androidx.compose.ui.unit.sp

@Composable
fun FoodDetailScreen(foodId: Long, onBack: () -> Unit) {
    val app = rememberTomadyApp()
    var detail by remember { mutableStateOf<FoodDetailResult?>(null) }
    var macros by remember { mutableStateOf<FoodMacros?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(foodId) {
        loading = true
        val result = app.foodbService.getFoodDetails(foodId)
        detail = result
        // FooDB itself carries no usable macro data (compound/phytochemical
        // composition only) — macros come from a separate provider, joined
        // by food name. See NutritionLookupService.
        macros = result?.food?.name?.let { app.nutritionLookupService.getMacros(it) }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TomadyTopBar(title = detail?.food?.name ?: "Aliment", onBack = onBack)

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TomadyColors.green)
            }
        } else if (detail == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Aliment introuvable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TomadyColors.muted
                )
            }
        } else {
            val d = detail!!
            val m = macros
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    if (!d.food.nameScientific.isNullOrBlank() || d.food.foodGroup != null || d.food.category != null) {
                        Text(
                            listOfNotNull(d.food.nameScientific, d.food.foodGroup ?: d.food.category)
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TomadyColors.muted,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    if (!d.food.description.isNullOrBlank()) {
                        Text(
                            d.food.description!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TomadyColors.inkSoft,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }
                    Text(
                        "Macronutriments",
                        style = MaterialTheme.typography.titleSmall,
                        color = TomadyColors.ink,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                item {
                    SectionCard {
                        if (m == null) {
                            Text(
                                "Aucune correspondance trouvée (source : USDA FoodData Central).",
                                style = MaterialTheme.typography.bodySmall,
                                color = TomadyColors.muted
                            )
                        } else {
                            HeroStat(
                                value = "${m.calories?.toInt() ?: 0}",
                                caption = "kcal · correspondance USDA « ${m.matchedName} »",
                                valueFontSize = 40.sp
                            )
                            Column(modifier = Modifier.padding(top = 18.dp)) {
                                MacroProgressRow("Protéines", (m.proteinG ?: 0.0).toInt(), 100, TomadyColors.green)
                                Box(modifier = Modifier.padding(top = 14.dp)) {
                                    MacroProgressRow("Glucides", (m.carbsG ?: 0.0).toInt(), 100, TomadyColors.amber)
                                }
                                Box(modifier = Modifier.padding(top = 14.dp)) {
                                    MacroProgressRow("Lipides", (m.fatG ?: 0.0).toInt(), 100, TomadyColors.coral)
                                }
                            }
                        }
                    }
                }
                item {
                    Text(
                        "Composition chimique",
                        style = MaterialTheme.typography.titleSmall,
                        color = TomadyColors.ink,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    Text(
                        "FooDB — chimie, pas macros",
                        style = MaterialTheme.typography.bodySmall,
                        color = TomadyColors.muted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (d.nutrients.isEmpty()) {
                    item {
                        Text(
                            "Aucune donnée de composition chimique disponible pour cet aliment.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TomadyColors.muted
                        )
                    }
                } else {
                    item {
                        SectionCard {
                            d.nutrients.forEachIndexed { index, nutrient ->
                                ListRow(
                                    title = nutrient.nutrientName ?: "Nutriment",
                                    trailingValue = "${nutrient.amount ?: 0.0}",
                                    trailingUnit = nutrient.unit,
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
