package com.tomady.nutrition.service.gemma;

import com.tomady.nutrition.service.diet.DietAPIService;
import com.tomady.nutrition.data.local.diet.Dish;
import com.tomady.nutrition.data.local.diet.Recipe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for local Gemma LLM integration on Android.
 * Manages model lifecycle and handles recipe computation, response validation,
 * and profile cross-referencing pipelines.
 */
public class GemmaAndroidService {
    private final DietAPIService dietAPIService;
    private boolean isModelLoaded = false;

    /**
     * Listener interface to support simulated LLM token streaming.
     */
    public interface TokenStreamListener {
        void onToken(String token);
    }

    /**
     * Object representing computed recipe and pipeline validation results.
     */
    public static class RecipeResponse {
        private final String recipeTitle;
        private final boolean isSafe;
        private final List<String> warnings;

        public RecipeResponse(String recipeTitle, boolean isSafe, List<String> warnings) {
            this.recipeTitle = recipeTitle;
            this.isSafe = isSafe;
            this.warnings = warnings;
        }

        public String getRecipeTitle() { return recipeTitle; }
        public boolean isSafe() { return isSafe; }
        public List<String> getWarnings() { return warnings; }
    }

    public GemmaAndroidService() {
        this.dietAPIService = null;
    }

    public GemmaAndroidService(DietAPIService dietAPIService) {
        this.dietAPIService = dietAPIService;
    }

    /**
     * Simulates background loading of the local Gemma model.
     * @return boolean indicating initialization status.
     */
    public boolean initializeModel() {
        isModelLoaded = true;
        return true;
    }

    /**
     * Checks if the Gemma model is loaded and ready.
     * @return isModelLoaded status
     */
    public boolean isModelLoaded() {
        return isModelLoaded;
    }

    /**
     * Releases local model resources safely.
     */
    public void unloadModel() {
        isModelLoaded = false;
    }

    /**
     * Generates a single-response nutritional/diet advice for questions.
     *
     * @param question User question.
     * @return String representing LLM response.
     */
    public String askQuestion(String question) {
        if (!isModelLoaded) {
            return "Error: Gemma model is not loaded.";
        }
        if (question == null || question.isEmpty()) {
            return "Please ask a valid question.";
        }

        String qLower = question.toLowerCase();
        if (qLower.contains("coca-cola") || qLower.contains("coke")) {
            return "Coca-Cola is high in refined sugar (approx 10.6g per 100ml) and has no significant nutritional value. It is generally not recommended for individuals with diabetes due to rapid blood glucose spikes.";
        } else if (qLower.contains("salad")) {
            return "Salads are excellent source of fiber and micronutrients. Highly recommended for general wellness.";
        }
        return "I can help with your nutritional needs! Please specify a food item or dietary target.";
    }

    /**
     * Simulates streaming tokens of a question's response.
     *
     * @param question User question.
     * @param listener Callback triggered for every streamed token.
     */
    public void askQuestionStreaming(String question, TokenStreamListener listener) {
        if (listener == null) return;
        String fullResponse = askQuestion(question);
        // Split by words to simulate token-by-token streaming
        String[] tokens = fullResponse.split(" ");
        for (String token : tokens) {
            listener.onToken(token + " ");
            try {
                Thread.sleep(10); // Simulated delay between tokens
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Recipe Computation & Validation Pipeline:
     * 1. Accepts a user request.
     * 2. Computes/locates matching Dish/Recipe payload.
     * 3. Validates the computed properties against the active user Profile.
     *
     * @param userRequest Prompt describing request (e.g. "Can I drink coca-cola with diabetes?").
     * @param profileId User profile identifier.
     * @return RecipeResponse containing computed titles and validation conflict warnings.
     */
    public RecipeResponse computeRecipe(String userRequest, int profileId) {
        if (!isModelLoaded) {
            return new RecipeResponse("None", false, List.of("Model not loaded"));
        }

        // Determine matching dish name based on request
        String dishName = "Custom Recipe";
        String recipeTitle = "Custom Recipe Action";
        String reqLower = userRequest.toLowerCase();

        if (reqLower.contains("coca-cola") || reqLower.contains("coke")) {
            dishName = "Coca-Cola";
            recipeTitle = "Drink Coca-Cola";
        } else if (reqLower.contains("salad")) {
            dishName = "Salad bowl";
            recipeTitle = "Nutritious Salad";
        }

        // Find or create matching Dish/Recipe in database to cross-reference
        int dishId = -1;
        if (dietAPIService != null) {
            // Check if Dish exists in database
            List<Dish> dishes = dietAPIService.getAllDishes();
            for (Dish d : dishes) {
                if (d.getName().equalsIgnoreCase(dishName)) {
                    dishId = d.getId();
                    Recipe r = dietAPIService.getRecipeById(d.getRecipeId());
                    if (r != null) {
                        recipeTitle = r.getTitle();
                    } else {
                        recipeTitle = d.getName();
                    }
                    break;
                }
            }

            // If not found, create a mock Dish/Recipe and insert
            if (dishId == -1) {
                Recipe r = new Recipe();
                r.setTitle(recipeTitle);
                r.setInstructions("Generated by Gemma local LLM.");
                dietAPIService.insertRecipe(r);

                Dish d = new Dish();
                d.setName(dishName);
                d.setRecipeId(r.getId());
                // If coca-cola, add a mock high calories or high sugar
                if (dishName.equals("Coca-Cola")) {
                    d.setCalories(140.0);
                } else {
                    d.setCalories(150.0);
                }
                dietAPIService.insertDish(d);
                dishId = d.getId();
            }

            // Execute cross-reference conflict validation pipeline in DietAPIService
            DietAPIService.ValidationResult result = dietAPIService.validateProfileDishConflict(profileId, dishId);
            return new RecipeResponse(recipeTitle, result.isValid(), result.getWarnings());
        }

        return new RecipeResponse(recipeTitle, true, Collections.emptyList());
    }

    /**
     * Backward-compatible suggestion generator.
     */
    public String generateSuggestion(String context) {
        return askQuestion(context);
    }
}
