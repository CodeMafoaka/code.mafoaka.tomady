package com.tomady.nutrition.service;

import com.tomady.nutrition.dto.UserProfile;
import com.tomady.nutrition.dto.BioRecord;
import com.tomady.nutrition.dto.Dish;
import com.tomady.nutrition.dto.DishHistoryEntry;
import com.tomady.nutrition.dto.NutrientProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DietServiceImpl implements DietService {

    private final Map<String, UserProfile> profiles = new ConcurrentHashMap<>();
    private final Map<String, List<BioRecord>> bioRecords = new ConcurrentHashMap<>();
    private final Map<String, List<DishHistoryEntry>> historyLogs = new ConcurrentHashMap<>();

    private final AtomicLong bioRecordIdGen = new AtomicLong(1);
    private final AtomicLong historyIdGen = new AtomicLong(1);

    public DietServiceImpl() {
        // Pre-populate a test user profile
        String defaultUser = "user123";
        profiles.put(defaultUser, new UserProfile(
            defaultUser,
            List.of("Diabetes", "Hypertension"),
            List.of("Peanuts"),
            List.of("Low Sugar", "High Protein")
        ));

        bioRecords.put(defaultUser, new ArrayList<>(List.of(
            new BioRecord(bioRecordIdGen.getAndIncrement(), 175.0, 70.0, LocalDateTime.now().minusDays(10))
        )));

        // Pre-populate some historical dishes
        Dish defaultDish = new Dish(
            "dish_apple",
            "Apple slices with salt",
            List.of("Apple"),
            List.of(
                new NutrientProperty(101L, "Sugar", 10.4, "g"),
                new NutrientProperty(102L, "Calories", 52.0, "kcal")
            )
        );

        historyLogs.put(defaultUser, new ArrayList<>(List.of(
            new DishHistoryEntry(historyIdGen.getAndIncrement(), defaultDish, LocalDateTime.now().minusDays(1))
        )));
    }

    @Override
    public UserProfile getProfile(String userId) {
        return profiles.computeIfAbsent(userId, id -> new UserProfile(
            id,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>()
        ));
    }

    @Override
    public UserProfile updateProfile(String userId, UserProfile profile) {
        UserProfile updated = new UserProfile(
            userId,
            profile.healthConditions() != null ? profile.healthConditions() : new ArrayList<>(),
            profile.allergies() != null ? profile.allergies() : new ArrayList<>(),
            profile.nutritionGoals() != null ? profile.nutritionGoals() : new ArrayList<>()
        );
        profiles.put(userId, updated);
        return updated;
    }

    @Override
    public BioRecord addBioRecord(String userId, BioRecord bioRecord) {
        BioRecord newRecord = new BioRecord(
            bioRecordIdGen.getAndIncrement(),
            bioRecord.height(),
            bioRecord.weight(),
            bioRecord.recordedAt() != null ? bioRecord.recordedAt() : LocalDateTime.now()
        );
        bioRecords.computeIfAbsent(userId, id -> new ArrayList<>()).add(newRecord);
        return newRecord;
    }

    @Override
    public List<BioRecord> getBioRecords(String userId) {
        return bioRecords.getOrDefault(userId, Collections.emptyList());
    }

    @Override
    public DishHistoryEntry addDishHistoryEntry(String userId, Dish dish) {
        // If dish is logged, aggregate its nutrition first if null
        Dish fullDish = dish;
        if (dish.nutrients() == null || dish.nutrients().isEmpty()) {
            fullDish = calculateDishNutrition(dish.name());
        }

        DishHistoryEntry entry = new DishHistoryEntry(
            historyIdGen.getAndIncrement(),
            fullDish,
            LocalDateTime.now()
        );
        historyLogs.computeIfAbsent(userId, id -> new ArrayList<>()).add(entry);
        return entry;
    }

    @Override
    public List<DishHistoryEntry> getDishHistory(String userId) {
        return historyLogs.getOrDefault(userId, Collections.emptyList());
    }

    @Override
    public Dish calculateDishNutrition(String dishIdOrName) {
        // Calculate nutrition properties by aggregating standard Mock Ingredient Nutrients
        // Simple mock rule:
        // If dish contains "apple" -> add sugar/calories
        // If dish contains "peanut" -> add protein/calories
        // If dish contains "shrimp" -> add protein/cholesterol

        List<String> ingredients = new ArrayList<>();
        List<NutrientProperty> nutrients = new ArrayList<>();
        double totalCalories = 0.0;
        double totalSugar = 0.0;
        double totalProtein = 0.0;
        double totalSodium = 0.0;
        double totalCholesterol = 0.0;

        String name = dishIdOrName;
        String lower = name.toLowerCase();

        if (lower.contains("apple")) {
            ingredients.add("Apple");
            totalCalories += 52.0;
            totalSugar += 10.4;
        }
        if (lower.contains("peanut") || lower.contains("nut")) {
            ingredients.add("Peanuts");
            totalCalories += 567.0;
            totalProtein += 25.8;
            totalSodium += 18.0;
        }
        if (lower.contains("shrimp") || lower.contains("prawn")) {
            ingredients.add("Shrimp");
            totalCalories += 99.0;
            totalProtein += 24.0;
            totalCholesterol += 189.0;
        }

        if (ingredients.isEmpty()) {
            // Default generic ingredient
            ingredients.add("Generic Food Item");
            totalCalories += 120.0;
            totalProtein += 5.0;
        }

        nutrients.add(new NutrientProperty(102L, "Calories", totalCalories, "kcal"));
        if (totalSugar > 0) nutrients.add(new NutrientProperty(101L, "Sugar", totalSugar, "g"));
        if (totalProtein > 0) nutrients.add(new NutrientProperty(104L, "Protein", totalProtein, "g"));
        if (totalSodium > 0) nutrients.add(new NutrientProperty(105L, "Sodium", totalSodium, "mg"));
        if (totalCholesterol > 0) nutrients.add(new NutrientProperty(106L, "Cholesterol", totalCholesterol, "mg"));

        return new Dish(
            "dish_" + UUID.randomUUID().toString().substring(0, 8),
            name,
            ingredients,
            nutrients
        );
    }
}
