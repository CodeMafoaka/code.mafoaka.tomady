package com.tomady.nutrition;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.tomady.nutrition.data.local.diet.*;
import com.tomady.nutrition.data.local.foodb.*;
import com.tomady.nutrition.service.gemma.*;
import com.tomady.nutrition.service.diet.*;
import com.tomady.nutrition.service.foodb.*;
import com.tomady.nutrition.worker.*;
import com.tomady.nutrition.bridge.*;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.ReactApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArchitecturalVerificationTest {

    // Helper classes for testing Database & DAO behaviour in a JVM-only environment
    static class FakeFooDBLocalDao implements FooDBLocalDao {
        private final List<FoodItem> foodItems = new ArrayList<>();
        private final List<NutrientProperty> nutrientProperties = new ArrayList<>();

        @Override
        public void insertFoodItem(FoodItem foodItem) {
            foodItems.add(foodItem);
        }

        @Override
        public void insertNutrientProperties(List<NutrientProperty> properties) {
            nutrientProperties.addAll(properties);
        }

        @Override
        public FoodItem getFoodItemById(int id) {
            for (FoodItem item : foodItems) {
                if (item.getId() == id) {
                    return item;
                }
            }
            return null;
        }

        @Override
        public List<NutrientProperty> getNutrientPropertiesByFoodId(int foodItemId) {
            List<NutrientProperty> result = new ArrayList<>();
            for (NutrientProperty prop : nutrientProperties) {
                if (prop.getFoodItemId() == foodItemId) {
                    result.add(prop);
                }
            }
            return result;
        }

        @Override
        public List<FoodItem> searchLocalFoods(String query) {
            String cleanQuery = query.replace("%", "").toLowerCase();
            List<FoodItem> result = new ArrayList<>();
            for (FoodItem item : foodItems) {
                if (item.getName().toLowerCase().contains(cleanQuery)) {
                    result.add(item);
                }
            }
            return result;
        }
    }

    static class FakeFooDBLocalDatabase extends FooDBLocalDatabase {
        private final FooDBLocalDao dao = new FakeFooDBLocalDao();

        @Override
        public FooDBLocalDao fooDBLocalDao() {
            return dao;
        }
    }

    static class FakeDietDao implements DietDao {
        private final List<User> users = new ArrayList<>();
        private final List<Profile> profiles = new ArrayList<>();
        private final List<BioRecord> bioRecords = new ArrayList<>();
        private final List<Dish> dishes = new ArrayList<>();
        private final List<Recipe> recipes = new ArrayList<>();
        private final List<RecipeIngredient> recipeIngredients = new ArrayList<>();
        private final List<DishHistory> dishHistories = new ArrayList<>();
        private final List<FoodItem> foodItems = new ArrayList<>();
        private final List<NutrientProperty> nutrientProperties = new ArrayList<>();

        @Override public void insertUser(User user) { if (user.getId() == 0) user.setId(users.size()+1); users.add(user); }
        @Override public void updateUser(User user) { /* no-op in fake */ }
        @Override public void deleteUser(User user) { users.remove(user); }
        @Override public User getUserById(int id) {
            for (User u : users) if (u.getId() == id) return u;
            return null;
        }
        @Override public List<User> getAllUsers() { return users; }

        @Override public void insertProfile(Profile profile) { if (profile.getId() == 0) profile.setId(profiles.size()+1); profiles.add(profile); }
        @Override public void updateProfile(Profile profile) { /* no-op in fake */ }
        @Override public void deleteProfile(Profile profile) { profiles.remove(profile); }
        @Override public Profile getProfileById(int id) {
            for (Profile p : profiles) if (p.getId() == id) return p;
            return null;
        }
        @Override public Profile getProfileByUserId(int userId) {
            for (Profile p : profiles) if (p.getUserId() == userId) return p;
            return null;
        }
        @Override public List<Profile> getAllProfiles() { return profiles; }

        @Override public void insertBioRecord(BioRecord bioRecord) { if (bioRecord.getId() == 0) bioRecord.setId(bioRecords.size()+1); bioRecords.add(bioRecord); }
        @Override public void updateBioRecord(BioRecord bioRecord) { /* no-op in fake */ }
        @Override public void deleteBioRecord(BioRecord bioRecord) { bioRecords.remove(bioRecord); }
        @Override public BioRecord getBioRecordById(int id) {
            for (BioRecord b : bioRecords) if (b.getId() == id) return b;
            return null;
        }
        @Override public List<BioRecord> getBioRecordsByProfileId(int profileId) {
            List<BioRecord> res = new ArrayList<>();
            for (BioRecord b : bioRecords) if (b.getProfileId() == profileId) res.add(b);
            return res;
        }
        @Override public List<BioRecord> getAllBioRecords() { return bioRecords; }

        @Override public void insertDish(Dish dish) { if (dish.getId() == 0) dish.setId(dishes.size()+1); dishes.add(dish); }
        @Override public void updateDish(Dish dish) { /* no-op in fake */ }
        @Override public void deleteDish(Dish dish) { dishes.remove(dish); }
        @Override public Dish getDishById(int id) {
            for (Dish d : dishes) if (d.getId() == id) return d;
            return null;
        }
        @Override public List<Dish> getAllDishes() { return dishes; }

        @Override public void insertRecipe(Recipe recipe) { if (recipe.getId() == 0) recipe.setId(recipes.size()+1); recipes.add(recipe); }
        @Override public void updateRecipe(Recipe recipe) { /* no-op in fake */ }
        @Override public void deleteRecipe(Recipe recipe) { recipes.remove(recipe); }
        @Override public Recipe getRecipeById(int id) {
            for (Recipe r : recipes) if (r.getId() == id) return r;
            return null;
        }
        @Override public List<Recipe> getAllRecipes() { return recipes; }

        @Override public void insertRecipeIngredient(RecipeIngredient recipeIngredient) { if (recipeIngredient.getId() == 0) recipeIngredient.setId(recipeIngredients.size()+1); recipeIngredients.add(recipeIngredient); }
        @Override public void updateRecipeIngredient(RecipeIngredient recipeIngredient) { /* no-op in fake */ }
        @Override public void deleteRecipeIngredient(RecipeIngredient recipeIngredient) { recipeIngredients.remove(recipeIngredient); }
        @Override public RecipeIngredient getRecipeIngredientById(int id) {
            for (RecipeIngredient ri : recipeIngredients) if (ri.getId() == id) return ri;
            return null;
        }
        @Override public List<RecipeIngredient> getRecipeIngredientsByRecipeId(int recipeId) {
            List<RecipeIngredient> res = new ArrayList<>();
            for (RecipeIngredient ri : recipeIngredients) if (ri.getRecipeId() == recipeId) res.add(ri);
            return res;
        }
        @Override public List<RecipeIngredient> getAllRecipeIngredients() { return recipeIngredients; }

        @Override public void insertDishHistory(DishHistory dishHistory) { if (dishHistory.getId() == 0) dishHistory.setId(dishHistories.size()+1); dishHistories.add(dishHistory); }
        @Override public void updateDishHistory(DishHistory dishHistory) { /* no-op in fake */ }
        @Override public void deleteDishHistory(DishHistory dishHistory) { dishHistories.remove(dishHistory); }
        @Override public DishHistory getDishHistoryById(int id) {
            for (DishHistory h : dishHistories) if (h.getId() == id) return h;
            return null;
        }
        @Override public List<DishHistory> getDishHistoryByUserId(int userId) {
            List<DishHistory> res = new ArrayList<>();
            for (DishHistory h : dishHistories) if (h.getUserId() == userId) res.add(h);
            return res;
        }
        @Override public List<DishHistory> getAllDishHistory() { return dishHistories; }

        @Override public void insertFoodItem(FoodItem foodItem) { if (foodItem.getId() == 0) foodItem.setId(foodItems.size()+1); foodItems.add(foodItem); }
        @Override public void updateFoodItem(FoodItem foodItem) { /* no-op in fake */ }
        @Override public void deleteFoodItem(FoodItem foodItem) { foodItems.remove(foodItem); }
        @Override public FoodItem getFoodItemById(int id) {
            for (FoodItem fi : foodItems) if (fi.getId() == id) return fi;
            return null;
        }
        @Override public List<FoodItem> getAllFoodItems() { return foodItems; }

        @Override public void insertNutrientProperty(NutrientProperty nutrientProperty) { if (nutrientProperty.getId() == 0) nutrientProperty.setId(nutrientProperties.size()+1); nutrientProperties.add(nutrientProperty); }
        @Override public void updateNutrientProperty(NutrientProperty nutrientProperty) { /* no-op in fake */ }
        @Override public void deleteNutrientProperty(NutrientProperty nutrientProperty) { nutrientProperties.remove(nutrientProperty); }
        @Override public NutrientProperty getNutrientPropertyById(int id) {
            for (NutrientProperty np : nutrientProperties) if (np.getId() == id) return np;
            return null;
        }
        @Override public List<NutrientProperty> getAllNutrientProperties() { return nutrientProperties; }
    }

    static class FakeDietDatabase extends DietDatabase {
        private final DietDao dao = new FakeDietDao();

        @Override
        public DietDao dietDao() {
            return dao;
        }
    }

    // Helper class to capture React Native Promise status
    static class MockPromise implements Promise {
        public Object resolvedValue = null;
        public String rejectCode = null;
        public String rejectMessage = null;

        @Override
        public void resolve(Object value) {
            this.resolvedValue = value;
        }

        @Override
        public void reject(String code, String message) {
            this.rejectCode = code;
            this.rejectMessage = message;
        }

        @Override
        public void reject(Throwable throwable) {
            this.rejectCode = "ERROR";
            this.rejectMessage = throwable.getMessage();
        }
    }

    @Test
    public void testEntityInstantiation() {
        User user = new User();
        user.setId(1);
        user.setName("Test User");
        user.setEmail("test@tomady.com");

        assertEquals(1, user.getId());
        assertEquals("Test User", user.getName());
        assertEquals("test@tomady.com", user.getEmail());
    }

    @Test
    public void testFoodBEntityInstantiation() {
        Compound compound = new Compound();
        compound.setId(101);
        compound.setName("Resveratrol");
        compound.setPublicId("FDB0001");

        assertEquals(101, compound.getId());
        assertEquals("Resveratrol", compound.getName());
        assertEquals("FDB0001", compound.getPublicId());
    }

    @Test
    public void testServiceStubs() {
        GemmaAndroidService gemmaService = new GemmaAndroidService();
        assertTrue(gemmaService.initializeModel());
        assertEquals("Error: Gemma model is not loaded.", new GemmaAndroidService().generateSuggestion("test"));
    }

    @Test
    public void testRNBridgeStubs() {
        GemmaModule gemmaModule = new GemmaModule(null);
        assertEquals("GemmaModule", gemmaModule.getName());
    }

    @Test
    public void testCacheFirstLogicAndDatabasePersistence() throws IOException {
        FakeFooDBLocalDatabase fakeDb = new FakeFooDBLocalDatabase();
        FooDBDataAPIService service = new FooDBDataAPIService(fakeDb);

        // Initially cache should be empty
        assertNull(fakeDb.fooDBLocalDao().getFoodItemById(123));

        // Get food details (should trigger a remote fetch and save to DB)
        FooDBDataAPIService.FoodDetails details = service.getFoodDetails(123L);
        assertNotNull(details);
        assertEquals("Remote Food #123", details.getFoodItem().getName());
        assertEquals(2, details.getNutrientProperties().size());

        // Now, verify it has been written to the database cache
        FoodItem cachedFood = fakeDb.fooDBLocalDao().getFoodItemById(123);
        assertNotNull(cachedFood);
        assertEquals("Remote Food #123", cachedFood.getName());

        // Modify local cache to verify subsequent lookup gets cache, not remote
        cachedFood.setName("Modified Cached Food");
        // Get details again (cache-hit logic)
        FooDBDataAPIService.FoodDetails detailsCached = service.getFoodDetails(123L);
        assertNotNull(detailsCached);
        assertEquals("Modified Cached Food", detailsCached.getFoodItem().getName());
    }

    @Test
    public void testSearchFoodFunctionality() {
        FakeFooDBLocalDatabase fakeDb = new FakeFooDBLocalDatabase();
        FooDBDataAPIService service = new FooDBDataAPIService(fakeDb);

        FoodItem f1 = new FoodItem();
        f1.setId(1);
        f1.setName("Apple Pie");
        f1.setGroupName("Desserts");
        fakeDb.fooDBLocalDao().insertFoodItem(f1);

        FoodItem f2 = new FoodItem();
        f2.setId(2);
        f2.setName("Pineapple Juice");
        f2.setGroupName("Beverages");
        fakeDb.fooDBLocalDao().insertFoodItem(f2);

        // Search for "apple"
        List<FoodItem> results = service.searchFood("apple");
        assertEquals(2, results.size()); // both Apple Pie and Pineapple Juice contain "apple"

        List<FoodItem> resultsPie = service.searchFood("pie");
        assertEquals(1, resultsPie.size());
        assertEquals("Apple Pie", resultsPie.get(0).getName());
    }

    @Test
    public void testReactBridgeFoodModuleSuccessfulLookup() {
        FakeFooDBLocalDatabase fakeDb = new FakeFooDBLocalDatabase();
        FooDBDataAPIService service = new FooDBDataAPIService(fakeDb);
        FooDBModule module = new FooDBModule(null, service);

        MockPromise promise = new MockPromise();
        module.getFoodDetails(15L, promise);

        assertNotNull(promise.resolvedValue);
        assertTrue(promise.resolvedValue instanceof WritableMap);

        WritableMap map = (WritableMap) promise.resolvedValue;
        assertTrue(map.hasKey("foodItem"));
        assertTrue(map.hasKey("nutrientProperties"));

        WritableMap foodItemMap = (WritableMap) map.getMap("foodItem");
        assertEquals("Remote Food #15", foodItemMap.getString("name"));

        WritableArray nutrientsArray = (WritableArray) map.getArray("nutrientProperties");
        assertEquals(2, nutrientsArray.size());
    }

    @Test
    public void testReactBridgeFoodModuleNetworkFailure() {
        FakeFooDBLocalDatabase fakeDb = new FakeFooDBLocalDatabase();
        FooDBDataAPIService service = new FooDBDataAPIService(fakeDb);
        FooDBModule module = new FooDBModule(null, service);

        MockPromise promise = new MockPromise();
        // ID 999 triggers simulated Network Failure
        module.getFoodDetails(999L, promise);

        assertNull(promise.resolvedValue);
        assertEquals("NETWORK_FAILURE", promise.rejectCode);
        assertTrue(promise.rejectMessage.contains("Remote connection timed out"));
    }

    @Test
    public void testReactBridgeFoodModuleSearch() {
        FakeFooDBLocalDatabase fakeDb = new FakeFooDBLocalDatabase();
        FooDBDataAPIService service = new FooDBDataAPIService(fakeDb);
        FooDBModule module = new FooDBModule(null, service);

        FoodItem f = new FoodItem();
        f.setId(42);
        f.setName("Broccoli");
        f.setGroupName("Vegetables");
        fakeDb.fooDBLocalDao().insertFoodItem(f);

        MockPromise promise = new MockPromise();
        module.searchFood("broc", promise);

        assertNotNull(promise.resolvedValue);
        assertTrue(promise.resolvedValue instanceof WritableArray);

        WritableArray array = (WritableArray) promise.resolvedValue;
        assertEquals(1, array.size());

        WritableMap map = (WritableMap) array.getMap(0);
        assertEquals(42, map.getInt("id"));
        assertEquals("Broccoli", map.getString("name"));
    }

    @Test
    public void testDietAPIServiceNutritionalAggregation() {
        FakeDietDatabase fakeDietDb = new FakeDietDatabase();
        FakeFooDBLocalDatabase fakeFooDb = new FakeFooDBLocalDatabase();
        FooDBDataAPIService fooDbService = new FooDBDataAPIService(fakeFooDb);

        DietAPIService dietService = new DietAPIService(fakeDietDb, fooDbService);

        // Let's configure a Recipe (id = 1) with 2 RecipeIngredients mapping to FooDB IDs
        Recipe recipe = new Recipe();
        recipe.setId(1);
        recipe.setTitle("Nutritious Salad");
        fakeDietDb.dietDao().insertRecipe(recipe);

        // Ingredient 1: 200g of Food Item 10 (which contains protein and calories)
        RecipeIngredient ri1 = new RecipeIngredient();
        ri1.setId(1);
        ri1.setRecipeId(1);
        ri1.setFoodItemId(10);
        ri1.setQuantity(200.0);
        fakeDietDb.dietDao().insertRecipeIngredient(ri1);

        // Ingredient 2: 50g of Food Item 11 (high sugar)
        RecipeIngredient ri2 = new RecipeIngredient();
        ri2.setId(2);
        ri2.setRecipeId(1);
        ri2.setFoodItemId(11);
        ri2.setQuantity(50.0);
        fakeDietDb.dietDao().insertRecipeIngredient(ri2);

        // Dish associating with Recipe 1
        Dish dish = new Dish();
        dish.setId(5);
        dish.setName("Salad bowl");
        dish.setRecipeId(1);
        dish.setCalories(350.0);
        fakeDietDb.dietDao().insertDish(dish);

        DietAPIService.DishNutritionalValue values = dietService.getDishNutritionalValue(5);
        assertNotNull(values);
        assertEquals(3.0, values.getProtein(), 0.001);
    }

    @Test
    public void testProfileValidationForDiabetesConflict() {
        FakeDietDatabase fakeDietDb = new FakeDietDatabase();
        FakeFooDBLocalDatabase fakeFooDb = new FakeFooDBLocalDatabase();
        FooDBDataAPIService fooDbService = new FooDBDataAPIService(fakeFooDb);

        DietAPIService dietService = new DietAPIService(fakeDietDb, fooDbService);

        // Create Profile (Diabetes)
        Profile profile = new Profile();
        profile.setId(1);
        profile.setUserId(42);
        profile.setDiseases("Diabetes Type II, Hypertension");
        fakeDietDb.dietDao().insertProfile(profile);

        // Create a dish with high sugar content
        Recipe recipe = new Recipe();
        recipe.setId(2);
        recipe.setTitle("Sweet Dessert");
        fakeDietDb.dietDao().insertRecipe(recipe);

        FoodItem foodItem = new FoodItem();
        foodItem.setId(99);
        foodItem.setName("Sugar Cube");
        fakeFooDb.fooDBLocalDao().insertFoodItem(foodItem);

        NutrientProperty sugarProp = new NutrientProperty();
        sugarProp.setFoodItemId(99);
        sugarProp.setPropertyName("Sugar");
        sugarProp.setPropertyValue(98.0); // 98g of sugar per 100g
        fakeFooDb.fooDBLocalDao().insertNutrientProperties(List.of(sugarProp));

        RecipeIngredient ri = new RecipeIngredient();
        ri.setRecipeId(2);
        ri.setFoodItemId(99);
        ri.setQuantity(50.0); // 50g -> 49g sugar
        fakeDietDb.dietDao().insertRecipeIngredient(ri);

        Dish dish = new Dish();
        dish.setId(12);
        dish.setName("Sweet Candy");
        dish.setRecipeId(2);
        fakeDietDb.dietDao().insertDish(dish);

        // Calculate and validate
        DietAPIService.DishNutritionalValue nutrition = dietService.getDishNutritionalValue(12);
        assertEquals(49.0, nutrition.getSugar(), 0.001);

        DietAPIService.ValidationResult validation = dietService.validateProfileDishConflict(1, 12);
        assertFalse(validation.isValid());
        assertEquals(1, validation.getWarnings().size());
        assertTrue(validation.getWarnings().get(0).contains("High sugar conflict"));
    }

    @Test
    public void testProfileValidationForAllergenConflict() {
        FakeDietDatabase fakeDietDb = new FakeDietDatabase();
        FakeFooDBLocalDatabase fakeFooDb = new FakeFooDBLocalDatabase();
        FooDBDataAPIService fooDbService = new FooDBDataAPIService(fakeFooDb);

        DietAPIService dietService = new DietAPIService(fakeDietDb, fooDbService);

        // Profile allergic to Peanut
        Profile profile = new Profile();
        profile.setId(2);
        profile.setUserId(43);
        profile.setAllergies("peanut, shell-fish");
        fakeDietDb.dietDao().insertProfile(profile);

        Recipe recipe = new Recipe();
        recipe.setId(3);
        fakeDietDb.dietDao().insertRecipe(recipe);

        RecipeIngredient ri = new RecipeIngredient();
        ri.setRecipeId(3);
        ri.setFoodItemId(50); // Food item is Peanut
        fakeDietDb.dietDao().insertRecipeIngredient(ri);

        // Insert FoodItem mapping in Diet DB so its name is known during validation
        FoodItem foodItem = new FoodItem();
        foodItem.setId(50);
        foodItem.setName("Peanut Butter");
        foodItem.setGroupName("Nuts");
        fakeDietDb.dietDao().insertFoodItem(foodItem);

        Dish dish = new Dish();
        dish.setId(15);
        dish.setName("Peanut Butter Toast");
        dish.setRecipeId(3);
        fakeDietDb.dietDao().insertDish(dish);

        // Validate allergy conflict
        DietAPIService.ValidationResult validation = dietService.validateProfileDishConflict(2, 15);
        assertFalse(validation.isValid());
        assertEquals(1, validation.getWarnings().size());
        assertTrue(validation.getWarnings().get(0).contains("Allergen warning"));
        assertTrue(validation.getWarnings().get(0).contains("Peanut Butter"));
    }

    @Test
    public void testDietModuleReactBridge() {
        FakeDietDatabase fakeDietDb = new FakeDietDatabase();
        DietAPIService dietService = new DietAPIService(fakeDietDb, null);
        DietModule module = new DietModule(null, dietService);

        // Populate Profile
        Profile profile = new Profile();
        profile.setId(10);
        profile.setUserId(500);
        profile.setAge(30);
        profile.setHeight(175.5);
        profile.setWeight(70.2);
        profile.setAllergies("None");
        profile.setDiseases("None");
        fakeDietDb.dietDao().insertProfile(profile);

        // 1. Test getProfile
        MockPromise getProfilePromise = new MockPromise();
        module.getProfile(500.0, getProfilePromise);

        assertNotNull(getProfilePromise.resolvedValue);
        assertTrue(getProfilePromise.resolvedValue instanceof WritableMap);
        WritableMap resolvedMap = (WritableMap) getProfilePromise.resolvedValue;
        assertEquals(10, resolvedMap.getInt("id"));
        assertEquals(500, resolvedMap.getInt("userId"));
        assertEquals(30, resolvedMap.getInt("age"));

        // 2. Test updateProfile
        WritableMap inputMap = Arguments.createMap();
        inputMap.putInt("id", 10);
        inputMap.putInt("userId", 500);
        inputMap.putInt("age", 35); // update age
        inputMap.putDouble("height", 175.5);
        inputMap.putDouble("weight", 72.0); // update weight
        inputMap.putString("allergies", "Gluten");
        inputMap.putString("diseases", "None");

        MockPromise updatePromise = new MockPromise();
        module.updateProfile(inputMap, updatePromise);

        assertNotNull(updatePromise.resolvedValue);
        WritableMap updatedMap = (WritableMap) updatePromise.resolvedValue;
        assertEquals(35, updatedMap.getInt("age"));
        assertEquals(72.0, updatedMap.getDouble("weight"));
        assertEquals("Gluten", updatedMap.getString("allergies"));

        // 3. Test logDishConsumption and getDishHistory
        MockPromise logPromise = new MockPromise();
        module.logDishConsumption(500.0, 123.0, "2024-10-31T12:00:00Z", logPromise);

        assertNotNull(logPromise.resolvedValue);
        WritableMap logResult = (WritableMap) logPromise.resolvedValue;
        assertEquals(500, logResult.getInt("userId"));
        assertEquals(123, logResult.getInt("dishId"));

        MockPromise historyPromise = new MockPromise();
        module.getDishHistory(500.0, historyPromise);

        assertNotNull(historyPromise.resolvedValue);
        assertTrue(historyPromise.resolvedValue instanceof WritableArray);
        WritableArray historyArray = (WritableArray) historyPromise.resolvedValue;
        assertEquals(1, historyArray.size());
        assertEquals(123, ((WritableMap) historyArray.getMap(0)).getInt("dishId"));
    }

    @Test
    public void testGemmaAndroidServiceStreaming() {
        GemmaAndroidService service = new GemmaAndroidService();
        service.initializeModel();

        final List<String> tokens = new ArrayList<>();
        service.askQuestionStreaming("Can I drink coca-cola with diabetes?", new GemmaAndroidService.TokenStreamListener() {
            @Override
            public void onToken(String token) {
                tokens.add(token);
            }
        });

        assertFalse(tokens.isEmpty());
        assertTrue(tokens.get(0).length() > 0);
    }

    @Test
    public void testGemmaAndroidServiceRecipeComputationAndValidationPipeline() {
        FakeDietDatabase fakeDietDb = new FakeDietDatabase();
        FakeFooDBLocalDatabase fakeFooDb = new FakeFooDBLocalDatabase();
        FooDBDataAPIService fooDbService = new FooDBDataAPIService(fakeFooDb);

        DietAPIService dietService = new DietAPIService(fakeDietDb, fooDbService);
        GemmaAndroidService gemmaService = new GemmaAndroidService(dietService);
        gemmaService.initializeModel();

        // Let's create a Profile with Diabetes
        Profile profile = new Profile();
        profile.setId(3);
        profile.setUserId(800);
        profile.setDiseases("Diabetes");
        fakeDietDb.dietDao().insertProfile(profile);

        // We pre-register "Coca-Cola" with High Sugar (approx 35g of sugar)
        Recipe r = new Recipe();
        r.setId(4);
        r.setTitle("Drink Coca-Cola");
        fakeDietDb.dietDao().insertRecipe(r);

        FoodItem fi = new FoodItem();
        fi.setId(200);
        fi.setName("Coca-Cola");
        fakeFooDb.fooDBLocalDao().insertFoodItem(fi);

        NutrientProperty sugarProp = new NutrientProperty();
        sugarProp.setFoodItemId(200);
        sugarProp.setPropertyName("Sugar");
        sugarProp.setPropertyValue(10.6); // 10.6g per 100ml
        fakeFooDb.fooDBLocalDao().insertNutrientProperties(List.of(sugarProp));

        RecipeIngredient ri = new RecipeIngredient();
        ri.setRecipeId(4);
        ri.setFoodItemId(200);
        ri.setQuantity(330.0); // 330ml -> 35g sugar
        fakeDietDb.dietDao().insertRecipeIngredient(ri);

        Dish dish = new Dish();
        dish.setId(30);
        dish.setName("Coca-Cola");
        dish.setRecipeId(4);
        fakeDietDb.dietDao().insertDish(dish);

        // Run the pipeline for a Coca-Cola request
        GemmaAndroidService.RecipeResponse resp = gemmaService.computeRecipe("Can I drink coca-cola?", 3);
        assertNotNull(resp);
        assertEquals("Drink Coca-Cola", resp.getRecipeTitle());
        assertFalse(resp.isSafe()); // should be unsafe due to high sugar and diabetes
        assertEquals(1, resp.getWarnings().size());
        assertTrue(resp.getWarnings().get(0).contains("High sugar conflict"));
    }

    @Test
    public void testGemmaModuleReactBridge() {
        FakeDietDatabase fakeDietDb = new FakeDietDatabase();
        DietAPIService dietService = new DietAPIService(fakeDietDb, null);
        GemmaAndroidService gemmaService = new GemmaAndroidService(dietService);
        ReactApplicationContext reactContext = new ReactApplicationContext();
        GemmaModule module = new GemmaModule(reactContext, gemmaService);

        // 1. Initialize model
        MockPromise initPromise = new MockPromise();
        module.initializeModel(initPromise);
        assertTrue((Boolean) initPromise.resolvedValue);

        // 2. Ask question
        MockPromise askPromise = new MockPromise();
        module.askQuestion("Will eating salad help me?", askPromise);
        assertNotNull(askPromise.resolvedValue);
        assertTrue(((String) askPromise.resolvedValue).contains("Salads are excellent"));

        // 3. Compute recipe (Safe option: Salad)
        Profile profile = new Profile();
        profile.setId(4);
        profile.setUserId(900);
        fakeDietDb.dietDao().insertProfile(profile);

        // Insert Dish Salad bowl
        Recipe r = new Recipe();
        r.setId(5);
        r.setTitle("Nutritious Salad");
        fakeDietDb.dietDao().insertRecipe(r);

        Dish d = new Dish();
        d.setId(40);
        d.setName("Salad bowl");
        d.setRecipeId(5);
        fakeDietDb.dietDao().insertDish(d);

        MockPromise computePromise = new MockPromise();
        module.computeRecipe("How to prepare a salad?", 4.0, computePromise);

        assertNotNull(computePromise.resolvedValue);
        assertTrue(computePromise.resolvedValue instanceof WritableMap);
        WritableMap map = (WritableMap) computePromise.resolvedValue;
        assertEquals("Nutritious Salad", map.getString("recipeTitle"));
        assertTrue(map.getBoolean("isSafe"));
    }
}
