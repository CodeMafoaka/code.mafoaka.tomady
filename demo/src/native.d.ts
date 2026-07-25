/**
 * TypeScript type declarations for the Tomady React Native native modules.
 *
 * These types mirror the `@ReactMethod`-annotated methods exposed by:
 * - `TomadyFooDB`  (FooDBModule.kt)
 * - `TomadyDiet`   (DietModule.kt)
 * - `TomadyGemma`  (GemmaModule.kt)
 *
 * All methods follow the RN Promise pattern and DeviceEventEmitter for streaming.
 */

// ══════════════════════════════════════════════════════════════════════════
// Shared
// ══════════════════════════════════════════════════════════════════════════

interface NutrientProperty {
  id: number;
  foodItemId: number;
  nutrientName: string | null;
  amount: number | null;
  unit: string | null;
  standardContent: number | null;
  preparationType: string | null;
  citation: string | null;
}

interface FoodItem {
  id: number;
  publicId: string | null;
  name: string | null;
  nameScientific: string | null;
  description: string | null;
  foodGroup: string | null;
  foodSubgroup: string | null;
  foodType: string | null;
  category: string | null;
}

// ══════════════════════════════════════════════════════════════════════════
// FooDB Module  —  NativeModuleName: 'TomadyFooDB'
// ══════════════════════════════════════════════════════════════════════════

interface FoodDetailResult {
  food: FoodItem;
  nutrients: NutrientProperty[];
}

interface TomadyFooDBNative {
  searchFood(query: string): Promise<FoodItem[]>;
  getNutrients(foodId: number): Promise<FoodDetailResult | null>;
  getFoodGroups(): Promise<string[]>;
  getNutrientNames(): Promise<string[]>;
}

// ══════════════════════════════════════════════════════════════════════════
// Diet Module  —  NativeModuleName: 'TomadyDiet'
// ══════════════════════════════════════════════════════════════════════════

interface UserResult {
  id: string;
  username: string;
  email: string;
  createdAt: number;
  updatedAt: number;
}

interface ProfileResult {
  id: string;
  userId: string;
  displayName: string | null;
  dateOfBirth: string | null;
  heightCm: number | null;
  weightKg: number | null;
  dailyCalorieTarget: number | null;
  proteinGramsTarget: number | null;
  carbsGramsTarget: number | null;
  fatGramsTarget: number | null;
  goal: string | null;
}

interface BioRecordResult {
  id: string;
  userId: string;
  date: string;
  weightKg: number | null;
  bodyFatPercentage: number | null;
  systolicBp: number | null;
  diastolicBp: number | null;
  notes: string | null;
}

interface DishHistoryResult {
  id: string;
  userId: string;
  dishId: string | null;
  date: string;
  mealType: string | null;
  servings: number;
  notes: string | null;
  createdAt: number;
}

interface NutritionSummaryResult {
  dishId: string;
  dishName: string;
  totalCalories: number;
  totalProteinG: number;
  totalCarbsG: number;
  totalFatG: number;
  totalFiberG: number;
  totalSugarG: number;
  totalSodiumMg: number;
  nutrientDetails: Array<{
    nutrientName: string;
    amount: number;
    unit: string;
    sourceFoodId: number;
    sourceFoodName: string;
  }>;
}

interface ValidationResult {
  dishId: string;
  isCompatible: boolean;
  warnings: string[];
}

interface DailyTargetsResult {
  calories: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
}

interface DailySummaryResult {
  userId: string;
  date: string;
  totalCalories: number;
  totalProteinG: number;
  totalCarbsG: number;
  totalFatG: number;
  meals: Array<{
    mealType: string;
    totalCalories: number;
    totalProteinG: number;
    totalCarbsG: number;
    totalFatG: number;
  }>;
}

interface TomadyDietNative {
  // User & Profile
  createUser(id: string, username: string, email: string): Promise<UserResult>;
  getUser(userId: string): Promise<UserResult | null>;
  getProfile(userId: string): Promise<ProfileResult | null>;
  updateProfile(params: Record<string, any>): Promise<ProfileResult>;

  // Bio Records
  recordBio(
    userId: string,
    date: string,
    weightKg: number,
    bodyFat: number,
    systolic: number,
    diastolic: number,
    notes: string
  ): Promise<BioRecordResult>;

  // Meal Logging
  logMeal(
    userId: string,
    dishId: string,
    date: string,
    mealType: string,
    servings: number,
    notes: string
  ): Promise<DishHistoryResult>;
  getDishHistory(userId: string, date: string): Promise<DishHistoryResult[]>;
  getHistory(userId: string, startDate: string, endDate: string): Promise<DishHistoryResult[]>;

  // Nutrition Analysis
  getDishNutrition(dishId: string): Promise<NutritionSummaryResult | null>;
  validateDish(dishId: string, userId: string): Promise<ValidationResult>;
  getDailyPlan(userId: string): Promise<DailyTargetsResult | null>;
  getTodaySummary(userId: string, date: string): Promise<DailySummaryResult | null>;

  // Suggestion Observation
  startSuggestionObserver(): void;
  stopSuggestionObserver(): void;
  checkPendingSuggestions(): Promise<any[]>;
}

// ══════════════════════════════════════════════════════════════════════════
// Gemma Module  —  NativeModuleName: 'TomadyGemma'
// ══════════════════════════════════════════════════════════════════════════

interface GemmaRecipeResult {
  dishId: string;
  dishName: string;
  recipeId: string;
  isCompatible: boolean;
  warnings: string[];
  rawResponse: string;
}

interface GemmaAnswerResult {
  answer: string;
  referencedDishId: string | null;
  referencedDishName: string | null;
  rawResponse: string;
}

interface GemmaTokenEvent {
  sessionId: string;
  token: string;
  accumulated: string;
  isFinal: boolean;
  error?: string;
}

interface TomadyGemmaNative {
  loadModel(modelPath: string): Promise<boolean>;
  isModelLoaded(): Promise<boolean>;
  releaseModel(): void;

  computeRecipe(prompt: string, userId: string): Promise<GemmaRecipeResult>;
  askQuestion(question: string, userId: string): Promise<GemmaAnswerResult>;

  streamTokens(query: string): Promise<string>;
  cancelStreaming(): void;
}

// ══════════════════════════════════════════════════════════════════════════
// DailySuggestion Event
// ══════════════════════════════════════════════════════════════════════════

interface DailySuggestionEvent {
  userId: string;
  dishId: string;
  dishName: string;
  date: string;
  isCompatible: boolean;
  warnings: string[];
}

// ══════════════════════════════════════════════════════════════════════════
// NativeModules declaration
// ══════════════════════════════════════════════════════════════════════════

declare module 'react-native' {
  interface NativeModulesStatic {
    TomadyFooDB: TomadyFooDBNative;
    TomadyDiet: TomadyDietNative;
    TomadyGemma: TomadyGemmaNative;
  }
}

export type {
  FoodItem,
  NutrientProperty,
  FoodDetailResult,
  UserResult,
  ProfileResult,
  BioRecordResult,
  DishHistoryResult,
  NutritionSummaryResult,
  ValidationResult,
  DailyTargetsResult,
  DailySummaryResult,
  GemmaRecipeResult,
  GemmaAnswerResult,
  GemmaTokenEvent,
  DailySuggestionEvent,
};
