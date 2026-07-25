/**
 * Tomady Native Modules — Demo Dashboard
 *
 * A tabbed React Native test UI that exercises all exposed native modules:
 *  - TomadyFooDB: Food search, cache-first read-through, food groups
 *  - TomadyDiet: Profile, bio records, meal logging, nutrition analysis
 *  - TomadyGemma: Recipe computation, Q&A, token streaming
 *  - Background Jobs: Worker trigger, suggestion event listener
 *
 * Each tab wraps a dedicated section component that manages its own state
 * and directly calls NativeModules methods.
 */
import React, { useState, useCallback } from 'react';
import {
  SafeAreaView,
  View,
  Text,
  TouchableOpacity,
  StatusBar,
  StyleSheet,
  Platform,
} from 'react-native';
import FooDBTest from './src/sections/FooDBTest';
import DietTest from './src/sections/DietTest';
import GemmaTest from './src/sections/GemmaTest';
import WorkerTest from './src/sections/WorkerTest';

// ── Tab definitions ─────────────────────────────────────────────────────

type TabId = 'foodb' | 'diet' | 'gemma' | 'worker';

interface Tab {
  id: TabId;
  label: string;
  icon: string;
  component: React.FC;
}

const TABS: Tab[] = [
  { id: 'foodb', label: 'FooDB', icon: '🔍', component: FooDBTest },
  { id: 'diet', label: 'Diet', icon: '🥗', component: DietTest },
  { id: 'gemma', label: 'Gemma', icon: '🤖', component: GemmaTest },
  { id: 'worker', label: 'Worker', icon: '⏰', component: WorkerTest },
];

// ── App Component ───────────────────────────────────────────────────────

const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabId>('foodb');

  const ActiveComponent = useCallback(() => {
    const tab = TABS.find((t) => t.id === activeTab)!;
    const Component = tab.component;
    return <Component />;
  }, [activeTab]);

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#fff" />

      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Tomady Demo</Text>
        <Text style={styles.headerSub}>Native Module Test Dashboard</Text>
      </View>

      {/* Tab Bar */}
      <View style={styles.tabBar}>
        {TABS.map((tab) => {
          const isActive = activeTab === tab.id;
          return (
            <TouchableOpacity
              key={tab.id}
              style={[styles.tab, isActive && styles.tabActive]}
              onPress={() => setActiveTab(tab.id)}
              activeOpacity={0.7}
            >
              <Text style={styles.tabIcon}>{tab.icon}</Text>
              <Text style={[styles.tabLabel, isActive && styles.tabLabelActive]}>
                {tab.label}
              </Text>
            </TouchableOpacity>
          );
        })}
      </View>

      {/* Content */}
      <View style={styles.content}>
        <ActiveComponent />
      </View>
    </SafeAreaView>
  );
};

// ── Styles ───────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f0f2f5',
    paddingTop: Platform.OS === 'android' ? StatusBar.currentHeight ?? 24 : 0,
  },
  header: {
    backgroundColor: '#1a1a2e',
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  headerTitle: {
    fontSize: 22,
    fontWeight: '800',
    color: '#fff',
    letterSpacing: 0.5,
  },
  headerSub: {
    fontSize: 13,
    color: '#a0a0b8',
    marginTop: 2,
  },
  tabBar: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
    paddingHorizontal: 4,
  },
  tab: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 10,
    borderBottomWidth: 3,
    borderBottomColor: 'transparent',
  },
  tabActive: {
    borderBottomColor: '#4CAF50',
  },
  tabIcon: {
    fontSize: 20,
    marginBottom: 2,
  },
  tabLabel: {
    fontSize: 12,
    fontWeight: '500',
    color: '#888',
  },
  tabLabelActive: {
    color: '#1a1a2e',
    fontWeight: '700',
  },
  content: {
    flex: 1,
  },
});

export default App;
