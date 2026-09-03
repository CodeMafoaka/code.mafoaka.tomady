package com.tomady.nutrition.service.diet

import com.tomady.nutrition.data.local.diet.DietDatabase
import com.tomady.nutrition.data.local.diet.entity.BioRecord
import com.tomady.nutrition.data.local.diet.entity.Dish
import com.tomady.nutrition.data.local.diet.entity.DishHistory
import com.tomady.nutrition.data.local.diet.entity.Profile
import com.tomady.nutrition.data.local.diet.entity.Recipe
import com.tomady.nutrition.data.local.diet.entity.RecipeIngredient
import com.tomady.nutrition.data.local.diet.entity.User
import com.tomady.nutrition.service.foodb.FooDBDataAPIService
import com.tomady.nutrition.service.nutrition.NutritionLookupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Service providing diet-planning business logic.
 *
 * Features:
 * - Complete CRUD operations for [User], [Profile], [BioRecord], [Dish],
 *   [Recipe], [RecipeIngredient], and [DishHistory]
 * - Nutritional value computation for a [Dish] by aggregating its [RecipeIngredient]
 *   entries via [NutritionLookupService] (name-based macro lookup — see
 *   [computeDishNutrition] for why this isn't [FooDBDataAPIService])
 * - Profile-based health validation (e.g., flagging high-sugar dishes for diabetes)
 * - Daily calorie/macro targets computed from [Profile] and [BioRecord]
 *
 * @param dietDatabase  Wrapper around the diet Room database DAOs.
 * @param foodbService  FooDB service — used to resolve an ingredient's name
 * from its catalogue id when [RecipeIngredient.name] isn't set directly.
 * @param nutritionLookupService Pluggable macro-nutrient lookup, used to
 * price out each recipe ingredient by name.
 */
class DietAPIService(
    private val dietDatabase: DietDatabase,
    private val foodbService: FooDBDataAPIService,
    private val nutritionLookupService: NutritionLookupService
) {

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 1 — User CRUD
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Creates a new [User] account.
     *
     * @param id       Unique user identifier.
     * @param username Display name.
     * @param email    Email address (must be unique).
     * @return The newly created [User].
     */
    suspend fun createUser(id: String, username: String, email: String): User = withContext(Dispatchers.IO) {
        val user = User(
            id = id,
            username = username,
            email = email
        )
        dietDatabase.insertUser(user)
        user
    }

    /**
     * Retrieves a [User] by their primary key.
     */
    suspend fun getUser(userId: String): User? = withContext(Dispatchers.IO) {
        dietDatabase.getUserById(userId)
    }

    /**
     * Retrieves a [User] by email address.
     */
    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        dietDatabase.getUserByEmail(email)
    }

    /**
     * Updates an existing [User]'s mutable fields (username, email).
     */
    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        dietDatabase.updateUser(user)
    }

    /**
     * Deletes a [User] and all associated records (cascading).
     */
    suspend fun deleteUser(userId: String) = withContext(Dispatchers.IO) {
        val user = dietDatabase.getUserById(userId) ?: return@withContext
        dietDatabase.deleteUser(user)
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 2 — Profile CRUD
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Creates or replaces a [Profile] for the given user.
     */
    suspend fun createProfile(
        userId: String,
        displayName: String? = null,
        dateOfBirth: String? = null,
        heightCm: Double? = null,
        weightKg: Double? = null,
        dailyCalorieTarget: Int? = null,
        proteinGramsTarget: Int? = null,
        carbsGramsTarget: Int? = null,
        fatGramsTarget: Int? = null,
        goal: String? = null,
        age: Int? = null,
        activityLevel: String? = null,
        allergies: String? = null,
        intolerances: String? = null,
        conditions: String? = null,
        restrictedFoods: String? = null,
        forbiddenByDoctor: String? = null
    ): Profile = withContext(Dispatchers.IO) {
        val profile = Profile(
            id = UUID.randomUUID().toString(),
            userId = userId,
            displayName = displayName,
            dateOfBirth = dateOfBirth,
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            dailyCalorieTarget = dailyCalorieTarget,
            proteinGramsTarget = proteinGramsTarget,
            carbsGramsTarget = carbsGramsTarget,
            fatGramsTarget = fatGramsTarget,
            goal = goal,
            activityLevel = activityLevel,
            allergies = allergies,
            intolerances = intolerances,
            conditions = conditions,
            restrictedFoods = restrictedFoods,
            forbiddenByDoctor = forbiddenByDoctor
        )
        dietDatabase.insertProfile(profile)
        profile
    }

    /**
     * Retrieves the [Profile] for a given user.
     */
    suspend fun getProfile(userId: String): Profile? = withContext(Dispatchers.IO) {
        dietDatabase.getProfileByUserId(userId)
    }

    /**
     * Updates an existing [Profile].
     */
    suspend fun updateProfile(profile: Profile) = withContext(Dispatchers.IO) {
        dietDatabase.updateProfile(profile)
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 3 — BioRecord CRUD
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Records a biometric measurement for a user on a given date.
     */
    suspend fun recordBio(
        userId: String,
        date: String,
        weightKg: Double? = null,
        bodyFatPercentage: Double? = null,
        systolicBp: Int? = null,
        diastolicBp: Int? = null,
        notes: String? = null
    ): BioRecord = withContext(Dispatchers.IO) {
        val record = BioRecord(
            id = UUID.randomUUID().toString(),
            userId = userId,
            date = date,
            weightKg = weightKg,
            bodyFatPercentage = bodyFatPercentage,
            systolicBp = systolicBp,
            diastolicBp = diastolicBp,
            notes = notes
        )
        dietDatabase.insertBioRecord(record)
        record
    }

    /**
     * Retrieves a bio record for a specific user and date.
     */
    suspend fun getBioRecord(userId: String, date: String): BioRecord? = withContext(Dispatchers.IO) {
        dietDatabase.getBioRecordByUserAndDate(userId, date)
    }

    /**
     * Retrieves bio records for a user within a date range.
     */
    suspend fun getBioRecordsInRange(
        userId: String,
        startDate: String,
        endDate: String
    ): List<BioRecord> = withContext(Dispatchers.IO) {
        dietDatabase.getBioRecordsByUserInRange(userId, startDate, endDate)
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 4 — Dish / Recipe CRUD
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Creates a new [Dish] in the catalog.
     */
    suspend fun createDish(
        name: String,
        description: String? = null,
        category: String? = null,
        imageUrl: String? = null,
        calories: Int? = null,
        proteinGrams: Double? = null,
        carbsGrams: Double? = null,
        fatGrams: Double? = null
    ): Dish = withContext(Dispatchers.IO) {
        val dish = Dish(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            category = category,
            imageUrl = imageUrl,
            calories = calories,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams
        )
        dietDatabase.insertDish(dish)
        dish
    }

    /**
     * Retrieves a [Dish] by its ID.
     */
    suspend fun getDish(dishId: String): Dish? = withContext(Dispatchers.IO) {
        dietDatabase.getDishById(dishId)
    }

    /**
     * Searches dishes by name or description.
     */
    suspend fun searchDishes(query: String): List<Dish> = withContext(Dispatchers.IO) {
        dietDatabase.searchDishes(query)
    }

    /**
     * Creates a [Recipe] and optionally its [RecipeIngredient] entries.
     *
     * @param dishId The [Dish] this recipe composes — pass the id returned by
     * [createDish] so [computeDishNutrition] can find it again by FK instead
     * of a fragile name match.
     */
    suspend fun createRecipe(
        name: String,
        dishId: String? = null,
        description: String? = null,
        instructions: String? = null,
        prepTimeMinutes: Int? = null,
        cookTimeMinutes: Int? = null,
        servings: Int? = null,
        imageUrl: String? = null,
        ingredients: List<RecipeIngredient> = emptyList()
    ): Recipe = withContext(Dispatchers.IO) {
        val recipe = Recipe(
            id = UUID.randomUUID().toString(),
            dishId = dishId,
            name = name,
            description = description,
            instructions = instructions,
            prepTimeMinutes = prepTimeMinutes,
            cookTimeMinutes = cookTimeMinutes,
            servings = servings,
            imageUrl = imageUrl
        )
        dietDatabase.insertRecipe(recipe)
        if (ingredients.isNotEmpty()) {
            val updatedIngredients = ingredients.map { it.copy(recipeId = recipe.id) }
            dietDatabase.insertRecipeIngredients(updatedIngredients)
        }
        recipe
    }

    /**
     * Adds a [RecipeIngredient] to an existing recipe.
     */
    suspend fun addRecipeIngredient(
        recipeId: String,
        foodItemId: String? = null,
        name: String? = null,
        quantity: Double? = null,
        unit: String? = null
    ): RecipeIngredient = withContext(Dispatchers.IO) {
        val ingredient = RecipeIngredient(
            id = UUID.randomUUID().toString(),
            recipeId = recipeId,
            foodItemId = foodItemId,
            name = name,
            quantity = quantity,
            unit = unit
        )
        dietDatabase.insertRecipeIngredient(ingredient)
        ingredient
    }

    /**
     * Retrieves all ingredients for a recipe.
     */
    suspend fun getRecipeIngredients(recipeId: String): List<RecipeIngredient> = withContext(Dispatchers.IO) {
        dietDatabase.getIngredientsByRecipe(recipeId)
    }

    /**
     * Retrieves a [Recipe] by its ID.
     */
    suspend fun getRecipe(recipeId: String): Recipe? = withContext(Dispatchers.IO) {
        dietDatabase.getRecipeById(recipeId)
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 5 — DishHistory / Meal Logging
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Logs a meal (dish consumption) for a user.
     *
     * @param time Time of day it was eaten, "HH:mm" — not every user eats on a
     * fixed meal schedule, so this is tracked instead of/alongside [mealType].
     * When [mealType] isn't given but [time] is, a bucket label is derived
     * from it so grouping (daily summary, dashboard) still has something to
     * group by.
     * @return The created [DishHistory] entry.
     */
    suspend fun logMealConsumption(
        userId: String,
        dishId: String?,
        date: String,
        mealType: String? = null,
        time: String? = null,
        servings: Double = 1.0,
        notes: String? = null
    ): DishHistory = withContext(Dispatchers.IO) {
        val history = DishHistory(
            id = UUID.randomUUID().toString(),
            userId = userId,
            dishId = dishId,
            date = date,
            mealType = mealType ?: time?.let { mealTypeBucketForTime(it) },
            time = time,
            servings = servings,
            notes = notes
        )
        dietDatabase.insertDishHistory(history)
        history
    }

    /** Buckets a "HH:mm" time into a coarse meal-type label for grouping/display. */
    private fun mealTypeBucketForTime(time: String): String {
        val hour = time.substringBefore(":").toIntOrNull() ?: return "Repas"
        return when (hour) {
            in 5..10 -> "Petit-déjeuner"
            in 11..14 -> "Déjeuner"
            in 15..18 -> "Collation"
            else -> "Dîner"
        }
    }

    /**
     * Retrieves meal history for a user on a specific date.
     */
    suspend fun getDishHistoryByDate(userId: String, date: String): List<DishHistory> = withContext(Dispatchers.IO) {
        dietDatabase.getDishHistoryByUserAndDate(userId, date)
    }

    /**
     * Returns the [limit] most recently eaten distinct dishes for a user
     * (most recent first), for quick re-logging without typing a search.
     */
    suspend fun getRecentDistinctDishes(userId: String, limit: Int = 3): List<Dish> = withContext(Dispatchers.IO) {
        val recentEntries = dietDatabase.getRecentDishHistoryByUser(userId, limit * 5)
        val seenDishIds = LinkedHashSet<String>()
        for (entry in recentEntries) {
            entry.dishId?.let { seenDishIds.add(it) }
            if (seenDishIds.size >= limit) break
        }
        seenDishIds.mapNotNull { dietDatabase.getDishById(it) }
    }

    /**
     * Retrieves meal history for a user within a date range.
     */
    suspend fun getDishHistoryInRange(
        userId: String,
        startDate: String,
        endDate: String
    ): List<DishHistory> = withContext(Dispatchers.IO) {
        dietDatabase.getDishHistoryByUserInRange(userId, startDate, endDate)
    }

    /**
     * Retrieves all generated suggestion entries for a specific date.
     */
    suspend fun getDailySuggestions(date: String): List<DishHistory> = withContext(Dispatchers.IO) {
        dietDatabase.getDishHistoryByDateAndMealType(date, "suggestion")
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 6 — Business Logic: Nutrition Computation
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Computes the total nutritional profile of a [Dish] by aggregating the
     * macro values of all [RecipeIngredient] entries across its recipes —
     * this is the primary path: a dish's nutrition comes from what it's made
     * of, not a number typed in once (see [createDish]'s `calories`/etc.
     * params, which are the manual-entry fallback for when no recipe exists).
     *
     * Each ingredient is priced out per 100g: first via [FooDBDataAPIService]
     * if its catalogue entry happens to carry real values (true for the
     * bundled seed/demo dataset, offline and instant), otherwise via
     * [nutritionLookupService] (USDA FoodData Central, by name) — real synced
     * FooDB data turned out to carry no usable calorie/macro values at all
     * (see [com.tomady.nutrition.service.nutrition.NutrientDataProvider]'s
     * kdoc), which is why that fallback exists.
     *
     * @param dishId The ID of the dish to analyze.
     * @return A [NutritionSummary] with aggregated values, or null if the dish
     *         cannot be found.
     */
    suspend fun computeDishNutrition(dishId: String): NutritionSummary? = withContext(Dispatchers.IO) {
        // 1. Resolve the dish
        val dish = dietDatabase.getDishById(dishId) ?: return@withContext null

        // 2. Find recipes linked to this dish by FK
        val recipes = dietDatabase.getRecipesByDishId(dishId)

        // 3. Aggregate macros across all recipe ingredients
        var totalCalories = 0.0
        var totalProteinG = 0.0
        var totalCarbsG = 0.0
        var totalFatG = 0.0
        var totalFiberG = 0.0
        val totalSugarG = 0.0
        val totalSodiumMg = 0.0
        val nutrientDetails = mutableListOf<NutrientDetail>()

        for (recipe in recipes) {
            val ingredients = dietDatabase.getIngredientsByRecipe(recipe.id)
            for (ingredient in ingredients) {
                val priced = priceIngredient(ingredient) ?: continue

                totalCalories += priced.calories
                totalProteinG += priced.proteinG
                totalCarbsG += priced.carbsG
                totalFatG += priced.fatG
                totalFiberG += priced.fiberG

                nutrientDetails.add(
                    NutrientDetail(
                        nutrientName = "calories",
                        amount = priced.calories,
                        unit = "kcal",
                        sourceFoodId = ingredient.foodItemId?.toLongOrNull(),
                        sourceFoodName = priced.matchedName
                    )
                )
            }
        }

        // 4. Use dish-level pre-computed fields as fallback (manual entry, no recipe)
        if (recipes.isEmpty()) {
            dish.calories?.let { totalCalories = it.toDouble() }
            dish.proteinGrams?.let { totalProteinG = it }
            dish.carbsGrams?.let { totalCarbsG = it }
            dish.fatGrams?.let { totalFatG = it }
        }

        NutritionSummary(
            dishId = dishId,
            dishName = dish.name,
            totalCalories = totalCalories,
            totalProteinG = totalProteinG,
            totalCarbsG = totalCarbsG,
            totalFatG = totalFatG,
            totalFiberG = totalFiberG,
            totalSugarG = totalSugarG,
            totalSodiumMg = totalSodiumMg,
            nutrientDetails = nutrientDetails
        )
    }

    /** One [RecipeIngredient] priced out to absolute (already-scaled) macros. */
    private data class PricedIngredient(
        val calories: Double,
        val proteinG: Double,
        val carbsG: Double,
        val fatG: Double,
        val fiberG: Double,
        val matchedName: String
    )

    /**
     * Resolves a [RecipeIngredient]'s macros, scaled to its actual quantity.
     * Tries [FooDBDataAPIService] first (real values for the bundled seed/demo
     * dataset, offline and instant); falls back to [nutritionLookupService]
     * (USDA, by name) since real synced FooDB data has no usable values.
     * Returns null if neither source can price it.
     */
    private suspend fun priceIngredient(ingredient: RecipeIngredient): PricedIngredient? {
        val foodItemId = ingredient.foodItemId?.toLongOrNull()
        val foodDetail = foodItemId?.let { foodbService.getFoodDetails(it) }
        // Ingredient quantity is grams; source macros are per 100g.
        val scale = (ingredient.quantity ?: 100.0) / 100.0

        val fooDbNutrients = foodDetail?.nutrients?.takeIf { list -> list.any { it.amount != null } }
        if (fooDbNutrients != null) {
            var cal = 0.0; var pro = 0.0; var carb = 0.0; var fat = 0.0; var fiber = 0.0
            for (np in fooDbNutrients) {
                val amount = (np.amount ?: 0.0) * scale
                when (np.nutrientName?.lowercase()) {
                    "energy", "calories", "kcal" -> cal += amount
                    "protein" -> pro += amount
                    "carbohydrate", "carbohydrates", "carbs" -> carb += amount
                    "fat", "total fat", "total lipid" -> fat += amount
                    "fiber", "fibre", "dietary fiber" -> fiber += amount
                }
            }
            return PricedIngredient(cal, pro, carb, fat, fiber, foodDetail.food.name ?: ingredient.name ?: "")
        }

        val ingredientName = ingredient.name ?: foodDetail?.food?.name ?: return null
        val macros = nutritionLookupService.getMacros(ingredientName) ?: return null
        return PricedIngredient(
            calories = (macros.calories ?: 0.0) * scale,
            proteinG = (macros.proteinG ?: 0.0) * scale,
            carbsG = (macros.carbsG ?: 0.0) * scale,
            fatG = (macros.fatG ?: 0.0) * scale,
            fiberG = (macros.fiberG ?: 0.0) * scale,
            matchedName = macros.matchedName
        )
    }

    /**
     * Recomputes [dishId]'s nutrition from its recipe (see [computeDishNutrition])
     * and writes the totals onto the [Dish]'s own flat fields, so screens that
     * read `Dish.calories`/etc directly (search results, catalogue lists) show
     * an up-to-date value without recomputing from the recipe on every read.
     */
    suspend fun refreshDishNutritionCache(dishId: String): Dish? = withContext(Dispatchers.IO) {
        val dish = dietDatabase.getDishById(dishId) ?: return@withContext null
        val nutrition = computeDishNutrition(dishId) ?: return@withContext dish
        val updated = dish.copy(
            calories = nutrition.totalCalories.toInt(),
            proteinGrams = nutrition.totalProteinG,
            carbsGrams = nutrition.totalCarbsG,
            fatGrams = nutrition.totalFatG,
            updatedAt = System.currentTimeMillis()
        )
        dietDatabase.updateDish(updated)
        updated
    }

    /**
     * Validates a [Dish] against a user's [Profile] health conditions.
     *
     * Flags potential conflicts such as:
     * - High sugar content for users with "diabetes" in their goal/condition
     * - High sodium for users with hypertension-related goals
     * - Calorie surplus relative to daily target
     *
     * @param dishId   The dish to validate.
     * @param profile  The user's health profile.
     * @return A [ProfileValidationResult] with any warnings.
     */
    suspend fun validateDishForProfile(dishId: String, profile: Profile): ProfileValidationResult = withContext(Dispatchers.IO) {
        val nutrition = computeDishNutrition(dishId)
        val warnings = mutableListOf<String>()

        if (nutrition == null) {
            return@withContext ProfileValidationResult(
                dishId = dishId,
                isCompatible = true,
                warnings = listOf("Unable to compute nutrition; dish may not have recipe data.")
            )
        }

        val goalLower = (profile.goal ?: "").lowercase()

        // Diabetes check: flag high sugar
        if (goalLower.contains("diabetes") || goalLower.contains("diabetic")) {
            if (nutrition.totalSugarG > SUGAR_THRESHOLD_DIABETES_G) {
                warnings.add(
                    "High sugar content (${String.format("%.1f", nutrition.totalSugarG)}g). " +
                            "This exceeds the ${SUGAR_THRESHOLD_DIABETES_G}g recommendation for diabetes management."
                )
            }
        }

        // Hypertension check: flag high sodium
        if (goalLower.contains("hypertension") || goalLower.contains("blood pressure") || goalLower.contains("low sodium")) {
            if (nutrition.totalSodiumMg > SODIUM_THRESHOLD_HYPERTENSION_MG) {
                warnings.add(
                    "High sodium content (${String.format("%.0f", nutrition.totalSodiumMg)}mg). " +
                            "This exceeds the ${SODIUM_THRESHOLD_HYPERTENSION_MG}mg recommendation for blood pressure management."
                )
            }
        }

        // Calorie target check
        profile.dailyCalorieTarget?.let { target ->
            val mealCalories = nutrition.totalCalories
            if (mealCalories > target * MEAL_CALORIE_FRACTION_WARNING) {
                warnings.add(
                    "This dish provides ${String.format("%.0f", mealCalories)} kcal, " +
                            "which is more than ${(MEAL_CALORIE_FRACTION_WARNING * 100).toInt()}% of your daily calorie target ($target kcal)."
                )
            }
        }

        // High fat check
        if (nutrition.totalFatG > FAT_THRESHOLD_HIGH_G) {
            warnings.add(
                "High fat content (${String.format("%.1f", nutrition.totalFatG)}g). " +
                        "Consider lighter alternatives."
            )
        }

        ProfileValidationResult(
            dishId = dishId,
            isCompatible = warnings.isEmpty(),
            warnings = warnings
        )
    }

    /**
     * Computes daily calorie and macronutrient targets based on a user's profile
     * and latest biometric record.
     *
     * Uses the Mifflin-St Jeor equation for BMR when height/weight/age are available,
     * adjusted for activity level and goal (weight loss/maintenance/gain).
     *
     * @param profile   The user's health profile.
     * @param latestBio The most recent bio record (for adjusting targets dynamically).
     * @return A [DailyTargets] result, or null if computation fails.
     */
    suspend fun computeDailyTargets(
        profile: Profile,
        latestBio: BioRecord?
    ): DailyTargets? = withContext(Dispatchers.IO) {
        val bmr = estimateBMR(profile)
            ?: // Fallback to profile targets if we can't compute BMR
            return@withContext DailyTargets(
                calories = profile.dailyCalorieTarget ?: 2000,
                proteinG = profile.proteinGramsTarget ?: 50,
                carbsG = profile.carbsGramsTarget ?: 250,
                fatG = profile.fatGramsTarget ?: 65
            )

        // Adjust calories based on goal
        val goalLower = (profile.goal ?: "").lowercase()
        val activityMultiplier = 1.2 // sedentary default; could be made configurable
        var maintenanceCalories = (bmr * activityMultiplier).toInt()

        val adjustedCalories = when {
            goalLower.contains("lose") || goalLower.contains("weight loss") ->
                (maintenanceCalories - CALORIE_DEFICIT).coerceAtLeast(1200)
            goalLower.contains("gain") || goalLower.contains("bulk") ->
                maintenanceCalories + CALORIE_SURPLUS
            else -> maintenanceCalories
        }

        // Macros: simple split
        val proteinG = (adjustedCalories * PROTEIN_PCT / 4).toInt()
        val carbsG = (adjustedCalories * CARBS_PCT / 4).toInt()
        val fatG = (adjustedCalories * FAT_PCT / 9).toInt()

        DailyTargets(
            calories = adjustedCalories,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG
        )
    }

    /**
     * Computes today's aggregate nutrition summary for a user based on their
     * meal log (DishHistory) for today.
     *
     * @param userId User identifier.
     * @param date   Today's date (yyyy-MM-dd).
     * @return A [DailySummary] or null if no meals logged.
     */
    suspend fun getDailySummary(userId: String, date: String): DailySummary? = withContext(Dispatchers.IO) {
        val historyEntries = dietDatabase.getDishHistoryByUserAndDate(userId, date)
        if (historyEntries.isEmpty()) return@withContext null

        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0
        val mealBreakdowns = mutableListOf<DailySummary.MealBreakdown>()

        // Group by meal type
        val grouped = historyEntries.groupBy { it.mealType ?: "Other" }
        for ((mealType, entries) in grouped) {
            var mealCal = 0.0
            var mealProtein = 0.0
            var mealCarbs = 0.0
            var mealFat = 0.0

            for (entry in entries) {
                val dishId = entry.dishId ?: continue
                val nutrition = computeDishNutrition(dishId)
                if (nutrition != null) {
                    val scale = entry.servings
                    mealCal += nutrition.totalCalories * scale
                    mealProtein += nutrition.totalProteinG * scale
                    mealCarbs += nutrition.totalCarbsG * scale
                    mealFat += nutrition.totalFatG * scale
                }
            }

            totalCalories += mealCal
            totalProtein += mealProtein
            totalCarbs += mealCarbs
            totalFat += mealFat

            mealBreakdowns.add(
                DailySummary.MealBreakdown(
                    mealType = mealType,
                    totalCalories = mealCal,
                    totalProteinG = mealProtein,
                    totalCarbsG = mealCarbs,
                    totalFatG = mealFat
                )
            )
        }

        DailySummary(
            userId = userId,
            date = date,
            totalCalories = totalCalories,
            totalProteinG = totalProtein,
            totalCarbsG = totalCarbs,
            totalFatG = totalFat,
            meals = mealBreakdowns
        )
    }

    // ── BMR Estimation (Mifflin-St Jeor) ────────────────────────────────

    private fun estimateBMR(profile: Profile): Double? {
        val weight = profile.weightKg ?: return null
        val height = profile.heightCm ?: return null
        // Age not in profile yet; default to 30 if not available
        val age = 30

        // Mifflin-St Jeor: BMR = 10 * weight(kg) + 6.25 * height(cm) - 5 * age + S
        // S = +5 for males, -161 for females (default to female for safety)
        return 10.0 * weight + 6.25 * height - 5.0 * age - 161.0
    }

    companion object {
        // Thresholds
        private const val SUGAR_THRESHOLD_DIABETES_G = 30.0
        private const val SODIUM_THRESHOLD_HYPERTENSION_MG = 800.0
        private const val FAT_THRESHOLD_HIGH_G = 30.0
        private const val MEAL_CALORIE_FRACTION_WARNING = 0.4

        // Calorie adjustment
        private const val CALORIE_DEFICIT = 500
        private const val CALORIE_SURPLUS = 300

        // Macro split percentages
        private const val PROTEIN_PCT = 0.30
        private const val CARBS_PCT = 0.40
        private const val FAT_PCT = 0.30
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Data classes for return types
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Aggregated nutritional information for a dish.
 */
data class NutritionSummary(
    val dishId: String,
    val dishName: String,
    val totalCalories: Double,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val totalFiberG: Double = 0.0,
    val totalSugarG: Double = 0.0,
    val totalSodiumMg: Double = 0.0,
    val nutrientDetails: List<NutrientDetail> = emptyList()
)

/**
 * Individual nutrient contribution from a single ingredient.
 */
data class NutrientDetail(
    val nutrientName: String,
    val amount: Double,
    val unit: String,
    val sourceFoodId: Long?,
    val sourceFoodName: String
)

/**
 * Result of validating a dish against a user's health profile.
 */
data class ProfileValidationResult(
    val dishId: String,
    val isCompatible: Boolean,
    val warnings: List<String>
)

/**
 * Daily calorie and macronutrient targets for a user.
 */
data class DailyTargets(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)

/**
 * Daily nutrition summary for a user, broken down by meal.
 */
data class DailySummary(
    val userId: String,
    val date: String,
    val totalCalories: Double,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val meals: List<MealBreakdown>
) {
    data class MealBreakdown(
        val mealType: String,
        val totalCalories: Double,
        val totalProteinG: Double,
        val totalCarbsG: Double,
        val totalFatG: Double
    )
}
