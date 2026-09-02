package com.tomady.nutrition.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device smoke test for the native Compose UI. Exists specifically to
 * catch crashes a JVM unit test can't — this app's screens hit the real
 * Room/SQLite database and the Compose runtime on an actual device/emulator,
 * so a bug like a foreign-key violation on first launch only shows up here
 * (or by manually installing and opening the app).
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesWithoutCrashingAndShowsDashboard() {
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("Bonjour", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun canNavigateThroughEveryTabWithoutCrashing() {
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("Bonjour", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        for (tab in TOMADY_TABS) {
            composeTestRule.onNodeWithText(tab.label).performClick()
            composeTestRule.waitForIdle()
        }
    }
}
