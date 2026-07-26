package com.tomady.nutrition.controller;

import com.tomady.nutrition.dto.Dish;
import com.tomady.nutrition.service.DailySuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/worker/suggestions/daily")
@Tag(name = "Suggestion & Background Worker Service", description = "Endpoints representing automated suggestions and manual worker triggers")
public class DailySuggestionController {

    private final DailySuggestionService suggestionService;

    public DailySuggestionController(DailySuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping
    @Operation(
        summary = "Fetch daily dish suggestions",
        description = "Returns current daily suggested foods and recipes.",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of daily suggestions retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dish.class)))
        }
    )
    public ResponseEntity<List<Dish>> getDailySuggestions() {
        return ResponseEntity.ok(suggestionService.getDailySuggestions());
    }

    @PostMapping
    @Operation(
        summary = "Manual suggestions generator trigger",
        description = "Explicitly trigger the worker schedule to re-generate daily suggested meal list.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Daily suggestion generation triggered and completed",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dish.class)))
        }
    )
    public ResponseEntity<List<Dish>> triggerDailySuggestions() {
        return ResponseEntity.ok(suggestionService.triggerDailySuggestions());
    }
}
