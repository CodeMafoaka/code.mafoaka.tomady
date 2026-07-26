package com.tomady.nutrition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomady.nutrition.dto.BioRecord;
import com.tomady.nutrition.dto.ComputeRecipeRequest;
import com.tomady.nutrition.dto.Dish;
import com.tomady.nutrition.dto.RawPromptRequest;
import com.tomady.nutrition.dto.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- FooDb Service Tests ---

    @Test
    void testSearchFoods() throws Exception {
        mockMvc.perform(get("/v1/food/search").param("q", "Apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", is("Apple")));
    }

    @Test
    void testGetFoodById_Hit() throws Exception {
        mockMvc.perform(get("/v1/food/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Apple")));
    }

    @Test
    void testGetFoodById_MissAndCache() throws Exception {
        mockMvc.perform(get("/v1/food/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.name", is("Peanuts")));
    }

    @Test
    void testGetFoodById_NotFound() throws Exception {
        mockMvc.perform(get("/v1/food/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.detail", containsString("not found")));
    }

    // --- Diet Service Tests ---

    @Test
    void testGetProfile_Default() throws Exception {
        mockMvc.perform(get("/v1/users/user123/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("user123")))
                .andExpect(jsonPath("$.healthConditions", contains("Diabetes", "Hypertension")))
                .andExpect(jsonPath("$.allergies", contains("Peanuts")));
    }

    @Test
    void testUpdateAndGetProfile() throws Exception {
        UserProfile newProfile = new UserProfile(
                "user123",
                List.of("Diabetes"),
                List.of("Shellfish"),
                List.of("Low Sodium")
        );

        mockMvc.perform(put("/v1/users/user123/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProfile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("user123")))
                .andExpect(jsonPath("$.healthConditions", contains("Diabetes")))
                .andExpect(jsonPath("$.allergies", contains("Shellfish")));
    }

    @Test
    void testAddAndGetBioRecords() throws Exception {
        BioRecord record = new BioRecord(null, 180.0, 75.0, LocalDateTime.now());

        mockMvc.perform(post("/v1/users/user123/biorecords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(record)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.height", is(180.0)))
                .andExpect(jsonPath("$.weight", is(75.0)));

        mockMvc.perform(get("/v1/users/user123/biorecords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void testAddAndGetDishHistory() throws Exception {
        Dish dish = new Dish(null, "Cinnamon Roasted Apple slices", List.of("Apple"), null);

        mockMvc.perform(post("/v1/users/user123/history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dish)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.dish.name", is("Cinnamon Roasted Apple slices")))
                .andExpect(jsonPath("$.dish.nutrients", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/v1/users/user123/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void testCalculateDishNutrition() throws Exception {
        mockMvc.perform(get("/v1/dishes/shrimp/nutrition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("shrimp")))
                .andExpect(jsonPath("$.ingredients", contains("Shrimp")))
                .andExpect(jsonPath("$.nutrients[0].name", is("Calories")));
    }

    // --- Gemma LLM Engine Tests ---

    @Test
    void testComputeRecipe_CompatibleAndAllergyWarning() throws Exception {
        // Prepare profile: user123 has peanut allergy (after the default reset or update above, let's update first)
        UserProfile profile = new UserProfile("user_gemma", List.of("Diabetes"), List.of("Peanuts"), List.of("Low Sugar"));
        mockMvc.perform(put("/v1/users/user_gemma/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk());

        ComputeRecipeRequest req = new ComputeRecipeRequest("Peanut satay", "user_gemma", List.of("Extra spicy"));
        mockMvc.perform(post("/v1/gemma/compute-recipe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatible", is(false)))
                .andExpect(jsonPath("$.safetyAlerts[0]", containsString("Peanuts")));
    }

    @Test
    void testAskGemma() throws Exception {
        RawPromptRequest req = new RawPromptRequest("What is healthy eating?");
        mockMvc.perform(post("/v1/gemma/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response", containsString("Gemma LLM Engine Stub response")));
    }

    // --- Suggestion Worker Tests ---

    @Test
    void testGetAndTriggerDailySuggestions() throws Exception {
        mockMvc.perform(get("/v1/worker/suggestions/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(post("/v1/worker/suggestions/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", containsString("Gemma-Recommended Garlic Shrimp")));
    }
}
