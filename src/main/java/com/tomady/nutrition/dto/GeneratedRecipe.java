package com.tomady.nutrition.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GeneratedRecipe(
    @JsonProperty("recipeName") String recipeName,
    @JsonProperty("ingredients") List<String> ingredients,
    @JsonProperty("instructions") String instructions,
    @JsonProperty("safetyAlerts") List<String> safetyAlerts,
    @JsonProperty("compatible") boolean compatible
) {}
