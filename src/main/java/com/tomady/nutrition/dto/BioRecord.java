package com.tomady.nutrition.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BioRecord(
    @JsonProperty("id") Long id,
    @JsonProperty("height") @NotNull Double height, // in cm
    @JsonProperty("weight") @NotNull Double weight, // in kg
    @JsonProperty("recordedAt") LocalDateTime recordedAt
) {}
