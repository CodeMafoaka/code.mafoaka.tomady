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
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;

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
        assertFalse(gemmaService.initializeModel());
        assertEquals("", gemmaService.generateSuggestion("test"));

        DietAPIService dietService = new DietAPIService();
        assertFalse(dietService.syncDietData(1));
        assertEquals("{}", dietService.calculateDailyTargets(1));
    }

    @Test
    public void testRNBridgeStubs() {
        GemmaModule gemmaModule = new GemmaModule(null);
        assertEquals("GemmaModule", gemmaModule.getName());

        DietModule dietModule = new DietModule(null);
        assertEquals("DietModule", dietModule.getName());

        FooDBModule fooDBModule = new FooDBModule(null);
        assertEquals("FooDBModule", fooDBModule.getName());
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
}
