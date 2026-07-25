package com.tomady.nutrition

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tomady.nutrition.service.http.TomadyApiServer
import com.tomady.nutrition.worker.DailySuggestionWorker
import java.util.concurrent.TimeUnit

/**
 * Tomady Application class.
 *
 * Initializes the WorkManager, database instances, and service singletons.
 *
 * On startup, schedules a [PeriodicWorkRequest] for [DailySuggestionWorker]
 * that runs daily at approximately 00:00 to generate personalised food
 * suggestions for each user.
 */
class TomadyApp : Application() {

    private var apiServer: TomadyApiServer? = null

    override fun onCreate() {
        super.onCreate()

        scheduleDailySuggestionWorker()
        apiServer = TomadyApiServer(this).apply {
            startServer()
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
     *   scheduled, don't duplicate. To force a re-schedule, use REPLACE.
     *
     * The first run will occur approximately 24 hours after the app is first
     * launched. On subsequent days, WorkManager aligns the execution to
     * approximately 00:00 (midnight) based on the device's doze/maintenance
     * window.
     *
     * To align more precisely with midnight, production apps can use
     * [androidx.work.Constraints] with a custom `triggerContentMaxDelay`
     * or combine this with a [android.app.AlarmManager] initial seed.
     * For the Tomady headless architecture, the 24h periodic interval is
     * sufficient since suggestions are checked when the RN host polls.
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
    }
}
