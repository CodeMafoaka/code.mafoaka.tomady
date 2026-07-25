/**
 * Diet Module Test Section
 *
 * Profile & Bio Record management, meal logging, and consumption history.
 */
import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  FlatList,
  ActivityIndicator,
  StyleSheet,
  Alert,
  ScrollView,
} from 'react-native';
import { NativeModules } from 'react-native';
import type {
  ProfileResult,
  BioRecordResult,
  DishHistoryResult,
  NutritionSummaryResult,
  ValidationResult,
} from '../native';

const { TomadyDiet } = NativeModules;

// ── Constants ────────────────────────────────────────────────────────────

const TEST_USER_ID = 'demo-user-1';
const TODAY = new Date().toISOString().slice(0, 10);

// ── Helpers ──────────────────────────────────────────────────────────────

const fmtDate = (d: Date = new Date()): string => d.toISOString().slice(0, 10);

// ── Component ────────────────────────────────────────────────────────────

const DietTest: React.FC = () => {
  // Profile state
  const [profile, setProfile] = useState<ProfileResult | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [goal, setGoal] = useState('');
  const [calorieTarget, setCalorieTarget] = useState('');

  // Bio record state
  const [weight, setWeight] = useState('');
  const [bodyFat, setBodyFat] = useState('');
  const [bioLoading, setBioLoading] = useState(false);

  // Meal logging state
  const [mealDishId, setMealDishId] = useState('');
  const [mealType, setMealType] = useState('Lunch');
  const [mealServings, setMealServings] = useState('1');
  const [logLoading, setLogLoading] = useState(false);

  // History state
  const [history, setHistory] = useState<DishHistoryResult[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  // Nutrition / Validation
  const [nutrition, setNutrition] = useState<NutritionSummaryResult | null>(null);
  const [validation, setValidation] = useState<ValidationResult | null>(null);
  const [analysisLoading, setAnalysisLoading] = useState(false);
  const [analysisDishId, setAnalysisDishId] = useState('');

  // ── Profile handlers ────────────────────────────────────────────────

  const handleFetchProfile = useCallback(async () => {
    setProfileLoading(true);
    setProfileError(null);
    try {
      // Ensure user exists first
      await TomadyDiet.createUser(TEST_USER_ID, 'DemoUser', 'demo@tomady.app').catch(() => {});
      const p = await TomadyDiet.getProfile(TEST_USER_ID);
      setProfile(p);
    } catch (e: any) {
      setProfileError(e.message ?? 'Failed to fetch profile');
    } finally {
      setProfileLoading(false);
    }
  }, []);

  const handleSaveProfile = useCallback(async () => {
    setProfileLoading(true);
    setProfileError(null);
    try {
      await TomadyDiet.createUser(TEST_USER_ID, 'DemoUser', 'demo@tomady.app').catch(() => {});
      const updated = await TomadyDiet.updateProfile({
        userId: TEST_USER_ID,
        goal: goal || null,
        dailyCalorieTarget: calorieTarget ? parseInt(calorieTarget, 10) : null,
      });
      setProfile(updated);
      Alert.alert('✅ Profile saved', `Goal: ${updated.goal ?? '—'}`);
    } catch (e: any) {
      setProfileError(e.message ?? 'Failed to save profile');
    } finally {
      setProfileLoading(false);
    }
  }, [goal, calorieTarget]);

  // ── Bio record handler ──────────────────────────────────────────────

  const handleRecordBio = useCallback(async () => {
    setBioLoading(true);
    try {
      await TomadyDiet.createUser(TEST_USER_ID, 'DemoUser', 'demo@tomady.app').catch(() => {});
      const record = await TomadyDiet.recordBio(
        TEST_USER_ID,
        TODAY,
        weight ? parseFloat(weight) : NaN,
        bodyFat ? parseFloat(bodyFat) : NaN,
        0, // systolic
        0, // diastolic
        'Recorded from demo UI',
      );
      Alert.alert('✅ Bio recorded', `Weight: ${record.weightKg ?? '—'} kg`);
    } catch (e: any) {
      Alert.alert('❌ Error', e.message ?? 'Bio record failed');
    } finally {
      setBioLoading(false);
    }
  }, [weight, bodyFat]);

  // ── Meal logging handler ────────────────────────────────────────────

  const handleLogMeal = useCallback(async () => {
    setLogLoading(true);
    try {
      const entry = await TomadyDiet.logMeal(
        TEST_USER_ID,
        mealDishId || '',
        TODAY,
        mealType,
        parseFloat(mealServings) || 1,
        'Logged from demo UI',
      );
      Alert.alert('✅ Meal logged', `${entry.mealType ?? 'Meal'} — ${entry.servings} serving(s)`);
    } catch (e: any) {
      Alert.alert('❌ Error', e.message ?? 'Log meal failed');
    } finally {
      setLogLoading(false);
    }
  }, [mealDishId, mealType, mealServings]);

  // ── History handler ─────────────────────────────────────────────────

  const handleFetchHistory = useCallback(async () => {
    setHistoryLoading(true);
    try {
      const weekAgo = new Date(Date.now() - 7 * 86400000).toISOString().slice(0, 10);
      const entries = await TomadyDiet.getHistory(TEST_USER_ID, weekAgo, TODAY);
      setHistory(entries);
    } catch (e: any) {
      Alert.alert('❌ Error', e.message ?? 'Fetch history failed');
    } finally {
      setHistoryLoading(false);
    }
  }, []);

  // ── Nutrition / validation handler ──────────────────────────────────

  const handleAnalyze = useCallback(async () => {
    if (!analysisDishId.trim()) return;
    setAnalysisLoading(true);
    try {
      const [nut, val] = await Promise.all([
        TomadyDiet.getDishNutrition(analysisDishId.trim()),
        TomadyDiet.validateDish(analysisDishId.trim(), TEST_USER_ID).catch(() => null),
      ]);
      setNutrition(nut);
      setValidation(val);
    } catch (e: any) {
      Alert.alert('❌ Error', e.message ?? 'Analysis failed');
    } finally {
      setAnalysisLoading(false);
    }
  }, [analysisDishId]);

  // ── Render helpers ──────────────────────────────────────────────────

  const renderHistoryItem = ({ item }: { item: DishHistoryResult }) => (
    <View style={styles.historyItem}>
      <Text style={styles.historyTitle}>
        {item.mealType ?? 'Meal'} — {item.servings} serving(s)
      </Text>
      <Text style={styles.historySub}>
        {item.date}  •  Dish: {item.dishId ?? '—'}
      </Text>
      {item.notes && <Text style={styles.historyNote}>{item.notes}</Text>}
    </View>
  );

  return (
    <ScrollView style={styles.section} contentContainerStyle={styles.sectionContent}>
      <Text style={styles.sectionTitle}>🥗 Diet Module Test</Text>

      {/* ── Profile Management ───────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>👤 Profile & Goals</Text>
        <TextInput
          style={styles.input}
          placeholder="Goal (e.g. lose weight, muscle gain, diabetes)"
          placeholderTextColor="#999"
          value={goal}
          onChangeText={setGoal}
        />
        <TextInput
          style={styles.input}
          placeholder="Daily calorie target"
          placeholderTextColor="#999"
          keyboardType="numeric"
          value={calorieTarget}
          onChangeText={setCalorieTarget}
        />
        <View style={styles.row}>
          <TouchableOpacity style={styles.button} onPress={handleFetchProfile} disabled={profileLoading}>
            <Text style={styles.buttonText}>{profileLoading ? '...' : 'Fetch Profile'}</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.button, styles.buttonSecondary]} onPress={handleSaveProfile} disabled={profileLoading}>
            <Text style={styles.buttonText}>Save Profile</Text>
          </TouchableOpacity>
        </View>
        {profileError && <Text style={styles.errorText}>{profileError}</Text>}
        {profile && (
          <View style={styles.resultBox}>
            <Text style={styles.resultText}>Goal: {profile.goal ?? '—'}</Text>
            <Text style={styles.resultText}>Calorie target: {profile.dailyCalorieTarget ?? '—'}</Text>
            <Text style={styles.resultText}>Weight: {profile.weightKg ?? '—'} kg</Text>
          </View>
        )}
      </View>

      {/* ── Bio Record ───────────────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>📊 Log Bio Record</Text>
        <TextInput
          style={styles.input}
          placeholder="Weight (kg)"
          placeholderTextColor="#999"
          keyboardType="decimal-pad"
          value={weight}
          onChangeText={setWeight}
        />
        <TextInput
          style={styles.input}
          placeholder="Body fat %"
          placeholderTextColor="#999"
          keyboardType="decimal-pad"
          value={bodyFat}
          onChangeText={setBodyFat}
        />
        <TouchableOpacity style={styles.button} onPress={handleRecordBio} disabled={bioLoading}>
          <Text style={styles.buttonText}>{bioLoading ? 'Saving...' : 'Save Bio Record'}</Text>
        </TouchableOpacity>
      </View>

      {/* ── Meal Logging ─────────────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>🍽️ Log Meal</Text>
        <TextInput
          style={styles.input}
          placeholder="Dish ID (or leave empty)"
          placeholderTextColor="#999"
          value={mealDishId}
          onChangeText={setMealDishId}
        />
        <View style={styles.row}>
          <TextInput
            style={[styles.input, { flex: 1 }]}
            placeholder="Meal type"
            placeholderTextColor="#999"
            value={mealType}
            onChangeText={setMealType}
          />
          <TextInput
            style={[styles.input, { width: 70 }]}
            placeholder="Srvs"
            placeholderTextColor="#999"
            keyboardType="decimal-pad"
            value={mealServings}
            onChangeText={setMealServings}
          />
        </View>
        <TouchableOpacity style={styles.button} onPress={handleLogMeal} disabled={logLoading}>
          <Text style={styles.buttonText}>{logLoading ? 'Logging...' : 'Log Meal'}</Text>
        </TouchableOpacity>
      </View>

      {/* ── History ──────────────────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>📜 Meal History</Text>
        <TouchableOpacity style={styles.button} onPress={handleFetchHistory} disabled={historyLoading}>
          <Text style={styles.buttonText}>{historyLoading ? 'Loading...' : 'Load History (7 days)'}</Text>
        </TouchableOpacity>
        <FlatList
          data={history}
          keyExtractor={(item) => item.id}
          renderItem={renderHistoryItem}
          scrollEnabled={false}
          ListEmptyComponent={<Text style={styles.emptyText}>No meals logged yet</Text>}
        />
      </View>

      {/* ── Nutrition Analysis ────────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>🔬 Dish Nutrition & Validation</Text>
        <View style={styles.row}>
          <TextInput
            style={[styles.input, { flex: 1 }]}
            placeholder="Dish ID to analyze"
            placeholderTextColor="#999"
            value={analysisDishId}
            onChangeText={setAnalysisDishId}
          />
          <TouchableOpacity style={styles.button} onPress={handleAnalyze} disabled={analysisLoading}>
            <Text style={styles.buttonText}>{analysisLoading ? '...' : 'Analyze'}</Text>
          </TouchableOpacity>
        </View>
        {nutrition && (
          <View style={styles.resultBox}>
            <Text style={styles.resultTitle}>{nutrition.dishName}</Text>
            <Text style={styles.resultText}>Calories: {nutrition.totalCalories.toFixed(0)} kcal</Text>
            <Text style={styles.resultText}>Protein: {nutrition.totalProteinG.toFixed(1)}g</Text>
            <Text style={styles.resultText}>Carbs: {nutrition.totalCarbsG.toFixed(1)}g</Text>
            <Text style={styles.resultText}>Fat: {nutrition.totalFatG.toFixed(1)}g</Text>
            <Text style={styles.resultText}>Fiber: {nutrition.totalFiberG.toFixed(1)}g</Text>
            <Text style={styles.resultText}>Sugar: {nutrition.totalSugarG.toFixed(1)}g</Text>
            <Text style={styles.resultText}>Sodium: {nutrition.totalSodiumMg.toFixed(0)}mg</Text>
          </View>
        )}
        {validation && (
          <View
            style={[
              styles.validationBox,
              { backgroundColor: validation.isCompatible ? '#E8F5E9' : '#FFF3E0' },
            ]}
          >
            <Text style={styles.validationTitle}>
              {validation.isCompatible ? '✅ Compatible' : '⚠️ Has Warnings'}
            </Text>
            {validation.warnings.map((w, i) => (
              <Text key={i} style={styles.validationWarn}>
                • {w}
              </Text>
            ))}
          </View>
        )}
      </View>
    </ScrollView>
  );
};

// ── Styles ───────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  section: { flex: 1 },
  sectionContent: { padding: 16, paddingBottom: 40 },
  sectionTitle: { fontSize: 22, fontWeight: '700', marginBottom: 16, color: '#1a1a2e' },
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOpacity: 0.08,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 2 },
    elevation: 3,
  },
  cardTitle: { fontSize: 15, fontWeight: '600', color: '#333', marginBottom: 10 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 8 },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 14,
    color: '#333',
    backgroundColor: '#fafafa',
    marginBottom: 8,
  },
  button: {
    backgroundColor: '#4CAF50',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 10,
    marginRight: 8,
  },
  buttonSecondary: { backgroundColor: '#1976D2' },
  buttonText: { color: '#fff', fontWeight: '600', fontSize: 14 },
  errorText: { color: '#e53935', fontSize: 13, marginTop: 6 },
  emptyText: { color: '#999', fontSize: 13, marginTop: 8, textAlign: 'center' },
  resultBox: {
    backgroundColor: '#f5f5f5',
    borderRadius: 8,
    padding: 12,
    marginTop: 8,
  },
  resultTitle: { fontSize: 15, fontWeight: '700', color: '#1a1a2e', marginBottom: 4 },
  resultText: { fontSize: 13, color: '#555', marginTop: 2 },
  historyItem: {
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  historyTitle: { fontSize: 14, fontWeight: '600', color: '#1a1a2e' },
  historySub: { fontSize: 12, color: '#888', marginTop: 2 },
  historyNote: { fontSize: 12, color: '#666', marginTop: 2, fontStyle: 'italic' },
  validationBox: {
    borderRadius: 8,
    padding: 12,
    marginTop: 8,
  },
  validationTitle: { fontSize: 14, fontWeight: '700', color: '#333', marginBottom: 4 },
  validationWarn: { fontSize: 12, color: '#555', marginTop: 2 },
});

export default DietTest;
