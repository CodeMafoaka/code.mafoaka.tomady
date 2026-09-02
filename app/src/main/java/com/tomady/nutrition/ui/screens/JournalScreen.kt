package com.tomady.nutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import java.time.LocalDate

private val MEAL_TYPES = listOf("Petit-déjeuner", "Déjeuner", "Collation", "Dîner")

@Composable
fun JournalScreen() {
    val app = rememberTomadyApp()
    val coroutineScope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toString() }

    var entries by remember { mutableStateOf<List<DishHistory>>(emptyList()) }
    var nutritionByDishId by remember { mutableStateOf<Map<String, NutritionSummary>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    suspend fun reload() {
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

    LaunchedEffect(refreshTrigger) {
        reload()
    }

    Scaffold(
        topBar = { TomadyTopBar(title = "Journal du jour") },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = TomadyColors.green) {
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

    if (showAddDialog) {
        AddMealDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, mealType, kcal, protein, carbs, fat ->
                showAddDialog = false
                coroutineScope.launch {
                    val dish = app.dietService.createDish(
                        name = name,
                        category = mealType,
                        calories = kcal,
                        proteinGrams = protein,
                        carbsGrams = carbs,
                        fatGrams = fat
                    )
                    app.dietService.logMealConsumption(
                        userId = CURRENT_USER_ID,
                        dishId = dish.id,
                        date = today,
                        mealType = mealType,
                        servings = 1.0,
                        notes = name
                    )
                    refreshTrigger++
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMealDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, mealType: String, kcal: Int?, protein: Double?, carbs: Double?, fat: Double?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf(MEAL_TYPES[0]) }
    var mealTypeExpanded by remember { mutableStateOf(false) }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un repas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du repas") },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = mealTypeExpanded,
                    onExpandedChange = { mealTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = mealType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type de repas") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mealTypeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = mealTypeExpanded,
                        onDismissRequest = { mealTypeExpanded = false }
                    ) {
                        MEAL_TYPES.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    mealType = option
                                    mealTypeExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    label = { Text("Calories (kcal)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text("Protéines (g)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text("Glucides (g)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("Lipides (g)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name.trim(),
                            mealType,
                            kcal.toIntOrNull(),
                            protein.toDoubleOrNull(),
                            carbs.toDoubleOrNull(),
                            fat.toDoubleOrNull()
                        )
                    }
                }
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
