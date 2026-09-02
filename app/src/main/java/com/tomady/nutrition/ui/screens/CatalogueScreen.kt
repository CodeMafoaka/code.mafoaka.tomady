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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.tomady.nutrition.data.local.foodb.entity.FoodItem
import com.tomady.nutrition.ui.components.SectionCard
import com.tomady.nutrition.ui.components.TomadyTopBar
import com.tomady.nutrition.ui.rememberTomadyApp
import com.tomady.nutrition.ui.theme.TomadyColors

/**
 * Food catalogue search — this screen has no Gemma/AI dependency at all,
 * it only queries [com.tomady.nutrition.service.foodb.FooDBDataAPIService],
 * so it works identically whether the AI model is downloaded or not.
 */
@Composable
fun CatalogueScreen(onOpenFood: (Long) -> Unit) {
    val app = rememberTomadyApp()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(query) {
        loading = true
        results = app.foodbService.searchFood(query).take(200)
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TomadyTopBar(title = "Aliments")

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Rechercher un aliment…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        )

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TomadyColors.green)
            }
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Aucun aliment trouvé.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TomadyColors.muted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(results, key = { it.id }) { food ->
                    SectionCard(
                        modifier = Modifier.clickable { onOpenFood(food.id) }
                    ) {
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
        }
    }
}
