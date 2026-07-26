package com.tomady.nutrition.controller;

import com.tomady.nutrition.dto.ComputeRecipeRequest;
import com.tomady.nutrition.dto.GeneratedRecipe;
import com.tomady.nutrition.dto.RawPromptRequest;
import com.tomady.nutrition.dto.RawPromptResponse;
import com.tomady.nutrition.service.GemmaLlmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/gemma")
@Tag(name = "Gemma LLM Engine Service", description = "Endpoints interacting with local Gemma Large Language Model engine")
public class GemmaLlmController {

    private final GemmaLlmService gemmaLlmService;

    public GemmaLlmController(GemmaLlmService gemmaLlmService) {
        this.gemmaLlmService = gemmaLlmService;
    }

    @PostMapping("/compute-recipe")
    @Operation(
        summary = "Compute safety-verified recipe",
        description = "Generates a tailored recipe based on user preferences and cross-checks safety allergies/conditions.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Recipe generated and safety validation processed successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneratedRecipe.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload details", content = @Content)
        }
    )
    public ResponseEntity<GeneratedRecipe> computeRecipe(@Valid @RequestBody ComputeRecipeRequest request) {
        return ResponseEntity.ok(gemmaLlmService.computeRecipe(request));
    }

    @PostMapping("/ask")
    @Operation(
        summary = "Ask raw prompt",
        description = "Sends raw text instructions/prompts directly to Gemma engine to process and response.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Response processed successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = RawPromptResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid raw prompt instruction", content = @Content)
        }
    )
    public ResponseEntity<RawPromptResponse> askGemma(@Valid @RequestBody RawPromptRequest request) {
        return ResponseEntity.ok(gemmaLlmService.askGemma(request));
    }
}
