package com.tomady.nutrition.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UserProfile(
    @JsonProperty("userId") @NotBlank String userId,
    @JsonProperty("healthConditions") List<String> healthConditions, // e.g. "Diabetes", "Hypertension", "Heart Disease"
    @JsonProperty("allergies") List<String> allergies,              // e.g. "Peanuts", "Shellfish"
    @JsonProperty("nutritionGoals") List<String> nutritionGoals      // e.g. "Low Sugar", "High Protein"
) {}
