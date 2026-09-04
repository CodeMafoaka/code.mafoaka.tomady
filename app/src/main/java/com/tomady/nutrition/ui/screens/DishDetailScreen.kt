package com.tomady.nutrition.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.tomady.nutrition.data.local.diet.entity.Dish
import com.tomady.nutrition.data.local.diet.entity.Recipe
import com.tomady.nutrition.service.diet.NutritionSummary
import com.tomady.nutrition.ui.components.SectionCard
import com.tomady.nutrition.ui.components.TomadyTopBar
import com.tomady.nutrition.ui.rememberTomadyApp
import com.tomady.nutrition.ui.theme.TomadyColors

/** One ingredient row, resolved to a display label and an optional cross-navigation target. */
private data class IngredientRow(
    val label: String,
    val quantityLabel: String,
    val linkedDishId: String? = null,
    val linkedFoodId: Long? = null
)

/**
 * Detail view for a [Dish] — a plat/aliment that (unlike a raw FooDB food)
 * can be composed of a recipe. Shows its computed nutrition plus, when a
 * recipe exists, the "Recette" (instructions/timing) and "Ingrédients"
 * sections — ingredients that are themselves another [Dish] (e.g. a
 * cocktail's rum, which has its own recipe) are tappable to navigate there,
 * so composition can nest arbitrarily deep.
 */
@Composable
fun DishDetailScreen(dishId: String, onBack: () -> Unit, onOpenDish: (String) -> Unit, onOpenFood: (Long) -> Unit) {
    val app = rememberTomadyApp()

    var dish by remember { mutableStateOf<Dish?>(null) }
    var nutrition by remember { mutableStateOf<NutritionSummary?>(null) }
    var recipe by remember { mutableStateOf<Recipe?>(null) }
    var ingredientRows by remember { mutableStateOf(listOf<IngredientRow>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(dishId) {
        loading = true
        dish = app.dietService.getDish(dishId)
        nutrition = app.dietService.computeDishNutrition(dishId)
        val foundRecipe = app.dietService.getRecipesForDish(dishId).firstOrNull()
        recipe = foundRecipe
        val ingredients = foundRecipe?.let { app.dietService.getRecipeIngredients(it.id) } ?: emptyList()
        ingredientRows = ingredients.map { ingredient ->
            val linkedDishId = ingredient.ingredientDishId
            val linkedFoodId = ingredient.foodItemId?.toLongOrNull()
            when {
                linkedDishId != null -> IngredientRow(
                    label = app.dietService.getDish(linkedDishId)?.name ?: "Aliment",
                    quantityLabel = "${formatQuantity(ingredient.quantity ?: 1.0)}×",
                    linkedDishId = linkedDishId
                )
                linkedFoodId != null -> IngredientRow(
                    label = ingredient.name ?: app.foodbService.getFoodDetails(linkedFoodId)?.food?.name ?: "Aliment",
                    quantityLabel = quantityLabel(ingredient.quantity, ingredient.unit),
                    linkedFoodId = linkedFoodId
                )
                else -> IngredientRow(
                    label = ingredient.name ?: "Ingrédient",
                    quantityLabel = quantityLabel(ingredient.quantity, ingredient.unit)
                )
            }
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TomadyTopBar(title = dish?.name ?: "Aliment", onBack = onBack)

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TomadyColors.green)
            }
        } else if (dish == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aliment introuvable.", style = MaterialTheme.typography.bodyMedium, color = TomadyColors.muted)
            }
        } else {
            val d = dish!!
            val n = nutrition
            val r = recipe
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SectionCard {
                        Text(d.name, style = MaterialTheme.typography.titleMedium, color = TomadyColors.ink)
                        if (!d.description.isNullOrBlank()) {
                            Text(
                                d.description!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TomadyColors.inkSoft,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                item {
                    SectionCard {
                        Text(
                            "Valeurs nutritionnelles",
                            style = MaterialTheme.typography.titleSmall,
                            color = TomadyColors.ink,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        if (n == null) {
                            Text(
                                "Non calculées.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TomadyColors.muted
                            )
                        } else {
                            Text(
                                listOfNotNull(
                                    "${n.totalCalories.toInt()} kcal",
                                    "P${n.totalProteinG.toInt()}g",
                                    "G${n.totalCarbsG.toInt()}g",
                                    "L${n.totalFatG.toInt()}g"
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TomadyColors.inkSoft
                            )
                        }
                    }
                }
                if (r != null) {
                    item {
                        SectionCard {
                            Text(
                                "Recette",
                                style = MaterialTheme.typography.titleSmall,
                                color = TomadyColors.ink,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            val timing = listOfNotNull(
                                r.prepTimeMinutes?.let { "Préparation ${it}min" },
                                r.cookTimeMinutes?.let { "Cuisson ${it}min" },
                                r.servings?.let { "$it portions" }
                            ).joinToString(" · ")
                            if (timing.isNotEmpty()) {
                                Text(timing, style = MaterialTheme.typography.bodySmall, color = TomadyColors.muted)
                            }
                            if (!r.instructions.isNullOrBlank()) {
                                Text(
                                    r.instructions!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TomadyColors.inkSoft,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
                if (ingredientRows.isNotEmpty()) {
                    item {
                        Text(
                            "Ingrédients",
                            style = MaterialTheme.typography.titleSmall,
                            color = TomadyColors.ink,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(ingredientRows) { row ->
                        val linkedDishId = row.linkedDishId
                        val linkedFoodId = row.linkedFoodId
                        val onClick: (() -> Unit)? = when {
                            linkedDishId != null -> ({ onOpenDish(linkedDishId) })
                            linkedFoodId != null -> ({ onOpenFood(linkedFoodId) })
                            else -> null
                        }
                        SectionCard(
                            modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                        ) {
                            Text(row.label, style = MaterialTheme.typography.titleSmall, color = TomadyColors.ink)
                            Text(row.quantityLabel, style = MaterialTheme.typography.bodySmall, color = TomadyColors.muted)
                        }
                    }
                } else if (r == null) {
                    item {
                        Text(
                            "Aucune recette associée — valeurs saisies manuellement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TomadyColors.muted
                        )
                    }
                }
            }
        }
    }
}

private fun quantityLabel(quantity: Double?, unit: String?): String {
    val q = quantity ?: return unit ?: ""
    val qLabel = if (q == q.toInt().toDouble()) q.toInt().toString() else q.toString()
    return listOfNotNull(qLabel, unit).joinToString(" ")
}

private fun formatQuantity(quantity: Double): String =
    if (quantity == quantity.toInt().toDouble()) quantity.toInt().toString() else quantity.toString()
