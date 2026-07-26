package com.tomady.nutrition.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record RawPromptRequest(
    @JsonProperty("prompt") @NotBlank String prompt
) {}
