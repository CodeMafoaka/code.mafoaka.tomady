package com.tomady.nutrition.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record Dish(
    @JsonProperty("id") String id,
    @JsonProperty("name") @NotBlank String name,
    @JsonProperty("ingredients") @NotEmpty List<String> ingredients,
    @JsonProperty("nutrients") List<NutrientProperty> nutrients
) {}
