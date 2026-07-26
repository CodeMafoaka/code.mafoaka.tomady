package com.tomady.nutrition.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tomady.nutrition.demo.DemoActivity
import com.tomady.nutrition.TomadyApp

/**
 * Foreground service that keeps the [TomadyRestApiServer] alive and shows a
 * persistent notification so the user knows the server is running.
 *
 * The notification displays the server URL and persists until the user
 * explicitly stops the service or the app is killed.
 *
 * On Android 13+ (API 33+) the user must grant `POST_NOTIFICATIONS`
 * permission for the notification to show — the service still runs
 * even without the permission.
 */
class TomadyServerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = applicationContext as TomadyApp

        // Ensure the API server is running
        val isInitialized = try { app.apiServer; true } catch (_: UninitializedPropertyAccessException) { false }
        if (!isInitialized || !app.apiServer.isAlive) {
            android.util.Log.w(TAG, "API server not running — starting it from service")
            if (isInitialized) {
                app.apiServer.start()
            }
        }

        val ip = TomadyRestApiServer.getLocalIpAddress()
        val url = "http://$ip:${TomadyRestApiServer.DEFAULT_PORT}"

        val openIntent = Intent(this, DemoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tomady Server Active")
            .setContentText("REST API running on port ${TomadyRestApiServer.DEFAULT_PORT}")
            .setSubText(url)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Optionally shut down the API server when service is destroyed
        try {
            val app = applicationContext as TomadyApp
            val isInitialized = try { app.apiServer; true } catch (_: UninitializedPropertyAccessException) { false }
            if (isInitialized) {
                app.apiServer.shutdown()
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Error shutting down server from service", e)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tomady Server",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent notification for the Tomady REST API server"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "TomadyServerService"
        private const val CHANNEL_ID = "tomady_server"
        private const val NOTIFICATION_ID = 7777
    }
}
