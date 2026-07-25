package com.tomady.nutrition

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tomady.nutrition.data.AppDatabase
import com.tomady.nutrition.data.local.diet.DietDatabase
import com.tomady.nutrition.data.local.foodb.FooDBLocalDatabase
import com.tomady.nutrition.server.TomadyRestApiServer
import com.tomady.nutrition.service.diet.DietAPIService
import com.tomady.nutrition.service.foodb.FooDBDataAPIService
import com.tomady.nutrition.service.gemma.GemmaAndroidService
import com.tomady.nutrition.worker.DailySuggestionWorker
import java.util.concurrent.TimeUnit

/**
 * Tomady Application class.
 *
 * Initializes the Room databases, service singletons, REST API server, and
 * WorkManager background jobs.
 *
 * On startup:
 * 1. Builds the [AppDatabase] (Diet + FooDB).
 * 2. Creates wrapper databases and service singletons.
 * 3. Starts the embedded [TomadyRestApiServer] for local-network HTTP access.
 * 4. Schedules a [PeriodicWorkRequest] for [DailySuggestionWorker].
 */
class TomadyApp : Application() {

    /** Shared database instance. */
    lateinit var database: AppDatabase
        private set

    /** Diet database wrapper. */
    lateinit var dietDatabase: DietDatabase
        private set

    /** FooDB local database wrapper. */
    lateinit var foodbLocal: FooDBLocalDatabase
        private set

    /** FooDB data access service. */
    lateinit var foodbService: FooDBDataAPIService
        private set

    /** Diet planning and CRUD service. */
    lateinit var dietService: DietAPIService
        private set

    /** Gemma on-device LLM service. */
    lateinit var gemmaService: GemmaAndroidService
        private set

    /** Embedded REST API server for local-network access. */
    lateinit var apiServer: TomadyRestApiServer
        private set

    override fun onCreate() {
        super.onCreate()

        instance = this
        initializeServices()
        startApiServer()
        scheduleDailySuggestionWorker()
    }

    /**
     * Initializes the Room database, wrapper databases, and service singletons.
     */
    private fun initializeServices() {
        // 1. Build the combined Room database
        database = AppDatabase.getInstance(this)

        // 2. Create wrapper databases
        dietDatabase = DietDatabase(
            userDao = database.userDao(),
            profileDao = database.profileDao(),
            bioRecordDao = database.bioRecordDao(),
            dishDao = database.dishDao(),
            recipeDao = database.recipeDao(),
            recipeIngredientDao = database.recipeIngredientDao(),
            dishHistoryDao = database.dishHistoryDao()
        )

        foodbLocal = FooDBLocalDatabase(
            foodItemDao = database.foodItemDao(),
            nutrientPropertyDao = database.nutrientPropertyDao()
        )

        // 3. Create service instances
        foodbService = FooDBDataAPIService(localDatabase = foodbLocal)
        dietService = DietAPIService(
            dietDatabase = dietDatabase,
            foodbService = foodbService
        )
        gemmaService = GemmaAndroidService(
            dietDatabase = dietDatabase,
            dietService = dietService,
            foodbService = foodbService
        )
    }

    /**
     * Starts the embedded REST API server for local-network access.
     *
     * The server binds to **0.0.0.0** on port **7777**, making it accessible
     * from any device on the same WiFi network. External React Native (or any
     * HTTP) clients can call the REST endpoints instead of using native bridge
     * modules.
     *
     * Server URL: `http://<device-wifi-ip>:7777/api/v1/health`
     */
    private fun startApiServer() {
        apiServer = TomadyRestApiServer(
            foodbService = foodbService,
            dietService = dietService,
            gemmaService = gemmaService,
            dietDatabase = dietDatabase,
            context = this
        )
        try {
            apiServer.start()
            android.util.Log.i(
                TAG,
                "REST API running at http://${TomadyRestApiServer.getLocalIpAddress()}:${TomadyRestApiServer.DEFAULT_PORT}/api/v1/health"
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to start REST API server", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        if (::apiServer.isInitialized) {
            apiServer.shutdown()
        }
    }

    /**
     * Schedules the [DailySuggestionWorker] as a periodic task.
     *
     * Configuration:
     * - **Interval**: 24 hours (run once per day).
     * - **Flex interval**: 1 hour (WorkManager can run the job up to 1 hour
     *   before or after the 24-hour mark to optimise battery).
     * - **Constraints**: Requires a device idle state (battery saver friendly)
     *   but does NOT require charging or network — Gemma runs on-device.
     * - **Existing policy**: [ExistingPeriodicWorkPolicy.KEEP] — if already
     *   scheduled, don't duplicate.
     */
    private fun scheduleDailySuggestionWorker() {
        val constraints = Constraints.Builder()
            .setRequiresDeviceIdle(true)
            .build()

        val dailySuggestionRequest = PeriodicWorkRequestBuilder<DailySuggestionWorker>(
            24, TimeUnit.HOURS,
            1, TimeUnit.HOURS  // flex interval
        )
            .setConstraints(constraints)
            .addTag(WORK_TAG_DAILY_SUGGESTION)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailySuggestionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailySuggestionRequest
        )
    }

    companion object {
        /** Tag used for identifying the daily suggestion work in logs and debugging. */
        const val WORK_TAG_DAILY_SUGGESTION = "daily_suggestion"
        private const val TAG = "TomadyApp"

        @Volatile
        private var instance: TomadyApp? = null

        /**
         * Returns the application singleton instance.
         */
        fun getInstance(): TomadyApp = instance
            ?: throw IllegalStateException("TomadyApp not initialized")
    }
}
