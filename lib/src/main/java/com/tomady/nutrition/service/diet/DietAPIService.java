package com.tomady.nutrition.service.diet;

import com.tomady.nutrition.data.local.diet.*;
import com.tomady.nutrition.service.foodb.FooDBDataAPIService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for Diet API operations.
 * Coordinates user dietary profiling, bio records logging, and consumption history tracking.
 */
public class DietAPIService {
    private final DietDatabase database;
    private final FooDBDataAPIService fooDBDataAPIService;

    /**
     * Nested class representing calculated nutritional values of a Dish.
     */
    public static class DishNutritionalValue {
        private final double calories;
        private final double protein;
        private final double fat;
        private final double carbs;
        private final double sugar;

        public DishNutritionalValue(double calories, double protein, double fat, double carbs, double sugar) {
            this.calories = calories;
            this.protein = protein;
            this.fat = fat;
            this.carbs = carbs;
            this.sugar = sugar;
        }

        public double getCalories() { return calories; }
        public double getProtein() { return protein; }
        public double getFat() { return fat; }
        public double getCarbs() { return carbs; }
        public double getSugar() { return sugar; }
    }

    /**
     * Nested class representing validation results against profile conditions/allergies.
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final List<String> warnings;

        public ValidationResult(boolean isValid, List<String> warnings) {
            this.isValid = isValid;
            this.warnings = warnings;
        }

        public boolean isValid() { return isValid; }
        public List<String> getWarnings() { return warnings; }
    }

    public DietAPIService() {
        this.database = null;
        this.fooDBDataAPIService = null;
    }

    public DietAPIService(DietDatabase database, FooDBDataAPIService fooDBDataAPIService) {
        this.database = database;
        this.fooDBDataAPIService = fooDBDataAPIService;
    }

    /**
     * Computes the total nutritional value of a Dish by aggregating its RecipeIngredients.
     * Uses FooDBDataAPIService to fetch detailed nutrient structures for each ingredient.
     *
     * @param dishId ID of the dish to compute.
     * @return DishNutritionalValue aggregated calorie, macro, and sugar values.
     */
    public DishNutritionalValue getDishNutritionalValue(int dishId) {
        if (database == null || fooDBDataAPIService == null) {
            return new DishNutritionalValue(0, 0, 0, 0, 0);
        }

        Dish dish = database.dietDao().getDishById(dishId);
        if (dish == null) {
            return new DishNutritionalValue(0, 0, 0, 0, 0);
        }

        List<RecipeIngredient> ingredients = database.dietDao().getRecipeIngredientsByRecipeId(dish.getRecipeId());
        if (ingredients == null || ingredients.isEmpty()) {
            return new DishNutritionalValue(dish.getCalories(), 0, 0, 0, 0);
        }

        double totalCalories = 0.0;
        double totalProtein = 0.0;
        double totalFat = 0.0;
        double totalCarbs = 0.0;
        double totalSugar = 0.0;

        for (RecipeIngredient ingredient : ingredients) {
            double multiplier = ingredient.getQuantity() / 100.0; // Assume quantity is in grams
            try {
                FooDBDataAPIService.FoodDetails foodDetails = fooDBDataAPIService.getFoodDetails(ingredient.getFoodItemId());
                if (foodDetails != null && foodDetails.getNutrientProperties() != null) {
                    for (NutrientProperty prop : foodDetails.getNutrientProperties()) {
                        String name = prop.getPropertyName().toLowerCase();
                        double value = prop.getPropertyValue();

                        if (name.contains("calori") || name.equals("energy")) {
                            totalCalories += value * multiplier;
                        } else if (name.contains("protein")) {
                            totalProtein += value * multiplier;
                        } else if (name.contains("fat") || name.contains("lipid")) {
                            totalFat += value * multiplier;
                        } else if (name.contains("carb") || name.contains("sacchar")) {
                            totalCarbs += value * multiplier;
                        } else if (name.contains("sugar")) {
                            totalSugar += value * multiplier;
                        }
                    }
                }
            } catch (IOException e) {
                // Ignore remote errors during aggregation and aggregate what is available
            }
        }

        // If totalCalories is 0, default to dish's preset calories
        if (totalCalories == 0.0) {
            totalCalories = dish.getCalories();
        }

        return new DishNutritionalValue(totalCalories, totalProtein, totalFat, totalCarbs, totalSugar);
    }

