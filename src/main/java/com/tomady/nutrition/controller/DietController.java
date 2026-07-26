package com.tomady.nutrition.controller;

import com.tomady.nutrition.dto.BioRecord;
import com.tomady.nutrition.dto.Dish;
import com.tomady.nutrition.dto.DishHistoryEntry;
import com.tomady.nutrition.dto.UserProfile;
import com.tomady.nutrition.service.DietService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1")
@Tag(name = "Diet Service", description = "Endpoints for profiles, biometrics, dish tracking, and nutrition calculation")
public class DietController {

    private final DietService dietService;

    public DietController(DietService dietService) {
        this.dietService = dietService;
    }

    @GetMapping("/users/{userId}/profile")
    @Operation(
        summary = "Get user profile",
        description = "Retrieve health conditions, allergies, and nutrition goals for a given user.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful profile retrieval",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfile.class)))
        }
    )
    public ResponseEntity<UserProfile> getProfile(
            @Parameter(description = "User ID string", required = true)
            @PathVariable("userId") String userId) {
        return ResponseEntity.ok(dietService.getProfile(userId));
    }

    @PutMapping("/users/{userId}/profile")
    @Operation(
        summary = "Update user profile",
        description = "Create or update user health conditions, allergies, and nutrition goals.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfile.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload provided", content = @Content)
        }
    )
    public ResponseEntity<UserProfile> updateProfile(
            @Parameter(description = "User ID string", required = true)
            @PathVariable("userId") String userId,
            @Valid @RequestBody UserProfile profile) {
        return ResponseEntity.ok(dietService.updateProfile(userId, profile));
    }

    @PostMapping("/users/{userId}/biorecords")
    @Operation(
        summary = "Log biometrics",
        description = "Submit height and weight biometric records for a user.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Biometric record registered successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BioRecord.class))),
            @ApiResponse(responseCode = "400", description = "Validation or payload error", content = @Content)
        }
    )
    public ResponseEntity<BioRecord> addBioRecord(
            @Parameter(description = "User ID string", required = true)
            @PathVariable("userId") String userId,
            @Valid @RequestBody BioRecord bioRecord) {
        BioRecord created = dietService.addBioRecord(userId, bioRecord);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/users/{userId}/biorecords")
    @Operation(
        summary = "Get biometrics",
        description = "Retrieve all recorded biometrics height and weight entries for a user.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful bio records retrieval",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BioRecord.class)))
        }
    )
    public ResponseEntity<List<BioRecord>> getBioRecords(
            @Parameter(description = "User ID string", required = true)
            @PathVariable("userId") String userId) {
        return ResponseEntity.ok(dietService.getBioRecords(userId));
    }

    @PostMapping("/users/{userId}/history")
    @Operation(
        summary = "Log meal consumption",
        description = "Record a dish or meal consumption event for a user.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Meal recorded successfully in consumption history",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = DishHistoryEntry.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload", content = @Content)
        }
    )
    public ResponseEntity<DishHistoryEntry> addDishHistory(
            @Parameter(description = "User ID string", required = true)
            @PathVariable("userId") String userId,
            @Valid @RequestBody Dish dish) {
        DishHistoryEntry created = dietService.addDishHistoryEntry(userId, dish);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/users/{userId}/history")
    @Operation(
        summary = "Query meal consumption history",
        description = "Fetch consumption event log list for a user.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful history retrieval",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = DishHistoryEntry.class)))
        }
    )
    public ResponseEntity<List<DishHistoryEntry>> getDishHistory(
            @Parameter(description = "User ID string", required = true)
            @PathVariable("userId") String userId) {
        return ResponseEntity.ok(dietService.getDishHistory(userId));
    }

    @GetMapping("/dishes/{dishId}/nutrition")
    @Operation(
        summary = "Calculate dish nutrition",
        description = "Computes aggregated macronutrients and micronutrients of a dish by name or ID.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Nutrition aggregated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Dish.class)))
        }
    )
    public ResponseEntity<Dish> calculateDishNutrition(
            @Parameter(description = "The ID or name of the dish", required = true)
            @PathVariable("dishId") String dishId) {
        return ResponseEntity.ok(dietService.calculateDishNutrition(dishId));
    }
}
