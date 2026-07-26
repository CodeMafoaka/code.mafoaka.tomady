package com.tomady.nutrition.service;

import com.tomady.nutrition.dto.GeneratedRecipe;
import com.tomady.nutrition.dto.ComputeRecipeRequest;
import com.tomady.nutrition.dto.RawPromptRequest;
import com.tomady.nutrition.dto.RawPromptResponse;
import com.tomady.nutrition.dto.UserProfile;
import com.tomady.nutrition.dto.Dish;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GemmaLlmServiceImpl implements GemmaLlmService {

    private final DietService dietService;

    public GemmaLlmServiceImpl(DietService dietService) {
        this.dietService = dietService;
    }

    @Override
    public GeneratedRecipe computeRecipe(ComputeRecipeRequest request) {
        // Fetch user profile to cross-check safety
        UserProfile profile = dietService.getProfile(request.userId());

        List<String> ingredients = new ArrayList<>();
        String instructions = "1. Prep ingredients.\n2. Mix and cook gently.\n3. Serve warm.";
        List<String> safetyAlerts = new ArrayList<>();
        boolean compatible = true;

        String dishNameLower = request.dishName().toLowerCase();

        // Simulate Recipe generation ingredients
        if (dishNameLower.contains("apple")) {
            ingredients.add("Fresh Apples");
            ingredients.add("Cinnamon");
            ingredients.add("Brown Sugar");
        } else if (dishNameLower.contains("peanut") || dishNameLower.contains("satay")) {
            ingredients.add("Peanut Butter");
            ingredients.add("Soy Sauce");
            ingredients.add("Chili");
        } else if (dishNameLower.contains("shrimp") || dishNameLower.contains("scampi")) {
            ingredients.add("Shrimp");
            ingredients.add("Garlic");
            ingredients.add("Butter");
            ingredients.add("Lemon");
        } else {
            ingredients.add("Generic Ingredient A");
            ingredients.add("Generic Ingredient B");
        }

        // Cross-check safety validation pipeline
        // 1. Allergies cross-checking
        if (profile.allergies() != null) {
            for (String allergy : profile.allergies()) {
                String allergyLower = allergy.toLowerCase();
                for (String ing : ingredients) {
                    if (ing.toLowerCase().contains(allergyLower) || (allergyLower.equals("peanuts") && ing.toLowerCase().contains("peanut"))) {
                        safetyAlerts.add("WARNING: Ingredient '" + ing + "' conflicts with your allergy to '" + allergy + "'!");
                        compatible = false;
                    }
                }
            }
        }

        // 2. Health conditions / goals cross-checking (e.g., high sugar warning for Diabetes, high cholesterol for Hypertension/Heart Disease)
        if (profile.healthConditions() != null) {
            if (profile.healthConditions().contains("Diabetes")) {
                // If dish is sweet or has sugar ingredients
                if (dishNameLower.contains("sweet") || dishNameLower.contains("dessert") || ingredients.contains("Brown Sugar") || dishNameLower.contains("apple")) {
                    safetyAlerts.add("WARNING: High sugar risk for Diabetes condition.");
                    compatible = false;
                }
            }
            if (profile.healthConditions().contains("Hypertension")) {
                if (dishNameLower.contains("salted") || dishNameLower.contains("soy sauce") || ingredients.contains("Soy Sauce")) {
                    safetyAlerts.add("WARNING: High sodium content risk for Hypertension condition.");
                    compatible = false;
                }
            }
            if (profile.healthConditions().contains("Heart Disease")) {
                if (dishNameLower.contains("butter") || dishNameLower.contains("shrimp") || ingredients.contains("Butter") || ingredients.contains("Shrimp")) {
                    safetyAlerts.add("WARNING: High cholesterol / saturated fat risk for Heart Disease condition.");
                    compatible = false;
                }
            }
        }

        return new GeneratedRecipe(
            request.dishName() + " Tailored Recipe",
            ingredients,
            instructions,
            safetyAlerts,
            compatible
        );
    }

    @Override
    public RawPromptResponse askGemma(RawPromptRequest request) {
        String prompt = request.prompt();
        String reply = "Gemma LLM Engine Stub response to prompt: '" + prompt + "'. Everything is balanced!";
        return new RawPromptResponse(reply);
    }
}
