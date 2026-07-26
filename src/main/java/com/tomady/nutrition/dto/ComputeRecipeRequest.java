package com.tomady.nutrition.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ComputeRecipeRequest(
    @JsonProperty("dishName") @NotBlank String dishName,
    @JsonProperty("userId") @NotBlank String userId,
    @JsonProperty("preferences") List<String> preferences
) {}
