package com.tomady.nutrition.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tomady.nutrition.data.local.diet.entity.Dish
import com.tomady.nutrition.data.local.foodb.entity.FoodItem
import com.tomady.nutrition.service.nutrition.FoodMacros
import com.tomady.nutrition.ui.CURRENT_USER_ID
import com.tomady.nutrition.ui.components.ListRow
import com.tomady.nutrition.ui.components.PillSearchField
import com.tomady.nutrition.ui.components.SectionCard
import com.tomady.nutrition.ui.components.TomadyFieldShape
import com.tomady.nutrition.ui.components.TomadyTopBar
import com.tomady.nutrition.ui.components.tomadyFieldColors
import com.tomady.nutrition.ui.rememberTomadyApp
import com.tomady.nutrition.ui.theme.TomadyColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

private fun currentTimeHHmm(): String {
    val now = LocalTime.now()
    return "%02d:%02d".format(now.hour, now.minute)
}

/**
 * One row of the ingredient list when building a new dish from its recipe.
 * [linkedDishId] is set when this ingredient IS another existing aliment/plat
 * (e.g. a cocktail's "rum", which has its own recipe) rather than a raw
 * name — [quantityG] is then read as a servings multiplier, not grams.
 */
private data class IngredientDraft(
    val name: String = "",
    val quantityG: String = "100",
    val linkedDishId: String? = null
)

/**
 * Full-screen "log what I ate" flow (the hackathon-demoed feature): search
 * across known dishes (real macros already on file) and the FooDB catalogue
 * (macros looked up live via [com.tomady.nutrition.service.nutrition.NutritionLookupService]),
 * jump straight back to a recently-eaten dish, or add a brand new
 * aliment/plat that gets saved to the catalogue for next time.
 */
