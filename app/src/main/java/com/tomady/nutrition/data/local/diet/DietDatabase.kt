package com.tomady.nutrition.data.local.diet

import com.tomady.nutrition.data.local.diet.dao.BioRecordDao
import com.tomady.nutrition.data.local.diet.dao.DishDao
import com.tomady.nutrition.data.local.diet.dao.DishHistoryDao
import com.tomady.nutrition.data.local.diet.dao.ProfileDao
import com.tomady.nutrition.data.local.diet.dao.RecipeDao
import com.tomady.nutrition.data.local.diet.dao.RecipeIngredientDao
import com.tomady.nutrition.data.local.diet.dao.UserDao
import com.tomady.nutrition.data.local.diet.entity.BioRecord
import com.tomady.nutrition.data.local.diet.entity.Dish
import com.tomady.nutrition.data.local.diet.entity.DishHistory
import com.tomady.nutrition.data.local.diet.entity.Profile
import com.tomady.nutrition.data.local.diet.entity.Recipe
import com.tomady.nutrition.data.local.diet.entity.RecipeIngredient
import com.tomady.nutrition.data.local.diet.entity.User

/**
 * High-level wrapper over the diet Room database DAOs.
 *
 * Provides convenient suspend functions for all diet-domain CRUD operations
 * without exposing DAO internals to the service layer.
 */
class DietDatabase(
    val userDao: UserDao,
    val profileDao: ProfileDao,
    val bioRecordDao: BioRecordDao,
    val dishDao: DishDao,
    val recipeDao: RecipeDao,
    val recipeIngredientDao: RecipeIngredientDao,
    val dishHistoryDao: DishHistoryDao
) {

    // ── User CRUD ──────────────────────────────────────────────────────

    suspend fun insertUser(user: User): Long = userDao.insert(user)
    suspend fun updateUser(user: User) = userDao.update(user)
    suspend fun deleteUser(user: User) = userDao.delete(user)
    suspend fun getUserById(id: String): User? = userDao.getById(id)
    suspend fun getUserByEmail(email: String): User? = userDao.getByEmail(email)

    // ── Profile CRUD ───────────────────────────────────────────────────

    suspend fun insertProfile(profile: Profile): Long = profileDao.insert(profile)
    suspend fun updateProfile(profile: Profile) = profileDao.update(profile)
    suspend fun deleteProfile(profile: Profile) = profileDao.delete(profile)
    suspend fun getProfileById(id: String): Profile? = profileDao.getById(id)
    suspend fun getProfileByUserId(userId: String): Profile? = profileDao.getByUserId(userId)

    // ── BioRecord CRUD ─────────────────────────────────────────────────

    suspend fun insertBioRecord(record: BioRecord): Long = bioRecordDao.insert(record)
    suspend fun updateBioRecord(record: BioRecord) = bioRecordDao.update(record)
    suspend fun deleteBioRecord(record: BioRecord) = bioRecordDao.delete(record)
    suspend fun getBioRecordById(id: String): BioRecord? = bioRecordDao.getById(id)
    suspend fun getBioRecordByUserAndDate(userId: String, date: String): BioRecord? =
        bioRecordDao.getByUserAndDate(userId, date)
    suspend fun getBioRecordsByUserInRange(
        userId: String, startDate: String, endDate: String
    ): List<BioRecord> = bioRecordDao.getByUserInDateRange(userId, startDate, endDate)

    // ── Dish CRUD ──────────────────────────────────────────────────────

    suspend fun insertDish(dish: Dish): Long = dishDao.insert(dish)
    suspend fun updateDish(dish: Dish) = dishDao.update(dish)
    suspend fun deleteDish(dish: Dish) = dishDao.delete(dish)
    suspend fun getDishById(id: String): Dish? = dishDao.getById(id)
    suspend fun searchDishes(query: String): List<Dish> = dishDao.search(query)

    // ── Recipe CRUD ────────────────────────────────────────────────────

    suspend fun insertRecipe(recipe: Recipe): Long = recipeDao.insert(recipe)
    suspend fun updateRecipe(recipe: Recipe) = recipeDao.update(recipe)
    suspend fun deleteRecipe(recipe: Recipe) = recipeDao.delete(recipe)
    suspend fun getRecipeById(id: String): Recipe? = recipeDao.getById(id)

    // ── RecipeIngredient CRUD ──────────────────────────────────────────

    suspend fun insertRecipeIngredient(ingredient: RecipeIngredient): Long =
        recipeIngredientDao.insert(ingredient)
    suspend fun insertRecipeIngredients(ingredients: List<RecipeIngredient>): List<Long> =
        recipeIngredientDao.insertAll(ingredients)
    suspend fun updateRecipeIngredient(ingredient: RecipeIngredient) =
        recipeIngredientDao.update(ingredient)
    suspend fun deleteRecipeIngredient(ingredient: RecipeIngredient) =
        recipeIngredientDao.delete(ingredient)
    suspend fun getIngredientsByRecipe(recipeId: String): List<RecipeIngredient> =
        recipeIngredientDao.getByRecipe(recipeId)

    // ── DishHistory CRUD ───────────────────────────────────────────────

    suspend fun insertDishHistory(history: DishHistory): Long = dishHistoryDao.insert(history)
    suspend fun updateDishHistory(history: DishHistory) = dishHistoryDao.update(history)
    suspend fun deleteDishHistory(history: DishHistory) = dishHistoryDao.delete(history)
    suspend fun getDishHistoryById(id: String): DishHistory? = dishHistoryDao.getById(id)
    suspend fun getDishHistoryByUserAndDate(userId: String, date: String): List<DishHistory> =
        dishHistoryDao.getByUserAndDate(userId, date)
    suspend fun getDishHistoryByUserInRange(
        userId: String, startDate: String, endDate: String
    ): List<DishHistory> = dishHistoryDao.getByUserInDateRange(userId, startDate, endDate)
    suspend fun getDishHistoryByUserDateAndMealType(
        userId: String, date: String, mealType: String
    ): List<DishHistory> = dishHistoryDao.getByUserDateAndMealType(userId, date, mealType)
}
