# Tomady Project Review

This document provides a comprehensive technical overview and architecture review of the **Tomady** project. It outlines the design goals, headless architectural pattern, database structures, service design, background processing, API layers, and coding conventions established within this repository.

---

## 1. Project Overview & Architecture

Tomady is a privacy-first, headless **Android application** designed to power an intelligent Health Coach experience. Instead of maintaining its own native UI (Activities, Fragments, Jetpack Compose layouts), it primarily operates in a **headless mode** or through an embedded web client.

Key structural components include:
- **Headless Application:** Designed to expose core nutrition services, planning modules, and AI endpoints to external clients (such as a React Native host application or network-connected devices) rather than directly serving users with a local native GUI.
- **Hybrid Native/Web Demo:** A demo interface is provided via `DemoActivity` (under the `demo` flavor/package) which runs a WebView loading a local `demo.html` dashboard. It connects to the underlying local services by exposing a native JS interface (`DemoJSBridge`) and performing direct HTTP requests.
- **Embedded HTTP Microservices:** Relies on local embedded HTTP servers (utilizing NanoHTTPD) to expose full OpenAPI-compliant REST APIs over localhost port `7777`. This replaces or complements legacy React Native bridges, enabling seamless, decoupled API access.

---

## 2. Technical Stack & Build Setup

The project is built on the following technologies and dependencies configured in `build.gradle.kts` and `gradle.properties`:
- **Language & Runtime:** Kotlin compiled for JDK 17 (Java 17 compatibility, JVM target `17`). Android Compile SDK and Target SDK are set to `34`, with a Minimum SDK of `26` (Android 8.0).
- **Gradle & Compilation:** Utilizes Gradle for dependency resolution. It supports annotation processing with **KSP** (Kotlin Symbol Processing) instead of KAPT for rapid builds.
- **Multi-Flavor Build Configuration:** Divided into three product flavors:
  - `services`: Headless flavor implementing background servers and worker tasks.
  - `demo`: Hybrid flavor with a WebView-based test/demo dashboard.
  - `full`: A complete build packaging both headless services and the demo components.
- **Key Libraries:**
  - **Jetpack Room (2.6.1):** SQLite object mapping library.
  - **Jetpack WorkManager (2.9.0):** Background job scheduler.
  - **NanoHTTPD (2.3.1):** Lightweight, embedded HTTP server.
  - **MediaPipe LLM Inference (0.10.33):** Google MediaPipe GenAI library for running on-device localized models (such as Google Gemma).
  - **Gson (2.10.1):** Serialization and deserialization of JSON objects.
  - **React Native (0.73.0 - `compileOnly`):** Provides base definitions to export React Native Bridge modules.

---

## 3. Package Structure

The package directory under `com.tomady.nutrition/` is modularized into specialized directories:

```
com.tomady.nutrition/
├── TomadyApp.kt     # Application class initializing the servers, Room DBs, and worker schedules
├── bridge/          # Native modules mapping core services to the React Native bridge
│   ├── BridgeUtils.kt
│   ├── DietModule.kt
│   ├── FooDBModule.kt
│   ├── GemmaModule.kt
│   └── TomadyBridgePackage.kt
├── data/            # Local data storage mechanisms and schema contexts
│   ├── local/
│   │   ├── diet/    # Personal bio and user history Room entities and DAOs
│   │   └── foodb/   # Food composition references based on the FooDB schema
│   ├── AppDatabase.kt     # Shared Room database instance managing diet and food tables
│   └── SeedDataCallback.kt# Seeds databases with mock/starter rows upon creation
├── demo/            # Local dashboard code
│   ├── DemoActivity.kt    # Loads demo.html and handles notification permissions (API 33+)
│   └── DemoJSBridge.kt    # Exposes a bridge API to the web-based demo view
├── server/          # REST server lifecycle wrapper and services
│   ├── TomadyRestApiServer.kt # Main NanoHTTPD implementation defining endpoints and routing
│   └── TomadyServerService.kt # Foreground Service keeping the REST API server alive
├── service/         # Internal business and domain logic
│   ├── diet/        # DietAPIService (target calculations, meal validation, summaries)
│   ├── foodb/       # FooDBDataAPIService (local food search, details retrieval)
│   ├── gemma/       # GemmaAndroidService (local Gemma inference, mock fallback, downloading)
│   └── http/        # Decoupled server instances and dependency injection modules
└── worker/          # WorkManager-scheduled background execution jobs
    ├── DailySuggestionWorker.kt # Formulates customized daily nutrition plans
    └── SuggestionEventBus.kt    # Distributes suggestion events across the application
```