@Composable
fun LogMealScreen(onDone: () -> Unit) {
    val app = rememberTomadyApp()
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toString() }

    var query by remember { mutableStateOf("") }
    var dishResults by remember { mutableStateOf(listOf<Dish>()) }
    var foodResults by remember { mutableStateOf(listOf<FoodItem>()) }
    var recentDishes by remember { mutableStateOf(listOf<Dish>()) }

    var selectedDish by remember { mutableStateOf<Dish?>(null) }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var selectedMacros by remember { mutableStateOf<FoodMacros?>(null) }
    var lookingUpMacros by remember { mutableStateOf(false) }

    var servings by remember { mutableStateOf("1") }
    var quantityG by remember { mutableStateOf("100") }
    var eatenTime by remember { mutableStateOf(currentTimeHHmm()) }
    var saving by remember { mutableStateOf(false) }
    var showManualEntry by remember { mutableStateOf(false) }

    var manualName by remember { mutableStateOf("") }
    var ingredientDrafts by remember { mutableStateOf(listOf(IngredientDraft())) }
    var useManualMacros by remember { mutableStateOf(false) }
    var manualKcal by remember { mutableStateOf("") }
    var manualProtein by remember { mutableStateOf("") }
    var manualCarbs by remember { mutableStateOf("") }
    var manualFat by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        recentDishes = app.dietService.getRecentDistinctDishes(CURRENT_USER_ID, limit = 3)
    }

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
        ingredientDrafts = listOf(IngredientDraft())
        useManualMacros = false
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
                        time = eatenTime,
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
                        calories = macros?.calories?.let { (it * scale).toInt() },
                        proteinGrams = macros?.proteinG?.let { it * scale },
                        carbsGrams = macros?.carbsG?.let { it * scale },
                        fatGrams = macros?.fatG?.let { it * scale }
                    )
                    app.dietService.logMealConsumption(
                        userId = CURRENT_USER_ID,
                        dishId = newDish.id,
                        date = today,
                        time = eatenTime,
                        servings = 1.0,
                        notes = name
                    )
                }
                showManualEntry && useManualMacros -> {
                    val newDish = app.dietService.createDish(
                        name = manualName,
                        calories = manualKcal.toIntOrNull(),
                        proteinGrams = manualProtein.toDoubleOrNull(),
                        carbsGrams = manualCarbs.toDoubleOrNull(),
                        fatGrams = manualFat.toDoubleOrNull()
                    )
                    app.dietService.logMealConsumption(
                        userId = CURRENT_USER_ID,
                        dishId = newDish.id,
                        date = today,
                        time = eatenTime,
                        servings = 1.0,
                        notes = manualName
                    )
                }
                showManualEntry -> {
                    // Primary path: the dish's nutrition comes from what it's
                    // made of (its recipe), computed by looking up each
                    // ingredient's macros — not a number typed in once.
                    val newDish = app.dietService.createDish(name = manualName)
                    val recipe = app.dietService.createRecipe(name = manualName, dishId = newDish.id)
                    for (draft in ingredientDrafts) {
                        if (draft.name.isBlank() && draft.linkedDishId == null) continue
                        if (draft.linkedDishId != null) {
                            app.dietService.addRecipeIngredient(
                                recipeId = recipe.id,
                                name = draft.name,
                                ingredientDishId = draft.linkedDishId,
                                quantity = draft.quantityG.toDoubleOrNull() ?: 1.0
                            )
                        } else {
                            app.dietService.addRecipeIngredient(
                                recipeId = recipe.id,
                                name = draft.name,
                                quantity = draft.quantityG.toDoubleOrNull() ?: 100.0,
                                unit = "g"
                            )
                        }
                    }
                    app.dietService.refreshDishNutritionCache(newDish.id)
                    app.dietService.logMealConsumption(
                        userId = CURRENT_USER_ID,
                        dishId = newDish.id,
                        date = today,
                        time = eatenTime,
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
        showManualEntry && useManualMacros -> manualName.isNotBlank()
        showManualEntry -> manualName.isNotBlank() &&
            ingredientDrafts.any { it.name.isNotBlank() || it.linkedDishId != null }
        else -> false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TomadyTopBar(title = "Ajouter un repas", onBack = onDone)

        PillSearchField(
            value = query,
            onValueChange = {
                query = it
                selectedDish = null
                selectedFood = null
                selectedMacros = null
                showManualEntry = false
            },
            placeholder = "Rechercher un plat ou un aliment…",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
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
                    eatenTime = eatenTime,
                    onEatenTimeChange = { eatenTime = it },
                    manualName = manualName,
                    onManualNameChange = { manualName = it },
                    ingredientDrafts = ingredientDrafts,
                    onIngredientChange = { index, draft ->
                        ingredientDrafts = ingredientDrafts.toMutableList().also { it[index] = draft }
                    },
                    onAddIngredient = { ingredientDrafts = ingredientDrafts + IngredientDraft() },
                    onRemoveIngredient = { index ->
                        ingredientDrafts = ingredientDrafts.toMutableList().also { it.removeAt(index) }
                            .ifEmpty { listOf(IngredientDraft()) }
                    },
                    useManualMacros = useManualMacros,
                    onUseManualMacrosChange = { useManualMacros = it },
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
                    item {
                        OutlinedButton(
                            onClick = { startManualEntry() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text(
                                if (query.isBlank()) {
                                    "Ajouter un nouvel aliment / plat"
                                } else {
                                    "Ajouter « $query » comme nouvel aliment / plat"
                                }
                            )
                        }
                    }
                    if (query.isBlank() && recentDishes.isNotEmpty()) {
                        item {
                            Text(
                                "Récemment mangé",
                                style = MaterialTheme.typography.titleSmall,
                                color = TomadyColors.ink,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )
                        }
                        item {
                            SectionCard {
                                recentDishes.forEachIndexed { index, dish ->
                                    ListRow(
                                        title = dish.name,
                                        trailingValue = dish.calories?.toString(),
                                        trailingUnit = if (dish.calories != null) "kcal" else null,
                                        showChevron = true,
                                        showDivider = index > 0,
                                        onClick = { selectDish(dish) }
                                    )
                                }
                            }
                        }
                    }
                    if (dishResults.isNotEmpty()) {
                        item {
                            Text(
                                "Vos plats",
                                style = MaterialTheme.typography.titleSmall,
                                color = TomadyColors.ink,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        item {
                            SectionCard {
                                dishResults.forEachIndexed { index, dish ->
                                    ListRow(
                                        title = dish.name,
                                        trailingValue = dish.calories?.toString(),
                                        trailingUnit = if (dish.calories != null) "kcal" else null,
                                        showChevron = true,
                                        showDivider = index > 0,
                                        onClick = { selectDish(dish) }
                                    )
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
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )
                        }
                        item {
                            SectionCard {
                                foodResults.forEachIndexed { index, food ->
                                    ListRow(
                                        title = food.name ?: "Aliment",
                                        subtitle = food.foodGroup ?: food.category ?: "Général",
                                        showChevron = true,
                                        showDivider = index > 0,
                                        onClick = { selectFood(food) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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
    eatenTime: String,
    onEatenTimeChange: (String) -> Unit,
    manualName: String,
    onManualNameChange: (String) -> Unit,
    ingredientDrafts: List<IngredientDraft>,
    onIngredientChange: (Int, IngredientDraft) -> Unit,
    onAddIngredient: () -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    useManualMacros: Boolean,
    onUseManualMacrosChange: (Boolean) -> Unit,
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
    val context = LocalContext.current
    val app = rememberTomadyApp()

    var linkingIndex by remember { mutableStateOf<Int?>(null) }
    var linkQuery by remember { mutableStateOf("") }
    var linkResults by remember { mutableStateOf(listOf<Dish>()) }

    LaunchedEffect(linkingIndex, linkQuery) {
        linkResults = if (linkingIndex != null && linkQuery.isNotBlank()) {
            app.dietService.searchDishes(linkQuery).take(10)
        } else {
            emptyList()
        }
    }

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
                        shape = TomadyFieldShape,
                        colors = tomadyFieldColors(),
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
                        shape = TomadyFieldShape,
                        colors = tomadyFieldColors(),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                }
                showManualEntry -> {
                    Text("Nouvel aliment / plat", style = MaterialTheme.typography.titleSmall, color = TomadyColors.ink)
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = onManualNameChange,
                        label = { Text("Nom") },
                        shape = TomadyFieldShape,
                        colors = tomadyFieldColors(),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )

                    if (useManualMacros) {
                        Text(
                            "Valeurs nutritionnelles",
                            style = MaterialTheme.typography.titleSmall,
                            color = TomadyColors.ink,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = manualKcal,
                            onValueChange = onManualKcalChange,
                            label = { Text("Calories (kcal)") },
                            shape = TomadyFieldShape,
                            colors = tomadyFieldColors(),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        )
                        OutlinedTextField(
                            value = manualProtein,
                            onValueChange = onManualProteinChange,
                            label = { Text("Protéines (g)") },
                            shape = TomadyFieldShape,
                            colors = tomadyFieldColors(),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        )
                        OutlinedTextField(
                            value = manualCarbs,
                            onValueChange = onManualCarbsChange,
                            label = { Text("Glucides (g)") },
                            shape = TomadyFieldShape,
                            colors = tomadyFieldColors(),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        )
                        OutlinedTextField(
                            value = manualFat,
                            onValueChange = onManualFatChange,
                            label = { Text("Lipides (g)") },
                            shape = TomadyFieldShape,
                            colors = tomadyFieldColors(),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        )
                        TextButton(
                            onClick = { onUseManualMacrosChange(false) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text("Revenir à la liste d'ingrédients")
                        }
                    } else {
                        Text(
                            "Ingrédients",
                            style = MaterialTheme.typography.titleSmall,
                            color = TomadyColors.ink,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                        Text(
                            "Les valeurs nutritionnelles sont calculées à partir des ingrédients.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TomadyColors.muted
                        )
                        ingredientDrafts.forEachIndexed { index, draft ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                if (draft.linkedDishId != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Filled.Link,
                                            contentDescription = null,
                                            tint = TomadyColors.violetDeep,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text(
                                            draft.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TomadyColors.ink
                                        )
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = draft.name,
                                        onValueChange = { onIngredientChange(index, draft.copy(name = it)) },
                                        label = { Text("Ingrédient") },
                                        singleLine = true,
                                        shape = TomadyFieldShape,
                                        colors = tomadyFieldColors(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = draft.quantityG,
                                    onValueChange = { onIngredientChange(index, draft.copy(quantityG = it)) },
                                    label = { Text(if (draft.linkedDishId != null) "portions" else "g") },
                                    singleLine = true,
                                    shape = TomadyFieldShape,
                                    colors = tomadyFieldColors(),
                                    modifier = Modifier.width(90.dp).padding(start = 8.dp)
                                )
                                IconButton(
                                    onClick = {
                                        if (draft.linkedDishId != null) {
                                            onIngredientChange(index, draft.copy(name = "", linkedDishId = null))
                                        } else {
                                            linkingIndex = index
                                            linkQuery = draft.name
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .border(
                                            1.dp,
                                            if (draft.linkedDishId != null) TomadyColors.violetDeep else TomadyColors.line,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        if (draft.linkedDishId != null) Icons.Filled.LinkOff else Icons.Filled.Link,
                                        contentDescription = "Lier à un aliment existant",
                                        tint = if (draft.linkedDishId != null) TomadyColors.violetDeep else TomadyColors.muted
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveIngredient(index) },
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .border(1.dp, TomadyColors.line, CircleShape)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Retirer l'ingrédient", tint = TomadyColors.muted)
                                }
                            }
                            if (linkingIndex == index) {
                                Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp)) {
                                    OutlinedTextField(
                                        value = linkQuery,
                                        onValueChange = { linkQuery = it },
                                        placeholder = { Text("Rechercher un aliment existant…") },
                                        singleLine = true,
                                        shape = TomadyFieldShape,
                                        colors = tomadyFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    linkResults.forEach { linkedDish ->
                                        SectionCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 6.dp)
                                                .clickable {
                                                    onIngredientChange(
                                                        index,
                                                        draft.copy(name = linkedDish.name, linkedDishId = linkedDish.id)
                                                    )
                                                    linkingIndex = null
                                                }
                                        ) {
                                            Text(
                                                linkedDish.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TomadyColors.ink
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = { linkingIndex = null },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Annuler")
                                    }
                                }
                            }
                        }
                        TextButton(
                            onClick = onAddIngredient,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Ajouter un ingrédient")
                        }
                        TextButton(
                            onClick = { onUseManualMacrosChange(true) },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text("Je connais déjà les valeurs nutritionnelles")
                        }
                    }
                }
            }
        }

        Text(
            "Heure du repas",
            style = MaterialTheme.typography.titleSmall,
            color = TomadyColors.ink
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TomadyFieldShape)
                .background(TomadyColors.card, TomadyFieldShape)
                .border(1.dp, TomadyColors.line, TomadyFieldShape)
                .clickable {
                    val parts = eatenTime.split(":")
                    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 12
                    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> onEatenTimeChange("%02d:%02d".format(hour, minute)) },
                        initialHour,
                        initialMinute,
                        true
                    ).show()
                }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = TomadyColors.muted)
            Text(eatenTime, style = MaterialTheme.typography.bodyLarge, color = TomadyColors.ink)
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
