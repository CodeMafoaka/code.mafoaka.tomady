package com.tomady.nutrition.data

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tomady.nutrition.data.local.diet.entity.Dish
import com.tomady.nutrition.data.local.diet.entity.Recipe
import com.tomady.nutrition.data.local.diet.entity.RecipeIngredient
import com.tomady.nutrition.data.local.foodb.entity.FoodItem
import com.tomady.nutrition.data.local.foodb.entity.NutrientProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Room [RoomDatabase.Callback] that pre-seeds the local database with
 * Malagasy food data on first creation.
 *
 * Seeds the following tables:
 * - `food_item`       — 10+ traditional Malagasy foods with local names
 * - `nutrient_property` — Nutritional values (kcal, protein, carbs, fat, fiber, sodium)
 * - `dish`            — Matching dish entries for the diet catalog
 * - `recipe`          — Basic recipe with ingredient references
 * - `recipe_ingredient` — Ingredient mappings for each recipe
 *
 * The seed runs only once, when the database is first created
 * ([RoomDatabase.Callback.onCreate]). On subsequent app launches,
 * the existing data is preserved.
 */
class SeedDataCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        Log.i(TAG, "Database created — seeding Malagasy food data...")
        // Note: CoroutineScope.launch is used because RoomDatabase.Callback.onCreate()
        // runs on the main thread but we need to use the DAOs (which are suspend).
        // We get the AppDatabase instance after creation and seed via a coroutine.
        // This is handled in AppDatabase.buildDatabase() via a callback chain.
    }

    /**
     * Seeds the database with Malagasy food data.
     *
     * Called from [AppDatabase.buildDatabase] after the database is constructed.
     *
     * @param appDatabase The newly created AppDatabase instance.
     */
    suspend fun seedDatabase(appDatabase: AppDatabase) {
        try {
            // ════════════════════════════════════════════════════════════
            // FOOD ITEMS — Malagasy traditional foods
            // ════════════════════════════════════════════════════════════

            val foodItems = listOf(
                FoodItem(
                    id = 10001, name = "Romazava",
                    foodGroup = "Plats locaux", foodSubgroup = "Plat principal",
                    foodType = "Dish",
                    description = "Ragoût malgache traditionnel de viande de zébu et de brèdes (feuilles vertes) avec un mélange d'épices locales."
                ),
                FoodItem(
                    id = 10002, name = "Ravitoto sy henakisoa",
                    foodGroup = "Plats locaux", foodSubgroup = "Plat principal",
                    foodType = "Dish",
                    description = "Feuilles de manioc pilées cuisinées avec de la viande de porc et du lait de coco, plat traditionnel de l'île."
                ),
                FoodItem(
                    id = 10003, name = "Vary amin'anana",
                    foodGroup = "Plats locaux", foodSubgroup = "Plat principal",
                    foodType = "Dish",
                    description = "Riz cuit avec des herbes et légumes verts, souvent accompagné de viande ou de poisson."
                ),
                FoodItem(
                    id = 10004, name = "Mofo gasy",
                    foodGroup = "Petit-déjeuner", foodSubgroup = "Viennoiserie",
                    foodType = "Bread",
                    description = "Petit pain malgache traditionnel à base de farine de riz, légèrement sucré, cuit dans des moules ronds."
                ),
                FoodItem(
                    id = 10005, name = "Ranon'ampango",
                    foodGroup = "Boissons", foodSubgroup = "Infusion",
                    foodType = "Beverage",
                    description = "Eau de riz caramélisée — boisson traditionnelle préparée à partir du riz brûlé au fond de la marmite."
                ),
                FoodItem(
                    id = 10006, name = "Lasopy",
                    foodGroup = "Plats locaux", foodSubgroup = "Soupe",
                    foodType = "Soup",
                    description = "Soupe malgache aux légumes variés (carottes, choux, poireaux) avec du bœuf ou du poulet."
                ),
                FoodItem(
                    id = 10007, name = "Voanjobory sy henakisoa",
                    foodGroup = "Plats locaux", foodSubgroup = "Plat principal",
                    foodType = "Dish",
                    description = "Graines de voanjobory (légumineuse locale) cuisinées avec de la viande de porc."
                ),
                FoodItem(
                    id = 10008, name = "Akoho sy voanio",
                    foodGroup = "Plats locaux", foodSubgroup = "Plat principal",
                    foodType = "Dish",
                    description = "Poulet au lait de coco, plat emblématique de la cuisine malgache."
                ),
                FoodItem(
                    id = 10009, name = "Koba",
                    foodGroup = "Snacks", foodSubgroup = "Pâtisserie",
                    foodType = "Snack",
                    description = "Gâteau traditionnel malgache à base de farine de riz, de banane et de cacahuètes, cuit dans une feuille de bananier."
                ),
                FoodItem(
                    id = 10010, name = "Salady voatabia",
                    foodGroup = "Fruits/Légumes", foodSubgroup = "Salade",
                    foodType = "Salad",
                    description = "Salade de tomates fraîches à la malgache, assaisonnée d'huile, de vinaigre et d'oignons."
                ),
                FoodItem(
                    id = 10011, name = "Anana",
                    foodGroup = "Légumes", foodSubgroup = "Feuilles vertes",
                    foodType = "Vegetable",
                    description = "Brèdes (feuilles vertes) — base de nombreux plats malgaches, riches en fer et vitamines."
                ),
                FoodItem(
                    id = 10012, name = "Vary sosoa",
                    foodGroup = "Céréales", foodSubgroup = "Riz",
                    foodType = "Staple",
                    description = "Riz blanc cuit à l'eau — aliment de base de l'alimentation malgache."
                ),
                FoodItem(
                    id = 10013, name = "Mokary",
                    foodGroup = "Petit-déjeuner", foodSubgroup = "Gâteau",
                    foodType = "Bread",
                    description = "Gâteau de riz traditionnel malgache, cuit à la vapeur dans des moules en feuilles de bananier."
                ),
                FoodItem(
                    id = 10014, name = "Sakay",
                    foodGroup = "Condiments", foodSubgroup = "Sauce piquante",
                    foodType = "Condiment",
                    description = "Piment vert confit dans du vinaigre — condiment malgache incontournable."
                ),
                FoodItem(
                    id = 10015, name = "Trempage",
                    foodGroup = "Petit-déjeuner", foodSubgroup = "Pain perdu",
                    foodType = "Bread",
                    description = "Pain perdu malgache — pain trempé dans du lait sucré et frit, souvent au petit-déjeuner."
                )
            )

            appDatabase.foodItemDao().insertAll(foodItems)
            Log.i(TAG, "Seeded ${foodItems.size} food items")

            // ════════════════════════════════════════════════════════════
            // NUTRIENT PROPERTIES — Per 100g serving
            // ════════════════════════════════════════════════════════════

            val nutrients = mutableListOf<NutrientProperty>()
            var npId = 20001L

            data class NutrientEntry(
                val foodId: Long,
                val kcal: Double, val proteinG: Double, val carbsG: Double,
                val fatG: Double, val fiberG: Double, val sodiumMg: Double
            )

            val nutrientData = listOf(
                // Romazava
                NutrientEntry(10001, 310.0, 28.0, 14.0, 15.0, 3.5, 620.0),
                // Ravitoto sy henakisoa
                NutrientEntry(10002, 480.0, 22.0, 12.0, 34.0, 6.0, 850.0),
                // Vary amin'anana
                NutrientEntry(10003, 260.0, 9.0, 45.0, 5.0, 2.0, 340.0),
                // Mofo gasy
                NutrientEntry(10004, 220.0, 4.0, 42.0, 3.5, 1.0, 180.0),
                // Ranon'ampango
                NutrientEntry(10005, 15.0, 0.3, 3.0, 0.1, 0.0, 2.0),
                // Lasopy
                NutrientEntry(10006, 180.0, 14.0, 16.0, 7.0, 4.0, 580.0),
                // Voanjobory sy henakisoa
                NutrientEntry(10007, 410.0, 26.0, 18.0, 25.0, 5.5, 720.0),
                // Akoho sy voanio
                NutrientEntry(10008, 350.0, 30.0, 8.0, 22.0, 2.5, 490.0),
                // Koba
                NutrientEntry(10009, 290.0, 6.0, 48.0, 9.0, 3.0, 95.0),
                // Salady voatabia
                NutrientEntry(10010, 45.0, 1.5, 7.0, 1.5, 2.0, 180.0),
                // Anana (brèdes)
                NutrientEntry(10011, 35.0, 3.0, 5.0, 0.5, 3.5, 60.0),
                // Vary sosoa (riz blanc)
                NutrientEntry(10012, 130.0, 2.7, 28.0, 0.3, 0.4, 1.0),
                // Mokary
                NutrientEntry(10013, 200.0, 3.5, 38.0, 4.0, 0.5, 120.0),
                // Sakay
                NutrientEntry(10014, 30.0, 1.0, 6.0, 0.5, 1.5, 450.0),
                // Trempage
                NutrientEntry(10015, 240.0, 6.0, 38.0, 7.0, 0.5, 210.0)
            )

            val nutrientNames = mapOf(
                "kcal" to "Energy",
                "protein" to "Protein",
                "carbs" to "Carbohydrate",
                "fat" to "Fat",
                "fiber" to "Fiber",
                "sodium" to "Sodium"
            )

            for (entry in nutrientData) {
                nutrients.addAll(
                    listOf(
                        NutrientProperty(npId++, entry.foodId, "Energy", entry.kcal, "kcal"),
                        NutrientProperty(npId++, entry.foodId, "Protein", entry.proteinG, "g"),
                        NutrientProperty(npId++, entry.foodId, "Carbohydrate", entry.carbsG, "g"),
                        NutrientProperty(npId++, entry.foodId, "Fat", entry.fatG, "g"),
                        NutrientProperty(npId++, entry.foodId, "Fiber", entry.fiberG, "g"),
                        NutrientProperty(npId++, entry.foodId, "Sodium", entry.sodiumMg, "mg")
                    )
                )
            }

            appDatabase.nutrientPropertyDao().insertAll(nutrients)
            Log.i(TAG, "Seeded ${nutrients.size} nutrient properties")

            // ════════════════════════════════════════════════════════════
            // DISH entries — Diet catalog
            // ════════════════════════════════════════════════════════════

            val dishes = listOf(
                Dish(
                    id = "dish-romazava", name = "Romazava",
                    description = "Ragoût de zébu aux brèdes",
                    category = "Plats locaux",
                    calories = 310, proteinGrams = 28.0, carbsGrams = 14.0, fatGrams = 15.0
                ),
                Dish(
                    id = "dish-ravitoto", name = "Ravitoto sy henakisoa",
                    description = "Feuilles de manioc pilées au porc",
                    category = "Plats locaux",
                    calories = 480, proteinGrams = 22.0, carbsGrams = 12.0, fatGrams = 34.0
                ),
                Dish(
                    id = "dish-vary-anana", name = "Vary amin'anana",
                    description = "Riz aux herbes et légumes",
                    category = "Plats locaux",
                    calories = 260, proteinGrams = 9.0, carbsGrams = 45.0, fatGrams = 5.0
                ),
                Dish(
                    id = "dish-mofo-gasy", name = "Mofo gasy",
                    description = "Pain de riz traditionnel",
                    category = "Petit-déjeuner",
                    calories = 220, proteinGrams = 4.0, carbsGrams = 42.0, fatGrams = 3.5
                ),
                Dish(
                    id = "dish-lasopy", name = "Lasopy",
                    description = "Soupe aux légumes variés",
                    category = "Plats locaux",
                    calories = 180, proteinGrams = 14.0, carbsGrams = 16.0, fatGrams = 7.0
                ),
                Dish(
                    id = "dish-voanjobory", name = "Voanjobory sy henakisoa",
                    description = "Légumineuses au porc",
                    category = "Plats locaux",
                    calories = 410, proteinGrams = 26.0, carbsGrams = 18.0, fatGrams = 25.0
                ),
                Dish(
                    id = "dish-akoho-voanio", name = "Akoho sy voanio",
                    description = "Poulet au lait de coco",
                    category = "Plats locaux",
                    calories = 350, proteinGrams = 30.0, carbsGrams = 8.0, fatGrams = 22.0
                ),
                Dish(
                    id = "dish-koba", name = "Koba",
                    description = "Gâteau riz-banane-cacahuète",
                    category = "Snacks",
                    calories = 290, proteinGrams = 6.0, carbsGrams = 48.0, fatGrams = 9.0
                ),
                Dish(
                    id = "dish-salady-voatabia", name = "Salady voatabia",
                    description = "Salade de tomates fraîches",
                    category = "Fruits",
                    calories = 45, proteinGrams = 1.5, carbsGrams = 7.0, fatGrams = 1.5
                ),
                Dish(
                    id = "dish-vary-sosoa", name = "Vary sosoa",
                    description = "Riz blanc cuit",
                    category = "Céréales",
                    calories = 130, proteinGrams = 2.7, carbsGrams = 28.0, fatGrams = 0.3
                ),
                Dish(
                    id = "dish-trempage", name = "Trempage",
                    description = "Pain perdu malgache",
                    category = "Petit-déjeuner",
                    calories = 240, proteinGrams = 6.0, carbsGrams = 38.0, fatGrams = 7.0
                )
            )

            appDatabase.dishDao().insertAll(dishes)
            Log.i(TAG, "Seeded ${dishes.size} dish entries")

            // ════════════════════════════════════════════════════════════
            // RECIPES + INGREDIENTS — For nutrition computation
            // ════════════════════════════════════════════════════════════

            val romazavaRecipeId = UUID.randomUUID().toString()
            val ravitotoRecipeId = UUID.randomUUID().toString()
            val varyAnanaRecipeId = UUID.randomUUID().toString()
            val akohoVoanioRecipeId = UUID.randomUUID().toString()

            val recipes = listOf(
                Recipe(
                    id = romazavaRecipeId, name = "Romazava",
                    description = "Ragoût de zébu aux brèdes",
                    instructions = "1. Faire revenir la viande de zébu dans une marmite.\\n2. Ajouter les oignons et l'ail hachés.\\n3. Verser de l'eau et laisser cuire 30 min.\\n4. Ajouter les brèdes (anana) et laisser mijoter 15 min.\\n5. Saler, poivrer et servir avec du riz blanc.",
                    prepTimeMinutes = 15, cookTimeMinutes = 50, servings = 4
                ),
                Recipe(
                    id = ravitotoRecipeId, name = "Ravitoto sy henakisoa",
                    description = "Feuilles de manioc pilées au porc et lait de coco",
                    instructions = "1. Piler les feuilles de manioc cuites.\\n2. Faire dorer la viande de porc.\\n3. Ajouter le manioc pilé et le lait de coco.\\n4. Laisser mijoter 20 min à feu doux.\\n5. Servir avec du riz.",
                    prepTimeMinutes = 20, cookTimeMinutes = 40, servings = 4
                ),
                Recipe(
                    id = varyAnanaRecipeId, name = "Vary amin'anana",
                    description = "Riz aux herbes et légumes verts",
                    instructions = "1. Laver le riz et le mettre à cuire.\\n2. Hacher finement les brèdes et les herbes.\\n3. Les ajouter au riz en fin de cuisson.\\n4. Assaisonner avec du sel et un filet d'huile.",
                    prepTimeMinutes = 10, cookTimeMinutes = 25, servings = 4
                ),
                Recipe(
                    id = akohoVoanioRecipeId, name = "Akoho sy voanio",
                    description = "Poulet au lait de coco",
                    instructions = "1. Découper le poulet en morceaux.\\n2. Le faire dorer dans une cocotte.\\n3. Ajouter l'oignon, l'ail et le gingembre.\\n4. Verser le lait de coco et laisser mijoter 25 min.\\n5. Servir avec du riz blanc.",
                    prepTimeMinutes = 15, cookTimeMinutes = 35, servings = 4
                )
            )

            appDatabase.recipeDao().insertAll(recipes)
            Log.i(TAG, "Seeded ${recipes.size} recipes")

            // ════════════════════════════════════════════════════════════
            // RECIPE INGREDIENTS
            // ════════════════════════════════════════════════════════════

            val recipeIngredients = listOf(
                // Romazava ingredients
                RecipeIngredient(UUID.randomUUID().toString(), romazavaRecipeId, "10001", "Viande de zébu", 200.0, "g"),
                RecipeIngredient(UUID.randomUUID().toString(), romazavaRecipeId, "10011", "Brèdes (anana)", 150.0, "g"),
                RecipeIngredient(UUID.randomUUID().toString(), romazavaRecipeId, "10012", "Riz blanc", 150.0, "g"),
                RecipeIngredient(UUID.randomUUID().toString(), romazavaRecipeId, null, "Oignon", 50.0, "g"),
                // Ravitoto ingredients
                RecipeIngredient(UUID.randomUUID().toString(), ravitotoRecipeId, "10002", "Feuilles de manioc", 200.0, "g"),
                RecipeIngredient(UUID.randomUUID().toString(), ravitotoRecipeId, null, "Viande de porc", 150.0, "g"),
                RecipeIngredient(UUID.randomUUID().toString(), ravitotoRecipeId, null, "Lait de coco", 100.0, "ml"),
                // Vary amin'anana ingredients
                RecipeIngredient(UUID.randomUUID().toString(), varyAnanaRecipeId, "10012", "Riz", 200.0, "g"),
                RecipeIngredient(UUID.randomUUID().toString(), varyAnanaRecipeId, "10011", "Brèdes", 100.0, "g"),
                RecipeIngredient(UUID.randomUUID().toString(), varyAnanaRecipeId, null, "Herbes fraîches", 20.0, "g"),
                // Akoho sy voanio ingredients
                RecipeIngredient(UUID.randomUUID().toString(), akohoVoanioRecipeId, "10008", "Poulet", 250.0, "g"),
                RecipeIngredient(UUID.randomUUID().toString(), akohoVoanioRecipeId, null, "Lait de coco", 200.0, "ml"),
                RecipeIngredient(UUID.randomUUID().toString(), akohoVoanioRecipeId, null, "Gingembre", 10.0, "g")
            )

            appDatabase.recipeIngredientDao().insertAll(recipeIngredients)
            Log.i(TAG, "Seeded ${recipeIngredients.size} recipe ingredients")

            Log.i(TAG, "Database seeding complete — ${foodItems.size} foods, ${nutrients.size} nutrients, ${dishes.size} dishes, ${recipes.size} recipes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed database: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "SeedData"
    }
}
