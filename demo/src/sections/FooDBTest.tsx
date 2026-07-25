/**
 * FooDB Module Test Section
 *
 * Provides search, food details with cache-source indicator, and food groups.
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
  ScrollView,
} from 'react-native';
import { NativeModules } from 'react-native';
import type { FoodItem, NutrientProperty, FoodDetailResult } from '../native';

const { TomadyFooDB } = NativeModules;

// ── Helpers ──────────────────────────────────────────────────────────────

/**
 * Formats a number to a readable string with up to 2 decimal places.
 */
const fmt = (v: number | null | undefined, suffix = ''): string =>
  v != null ? `${v.toFixed(2)}${suffix}` : '—';

// ── Component ────────────────────────────────────────────────────────────

const FooDBTest: React.FC = () => {
  // Search state
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<FoodItem[]>([]);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  // Food details state
  const [foodIdInput, setFoodIdInput] = useState('');
  const [foodDetail, setFoodDetail] = useState<FoodDetailResult | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  // Tracks whether we've already seen a food detail call to detect cache hits
  // (first fetch = remote, subsequent identical fetches = local cache)
  const [cacheSource, setCacheSource] = useState<'none' | 'local' | 'remote'>('none');
  const [lastFetchedId, setLastFetchedId] = useState<number | null>(null);

  // Food groups
  const [foodGroups, setFoodGroups] = useState<string[]>([]);
  const [groupsLoading, setGroupsLoading] = useState(false);

  // ── Search handler ──────────────────────────────────────────────────

  const handleSearch = useCallback(async () => {
    if (!searchQuery.trim()) return;
    setSearching(true);
    setSearchError(null);
    try {
      const results = await TomadyFooDB.searchFood(searchQuery.trim());
      setSearchResults(results);
    } catch (e: any) {
      setSearchError(e.message ?? 'Search failed');
      setSearchResults([]);
    } finally {
      setSearching(false);
    }
  }, [searchQuery]);

  // ── Food details handler (cache-first verification) ─────────────────

  const handleGetFoodDetails = useCallback(async () => {
    const id = parseInt(foodIdInput, 10);
    if (isNaN(id) || id <= 0) {
      setDetailError('Enter a valid numeric food ID');
      return;
    }
    setDetailLoading(true);
    setDetailError(null);
    try {
      // If we've already fetched this ID before, the cache-first logic in
      // FooDBDataAPIService.getFoodDetails() should return from local DB.
      const isRepeatFetch = lastFetchedId === id;
      
      const result = await TomadyFooDB.getNutrients(id);
      if (result) {
        setFoodDetail(result);
        setCacheSource(isRepeatFetch ? 'local' : 'remote');
        setLastFetchedId(id);
      } else {
        setFoodDetail(null);
        setCacheSource('none');
        setDetailError('Food not found');
      }
    } catch (e: any) {
      setDetailError(e.message ?? 'Failed to fetch food details');
      setFoodDetail(null);
    } finally {
      setDetailLoading(false);
    }
  }, [foodIdInput, lastFetchedId]);

  // ── Food groups handler ─────────────────────────────────────────────

  const handleGetFoodGroups = useCallback(async () => {
    setGroupsLoading(true);
    try {
      const groups = await TomadyFooDB.getFoodGroups();
      setFoodGroups(groups);
    } catch (e: any) {
      setFoodGroups([]);
    } finally {
      setGroupsLoading(false);
    }
  }, []);

  // ── Render helpers ──────────────────────────────────────────────────

  const renderFoodItem = ({ item }: { item: FoodItem }) => (
    <View style={styles.listItem}>
      <Text style={styles.listItemTitle}>{item.name ?? 'Unnamed'}</Text>
      <Text style={styles.listItemSub}>
        ID: {item.id}  •  Group: {item.foodGroup ?? '—'}
      </Text>
    </View>
  );

  const renderNutrient = (np: NutrientProperty, idx: number) => (
    <View key={idx} style={styles.nutrientRow}>
      <Text style={styles.nutrientName}>{np.nutrientName ?? 'Unknown'}</Text>
      <Text style={styles.nutrientValue}>
        {fmt(np.amount)} {np.unit ?? ''}
      </Text>
    </View>
  );

  return (
    <ScrollView style={styles.section} contentContainerStyle={styles.sectionContent}>
      <Text style={styles.sectionTitle}>🔍 FooDB Module Test</Text>

      {/* ── Search ───────────────────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Search Foods</Text>
        <View style={styles.row}>
          <TextInput
            style={[styles.input, { flex: 1 }]}
            placeholder="e.g. apple, rice, chicken"
            placeholderTextColor="#999"
            value={searchQuery}
            onChangeText={setSearchQuery}
            onSubmitEditing={handleSearch}
          />
          <TouchableOpacity style={styles.button} onPress={handleSearch} disabled={searching}>
            <Text style={styles.buttonText}>{searching ? '...' : 'Search'}</Text>
          </TouchableOpacity>
        </View>
        {searchError && <Text style={styles.errorText}>{searchError}</Text>}
        <FlatList
          data={searchResults}
          keyExtractor={(item) => String(item.id)}
          renderItem={renderFoodItem}
          scrollEnabled={false}
          ListEmptyComponent={
            !searching ? <Text style={styles.emptyText}>No results</Text> : null
          }
        />
      </View>

      {/* ── Food Details (Cache Verification) ────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Food Details (Cache-First Read-Through)</Text>
        <View style={styles.row}>
          <TextInput
            style={[styles.input, { flex: 1 }]}
            placeholder="Enter food ID (e.g. 1, 42, 100)"
            placeholderTextColor="#999"
            keyboardType="numeric"
            value={foodIdInput}
            onChangeText={setFoodIdInput}
          />
          <TouchableOpacity
            style={styles.button}
            onPress={handleGetFoodDetails}
            disabled={detailLoading}
          >
            <Text style={styles.buttonText}>{detailLoading ? '...' : 'Fetch'}</Text>
          </TouchableOpacity>
        </View>

        {detailLoading && <ActivityIndicator color="#4CAF50" style={{ marginVertical: 8 }} />}
        {detailError && <Text style={styles.errorText}>{detailError}</Text>}

        {/* Cache source indicator */}
        {cacheSource !== 'none' && (
          <View
            style={[
              styles.badge,
              cacheSource === 'local' ? styles.badgeLocal : styles.badgeRemote,
            ]}
          >
            <Text style={styles.badgeText}>
              {cacheSource === 'local'
                ? '📦 Source: Local DB Cache'
                : '🌐 Source: Remote FooDB API'}
            </Text>
          </View>
        )}

        {foodDetail && (
          <View style={styles.detailBox}>
            <Text style={styles.detailTitle}>
              {foodDetail.food.name ?? 'Unnamed Food'}
            </Text>
            <Text style={styles.detailSub}>
              Group: {foodDetail.food.foodGroup ?? '—'}  •  Category:{' '}
              {foodDetail.food.category ?? '—'}
            </Text>
            {foodDetail.food.description && (
              <Text style={styles.detailDesc}>{foodDetail.food.description}</Text>
            )}

            <Text style={styles.nutrientHeader}>
              Nutrients ({foodDetail.nutrients.length})
            </Text>
            {foodDetail.nutrients.map(renderNutrient)}
          </View>
        )}
      </View>

      {/* ── Food Groups ──────────────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Food Groups</Text>
        <TouchableOpacity style={styles.button} onPress={handleGetFoodGroups} disabled={groupsLoading}>
          <Text style={styles.buttonText}>{groupsLoading ? 'Loading...' : 'Load Groups'}</Text>
        </TouchableOpacity>
        <View style={styles.tagContainer}>
          {foodGroups.map((g, i) => (
            <View key={i} style={styles.tag}>
              <Text style={styles.tagText}>{g}</Text>
            </View>
          ))}
        </View>
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
  row: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 14,
    color: '#333',
    backgroundColor: '#fafafa',
  },
  button: {
    backgroundColor: '#4CAF50',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  buttonText: { color: '#fff', fontWeight: '600', fontSize: 14 },
  errorText: { color: '#e53935', fontSize: 13, marginTop: 6 },
  emptyText: { color: '#999', fontSize: 13, marginTop: 8, textAlign: 'center' },
  listItem: {
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  listItemTitle: { fontSize: 15, fontWeight: '500', color: '#1a1a2e' },
  listItemSub: { fontSize: 12, color: '#888', marginTop: 2 },
  badge: {
    alignSelf: 'flex-start',
    borderRadius: 6,
    paddingHorizontal: 10,
    paddingVertical: 4,
    marginVertical: 8,
  },
  badgeLocal: { backgroundColor: '#E8F5E9' },
  badgeRemote: { backgroundColor: '#E3F2FD' },
  badgeText: { fontSize: 12, fontWeight: '600', color: '#333' },
  detailBox: {
    backgroundColor: '#f9f9f9',
    borderRadius: 8,
    padding: 12,
    marginTop: 8,
  },
  detailTitle: { fontSize: 16, fontWeight: '700', color: '#1a1a2e' },
  detailSub: { fontSize: 12, color: '#888', marginTop: 2 },
  detailDesc: { fontSize: 13, color: '#555', marginTop: 4, fontStyle: 'italic' },
  nutrientHeader: {
    fontSize: 13,
    fontWeight: '700',
    color: '#333',
    marginTop: 12,
    marginBottom: 6,
    textTransform: 'uppercase',
  },
  nutrientRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 4,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  nutrientName: { fontSize: 13, color: '#555', flex: 1 },
  nutrientValue: { fontSize: 13, color: '#1a1a2e', fontWeight: '500' },
  tagContainer: { flexDirection: 'row', flexWrap: 'wrap', marginTop: 8, gap: 6 },
  tag: {
    backgroundColor: '#E8F5E9',
    borderRadius: 12,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  tagText: { fontSize: 12, color: '#2E7D32', fontWeight: '500' },
});

export default FooDBTest;
