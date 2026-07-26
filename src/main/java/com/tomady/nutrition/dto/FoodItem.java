package com.tomady.nutrition.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FoodItem(
    @JsonProperty("id") @NotNull Long id,
    @JsonProperty("name") @NotBlank String name,
    @JsonProperty("nameScientific") String nameScientific,
    @JsonProperty("description") String description,
    @JsonProperty("foodGroup") String foodGroup,
    @JsonProperty("foodSubgroup") String foodSubgroup,
    @JsonProperty("category") String category,
    @JsonProperty("nutrients") List<NutrientProperty> nutrients
) {}
