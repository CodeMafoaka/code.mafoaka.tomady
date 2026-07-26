package com.tomady.nutrition.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NutrientProperty(
    @JsonProperty("id") @NotNull Long id,
    @JsonProperty("name") @NotBlank String name,
    @JsonProperty("amount") @NotNull Double amount,
    @JsonProperty("unit") @NotBlank String unit
) {}
