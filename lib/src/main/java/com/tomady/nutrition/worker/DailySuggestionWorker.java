package com.tomady.nutrition.worker;

import android.content.Context;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.tomady.nutrition.service.diet.DietAPIService;
import com.tomady.nutrition.service.gemma.GemmaAndroidService;
import com.tomady.nutrition.data.local.diet.*;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Background WorkManager Worker responsible for compiling user consumption history,
 * querying local models, and generating fresh dietary suggestions on a daily basis.
 */
public class DailySuggestionWorker extends Worker {

    // Dependency injection points for mock/stub testing
    private static DietAPIService dietAPIServiceReference = null;
    private static GemmaAndroidService gemmaServiceReference = null;
    private static ReactApplicationContext reactContextReference = null;

    public static void setDependencies(DietAPIService diet, GemmaAndroidService gemma, ReactApplicationContext reactContext) {
        dietAPIServiceReference = diet;
        gemmaServiceReference = gemma;
        reactContextReference = reactContext;
    }

    public DailySuggestionWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public Result doWork() {
        System.out.println("Running daily suggestion task...");

        DietAPIService dietService = dietAPIServiceReference;
        GemmaAndroidService gemmaService = gemmaServiceReference;

        if (dietService == null || gemmaService == null) {
            return Result.failure();
        }

        // 1. Fetch User and active Profile from DietDatabase
        List<User> users = dietService.getAllUsers();
        if (users.isEmpty()) {
            System.out.println("No users found. Cannot generate daily suggestions.");
            return Result.failure();
        }

        User activeUser = users.get(0);
        Profile profile = dietService.getProfileByUserId(activeUser.getId());
        if (profile == null) {
            System.out.println("No active profile found for user. Cannot generate daily suggestions.");
            return Result.failure();
        }

        // 2. Fetch User's DishHistory
        List<DishHistory> history = dietService.getDishHistoryByUserId(activeUser.getId());

        // 3. Compile prompt constraints and ask local Gemma model for personalized daily suggestion
        String prompt = "Generate personalized daily dish suggestion. Preferences: age=" + profile.getAge() +
                        ", allergies=" + profile.getAllergies() +
                        ", diseases=" + profile.getDiseases() +
                        ". Previous logs count=" + history.size();

        gemmaService.initializeModel();
        String suggestionText = gemmaService.askQuestion(prompt);

        // 4. Store generated suggestions in DietDatabase as suggested dishes
        Dish suggestedDish = new Dish();
        suggestedDish.setName("Suggested: " + (suggestionText.length() > 50 ? "Healthy meal suggestion" : suggestionText));
        suggestedDish.setCalories(350.0);
        dietService.insertDish(suggestedDish);

        // 5. Trigger event dispatch through React Native bridge to notify client
        if (reactContextReference != null) {
            DeviceEventManagerModule.RCTDeviceEventEmitter emitter =
                reactContextReference.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
            if (emitter != null) {
                emitter.emit("onDailySuggestionsReady", suggestionText);
            }
        }

        return Result.success();
    }

    /**
     * Helper to configure and schedule the PeriodicWorkRequest to run daily at 00:00 Midnight.
     *
     * @param context Android Application Context.
     */
    public static void scheduleDailyWorker(Context context) {
        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();

        // Calculate delay until next midnight
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long midnight = calendar.getTimeInMillis();
        long initialDelayMs = midnight - now;

        PeriodicWorkRequest dailyWorkRequest = new PeriodicWorkRequest.Builder(
            DailySuggestionWorker.class,
            24,
            TimeUnit.HOURS
        )
        .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
        .build();

        WorkManager.getInstance(context).enqueue(dailyWorkRequest);
    }
}
