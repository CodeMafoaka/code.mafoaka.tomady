package com.tomady.nutrition.service.nutrition

/**
 * A pluggable source of macro-nutrient values for a food, looked up by name.
 *
 * FooDB (see [com.tomady.nutrition.service.foodb.FooDBRemoteSyncService]) turned
 * out to carry almost no usable calorie/protein/carb/fat data — 5.69M `content`
 * rows, only 39 referencing a standard `nutrient` row, and none of those 39
 * carry an actual value. It's a compound/phytochemical composition database,
 * not a macro-nutrition one. This interface exists so the macro data source
 * (currently USDA FoodData Central) can be swapped or supplemented later
 * (e.g. if a usable FooDB path is ever found) without touching callers —
 * see [NutritionLookupService].
 */
interface NutrientDataProvider {
    /** Short identifier, e.g. "usda_fdc" — also used as the `nutrition.provider` config value. */
    val id: String

    /** Looks up macros for a food by free-text name. Returns null if no match or on failure. */
    suspend fun getMacros(foodName: String): FoodMacros?
}

/**
 * Macro-nutrient values for a food, as resolved by some [NutrientDataProvider].
 * All nutrient fields are per the provider's native serving basis (USDA FDC:
 * per 100g for Foundation/SR Legacy foods, per labeled serving for Branded).
 */
data class FoodMacros(
    val source: String,
    val matchedName: String,
    val calories: Double? = null,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
    val fiberG: Double? = null
)
