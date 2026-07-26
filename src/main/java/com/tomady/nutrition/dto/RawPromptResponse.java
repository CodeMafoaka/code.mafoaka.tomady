package com.tomady.nutrition.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RawPromptResponse(
    @JsonProperty("response") String response
) {}