    /**
     * Validates if a dish conflicts with user conditions defined in their Profile.
     *
     * @param profileId User profile identifier.
     * @param dishId Dish identifier.
     * @return ValidationResult indicating if any allergen or condition alerts exist.
     */
    public ValidationResult validateProfileDishConflict(int profileId, int dishId) {
        List<String> warnings = new ArrayList<>();
        if (database == null) {
            return new ValidationResult(true, warnings);
        }

        Profile profile = database.dietDao().getProfileById(profileId);
        DishNutritionalValue nutrition = getDishNutritionalValue(dishId);

        if (profile != null && nutrition != null) {
            String diseases = profile.getDiseases();
            if (diseases != null) {
                String lcaseDiseases = diseases.toLowerCase();
                if (lcaseDiseases.contains("diabete")) {
                    if (nutrition.getSugar() > 20.0) {
                        warnings.add("High sugar conflict: " + String.format("%.1fg", nutrition.getSugar()) + " exceeds limit for diabetes.");
                    }
                }
                if (lcaseDiseases.contains("hypertension") || lcaseDiseases.contains("heart")) {
                    if (nutrition.getCalories() > 800.0) {
                        warnings.add("High calories conflict: " + String.format("%.1f kcal", nutrition.getCalories()) + " exceeds safe limits for hypertension.");
                    }
                }
            }

            // Check allergen conflicts by scanning ingredients of the recipe
            String allergies = profile.getAllergies();
            if (allergies != null && !allergies.isEmpty()) {
                String[] allergyList = allergies.split("[,;\\s]+");
                Dish dish = database.dietDao().getDishById(dishId);
                if (dish != null) {
                    List<RecipeIngredient> ingredients = database.dietDao().getRecipeIngredientsByRecipeId(dish.getRecipeId());
                    if (ingredients != null) {
                        for (RecipeIngredient ri : ingredients) {
                            FoodItem item = database.dietDao().getFoodItemById(ri.getFoodItemId());
                            if (item != null) {
                                String itemName = item.getName().toLowerCase();
                                String itemGroup = item.getGroupName() != null ? item.getGroupName().toLowerCase() : "";
                                for (String allergy : allergyList) {
                                    String cleanAllergy = allergy.trim().toLowerCase();
                                    if (!cleanAllergy.isEmpty() && (itemName.contains(cleanAllergy) || itemGroup.contains(cleanAllergy))) {
                                        warnings.add("Allergen warning: Dish contains " + item.getName() + " which is flagged in profile.");
                                        break; // Warning triggered for this ingredient, skip other allergy checks on same ingredient
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return new ValidationResult(warnings.isEmpty(), warnings);
    }

    /* ==========================================================
     * COMPLETE CRUD METHODS wrappers for Database Access
     * ========================================================== */

    public void insertUser(User user) {
        if (database != null) database.dietDao().insertUser(user);
    }

    public void updateUser(User user) {
        if (database != null) database.dietDao().updateUser(user);
    }

    public void deleteUser(User user) {
        if (database != null) database.dietDao().deleteUser(user);
    }

    public User getUserById(int id) {
        return database != null ? database.dietDao().getUserById(id) : null;
    }

    public List<User> getAllUsers() {
        return database != null ? database.dietDao().getAllUsers() : Collections.emptyList();
    }

    public void insertProfile(Profile profile) {
        if (database != null) database.dietDao().insertProfile(profile);
    }

    public void updateProfile(Profile profile) {
        if (database != null) database.dietDao().updateProfile(profile);
    }

    public void deleteProfile(Profile profile) {
        if (database != null) database.dietDao().deleteProfile(profile);
    }

    public Profile getProfileById(int id) {
        return database != null ? database.dietDao().getProfileById(id) : null;
    }

    public Profile getProfileByUserId(int userId) {
        return database != null ? database.dietDao().getProfileByUserId(userId) : null;
    }

    public List<Profile> getAllProfiles() {
        return database != null ? database.dietDao().getAllProfiles() : Collections.emptyList();
    }

    public void insertBioRecord(BioRecord bioRecord) {
        if (database != null) database.dietDao().insertBioRecord(bioRecord);
    }

    public void updateBioRecord(BioRecord bioRecord) {
        if (database != null) database.dietDao().updateBioRecord(bioRecord);
    }

    public void deleteBioRecord(BioRecord bioRecord) {
        if (database != null) database.dietDao().deleteBioRecord(bioRecord);
    }

    public BioRecord getBioRecordById(int id) {
        return database != null ? database.dietDao().getBioRecordById(id) : null;
    }

    public List<BioRecord> getBioRecordsByProfileId(int profileId) {
        return database != null ? database.dietDao().getBioRecordsByProfileId(profileId) : Collections.emptyList();
    }

    public void insertDish(Dish dish) {
        if (database != null) database.dietDao().insertDish(dish);
    }

    public void updateDish(Dish dish) {
        if (database != null) database.dietDao().updateDish(dish);
    }

    public void deleteDish(Dish dish) {
        if (database != null) database.dietDao().deleteDish(dish);
    }

    public Dish getDishById(int id) {
        return database != null ? database.dietDao().getDishById(id) : null;
    }

    public List<Dish> getAllDishes() {
        return database != null ? database.dietDao().getAllDishes() : Collections.emptyList();
    }

    public void insertRecipe(Recipe recipe) {
        if (database != null) database.dietDao().insertRecipe(recipe);
    }

    public void updateRecipe(Recipe recipe) {
        if (database != null) database.dietDao().updateRecipe(recipe);
    }

    public void deleteRecipe(Recipe recipe) {
        if (database != null) database.dietDao().deleteRecipe(recipe);
    }

    public Recipe getRecipeById(int id) {
        return database != null ? database.dietDao().getRecipeById(id) : null;
    }

    public void insertRecipeIngredient(RecipeIngredient recipeIngredient) {
        if (database != null) database.dietDao().insertRecipeIngredient(recipeIngredient);
    }

    public List<RecipeIngredient> getRecipeIngredientsByRecipeId(int recipeId) {
        return database != null ? database.dietDao().getRecipeIngredientsByRecipeId(recipeId) : Collections.emptyList();
    }

    public void logDishConsumption(DishHistory dishHistory) {
        if (database != null) database.dietDao().insertDishHistory(dishHistory);
    }

    public List<DishHistory> getDishHistoryByUserId(int userId) {
        return database != null ? database.dietDao().getDishHistoryByUserId(userId) : Collections.emptyList();
    }

    public void insertFoodItem(FoodItem foodItem) {
        if (database != null) database.dietDao().insertFoodItem(foodItem);
    }

    public FoodItem getFoodItemById(int id) {
        return database != null ? database.dietDao().getFoodItemById(id) : null;
    }

    /* ==========================================================
     * BACKWARD COMPATIBLE STUBS
     * ========================================================== */

    public boolean syncDietData(int userId) {
        return true;
    }

    public String calculateDailyTargets(int profileId) {
        return "{\"caloriesTarget\": 2000, \"status\": \"Active\"}";
    }

    public List<Integer> getRecommendedDishIds(int profileId) {
        return Collections.emptyList();
    }
}
