/**
 * Gemma Module Test Section
 *
 * Recipe computation with profile safety check, question answering,
 * and token streaming via DeviceEventEmitter.
 */
import React, { useState, useCallback, useEffect, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
  Alert,
  ScrollView,
  NativeEventEmitter,
  NativeModules,
  Platform,
} from 'react-native';
import type { GemmaRecipeResult, GemmaAnswerResult, GemmaTokenEvent } from '../native';

const { TomadyGemma } = NativeModules;

// Create event emitter for streaming events
const eventEmitter = Platform.OS === 'android'
  ? new NativeEventEmitter(TomadyGemma as any)
  : null;

const TEST_USER_ID = 'demo-user-1';

const GemmaTest: React.FC = () => {
  // Model lifecycle
  const [modelLoaded, setModelLoaded] = useState(false);
  const [modelLoading, setModelLoading] = useState(false);

  // Recipe state
  const [recipePrompt, setRecipePrompt] = useState('');
  const [recipeResult, setRecipeResult] = useState<GemmaRecipeResult | null>(null);
  const [recipeLoading, setRecipeLoading] = useState(false);
  const [recipeError, setRecipeError] = useState<string | null>(null);

  // Question state
  const [question, setQuestion] = useState('');
  const [answerResult, setAnswerResult] = useState<GemmaAnswerResult | null>(null);
  const [answerLoading, setAnswerLoading] = useState(false);
  const [answerError, setAnswerError] = useState<string | null>(null);

  // Streaming state
  const [streamQuery, setStreamQuery] = useState('');
  const [streamText, setStreamText] = useState('');
  const [streaming, setStreaming] = useState(false);
  const [streamSessionId, setStreamSessionId] = useState<string | null>(null);
  const subscriptionRef = useRef<any>(null);

  // ── Model lifecycle ────────────────────────────────────────────────

  const handleLoadModel = useCallback(async () => {
    setModelLoading(true);
    try {
      const loaded = await TomadyGemma.loadModel('');
      setModelLoaded(loaded);
      Alert.alert(loaded ? '✅ Model loaded' : '❌ Failed to load model');
    } catch (e: any) {
      Alert.alert('❌ Error', e.message ?? 'Load model failed');
    } finally {
      setModelLoading(false);
    }
  }, []);

  const handleReleaseModel = useCallback(() => {
    TomadyGemma.releaseModel();
    setModelLoaded(false);
    setStreamText('');
    Alert.alert('🔄 Model released');
  }, []);

  // ── Recipe computation ─────────────────────────────────────────────

  const handleComputeRecipe = useCallback(async () => {
    if (!recipePrompt.trim()) return;
    setRecipeLoading(true);
    setRecipeError(null);
    try {
      const result = await TomadyGemma.computeRecipe(recipePrompt.trim(), TEST_USER_ID);
      setRecipeResult(result);
    } catch (e: any) {
      setRecipeError(e.message ?? 'Recipe computation failed');
      setRecipeResult(null);
    } finally {
      setRecipeLoading(false);
    }
  }, [recipePrompt]);

  // ── Question answering ─────────────────────────────────────────────

  const handleAskQuestion = useCallback(async () => {
    if (!question.trim()) return;
    setAnswerLoading(true);
    setAnswerError(null);
    try {
      const result = await TomadyGemma.askQuestion(question.trim(), TEST_USER_ID);
      setAnswerResult(result);
    } catch (e: any) {
      setAnswerError(e.message ?? 'Question failed');
      setAnswerResult(null);
    } finally {
      setAnswerLoading(false);
    }
  }, [question]);

  // ── Token streaming ────────────────────────────────────────────────

  const handleStartStreaming = useCallback(async () => {
    if (!streamQuery.trim()) return;

    // Subscribe to DeviceEventEmitter events
    if (eventEmitter) {
      subscriptionRef.current = eventEmitter.addListener('GemmaToken', (event: GemmaTokenEvent) => {
        if (event.isFinal) {
          setStreaming(false);
          if (event.error) {
            setStreamText((prev) => prev + `\n\n[Error: ${event.error}]`);
          }
        } else {
          setStreamText(event.accumulated);
        }
      });
    }

    setStreaming(true);
    setStreamText('');
    try {
      const sessionId = await TomadyGemma.streamTokens(streamQuery.trim());
      setStreamSessionId(sessionId);
    } catch (e: any) {
      setStreaming(false);
      setStreamText(`Error: ${e.message ?? 'Stream failed'}`);
    }
  }, [streamQuery]);

  const handleCancelStreaming = useCallback(() => {
    TomadyGemma.cancelStreaming();
    setStreaming(false);
    if (subscriptionRef.current) {
      subscriptionRef.current.remove();
      subscriptionRef.current = null;
    }
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      handleCancelStreaming();
    };
  }, [handleCancelStreaming]);

  // ── Recipe result renderer ─────────────────────────────────────────

  const renderRecipeResult = (r: GemmaRecipeResult) => (
    <View style={styles.resultBox}>
      <Text style={styles.resultTitle}>{r.dishName}</Text>
      <Text style={styles.resultSub}>Dish ID: {r.dishId}</Text>
      <View
        style={[
          styles.compatBadge,
          { backgroundColor: r.isCompatible ? '#E8F5E9' : '#FFF3E0' },
        ]}
      >
        <Text style={styles.compatText}>
          {r.isCompatible ? '✅ Compatible with profile' : '⚠️ Warnings present'}
        </Text>
      </View>
      {r.warnings.map((w, i) => (
        <Text key={i} style={styles.warnText}>
          • {w}
        </Text>
      ))}
      <Text style={styles.rawLabel}>Raw Response:</Text>
      <Text style={styles.rawText} numberOfLines={10}>
        {r.rawResponse}
      </Text>
    </View>
  );

  return (
    <ScrollView style={styles.section} contentContainerStyle={styles.sectionContent}>
      <Text style={styles.sectionTitle}>🤖 Gemma Module Test</Text>

      {/* ── Model Lifecycle ──────────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>📦 Model Lifecycle</Text>
        <View style={styles.row}>
          <TouchableOpacity
            style={[styles.button, modelLoaded && styles.buttonDisabled]}
            onPress={handleLoadModel}
            disabled={modelLoading || modelLoaded}
          >
            <Text style={styles.buttonText}>
              {modelLoading ? 'Loading...' : modelLoaded ? '✅ Loaded' : 'Load Model'}
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.button, styles.buttonDanger]}
            onPress={handleReleaseModel}
            disabled={!modelLoaded}
          >
            <Text style={styles.buttonText}>Release</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* ── Recipe Computation ───────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>🍳 Compute Recipe</Text>
        <TextInput
          style={[styles.input, styles.multilineInput]}
          placeholder="e.g. Suggest a low-sugar breakfast for someone with diabetes"
          placeholderTextColor="#999"
          multiline
          numberOfLines={3}
          value={recipePrompt}
          onChangeText={setRecipePrompt}
        />
        <TouchableOpacity
          style={[styles.button, !modelLoaded && styles.buttonDisabled]}
          onPress={handleComputeRecipe}
          disabled={recipeLoading || !modelLoaded}
        >
          <Text style={styles.buttonText}>
            {recipeLoading ? 'Computing...' : 'Compute Recipe'}
          </Text>
        </TouchableOpacity>
        {recipeError && <Text style={styles.errorText}>{recipeError}</Text>}
        {recipeResult && renderRecipeResult(recipeResult)}
        {!modelLoaded && (
          <Text style={styles.hintText}>Load the model first above ☝️</Text>
        )}
      </View>

      {/* ── Question Answering ───────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>💬 Ask a Question</Text>
        <TextInput
          style={[styles.input, styles.multilineInput]}
          placeholder="e.g. Can I drink coca-cola with diabetes?"
          placeholderTextColor="#999"
          multiline
          numberOfLines={2}
          value={question}
          onChangeText={setQuestion}
        />
        <TouchableOpacity
          style={[styles.button, !modelLoaded && styles.buttonDisabled]}
          onPress={handleAskQuestion}
          disabled={answerLoading || !modelLoaded}
        >
          <Text style={styles.buttonText}>
            {answerLoading ? 'Thinking...' : 'Ask Question'}
          </Text>
        </TouchableOpacity>
        {answerError && <Text style={styles.errorText}>{answerError}</Text>}
        {answerResult && (
          <View style={styles.resultBox}>
            <Text style={styles.answerText}>{answerResult.answer}</Text>
            {answerResult.referencedDishId && (
              <Text style={styles.refText}>
                Referenced dish: {answerResult.referencedDishName ?? answerResult.referencedDishId}
              </Text>
            )}
          </View>
        )}
      </View>

      {/* ── Token Streaming ──────────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>⚡ Token Streaming (DeviceEventEmitter)</Text>
        <TextInput
          style={[styles.input, styles.multilineInput]}
          placeholder="Enter a prompt to stream tokens"
          placeholderTextColor="#999"
          multiline
          numberOfLines={2}
          value={streamQuery}
          onChangeText={setStreamQuery}
        />
        <View style={styles.row}>
          <TouchableOpacity
            style={[styles.button, (!modelLoaded || streaming) && styles.buttonDisabled]}
            onPress={handleStartStreaming}
            disabled={streaming || !modelLoaded}
          >
            <Text style={styles.buttonText}>{streaming ? 'Streaming...' : 'Stream'}</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.button, styles.buttonDanger]}
            onPress={handleCancelStreaming}
            disabled={!streaming}
          >
            <Text style={styles.buttonText}>Stop</Text>
          </TouchableOpacity>
        </View>
        {streamText ? (
          <View style={styles.streamBox}>
            <Text style={styles.streamText}>{streamText}</Text>
          </View>
        ) : null}
        {streaming && <ActivityIndicator color="#4CAF50" style={{ marginTop: 8 }} />}
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
  multilineInput: { minHeight: 60, textAlignVertical: 'top' },
  button: {
    backgroundColor: '#4CAF50',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  buttonDisabled: { opacity: 0.5 },
  buttonDanger: { backgroundColor: '#e53935' },
  buttonText: { color: '#fff', fontWeight: '600', fontSize: 14 },
  errorText: { color: '#e53935', fontSize: 13, marginTop: 6 },
  hintText: { color: '#999', fontSize: 12, marginTop: 6, fontStyle: 'italic' },
  resultBox: {
    backgroundColor: '#f5f5f5',
    borderRadius: 8,
    padding: 12,
    marginTop: 8,
  },
  resultTitle: { fontSize: 16, fontWeight: '700', color: '#1a1a2e', marginBottom: 2 },
  resultSub: { fontSize: 12, color: '#888', marginBottom: 6 },
  compatBadge: { alignSelf: 'flex-start', borderRadius: 6, paddingHorizontal: 8, paddingVertical: 3, marginBottom: 6 },
  compatText: { fontSize: 12, fontWeight: '600', color: '#333' },
  warnText: { fontSize: 12, color: '#E65100', marginTop: 2 },
  rawLabel: { fontSize: 11, fontWeight: '700', color: '#999', marginTop: 8, textTransform: 'uppercase' },
  rawText: { fontSize: 11, color: '#666', fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace', marginTop: 4 },
  answerText: { fontSize: 14, color: '#333', lineHeight: 20 },
  refText: { fontSize: 12, color: '#1976D2', marginTop: 6, fontStyle: 'italic' },
  streamBox: {
    backgroundColor: '#1a1a2e',
    borderRadius: 8,
    padding: 12,
    marginTop: 8,
    maxHeight: 200,
  },
  streamText: { fontSize: 13, color: '#4CAF50', fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace', lineHeight: 18 },
});

export default GemmaTest;
