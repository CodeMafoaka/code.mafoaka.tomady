package com.tomady.nutrition.demo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * WebView-based demo activity that loads the Tomady test dashboard.
 *
 * The WebView loads [demo.html] from the app's assets and exposes a
 * [DemoJSBridge] instance to JavaScript via [android.webkit.WebView.addJavascriptInterface].
 *
 * On Android 13+ (API 33+) this activity requests [Manifest.permission.POST_NOTIFICATIONS]
 * at runtime so the foreground server notification can be displayed.
 */
class DemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DemoActivity"
        /** Request code for the [Manifest.permission.POST_NOTIFICATIONS] permission. */
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false

            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()

            addJavascriptInterface(DemoJSBridge(applicationContext), "TomadyBridge")
            loadUrl("file:///android_asset/demo.html")
        }

        setContentView(webView)

        // Request notification permission on Android 13+
        requestNotificationPermission()
    }

    /**
     * Requests [Manifest.permission.POST_NOTIFICATIONS] on Android 13+.
     *
     * This permission is required for the foreground server notification
     * (shown via [com.tomady.nutrition.server.TomadyServerService]) to display.
     * The service still runs even without the permission, but the notification
     * will be silent/hidden.
     *
     * On Android 12 and below the permission is not needed.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Permission not needed below Android 13
            return
        }

        when {
            // Already granted
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                android.util.Log.d(TAG, "POST_NOTIFICATIONS already granted")
            }

            // Should show rationale first
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) -> {
                showNotificationPermissionRationale()
            }

            // First time asking — just request
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_POST_NOTIFICATIONS
                )
            }
        }
    }

    /**
     * Shows a simple dialog explaining why the notification permission is
     * needed before requesting it.
     */
    private fun showNotificationPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Notification Permission")
            .setMessage(
                "Tomady runs a local HTTP server on your device so other apps " +
                        "can access the nutrition services. A persistent notification " +
                        "shows the server status and URL.\n\n" +
                        "The server works without this permission, but the " +
                        "notification will not appear until you grant it."
            )
            .setPositiveButton("Grant") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_POST_NOTIFICATIONS
                )
            }
            .setNegativeButton("Not now", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.i(TAG, "POST_NOTIFICATIONS permission granted")
            } else {
                android.util.Log.w(TAG, "POST_NOTIFICATIONS permission denied")
            }
        }
    }
}
