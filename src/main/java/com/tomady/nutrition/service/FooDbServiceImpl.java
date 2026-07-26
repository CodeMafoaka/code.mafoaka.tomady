package com.tomady.nutrition.service;

import com.tomady.nutrition.dto.FoodItem;
import com.tomady.nutrition.dto.NutrientProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FooDbServiceImpl implements FooDbService {

    // Remote database mock (simulates external search)
    private final Map<Long, FoodItem> remoteDb = new ConcurrentHashMap<>();
    // Local cache database mock (representing cache-first logic)
    private final Map<Long, FoodItem> localCacheDb = new ConcurrentHashMap<>();

    public FooDbServiceImpl() {
        // Initialize remote DB with some mockup data
        FoodItem food1 = new FoodItem(
            1L, "Apple", "Malus domestica", "Fresh crunchy red apple",
            "Fruits", "Apples", "Fresh Foods",
            List.of(
                new NutrientProperty(101L, "Sugar", 10.4, "g"),
                new NutrientProperty(102L, "Calories", 52.0, "kcal"),
                new NutrientProperty(103L, "Vitamin C", 4.6, "mg")
            )
        );
        FoodItem food2 = new FoodItem(
            2L, "Peanuts", "Arachis hypogaea", "Roasted salted peanuts",
            "Nuts", "Legumes", "Snacks",
            List.of(
                new NutrientProperty(102L, "Calories", 567.0, "kcal"),
                new NutrientProperty(104L, "Protein", 25.8, "g"),
                new NutrientProperty(105L, "Sodium", 18.0, "mg")
            )
        );
        FoodItem food3 = new FoodItem(
            3L, "Shrimp", "Caridea", "Boiled pink shrimp",
            "Seafood", "Shellfish", "Fresh Foods",
            List.of(
                new NutrientProperty(102L, "Calories", 99.0, "kcal"),
                new NutrientProperty(104L, "Protein", 24.0, "g"),
                new NutrientProperty(106L, "Cholesterol", 189.0, "mg")
            )
        );

        remoteDb.put(1L, food1);
        remoteDb.put(2L, food2);
        remoteDb.put(3L, food3);

        // Pre-populate cache with Apple only to demonstrate cache-first hits vs misses
        localCacheDb.put(1L, food1);
    }

    @Override
    public List<FoodItem> searchFoods(String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>(localCacheDb.values());
        }
        String lowerQuery = query.toLowerCase();
        // Return matching items from remote simulation
        return remoteDb.values().stream()
            .filter(f -> f.name().toLowerCase().contains(lowerQuery)
                      || (f.description() != null && f.description().toLowerCase().contains(lowerQuery)))
            .toList();
    }

    @Override
    public FoodItem getFoodById(Long id) {
        // Cache-first check
        if (localCacheDb.containsKey(id)) {
            System.out.println("Cache HIT for foodId: " + id);
            return localCacheDb.get(id);
        }

        // Cache miss -> Fetch from remote DB, and cache it locally
        if (remoteDb.containsKey(id)) {
            System.out.println("Cache MISS. Fetching from remote and caching foodId: " + id);
            FoodItem food = remoteDb.get(id);
            localCacheDb.put(id, food);
            return food;
        }

        throw new NoSuchElementException("Food item with ID " + id + " not found in local or remote FooDB.");
    }
}
