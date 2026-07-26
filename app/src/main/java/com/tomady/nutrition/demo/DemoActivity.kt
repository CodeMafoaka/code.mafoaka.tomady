package com.tomady.nutrition.demo

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Native + WebView hybrid demo activity that loads the Tomady test dashboard
 * and provides a simple, direct native GUI to ping the embedded HTTP server on port 7777.
 *
 * On Android 13+ (API 33+) this activity requests [Manifest.permission.POST_NOTIFICATIONS]
 * at runtime so the foreground server notification can be displayed.
 */
class DemoActivity : AppCompatActivity() {

    companion object {
        /** Request code for the [Manifest.permission.POST_NOTIFICATIONS] permission. */
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#0f0f23")) // Match dashboard background
        }

        // Native Panel Header
        val header = TextView(this).apply {
            text = "🔌 Native HTTP Server Console (Port 7777)"
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(32, 24, 32, 8)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        container.addView(header)

        // Native Ping Layout
        val pingLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 8, 32, 16)
        }

        val statusView = TextView(this).apply {
            text = "Status: Idle (Ready to ping)"
            textSize = 13f
            setTextColor(Color.parseColor("#aaaaaa"))
            setTypeface(Typeface.MONOSPACE)
            setBackgroundColor(Color.parseColor("#16213e"))
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        val pingBtn = Button(this).apply {
            text = "PING HTTP SERVER"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                performPing(statusView)
            }
        }

        pingLayout.addView(statusView)
        pingLayout.addView(pingBtn)
        container.addView(pingLayout)

        // Divider
        val divider = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                topMargin = 8
                bottomMargin = 8
            }
            setBackgroundColor(Color.parseColor("#2a2a4a"))
        }
        container.addView(divider)

        // WebView for testing dashboard
        val webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
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
        container.addView(webView)

        setContentView(container)
    }

    private fun performPing(statusView: TextView) {
        statusView.text = "Status: Pinging..."
        statusView.setTextColor(Color.YELLOW)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://127.0.0.1:7777/ping")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    withContext(Dispatchers.Main) {
                        statusView.text = "Success (200 OK):\n$responseText"
                        statusView.setTextColor(Color.GREEN)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        statusView.text = "Error: HTTP $responseCode"
                        statusView.setTextColor(Color.RED)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusView.text = "Failed: ${e.message ?: "Unknown Exception"}"
                    statusView.setTextColor(Color.RED)
                }
            }
        }
    }
}
