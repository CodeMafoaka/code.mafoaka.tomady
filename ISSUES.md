# Tomady Issues, Limitations & Technical Gaps

This document catalogues identified stubs, fallbacks, architectural limitations, potential bugs, and edge cases discovered during the analysis of the **Tomady** codebase. It provides developers and maintainers with a prioritized overview of areas requiring optimization, hardening, or implementation.

---

## 1. Local LLM / Gemma Gaps & Mock Fallbacks

Running a local Generative AI model on-device involves several design trade-offs and safety mechanisms that introduce developer-facing stubs and constraints:

### A. Silent Mock LLM Fallback (`MockLLMEngine`)
- **Location:** `com.tomady.nutrition.service.gemma.GemmaAndroidService.kt`
- **Issue:** If the real ~2 GB Gemma model binary is not yet downloaded, cached, or fails to load, the system **silently** activates a `MockLLMEngine` to mock generation results.
- **Impact:**
  - Submitting prompts via `/v1/gemma/ask` or `/v1/gemma/compute-recipe` succeeds but yields hardcoded mock responses (e.g. `generateMockRecipe`, `generateMockInsight`).
  - Developers or RN clients might believe the real LLM is performing inference when it is actually a rule-based mock engine.
- **Mitigation:** The REST client must explicitly poll the `/v1/gemma/model` endpoint and inspect the `usingMock` parameter to know whether real or mock fallback inference is active.

### B. High Memory & Performance Requirements
- **Location:** `GemmaAndroidService.kt`
- **Issue:** Loading a 2 GB Gemma model and performing token inference on-device is highly resource-intensive.
- **Impact:** On devices with low RAM (under 6-8 GB) or without GPU/NPU hardware acceleration, model loading can lead to **Out of Memory (OOM) crashes**, UI freezing, or slow token-generation speeds (>30 seconds per query).

### C. Download Fragility & Lack of Resume Support
- **Location:** `com.tomady.nutrition.service.gemma.ModelDownloader.kt`
- **Issue:** The `ModelDownloader` downloads the massive ~2 GB model over a single HTTP connection.
- **Impact:**
  - If the network drops or is interrupted mid-way, the download fails and must be restarted from 0% (no HTTP Range / Resume-able downloads supported).
  - Uses `java.net.URL.openStream()` which blocks IO thread and lacks native chunked progress management, potentially leading to timeouts on slow networks.

---

## 2. API Thread Safety & Blocking Operations

### A. Synchronous `runBlocking` on HTTP Threads
- **Location:** `com.tomady.nutrition.server.TomadyRestApiServer.kt` & `TomadyApiServer.kt`
- **Issue:** To bridge Kotlin coroutines (used in Room and services) with NanoHTTPD's synchronous thread handlers, the API servers use `kotlinx.coroutines.runBlocking(Dispatchers.IO) { ... }`.
- **Impact:**
  - Under load (e.g., multiple rapid requests from React Native), this completely blocks NanoHTTPD worker threads.
  - Could result in thread exhaustion, increased latency, or internal socket pool starvation, making the local server unresponsive.

### B. Concurrent Database Access (WAL Configuration)
- **Location:** `com.tomady.nutrition.data.AppDatabase.kt`
- **Issue:** While Room utilizes SQLite WAL (Write-Ahead Logging) mode, intensive write operations (like streaming bio logs and dish histories simultaneously) can block read queries or lead to `SQLiteDatabaseLockedException` if not carefully pooled on single-threaded dispatchers.

---

## 3. Configuration & Networking Edge Cases

### A. Port Collision Risk
- **Location:** `com.tomady.nutrition.BuildConfig.SERVICE_API_PORT` (defaults to `7777`)
- **Issue:** The local REST API port is hardcoded to `7777`.
- **Impact:** If another background app on the user's Android device occupies port `7777`, starting `TomadyServerService` will fail with an `IOException: Bind already in use`. The server does not dynamically try fallback ports.

### B. Security / Cleartext Traffic Restriction
- **Location:** `app/src/main/AndroidManifest.xml`
- **Issue:** Local clients connect to `http://localhost:7777/` or `http://127.0.0.1:7777/` over plain HTTP.
- **Impact:** Starting with Android 9 (API 28), plain HTTP cleartext traffic is disabled by default. If the app's network security configuration does not explicitly allow cleartext traffic to `127.0.0.1` and `localhost`, HTTP requests will fail with security exceptions.

### C. Background Foreground Service Limits
- **Location:** `com.tomady.nutrition.server.TomadyServerService.kt`
- **Issue:** The local server is run within an Android Foreground Service.
- **Impact:** Since Android 14 (API 34), foreground services have highly strict type definitions and require explicit user permissions and OS-specific approvals. If started in the background without proper context, the system will raise `ForegroundServiceStartNotAllowedException`.

---

## 4. Database Schema & Room Gaps

### A. Incomplete FooDB Mapping
- **Location:** `com.tomady.nutrition.data.local.foodb` vs. `init_ressources/foodb_generated_schema_only.sql`
- **Issue:** Only `FoodItem` and `NutrientProperty` Room entities are currently mapped.
- **Impact:** Many valuable tables in the original FooDB database (such as compounds, flavors, enzymes, pathways, health effects, and references) are missing entity mappings and are inaccessible via the `FooDBDataAPIService`.

### B. Lacking Data Sanitization in SQL Queries
- **Location:** `com.tomady.nutrition.data.local.foodb.dao.FoodItemDao.kt`
- **Issue:** Full-text searches or wildcard matches (e.g. `LIKE '%' || :query || '%'`) do not escape special SQLite wildcard characters (like `%`, `_`).
- **Impact:** Users typing those characters can trigger erratic search matches or unexpected database query times.
