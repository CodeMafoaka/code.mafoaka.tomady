package com.tomady.nutrition.demo

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/**
 * WebView-based demo activity that loads the Tomady test dashboard.
 *
 * The WebView loads [demo.html] from the app's assets and exposes a
 * [DemoJSBridge] instance to JavaScript via [android.webkit.WebView.addJavascriptInterface].
 *
 * The HTML page provides a full test UI for all three backend services:
 * - **FooDB**: Food search, nutrient lookup with cache-first verification
 * - **Diet**: Profile management, meal logging, nutrition analysis
 * - **Gemma**: Recipe computation, Q&A, model lifecycle
 * - **Worker**: Force-run daily suggestion worker
 */
class DemoActivity : AppCompatActivity() {

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
    }
}
