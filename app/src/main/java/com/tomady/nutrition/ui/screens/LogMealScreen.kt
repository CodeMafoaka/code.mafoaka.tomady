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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomady.nutrition.data.local.diet.entity.Dish
import com.tomady.nutrition.data.local.foodb.entity.FoodItem
import com.tomady.nutrition.service.nutrition.FoodMacros
import com.tomady.nutrition.ui.CURRENT_USER_ID
import com.tomady.nutrition.ui.components.SectionCard
import com.tomady.nutrition.ui.components.TomadyTopBar
import com.tomady.nutrition.ui.rememberTomadyApp
import com.tomady.nutrition.ui.theme.TomadyColors
import kotlinx.coroutines.launch
import java.time.LocalDate

private val MEAL_TYPES = listOf("Petit-déjeuner", "Déjeuner", "Collation", "Dîner")

/**
 * Full-screen "log what I ate" flow (the hackathon-demoed feature): search
 * across known dishes (real macros already on file) and the FooDB catalogue
 * (macros looked up live via [com.tomady.nutrition.service.nutrition.NutritionLookupService]),
 * or fall back to manual entry for anything not found — e.g. a hamburger
 * that's in neither catalogue.
 */
@Composable
fun LogMealScreen(onDone: () -> Unit) {
    val app = rememberTomadyApp()
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toString() }

    var query by remember { mutableStateOf("") }
    var dishResults by remember { mutableStateOf(listOf<Dish>()) }
    var foodResults by remember { mutableStateOf(listOf<FoodItem>()) }

    var selectedDish by remember { mutableStateOf<Dish?>(null) }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var selectedMacros by remember { mutableStateOf<FoodMacros?>(null) }
    var lookingUpMacros by remember { mutableStateOf(false) }

    var servings by remember { mutableStateOf("1") }
    var quantityG by remember { mutableStateOf("100") }
    var mealType by remember { mutableStateOf(MEAL_TYPES[0]) }
    var mealTypeExpanded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var showManualEntry by remember { mutableStateOf(false) }

    var manualName by remember { mutableStateOf("") }
    var manualKcal by remember { mutableStateOf("") }
    var manualProtein by remember { mutableStateOf("") }
    var manualCarbs by remember { mutableStateOf("") }
    var manualFat by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            dishResults = emptyList()
            foodResults = emptyList()
        } else {
            dishResults = app.dietService.searchDishes(query)
            foodResults = app.foodbService.searchFood(query).take(15)
        }
    }

    fun selectDish(dish: Dish) {
        selectedDish = dish
        selectedFood = null
        selectedMacros = null
        showManualEntry = false
        servings = "1"
    }

    fun selectFood(food: FoodItem) {
        selectedFood = food
        selectedDish = null
        selectedMacros = null
        showManualEntry = false
        quantityG = "100"
        scope.launch {
            lookingUpMacros = true
            selectedMacros = app.nutritionLookupService.getMacros(food.name ?: query)
            lookingUpMacros = false
        }
    }

    fun startManualEntry() {
        selectedDish = null
        selectedFood = null
        selectedMacros = null
        showManualEntry = true
        manualName = query
    }

    fun save() {
        saving = true
        scope.launch {
            val dish = selectedDish
            val food = selectedFood
            when {
                dish != null -> {
                    app.dietService.logMealConsumption(
                        userId = CURRENT_USER_ID,
                        dishId = dish.id,
                        date = today,
                        mealType = mealType,
                        servings = servings.toDoubleOrNull() ?: 1.0,
                        notes = dish.name
                    )
                }
                food != null -> {
                    val scale = (quantityG.toDoubleOrNull() ?: 100.0) / 100.0
                    val macros = selectedMacros
                    val name = food.name ?: query
                    val newDish = app.dietService.createDish(
                        name = name,
                        category = mealType,
                        calories = macros?.calories?.let { (it * scale).toInt() },
                        proteinGrams = macros?.proteinG?.let { it * scale },
                        carbsGrams = macros?.carbsG?.let { it * scale },
                        fatGrams = macros?.fatG?.let { it * scale }
                    )
                    app.dietService.logMealConsumption(
                        userId = CURRENT_USER_ID,
                        dishId = newDish.id,
                        date = today,
                        mealType = mealType,
                        servings = 1.0,
                        notes = name
                    )
                }
                showManualEntry -> {
                    val newDish = app.dietService.createDish(
                        name = manualName,
                        category = mealType,
                        calories = manualKcal.toIntOrNull(),
                        proteinGrams = manualProtein.toDoubleOrNull(),
                        carbsGrams = manualCarbs.toDoubleOrNull(),
                        fatGrams = manualFat.toDoubleOrNull()
                    )
                    app.dietService.logMealConsumption(
                        userId = CURRENT_USER_ID,
                        dishId = newDish.id,
                        date = today,
                        mealType = mealType,
                        servings = 1.0,
                        notes = manualName
                    )
                }
            }
            saving = false
            onDone()
        }
    }

    val canSave = when {
        selectedDish != null -> true
        selectedFood != null -> !lookingUpMacros
        showManualEntry -> manualName.isNotBlank()
        else -> false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TomadyTopBar(title = "Ajouter un repas", onBack = onDone)

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                selectedDish = null
                selectedFood = null
                selectedMacros = null
                showManualEntry = false
            },
            placeholder = { Text("Rechercher un plat ou un aliment…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
        )

        when {
            selectedDish != null || selectedFood != null || showManualEntry -> {
                SelectionDetail(
                    dish = selectedDish,
                    food = selectedFood,
                    macros = selectedMacros,
                    lookingUpMacros = lookingUpMacros,
                    showManualEntry = showManualEntry,
                    servings = servings,
                    onServingsChange = { servings = it },
                    quantityG = quantityG,
                    onQuantityChange = { quantityG = it },
                    mealType = mealType,
                    mealTypeExpanded = mealTypeExpanded,
                    onMealTypeExpandedChange = { mealTypeExpanded = it },
                    onMealTypeSelected = { mealType = it; mealTypeExpanded = false },
                    manualName = manualName,
                    onManualNameChange = { manualName = it },
                    manualKcal = manualKcal,
                    onManualKcalChange = { manualKcal = it },
                    manualProtein = manualProtein,
                    onManualProteinChange = { manualProtein = it },
                    manualCarbs = manualCarbs,
                    onManualCarbsChange = { manualCarbs = it },
                    manualFat = manualFat,
                    onManualFatChange = { manualFat = it },
                    saving = saving,
                    canSave = canSave,
                    onCancel = {
                        selectedDish = null
                        selectedFood = null
                        showManualEntry = false
                    },
                    onSave = { save() }
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (dishResults.isNotEmpty()) {
                        item {
                            Text(
                                "Vos plats",
                                style = MaterialTheme.typography.titleSmall,
                                color = TomadyColors.ink
                            )
                        }
                        items(dishResults, key = { "dish-${it.id}" }) { dish ->
                            SectionCard(modifier = Modifier.clickable { selectDish(dish) }) {
                                Text(dish.name, style = MaterialTheme.typography.titleSmall, color = TomadyColors.ink)
                                val macroLine = listOfNotNull(
                                    dish.calories?.let { "$it kcal" },
                                    dish.proteinGrams?.let { "P${it.toInt()}g" },
                                    dish.carbsGrams?.let { "G${it.toInt()}g" },
                                    dish.fatGrams?.let { "L${it.toInt()}g" }
                                ).joinToString(" · ")
                                if (macroLine.isNotEmpty()) {
                                    Text(macroLine, style = MaterialTheme.typography.bodySmall, color = TomadyColors.muted)
                                }
                            }
                        }
                    }
                    if (foodResults.isNotEmpty()) {
                        item {
                            Text(
                                "Aliments (FooDB)",
                                style = MaterialTheme.typography.titleSmall,
                                color = TomadyColors.ink,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(foodResults, key = { "food-${it.id}" }) { food ->
                            SectionCard(modifier = Modifier.clickable { selectFood(food) }) {
                                Text(
                                    food.name ?: "Aliment",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TomadyColors.ink
                                )
                                Text(
                                    food.foodGroup ?: food.category ?: "Général",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TomadyColors.muted
                                )
                            }
                        }
                    }
                    if (query.isNotBlank()) {
                        item {
                            TextButton(
                                onClick = { startManualEntry() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Aucun résultat ? Ajouter « $query » manuellement")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionDetail(
    dish: Dish?,
    food: FoodItem?,
    macros: FoodMacros?,
    lookingUpMacros: Boolean,
    showManualEntry: Boolean,
    servings: String,
    onServingsChange: (String) -> Unit,
    quantityG: String,
    onQuantityChange: (String) -> Unit,
    mealType: String,
    mealTypeExpanded: Boolean,
    onMealTypeExpandedChange: (Boolean) -> Unit,
    onMealTypeSelected: (String) -> Unit,
    manualName: String,
    onManualNameChange: (String) -> Unit,
    manualKcal: String,
    onManualKcalChange: (String) -> Unit,
    manualProtein: String,
    onManualProteinChange: (String) -> Unit,
    manualCarbs: String,
    onManualCarbsChange: (String) -> Unit,
    manualFat: String,
    onManualFatChange: (String) -> Unit,
    saving: Boolean,
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionCard {
            when {
                dish != null -> {
                    Text(dish.name, style = MaterialTheme.typography.titleMedium, color = TomadyColors.ink)
                    val macroLine = listOfNotNull(
                        dish.calories?.let { "$it kcal" },
                        dish.proteinGrams?.let { "P${it.toInt()}g" },
                        dish.carbsGrams?.let { "G${it.toInt()}g" },
                        dish.fatGrams?.let { "L${it.toInt()}g" }
                    ).joinToString(" · ")
                    Text(
                        "Par portion : $macroLine",
                        style = MaterialTheme.typography.bodySmall,
                        color = TomadyColors.muted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    OutlinedTextField(
                        value = servings,
                        onValueChange = onServingsChange,
                        label = { Text("Portions") },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                }
                food != null -> {
                    Text(
                        food.name ?: "Aliment",
                        style = MaterialTheme.typography.titleMedium,
                        color = TomadyColors.ink
                    )
                    if (lookingUpMacros) {
                        Box(modifier = Modifier.padding(top = 8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp), color = TomadyColors.green)
                        }
                    } else if (macros == null) {
                        Text(
                            "Aucune valeur nutritionnelle trouvée (USDA FoodData Central).",
                            style = MaterialTheme.typography.bodySmall,
                            color = TomadyColors.muted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        val macroLine = listOfNotNull(
                            macros.calories?.let { "${it.toInt()} kcal" },
                            macros.proteinG?.let { "P${it.toInt()}g" },
                            macros.carbsG?.let { "G${it.toInt()}g" },
                            macros.fatG?.let { "L${it.toInt()}g" }
                        ).joinToString(" · ")
                        Text(
                            "Pour 100g (${macros.matchedName}) : $macroLine",
                            style = MaterialTheme.typography.bodySmall,
                            color = TomadyColors.muted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    OutlinedTextField(
                        value = quantityG,
                        onValueChange = onQuantityChange,
                        label = { Text("Quantité (g)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                }
                showManualEntry -> {
                    Text("Entrée manuelle", style = MaterialTheme.typography.titleSmall, color = TomadyColors.ink)
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = onManualNameChange,
                        label = { Text("Nom du repas") },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                    OutlinedTextField(
                        value = manualKcal,
                        onValueChange = onManualKcalChange,
                        label = { Text("Calories (kcal)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                    OutlinedTextField(
                        value = manualProtein,
                        onValueChange = onManualProteinChange,
                        label = { Text("Protéines (g)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                    OutlinedTextField(
                        value = manualCarbs,
                        onValueChange = onManualCarbsChange,
                        label = { Text("Glucides (g)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                    OutlinedTextField(
                        value = manualFat,
                        onValueChange = onManualFatChange,
                        label = { Text("Lipides (g)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                }
            }
        }

        SectionCard {
            ExposedDropdownMenuBox(
                expanded = mealTypeExpanded,
                onExpandedChange = onMealTypeExpandedChange
            ) {
                OutlinedTextField(
                    value = mealType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type de repas") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mealTypeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                DropdownMenu(
                    expanded = mealTypeExpanded,
                    onDismissRequest = { onMealTypeExpandedChange(false) }
                ) {
                    MEAL_TYPES.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { onMealTypeSelected(option) }
                        )
                    }
                }
            }
        }

        Button(
            onClick = onSave,
            enabled = canSave && !saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (saving) "Enregistrement…" else "Enregistrer dans le journal")
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Changer de sélection")
        }
    }
}
