package com.tomady.nutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.tomady.nutrition.service.foodb.FoodDetailResult
import com.tomady.nutrition.service.nutrition.FoodMacros
import com.tomady.nutrition.ui.components.SectionCard
import com.tomady.nutrition.ui.components.TomadyTopBar
import com.tomady.nutrition.ui.rememberTomadyApp
import com.tomady.nutrition.ui.theme.TomadyColors

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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SectionCard {
                        Text(
                            d.food.name ?: "Aliment",
                            style = MaterialTheme.typography.titleMedium,
                            color = TomadyColors.ink
                        )
                        if (!d.food.nameScientific.isNullOrBlank()) {
                            Text(
                                d.food.nameScientific!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = TomadyColors.muted
                            )
                        }
                        Text(
                            d.food.foodGroup ?: d.food.category ?: "Général",
                            style = MaterialTheme.typography.bodySmall,
                            color = TomadyColors.muted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (!d.food.description.isNullOrBlank()) {
                            Text(
                                d.food.description!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TomadyColors.inkSoft,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                item {
                    val m = macros
                    SectionCard {
                        Text(
                            "Macronutriments",
                            style = MaterialTheme.typography.titleSmall,
                            color = TomadyColors.ink,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        if (m == null) {
                            Text(
                                "Aucune correspondance trouvée (source : USDA FoodData Central).",
                                style = MaterialTheme.typography.bodySmall,
                                color = TomadyColors.muted
                            )
                        } else {
                            Text(
                                "Pour : ${m.matchedName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TomadyColors.muted
                            )
                            Text(
                                listOfNotNull(
                                    m.calories?.let { "${it.toInt()} kcal" },
                                    m.proteinG?.let { "P${it.toInt()}g" },
                                    m.carbsG?.let { "G${it.toInt()}g" },
                                    m.fatG?.let { "L${it.toInt()}g" },
                                    m.fiberG?.let { "Fibres ${it.toInt()}g" }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TomadyColors.inkSoft,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                if (d.nutrients.isEmpty()) {
                    item {
                        Text(
                            "Aucune donnée de composition chimique disponible pour cet aliment (FooDB).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TomadyColors.muted
                        )
                    }
                } else {
                    item {
                        Text(
                            "Composition chimique (FooDB)",
                            style = MaterialTheme.typography.titleSmall,
                            color = TomadyColors.ink
                        )
                    }
                    items(d.nutrients, key = { it.id }) { nutrient ->
                        SectionCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    nutrient.nutrientName ?: "Nutriment",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TomadyColors.ink
                                )
                                Text(
                                    "${nutrient.amount ?: 0.0} ${nutrient.unit ?: ""}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TomadyColors.muted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
