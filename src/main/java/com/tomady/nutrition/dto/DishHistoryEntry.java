package com.tomady.nutrition.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record DishHistoryEntry(
    @JsonProperty("id") Long id,
    @JsonProperty("dish") @NotNull Dish dish,
    @JsonProperty("consumedAt") LocalDateTime consumedAt
) {}
