package com.tomady.nutrition.ui

import com.tomady.nutrition.data.local.diet.entity.Profile

/**
 * Ensures a [com.tomady.nutrition.data.local.diet.entity.User] row exists for
 * [CURRENT_USER_ID], then returns its [Profile], creating a default one on
 * first launch.
 *
 * `Profile.userId` has a hard `ForeignKey` onto `User` (see Profile.kt) — a
 * Profile can't be inserted before its User row exists, or Room throws an
 * uncaught SQLiteConstraintException that crashes the app. Always go through
 * this instead of calling `dietService.createProfile()` directly.
 */
suspend fun ensureDefaultProfile(app: TomadyApp): Profile {
    if (app.dietService.getUser(CURRENT_USER_ID) == null) {
        app.dietService.createUser(
            id = CURRENT_USER_ID,
            username = CURRENT_USER_ID,
            email = "$CURRENT_USER_ID@tomady.local"
        )
    }

    return app.dietService.getProfile(CURRENT_USER_ID)
        ?: app.dietService.createProfile(
            userId = CURRENT_USER_ID,
            displayName = "Utilisateur",
            goal = "Maintien",
            dailyCalorieTarget = 2000,
            proteinGramsTarget = 120,
            carbsGramsTarget = 220,
            fatGramsTarget = 65
        )
}
