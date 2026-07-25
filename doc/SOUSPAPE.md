# Tomady Nutrition — Architecture & Conventions

## Overview

Tomady is a **headless Android application** powering a Health Coach experience.
It has no user interface — all interactions happen through the **React Native bridge**
modules exposed to a React Native host application.

---

## Package Architecture

```
com.tomady.nutrition/
├── bridge/          # React Native native modules (RN → native bridge)
├── data/
│   ├── local/
│   │   ├── diet/    # Diet domain Room entities & DAOs
│   │   └── foodb/   # FooDB food/compound Room entities & DAOs
│   └── AppDatabase.kt
├── service/
│   ├── diet/        # Diet planning & recommendation logic
│   ├── foodb/       # FooDB data access & parsing
│   └── gemma/       # Gemma AI/ML inference service
└── worker/          # WorkManager background jobs
```

## Data Layer

### Diet Database (`data.local.diet`)

User-centric domain tables for personal health tracking:

| Entity           | Purpose                                      |
|------------------|----------------------------------------------|
| `User`           | Authenticated user account                   |
| `Profile`        | Extended user profile (goals, metrics)       |
| `BioRecord`      | Biometric time-series (weight, body fat, BP) |
| `Dish`           | Catalog of known dishes/meals                |
| `Recipe`         | Full recipe with instructions                |
| `RecipeIngredient`| Links recipes → food items with quantities  |
| `DishHistory`    | User's meal log (what, when, how much)       |

### FooDB Database (`data.local.foodb`)

Read-only mirror of the FooDB food composition schema.

| Entity             | Source Table          | Purpose                          |
|--------------------|-----------------------|----------------------------------|
| `FoodItem`         | `food`                | Food catalog entries             |
| `NutrientProperty` | `content` / `nutrient`| Nutrient/compound content values |

**Schema reference:** `init_ressources/foodb_generated_schema_only.sql`

Full FooDB tables (compound, flavor, enzyme, pathway, healtheffect, etc.) are
available for extension but are not yet mapped to Room entities.

## Service Layer

### `GemmaAndroidService`
On-device inference via Google Gemma. Runs quantized models for:
- Natural language meal descriptions → structured nutrition data
- Personalized meal suggestions based on user bio records

### `DietAPIService`
Business logic for diet planning:
- Daily calorie/macro targets from profile + bio records
- Meal plan generation and optimization

### `FooDBDataAPIService`
Data access facade over the local FooDB Room database:
- Search foods by name, group, nutrient profile
- Look up nutrient values for a given food

## Worker Layer

### `DailySuggestionWorker`
Scheduled daily (via WorkManager `PeriodicWorkRequest`) to:
- Check today's bio records vs. goals
- Generate a personalized suggestion via Gemma
- Expose result through bridge module for RN to display

## React Native Bridge

Three native modules expose functionality to the RN host:

| Module          | Exposes                                     |
|-----------------|---------------------------------------------|
| `GemmaModule`   | `analyzeMeal(text)`, `suggestMeal(profile)` |
| `DietModule`    | `getDailyPlan(userId)`, `logMeal(data)`     |
| `FooDBModule`   | `searchFood(query)`, `getNutrients(foodId)` |

## Key Decisions

1. **Headless**: No Activities, Fragments, or Compose UI. RN host provides all UI.
2. **Two databases**: Diet DB (WAL-backed, user-writable) and FooDB DB (read-only, shipped or downloaded).
3. **Gemma on-device**: Privacy-first; no user data leaves the device for AI inference.
4. **WorkManager for scheduling**: Reliable background work even when app is killed.
5. **KSP over KAPT**: Faster annotation processing for Room.
