package com.tomady.nutrition.controller;

import com.tomady.nutrition.dto.FoodItem;
import com.tomady.nutrition.service.FooDbService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/food")
@Tag(name = "FooDB Service", description = "Endpoints for searching and retrieving details of food items and nutrients")
public class FooDbController {

    private final FooDbService fooDbService;

    public FooDbController(FooDbService fooDbService) {
        this.fooDbService = fooDbService;
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search foods",
        description = "Searches cached or remote food records matching the query string.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful search operation",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FoodItem.class)))
        }
    )
    public ResponseEntity<List<FoodItem>> searchFoods(
            @Parameter(description = "Search query string", required = true)
            @RequestParam("q") String query) {
        return ResponseEntity.ok(fooDbService.searchFoods(query));
    }

    @GetMapping("/{foodId}")
    @Operation(
        summary = "Retrieve food details",
        description = "Returns full details and nutrient properties of a specific food item using a cache-first approach.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Food item details retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FoodItem.class))),
            @ApiResponse(responseCode = "404", description = "Food item not found", content = @Content)
        }
    )
    public ResponseEntity<FoodItem> getFoodById(
            @Parameter(description = "The ID of the food item", required = true)
            @PathVariable("foodId") Long foodId) {
        return ResponseEntity.ok(fooDbService.getFoodById(foodId));
    }
}
