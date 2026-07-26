package com.tomady.nutrition

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tomady.nutrition.data.AppDatabase
import com.tomady.nutrition.data.local.diet.DietDatabase
import com.tomady.nutrition.data.local.foodb.FooDBLocalDatabase
import com.tomady.nutrition.server.TomadyRestApiServer
import com.tomady.nutrition.server.TomadyServerService
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
 * 3. Creates notification channels.
 * 4. Starts the embedded [TomadyRestApiServer] via [TomadyServerService] (foreground).
 * 5. Schedules a [PeriodicWorkRequest] for [DailySuggestionWorker].
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

    /** Helper to check if the api server is initialized */
    fun isApiServerInitialized(): Boolean = ::apiServer.isInitialized

    override fun onCreate() {
        super.onCreate()

        instance = this
        createNotificationChannels()
        initializeServices()
        startApiServer()
        scheduleDailySuggestionWorker()
    }

    /**
     * Creates notification channels required for foreground service
     * notifications and WorkManager progress notifications.
     */
    private fun createNotificationChannels() {
        // The TomadyServerService also creates its own channel, but we create
        // it here too to ensure it exists before the service starts.
        val serverChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_SERVER,
            "Tomady Server",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when the Tomady REST API server is running"
            setShowBadge(false)
        }

        val workerChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_WORKER,
            "Tomady Background Tasks",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Notifications for background task progress"
            setShowBadge(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serverChannel)
        manager.createNotificationChannel(workerChannel)
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
     * Starts the embedded REST API server and foreground service.
     *
     * The server binds to **0.0.0.0** on port **7777**, making it accessible
     * from any device on the same WiFi network.
     *
     * A persistent notification is shown via [TomadyServerService] so the
     * user knows the server is running. On Android 13+, the notification
     * requires the `POST_NOTIFICATIONS` runtime permission to display.
     *
     * Server URL: `http://<device-wifi-ip>:7777/api/v1/health`
     */
    private fun startApiServer() {
        apiServer = TomadyRestApiServer(
            foodbService = foodbService,
            dietService = dietService,
            gemmaService = gemmaService,
            dietDatabase = dietDatabase,
            context = this,
            port = BuildConfig.SERVICE_API_PORT
        )
        try {
            apiServer.start()
            android.util.Log.i(
                TAG,
                "REST API running at http://${TomadyRestApiServer.getLocalIpAddress()}:${TomadyRestApiServer.DEFAULT_PORT}/api/v1/health"
            )

            // Start foreground service to keep the server alive
            val serviceIntent = Intent(this, TomadyServerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
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

        /** Notification channel for the persistent server notification. */
        const val NOTIFICATION_CHANNEL_SERVER = "tomady_server"

        /** Notification channel for WorkManager background task progress. */
        const val NOTIFICATION_CHANNEL_WORKER = "tomady_worker"

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