---

## 4. Data Layer & Databases

Tomady leverages **Room Database** (`AppDatabase.kt`) configured with version `3`. It aggregates two distinctive logical databases under a single physical framework:

### A. Diet Database Context (`data.local.diet`)
A writable, WAL-backed database capturing personal health metrics and dietary historical trends.
- **User:** Primary user profiles.
- **Profile:** Health goals (e.g., target macro distribution, calorie limits, allergies, medical exclusions).
- **BioRecord:** Time-series logs of body weight, fat percentages, body temperature, blood pressure, and daily notes.
- **Dish / Recipe / RecipeIngredient:** Catalogs meal templates and ingredients.
- **DishHistory:** Logs specific food items consumed at particular times of the day, servings consumed, and related meal tags.

### B. FooDB Database Context (`data.local.foodb`)
A read-only local database mirroring standard FooDB composition properties to evaluate food components.
- **FoodItem:** Map of foods with taxonomic groups, names, subgroups, and category descriptions.
- **NutrientProperty:** Content properties detailing the nutrient profile, unit of measurement, and content values associated with individual foods.
- **Referential SQL:** Initial database schemas and reference rules are documented in `init_ressources/foodb_generated_schema_only.sql`.

---

## 5. REST API Services & NanoHTTPD Server

Tomady transitions from native React Native bridges to an **OpenAPI-driven REST architecture** by running local HTTP endpoints on port `7777`. This server runs within an Android foreground service (`TomadyServerService`), ensuring consistent execution across the local network.

### Primary Endpoints (documented in `openapi.yaml` & `TomadyRestApiServer.kt`)

#### 📡 Health Check
- `GET /api/v1/health` - Check service health, IP configurations, and Gemma loading states.

#### 🔍 Food Lookup (FooDB)
- `GET /v1/food/search?q=<query>` - Searches matching raw foods or scientific classifications.
- `GET /v1/food/{foodId}` - Retrieves a food item along with its nutrient properties.

#### 🥗 Diet Planning & Profiles
- `GET /v1/users/{userId}/profile` - Fetches the personal target profile.
- `PUT /v1/users/{userId}/profile` - Updates personal goal vectors, height/weight metrics, and dietary exclusions.
- `POST /v1/users/{userId}/biorecords` - Logs height and weight readings.
- `GET /v1/users/{userId}/history` - Retrieves log lists filtered by start/end dates.
- `POST /v1/users/{userId}/history` - Logs new dish consumption history.
- `GET /v1/dishes/{dishId}/nutrition` - Computes accumulated micro/macro targets of compound dishes.

#### 🤖 Local Gemma AI Inference
- `POST /v1/gemma/compute-recipe` - Generates custom recipes tailored to user constraints.
- `POST /v1/gemma/ask` - Submits a prompt to the local LLM.
- `GET /v1/gemma/model` - Queries loading progress and check mock configurations.
- `POST /v1/gemma/download` - Downloads the quantized Gemma file in the background.

---

## 6. Gemma LLM & WorkManager Integration

### On-Device GenAI (`GemmaAndroidService`)
Utilizes on-device LLMs via **MediaPipe LLM Inference** to process natural language queries while keeping all data private:
- It supports background downloding and loading of the model binary.
- Offers a **Mock Fallback** strategy if the real model file is not yet cached or if resources are constrained, ensuring the API remains functional.
- Implements distinct routines for generic questions (`askQuestion`) and structure-aware recipes (`computeRecipe`).

### Scheduled Workers (`DailySuggestionWorker`)
Integrated via Android's **WorkManager** to trigger background jobs:
- Calculates current calories/macros against personal goals.
- Generates tailored daily suggestions based on recent bio records.
- Communicates updates via a centralized `SuggestionEventBus` and makes them available to the user interface.

---

## 7. Developer Conventions & Rules

All development within the Tomady codebase must follow the standards outlined in `.clinerules`:

### A. Conventional Commits (v1.0.0)
Commits must use exact structure prefixes:
- **Types:** `feat` (new feature), `fix` (bug fix), `docs` (documentation), `style` (formatting), `refactor` (refactoring), `perf` (performance), `test` (tests), `chore` (build/dependency tools), `config` (configurations).
- **Scopes:** `db`, `diet`, `foodb`, `gemma`, `worker`, `bridge`, `docs`, `build`.

### B. Coding Style
- All source files are written in **Kotlin**.
- Strict casing rules: `camelCase` for variables and functions, `PascalCase` for classes and interfaces.
- Clear KDoc commentary required on all public classes and interfaces.
- Empty implementations/stubs should return clean default values without complex inline processing.
