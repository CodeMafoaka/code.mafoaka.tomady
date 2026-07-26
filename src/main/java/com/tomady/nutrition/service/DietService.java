package com.tomady.nutrition.service;

import com.tomady.nutrition.dto.UserProfile;
import com.tomady.nutrition.dto.BioRecord;
import com.tomady.nutrition.dto.Dish;
import com.tomady.nutrition.dto.DishHistoryEntry;
import java.util.List;

public interface DietService {
    UserProfile getProfile(String userId);
    UserProfile updateProfile(String userId, UserProfile profile);
    BioRecord addBioRecord(String userId, BioRecord bioRecord);
    List<BioRecord> getBioRecords(String userId);
    DishHistoryEntry addDishHistoryEntry(String userId, Dish dish);
    List<DishHistoryEntry> getDishHistory(String userId);
    Dish calculateDishNutrition(String dishId);
}
