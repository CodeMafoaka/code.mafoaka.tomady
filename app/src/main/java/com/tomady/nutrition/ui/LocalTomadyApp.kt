package com.tomady.nutrition.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.tomady.nutrition.TomadyApp

/**
 * Resolves the running [TomadyApp] singleton for use inside Composables.
 *
 * The native UI lives in the same process as the backend services (Room DB,
 * [com.tomady.nutrition.service.diet.DietAPIService], etc.), so screens call
 * them directly — no HTTP round-trip needed, unlike the separate React Native
 * mobile app which talks to this same backend over REST.
 */
@Composable
fun rememberTomadyApp(): TomadyApp {
    val context = LocalContext.current
    return context.applicationContext as TomadyApp
}

/** Fixed single-user id — this app has no auth/multi-user system yet. */
const val CURRENT_USER_ID = "user-1"
