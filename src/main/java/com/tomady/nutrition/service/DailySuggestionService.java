package com.tomady.nutrition.service;

import com.tomady.nutrition.dto.Dish;
import com.tomady.nutrition.dto.NutrientProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class DailySuggestionService {

    private final List<Dish> dailySuggestions = new CopyOnWriteArrayList<>();

    public DailySuggestionService() {
        // Initialize with default daily suggestions
        dailySuggestions.add(new Dish(
            "sug_1",
            "Apple Salad",
            List.of("Apple", "Lettuce", "Olive Oil"),
            List.of(
                new NutrientProperty(102L, "Calories", 150.0, "kcal"),
                new NutrientProperty(101L, "Sugar", 10.4, "g")
            )
        ));
    }

    public List<Dish> getDailySuggestions() {
        return new ArrayList<>(dailySuggestions);
    }

    public List<Dish> triggerDailySuggestions() {
        // Clear and generate fresh new daily suggestions
        dailySuggestions.clear();
        dailySuggestions.add(new Dish(
            "sug_2",
            "Gemma-Recommended Garlic Shrimp",
            List.of("Shrimp", "Garlic", "Lemon"),
            List.of(
                new NutrientProperty(102L, "Calories", 120.0, "kcal"),
                new NutrientProperty(104L, "Protein", 24.0, "g"),
                new NutrientProperty(106L, "Cholesterol", 189.0, "mg")
            )
        ));
        dailySuggestions.add(new Dish(
            "sug_3",
            "Cinnamon Roasted Apple slices",
            List.of("Apple", "Cinnamon"),
            List.of(
                new NutrientProperty(102L, "Calories", 60.0, "kcal"),
                new NutrientProperty(101L, "Sugar", 10.4, "g")
            )
        ));
        return getDailySuggestions();
    }
}
