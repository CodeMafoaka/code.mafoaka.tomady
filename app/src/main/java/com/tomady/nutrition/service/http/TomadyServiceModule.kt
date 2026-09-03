package com.tomady.nutrition.service.http

import android.content.Context
import com.tomady.nutrition.config.ConfigManager
import com.tomady.nutrition.data.AppDatabase
import com.tomady.nutrition.data.local.diet.DietDatabase
import com.tomady.nutrition.data.local.foodb.FooDBLocalDatabase
import com.tomady.nutrition.service.diet.DietAPIService
import com.tomady.nutrition.service.foodb.FooDBDataAPIService
import com.tomady.nutrition.service.gemma.GemmaAndroidService
import com.tomady.nutrition.service.nutrition.NutritionLookupService

internal class TomadyServiceModule(context: Context) {

    internal val appDatabase: AppDatabase by lazy {
        AppDatabase.getInstance(context.applicationContext)
    }

    internal val fooDBLocalDatabase: FooDBLocalDatabase by lazy {
        FooDBLocalDatabase(
            foodItemDao = appDatabase.foodItemDao(),
            nutrientPropertyDao = appDatabase.nutrientPropertyDao()
        )
    }

    internal val foodbService: FooDBDataAPIService by lazy {
        FooDBDataAPIService(localDatabase = fooDBLocalDatabase)
    }

    internal val dietDatabase: DietDatabase by lazy {
        DietDatabase(
            userDao = appDatabase.userDao(),
            profileDao = appDatabase.profileDao(),
            bioRecordDao = appDatabase.bioRecordDao(),
            dishDao = appDatabase.dishDao(),
            recipeDao = appDatabase.recipeDao(),
            recipeIngredientDao = appDatabase.recipeIngredientDao(),
            dishHistoryDao = appDatabase.dishHistoryDao()
        )
    }

    internal val dietService: DietAPIService by lazy {
        DietAPIService(
            dietDatabase = dietDatabase,
            foodbService = foodbService,
            nutritionLookupService = NutritionLookupService(ConfigManager(context.applicationContext))
        )
    }

    internal val gemmaService: GemmaAndroidService by lazy {
        GemmaAndroidService(
            context = context.applicationContext,
            dietDatabase = dietDatabase,
            dietService = dietService,
            foodbService = foodbService
        )
    }
}
