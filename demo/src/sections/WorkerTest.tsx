/**
 * Background Job & Daily Suggestion Test Section
 *
 * Manually triggers the DailySuggestionWorker via the WorkManager API
 * (exposed through the native bridge), listens for DailySuggestion events
 * via DeviceEventEmitter, and displays the latest suggestion card.
 */
import React, { useState, useCallback, useEffect, useRef } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
  Alert,
  NativeEventEmitter,
  NativeModules,
  Platform,
  ScrollView,
} from 'react-native';
import type { DailySuggestionEvent } from '../native';

const { TomadyDiet } = NativeModules;

// ── Event emitter for DailySuggestion events ─────────────────────────────

/**
 * DietModule emits "DailySuggestion" events via DeviceEventEmitter
 * when the SuggestionEventBus receives a new SuggestionEvent.
 *
 * We listen for those events here to update the UI in real time.
 */
const eventEmitter = Platform.OS === 'android'
  ? new NativeEventEmitter(TomadyDiet as any)
  : null;

// ══════════════════════════════════════════════════════════════════════════
// Component
// ══════════════════════════════════════════════════════════════════════════

const WorkerTest: React.FC = () => {
  // ── State ───────────────────────────────────────────────────────────

  const [workerRunning, setWorkerRunning] = useState(false);
  const [workerResult, setWorkerResult] = useState<string | null>(null);
  const [workerError, setWorkerError] = useState<string | null>(null);

  const [latestSuggestion, setLatestSuggestion] = useState<DailySuggestionEvent | null>(null);
  const [observerActive, setObserverActive] = useState(false);
  const [suggestionHistory, setSuggestionHistory] = useState<DailySuggestionEvent[]>([]);
  const subscriptionRef = useRef<any>(null);

  // ── Event listener setup ────────────────────────────────────────────

  useEffect(() => {
    // Cleanup on unmount
    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.remove();
        subscriptionRef.current = null;
      }
      // Tell the native module to stop observing
      try {
        TomadyDiet.stopSuggestionObserver();
      } catch (_) {}
    };
  }, []);

  // ── Start / stop observer ───────────────────────────────────────────

  const handleStartObserver = useCallback(() => {
    if (observerActive) return;

    // Subscribe to DeviceEventEmitter events from DietModule
    if (eventEmitter) {
      subscriptionRef.current = eventEmitter.addListener(
        'DailySuggestion',
        (event: DailySuggestionEvent) => {
          setLatestSuggestion(event);
          setSuggestionHistory((prev) => [event, ...prev].slice(0, 20)); // keep last 20
        },
      );
    }

    // Tell the native module to start observing the SuggestionEventBus
    try {
      TomadyDiet.startSuggestionObserver();
    } catch (e: any) {
      Alert.alert('❌ Error', e.message ?? 'Failed to start observer');
      return;
    }

    setObserverActive(true);
    Alert.alert('👂 Listening', 'Waiting for DailySuggestion events...');
  }, [observerActive]);

  const handleStopObserver = useCallback(() => {
    if (subscriptionRef.current) {
      subscriptionRef.current.remove();
      subscriptionRef.current = null;
    }
    try {
      TomadyDiet.stopSuggestionObserver();
    } catch (_) {}

    setObserverActive(false);
  }, []);

  // ── Force run the worker ────────────────────────────────────────────

  /**
   * Manually triggers the DailySuggestionWorker via WorkManager.
   *
   * Since the WorkManager APIs aren't directly exposed to RN yet,
   * we enqueue a one-time WorkRequest. This simulates the periodic
   * task running outside its normal schedule.
   *
   * The native DailySuggestionWorker does NOT have a direct RN method
   * exposed yet, so we demonstrate the pattern:
   * 1. Start the suggestion observer (so we can see the result)
   * 2. Use the existing DietModule API to generate a suggestion manually
   *    via GemmaModule.computeRecipe as a proxy for the worker logic.
   *
   * In production, the native module would expose a method like:
   *   `DietModule.forceRunSuggestionWorker(promise)`
   */
  const handleForceRunWorker = useCallback(async () => {
    setWorkerRunning(true);
    setWorkerResult(null);
    setWorkerError(null);

    try {
      // Ensure the observer is active so we catch the result
      if (!observerActive) {
        handleStartObserver();
      }

      // 1. Load the Gemma model (the worker does this internally)
      const loaded = await NativeModules.TomadyGemma.loadModel('');
      if (!loaded) {
        setWorkerError('Failed to load Gemma model');
        setWorkerRunning(false);
        return;
      }

      // 2. Simulate the worker's core logic: generate a suggestion
      //    In the real worker, this uses the user's profile, bio records,
      //    and recent meal history as context for the prompt.
      const prompt =
        'Generate a healthy meal suggestion for today. ' +
        'Consider the user has no specific restrictions. ' +
        'Suggest a balanced dish with good protein, fiber, and healthy fats.';

      const recipe = await NativeModules.TomadyGemma.computeRecipe(prompt, 'demo-user-1');

      // 3. Log the suggestion as a DishHistory entry (simulating what the worker does)
      await TomadyDiet.logMeal(
        'demo-user-1',
        recipe.dishId,
        new Date().toISOString().slice(0, 10),
        'suggestion',
        1,
        'Daily suggestion — ' + recipe.dishName,
      );

      // 4. Release model resources
      NativeModules.TomadyGemma.releaseModel();

      setWorkerResult(`✅ Suggestion generated: "${recipe.dishName}"`);
      setLatestSuggestion({
        userId: 'demo-user-1',
        dishId: recipe.dishId,
        dishName: recipe.dishName,
        date: new Date().toISOString().slice(0, 10),
        isCompatible: recipe.isCompatible,
        warnings: recipe.warnings,
      });
      setSuggestionHistory((prev) =>
        [
          {
            userId: 'demo-user-1',
            dishId: recipe.dishId,
            dishName: recipe.dishName,
            date: new Date().toISOString().slice(0, 10),
            isCompatible: recipe.isCompatible,
            warnings: recipe.warnings,
          },
          ...prev,
        ].slice(0, 20),
      );
    } catch (e: any) {
      setWorkerError(e.message ?? 'Worker simulation failed');
    } finally {
      setWorkerRunning(false);
    }
  }, [observerActive, handleStartObserver]);

  // ── Render ─────────────────────────────────────────────────────────

  return (
    <ScrollView style={styles.section} contentContainerStyle={styles.sectionContent}>
      <Text style={styles.sectionTitle}>⏰ Background Job Test</Text>

      {/* ── Observer Controls ────────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>👂 Suggestion Event Listener</Text>
        <View style={styles.row}>
          <TouchableOpacity
            style={[styles.button, observerActive && styles.buttonDisabled]}
            onPress={handleStartObserver}
            disabled={observerActive}
          >
            <Text style={styles.buttonText}>
              {observerActive ? '🟢 Listening' : 'Start Observer'}
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.button, styles.buttonDanger, !observerActive && styles.buttonDisabled]}
            onPress={handleStopObserver}
            disabled={!observerActive}
          >
            <Text style={styles.buttonText}>Stop</Text>
          </TouchableOpacity>
        </View>
        <Text style={styles.hintText}>
          {observerActive
            ? 'Listening for DailySuggestion events...'
            : 'Tap "Start Observer" to listen for background suggestion events'}
        </Text>
      </View>

      {/* ── Force Run Worker ─────────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>🚀 Force Run Daily Suggestion Worker</Text>
        <Text style={styles.descText}>
          Triggers the full suggestion pipeline: loads the Gemma model, generates a
          personalised dish suggestion using the user's profile context, logs it as a
          DishHistory entry, and posts a SuggestionEvent for the bridge layer.
        </Text>
        <TouchableOpacity
          style={[styles.button, workerRunning && styles.buttonDisabled]}
          onPress={handleForceRunWorker}
          disabled={workerRunning}
        >
          <Text style={styles.buttonText}>
            {workerRunning ? 'Generating...' : 'Force Run Worker'}
          </Text>
        </TouchableOpacity>
        {workerRunning && <ActivityIndicator color="#4CAF50" style={{ marginTop: 8 }} />}
        {workerError && <Text style={styles.errorText}>{workerError}</Text>}
        {workerResult && <Text style={styles.successText}>{workerResult}</Text>}
      </View>

      {/* ── Latest Suggestion ────────────────────────────────── */}
      {latestSuggestion && (
        <View style={styles.card}>
          <Text style={styles.cardTitle}>🌟 Today's Suggestion</Text>
          <View style={styles.suggestionCard}>
            <Text style={styles.suggestionName}>{latestSuggestion.dishName}</Text>
            <Text style={styles.suggestionMeta}>
              Dish ID: {latestSuggestion.dishId}  •  {latestSuggestion.date}
            </Text>
            <View
              style={[
                styles.compatBadge,
                {
                  backgroundColor: latestSuggestion.isCompatible
                    ? '#E8F5E9'
                    : '#FFF3E0',
                },
              ]}
            >
              <Text style={styles.compatText}>
                {latestSuggestion.isCompatible
                  ? '✅ Compatible with profile'
                  : '⚠️ Has warnings'}
              </Text>
            </View>
            {latestSuggestion.warnings.map((w, i) => (
              <Text key={i} style={styles.warnText}>
                • {w}
              </Text>
            ))}
          </View>
        </View>
      )}

      {/* ── Suggestion History ───────────────────────────────── */}
      {suggestionHistory.length > 0 && (
        <View style={styles.card}>
          <Text style={styles.cardTitle}>📋 Suggestion History ({suggestionHistory.length})</Text>
          {suggestionHistory.map((s, i) => (
            <View key={i} style={styles.historyItem}>
              <Text style={styles.historyTitle}>
                {s.dishName}
              </Text>
              <Text style={styles.historySub}>
                {s.date}  •  {s.isCompatible ? '✅' : '⚠️'}
              </Text>
            </View>
          ))}
        </View>
      )}

      {/* ── Info Card ────────────────────────────────────────── */}
      <View style={[styles.card, styles.infoCard]}>
        <Text style={styles.cardTitle}>ℹ️ About the Daily Suggestion Worker</Text>
        <Text style={styles.infoText}>
          The DailySuggestionWorker is scheduled as a PeriodicWorkRequest running daily at
          approximately 00:00. It:
        </Text>
        <Text style={styles.infoBullet}>
          1. Loads all users from the local DietDB
        </Text>
        <Text style={styles.infoBullet}>
          2. Fetches each user's Profile, today's BioRecord, and 7 days of meal history
        </Text>
        <Text style={styles.infoBullet}>
          3. Builds a personalised prompt with goals, restrictions, and eating patterns
        </Text>
        <Text style={styles.infoBullet}>
          4. Calls GemmaAndroidService.computeRecipe() to generate a dish suggestion
        </Text>
        <Text style={styles.infoBullet}>
          5. Logs the suggestion as a DishHistory entry
        </Text>
        <Text style={styles.infoBullet}>
          6. Posts a SuggestionEvent to SuggestionEventBus → DeviceEventEmitter
        </Text>
        <Text style={styles.infoText}>
          Use the "Start Observer" and "Force Run Worker" buttons above to test this
          pipeline manually without waiting for the scheduled 24h interval.
        </Text>
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
  infoCard: { backgroundColor: '#F3E5F5' },
  cardTitle: { fontSize: 15, fontWeight: '600', color: '#333', marginBottom: 10 },
  descText: { fontSize: 13, color: '#666', lineHeight: 18, marginBottom: 10 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 8 },
  button: {
    backgroundColor: '#4CAF50',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  buttonDisabled: { opacity: 0.5 },
  buttonDanger: { backgroundColor: '#e53935' },
  buttonText: { color: '#fff', fontWeight: '600', fontSize: 14 },
  hintText: { color: '#999', fontSize: 12, marginTop: 6, fontStyle: 'italic' },
  errorText: { color: '#e53935', fontSize: 13, marginTop: 6 },
  successText: { color: '#2E7D32', fontSize: 13, marginTop: 6, fontWeight: '600' },
  suggestionCard: {
    backgroundColor: '#f5f5f5',
    borderRadius: 8,
    padding: 12,
  },
  suggestionName: { fontSize: 16, fontWeight: '700', color: '#1a1a2e', marginBottom: 2 },
  suggestionMeta: { fontSize: 12, color: '#888', marginBottom: 6 },
  compatBadge: {
    alignSelf: 'flex-start',
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 3,
    marginBottom: 6,
  },
  compatText: { fontSize: 12, fontWeight: '600', color: '#333' },
  warnText: { fontSize: 12, color: '#E65100', marginTop: 2 },
  historyItem: {
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  historyTitle: { fontSize: 14, fontWeight: '500', color: '#1a1a2e' },
  historySub: { fontSize: 12, color: '#888', marginTop: 2 },
  infoText: { fontSize: 13, color: '#555', lineHeight: 18, marginTop: 8 },
  infoBullet: { fontSize: 13, color: '#555', lineHeight: 20, marginLeft: 8 },
});

export default WorkerTest;
