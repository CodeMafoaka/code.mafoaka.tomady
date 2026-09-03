package com.tomady.nutrition.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tomady.nutrition.config.ConfigManager
import com.tomady.nutrition.data.local.diet.entity.Profile
import com.tomady.nutrition.ui.components.SectionCard
import com.tomady.nutrition.ui.components.TomadyTopBar
import com.tomady.nutrition.ui.ensureDefaultProfile
import com.tomady.nutrition.ui.rememberTomadyApp
import com.tomady.nutrition.ui.theme.ThemeManager
import com.tomady.nutrition.ui.theme.TomadyColors
import kotlinx.coroutines.launch

private val THEME_LABELS = mapOf("light" to "Clair", "night" to "Nuit")

@Composable
fun ProfileScreen() {
    val app = rememberTomadyApp()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<Profile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    var displayName by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var calorieTarget by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val resolved = ensureDefaultProfile(app)
        profile = resolved
        displayName = resolved.displayName ?: ""
        goal = resolved.goal ?: ""
        calorieTarget = resolved.dailyCalorieTarget?.toString() ?: ""
        heightCm = resolved.heightCm?.toString() ?: ""
        weightKg = resolved.weightKg?.toString() ?: ""
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TomadyTopBar(title = "Profil")

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TomadyColors.green)
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard {
                Text(
                    "Informations",
                    style = MaterialTheme.typography.titleSmall,
                    color = TomadyColors.ink,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text("Objectif") },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                OutlinedTextField(
                    value = calorieTarget,
                    onValueChange = { calorieTarget = it },
                    label = { Text("Objectif calorique (kcal)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                OutlinedTextField(
                    value = heightCm,
                    onValueChange = { heightCm = it },
                    label = { Text("Taille (cm)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                OutlinedTextField(
                    value = weightKg,
                    onValueChange = { weightKg = it },
                    label = { Text("Poids (kg)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
            }

            Button(
                onClick = {
                    val current = profile ?: return@Button
                    saving = true
                    coroutineScope.launch {
                        val updated = current.copy(
                            displayName = displayName.ifBlank { null },
                            goal = goal.ifBlank { null },
                            dailyCalorieTarget = calorieTarget.toIntOrNull(),
                            heightCm = heightCm.toDoubleOrNull(),
                            weightKg = weightKg.toDoubleOrNull(),
                            updatedAt = System.currentTimeMillis()
                        )
                        app.dietService.updateProfile(updated)
                        profile = updated
                        saving = false
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (saving) "Enregistrement…" else "Enregistrer")
            }

            SectionCard {
                Text(
                    "Thème",
                    style = MaterialTheme.typography.titleSmall,
                    color = TomadyColors.ink,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                fun selectTheme(name: String) {
                    TomadyColors.applyTheme(ThemeManager.load(context, name))
                    coroutineScope.launch {
                        ConfigManager(context).update(
                            com.google.gson.JsonParser.parseString("""{"ui":{"theme":"$name"}}""").asJsonObject
                        )
                    }
                }

                ThemeManager.availableThemes.forEach { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectTheme(name) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = TomadyColors.themeName == name,
                            onClick = { selectTheme(name) }
                        )
                        Text(
                            THEME_LABELS[name] ?: name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TomadyColors.ink,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            SectionCard {
                Text(
                    "Sources de données",
                    style = MaterialTheme.typography.titleSmall,
                    color = TomadyColors.ink,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    "Composition chimique des aliments : FooDB (foodb.ca). " +
                        "This work is licensed under a Creative Commons " +
                        "Attribution-NonCommercial 4.0 International License.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TomadyColors.muted
                )
                Text(
                    "Valeurs nutritionnelles (calories, macros) : USDA FoodData Central (fdc.nal.usda.gov).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TomadyColors.muted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
