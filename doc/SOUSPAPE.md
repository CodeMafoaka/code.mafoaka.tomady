# Architecture & Development Guidelines (SOUSPAPE)

This document details the architectural decisions, design patterns, and instructions for working on the `nutrition` headless Android application codebase.

---

## 1. Architectural Overview

The application utilizes a clean, decoupled architecture partitioned into two primary databases and multiple background/bridging layers to build a high-performance headless Android service.

### 1.1 Package Directory Structure

The codebase is organized under the root package `com.tomady.nutrition`:

*   **`data.local.diet`**: Contains the Room entities, DAO, and Database configurations for the core user, profile, recipes, dishes, and diet histories.
*   **`data.local.foodb`**: Holds the comprehensive FooDB representation. It includes 23 Room entities representing the complete SQL schema of FooDB (derived from the `init_ressources/foodb_generated_schema_only.sql` dump file).
*   **`service.gemma`**: Skeletons and interfaces for the local Gemma model-serving and prompt validation logic.
*   **`service.diet`**: Core logic to compute recipes, validate payloads, query diet databases, and manage profiles.
*   **`service.foodb`**: Data access services to manage and search food-related nutrients and compound data across local/full FooDB databases.
*   **`worker`**: Background Tasks (such as WorkManager's `DailySuggestionWorker`) for recurring tasks.
*   **`bridge`**: React Native bridging modules (`GemmaModule`, `DietModule`, `FooDBModule`) and the registration package (`NutritionBridgePackage`).

---

## 2. Core Databases Design

### 2.1 Diet Database (Local & Personal)
The `DietDatabase` contains structural entities to track user information, current diet profiles, and cooking data:
*   `User`: Represents the primary user.
*   `Profile`: Stores user goals, allergen checklists, and specific conditions.
*   `BioRecord`: Tracks weight/height timeline records.
*   `Dish`: Individual meals, configured to map variants and reference generated recipe states.
*   `Recipe`: Set of actions or formulas describing a dish.
*   `RecipeIngredient`: Mapping table for specific foods/nutrients used in a Recipe, with ingredient-level processing status (e.g., boiled, fried, raw).
*   `DishHistory`: Event timeline mapping which user ate what dish, when, and matching portions.
*   `FoodItem`: Identifies foods mapped from FooDB.
*   `NutrientProperty`: Nutrient traits associated with food or recipes.

### 2.2 FooDB (Local vs Full)
The FooDB schema is large and identical between **FooDB Full** and **FooDB Local**.
*   **FooDB Full**: A full read-only database containing the complete food, compound, and nutrient mapping dataset compiled from `foodb.ca`.
*   **FooDB Local**: A subset containing only the rows representing actually consumed or queried foods, allowing the overall Android app size to remain compact while keeping schema-level compatibility.
*   All 23 FooDB entities (e.g., `Compound`, `Content`, `Nutrient`, `Food`, `Reference`, etc.) are mapped to corresponding Room structures under `com.tomady.nutrition.data.local.foodb`.

---

## 3. Mock & Stub Environment

To facilitate standard JDK-based developer compilation (offline and speed-oriented) without demanding heavy Android Gradle Plugin (AGP) toolchains or Android emulators:
- Custom mock class frameworks are declared in-source under package roots matching standard Android/React Native signatures (e.g., `androidx.room.*`, `androidx.work.*`, `com.facebook.react.*`).
- **Rule:** When migrating this project into a concrete production Android application project, replace these local stub source paths with actual Gradle build dependencies:
  ```groovy
  implementation "androidx.room:room-runtime:2.6.1"
  implementation "androidx.work:work-runtime:2.9.0"
  implementation "com.facebook.react:react-android:+"
  ```

---

## 4. Workflows and Quality Control

- **Pre-commit Checks:** Always verify source-compilability and run unit test configurations using:
  ```bash
  ./gradlew clean test
  ```
- **Commit Strategy:** Write clean commits under Conventional Commits. Separate unrelated tasks into individual commits to simplify reviews and rollbacks.
