package com.tomady.nutrition.service;

import com.tomady.nutrition.dto.FoodItem;
import java.util.List;

public interface FooDbService {
    List<FoodItem> searchFoods(String query);
    FoodItem getFoodById(Long id);
}
