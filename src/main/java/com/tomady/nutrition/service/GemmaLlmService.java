package com.tomady.nutrition.service;

import com.tomady.nutrition.dto.GeneratedRecipe;
import com.tomady.nutrition.dto.ComputeRecipeRequest;
import com.tomady.nutrition.dto.RawPromptRequest;
import com.tomady.nutrition.dto.RawPromptResponse;

public interface GemmaLlmService {
    GeneratedRecipe computeRecipe(ComputeRecipeRequest request);
    RawPromptResponse askGemma(RawPromptRequest request);
}
