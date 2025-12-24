import React, { useState, useEffect } from 'react';
import {
  SafeAreaView,
  StyleSheet,
  View,
  Button,
  Text,
  StatusBar,
  Alert,
  NativeModules,
  Platform,
  ScrollView,
  Switch,
  TextInput,
  TouchableOpacity,
  FlatList,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

const { CallBlockerModule } = NativeModules;
const BLOCKLIST_KEY = 'blocklist';

const App = () => {
  const [blocklist, setBlocklist] = useState<string[]>([]);
  const [numberInput, setNumberInput] = useState('');
  const [appStatus, setAppStatus] = useState('Initializing...');
  const [activeTab, setActiveTab] = useState('settings');
  const [history, setHistory] = useState<any[]>([]);

  useEffect(() => {
    const initializeApp = async () => {
      // Permissions are now requested from MainActivity
      // Just request the Call Screening role if needed
      if (Platform.OS === 'android') {
        try {
          console.log('App initialized - permissions are handled by MainActivity');
          setAppStatus('✓ App initialized');
          await CallBlockerModule.requestRole();
          setAppStatus('✓ Call Screening Role Requested');
        } catch (err) {
          console.error('Initialization error:', err);
          setAppStatus('⚠ Initialization pending');
        }
      }
    };

    initializeApp();
    loadBlocklist();
  }, []);

  useEffect(() => {
    CallBlockerModule.setBlocklist(blocklist);
    saveBlocklist();
  }, [blocklist]);

  useEffect(() => {
    if (activeTab === 'history') {
      loadHistory();
    }
  }, [activeTab]);

  const loadHistory = async () => {
    try {
      const data = await CallBlockerModule.getBlockedHistory();
      // Sort by timestamp descending (newest first)
      const sorted = data.sort((a: any, b: any) => b.timestamp - a.timestamp);
      setHistory(sorted);
    } catch (error) {
      console.error('Failed to load history:', error);
    }
  };

  const loadBlocklist = async () => {
    try {
      const storedBlocklist = await AsyncStorage.getItem(BLOCKLIST_KEY);
      
      if (storedBlocklist !== null) {
        const parsedBlocklist = JSON.parse(storedBlocklist);
        setBlocklist(parsedBlocklist);
      }
    } catch (error) {
      console.error('Failed to load blocklist:', error);
    }
  };

  const saveBlocklist = async () => {
    try {
      await AsyncStorage.setItem(BLOCKLIST_KEY, JSON.stringify(blocklist));
    } catch (error) {
      console.error('Failed to save blocklist:', error);
    }
  };

  const addToBlocklist = () => {
    const number = numberInput.trim();
    if (number === '') {
      Alert.alert('Error', 'Please enter a phone number.');
      return;
    }
    
    if (blocklist.includes(number)) {
      Alert.alert('Error', 'This number is already in the blocklist.');
      return;
    }

    const newBlocklist = [...blocklist, number];
    setBlocklist(newBlocklist);
    setNumberInput('');
    Alert.alert('Success', `${number} added to blocklist.`);
  };

  const removeFromBlocklist = (number: string) => {
    const newBlocklist = blocklist.filter(item => item !== number);
    setBlocklist(newBlocklist);
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#1a1a1a" />
      
      <View style={styles.tabContainer}>
        <TouchableOpacity 
          style={[styles.tab, activeTab === 'settings' && styles.activeTab]}
          onPress={() => setActiveTab('settings')}
        >
          <Text style={[styles.tabText, activeTab === 'settings' && styles.activeTabText]}>⚙️ Settings</Text>
        </TouchableOpacity>
        <TouchableOpacity 
          style={[styles.tab, activeTab === 'history' && styles.activeTab]}
          onPress={() => setActiveTab('history')}
        >
          <Text style={[styles.tabText, activeTab === 'history' && styles.activeTabText]}>🕒 History</Text>
        </TouchableOpacity>
      </View>

      {activeTab === 'settings' ? (
        <ScrollView style={styles.content}>
          <Text style={styles.title}>🚫 Call Blocker</Text>
        
        <View style={styles.statusBox}>
          <Text style={styles.statusLabel}>App Status:</Text>
          <Text style={styles.statusText}>{appStatus}</Text>
        </View>

        

        <View style={styles.inputBox}>
          <Text style={styles.inputLabel}>Enter phone number to block:</Text>
          <TextInput
            style={styles.input}
            placeholder="e.g., +91XXXXXXXXXX or 91* for country code"
            placeholderTextColor="#666"
            keyboardType="phone-pad"
            value={numberInput}
            onChangeText={setNumberInput}
            maxLength={20}
          />
          <Text style={styles.wildcardHint}>
            💡 Use * as wildcard: e.g., "91*" blocks all numbers starting with 91
          </Text>
          <Button
            title="+ Add to Blocklist"
            onPress={addToBlocklist}
            color="#007AFF"
          />
        </View>

        <View style={styles.blocklistBox}>
          <Text style={styles.blocklistTitle}>Blocked Numbers ({blocklist.length})</Text>
          
          {blocklist.length === 0 ? (
            <Text style={styles.emptyText}>No blocked numbers - all calls will be allowed</Text>
          ) : (
            blocklist.map((number, index) => (
              <View key={index} style={styles.blocklistItem}>
                <View style={styles.numberContainer}>
                  <Text style={styles.blocklistNumber}>{number}</Text>
                  {number.includes('*') && (
                    <Text style={styles.wildcardBadge}>Wildcard</Text>
                  )}
                </View>
                <Button
                  title="Remove"
                  onPress={() => removeFromBlocklist(number)}
                  color="#ff1744"
                />
              </View>
            ))
          )}
        </View>

        <View style={styles.infoBox}>
          <Text style={styles.infoTitle}>ℹ️ How It Works</Text>
          <Text style={styles.infoText}>
            • Default: All incoming calls are allowed{'\n'}
            • Add numbers to blocklist to prevent them from reaching you{'\n'}
            • Supports wildcard patterns (e.g., "91*" blocks all Indian numbers){'\n'}
            • Uses CallScreeningService (Android 10+){'\n'}
            • Settings are saved automatically
          </Text>
        </View>

        <View style={styles.examplesBox}>
          <Text style={styles.examplesTitle}>📝 Examples</Text>
          <Text style={styles.examplesText}>
            • "1234567890" - blocks exact number{'\n'}
            • "91*" - blocks all numbers starting with 91{'\n'}
            • "+1*" - blocks all +1 country code{'\n'}
            • "555*" - blocks all numbers starting with 555
          </Text>
        </View>
        </ScrollView>
      ) : (
        <View style={styles.content}>
          <Text style={styles.title}>🚫 Blocked History</Text>
          <FlatList
            data={history}
            keyExtractor={(item, index) => index.toString()}
            renderItem={({ item }) => (
              <View style={styles.historyItem}>
                <Text style={styles.historyNumber}>{item.number}</Text>
                <Text style={styles.historyTime}>
                  {new Date(item.timestamp).toLocaleString()}
                </Text>
              </View>
            )}
            ListEmptyComponent={
              <Text style={styles.emptyText}>No blocked calls recorded yet.</Text>
            }
            onRefresh={loadHistory}
            refreshing={false}
          />
        </View>
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#1a1a1a',
  },
  content: {
    flex: 1,
    padding: 16,
  },
  tabContainer: {
    flexDirection: 'row',
    backgroundColor: '#121212',
    margin: 16,
    borderRadius: 12,
    padding: 4,
    borderWidth: 1,
    borderColor: '#333',
  },
  tab: {
    flex: 1,
    paddingVertical: 12,
    alignItems: 'center',
    borderRadius: 8,
  },
  activeTab: {
    backgroundColor: '#007AFF',
  },
  tabText: {
    color: '#888',
    fontSize: 16,
    fontWeight: '600',
  },
  activeTabText: {
    color: '#fff',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#fff',
    marginBottom: 20,
    textAlign: 'center',
  },
  statusBox: {
    backgroundColor: '#2a2a2a',
    padding: 12,
    borderRadius: 8,
    marginBottom: 16,
    borderLeftWidth: 4,
    borderLeftColor: '#007AFF',
  },
  statusLabel: {
    color: '#888',
    fontSize: 12,
    fontWeight: '600',
    textTransform: 'uppercase',
  },
  statusText: {
    color: '#fff',
    fontSize: 14,
    marginTop: 4,
  },
  settingsBox: {
    backgroundColor: '#2a2a2a',
    padding: 16,
    borderRadius: 8,
    marginBottom: 16,
  },
  settingsTitle: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  settingDescription: {
    color: '#aaa',
    fontSize: 13,
    marginBottom: 4,
  },
  inputBox: {
    backgroundColor: '#2a2a2a',
    padding: 16,
    borderRadius: 8,
    marginBottom: 16,
  },
  inputLabel: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 10,
  },
  input: {
    backgroundColor: '#1a1a1a',
    color: '#fff',
    borderWidth: 1,
    borderColor: '#444',
    borderRadius: 6,
    paddingHorizontal: 12,
    paddingVertical: 10,
    marginBottom: 10,
    fontSize: 14,
  },
  wildcardHint: {
    color: '#ffb300',
    fontSize: 12,
    fontStyle: 'italic',
    marginBottom: 12,
    padding: 8,
    backgroundColor: 'rgba(255, 179, 0, 0.1)',
    borderRadius: 4,
    borderLeftWidth: 3,
    borderLeftColor: '#ffb300',
  },
  blocklistBox: {
    backgroundColor: '#2a2a2a',
    padding: 16,
    borderRadius: 8,
    marginBottom: 16,
  },
  blocklistTitle: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 12,
  },
  emptyText: {
    color: '#888',
    fontSize: 13,
    fontStyle: 'italic',
    marginBottom: 12,
    padding: 8,
    backgroundColor: '#1a1a1a',
    borderRadius: 4,
  },
  blocklistItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 10,
    marginBottom: 8,
    backgroundColor: '#1a1a1a',
    borderRadius: 6,
    borderLeftWidth: 3,
    borderLeftColor: '#ff1744',
  },
  numberContainer: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  blocklistNumber: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '500',
  },
  wildcardBadge: {
    color: '#ffb300',
    fontSize: 11,
    fontWeight: '600',
    backgroundColor: 'rgba(255, 179, 0, 0.2)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 3,
  },
  infoBox: {
    backgroundColor: '#2a2a2a',
    padding: 12,
    borderRadius: 8,
    marginBottom: 16,
    borderLeftWidth: 4,
    borderLeftColor: '#FFC107',
  },
  infoTitle: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  infoText: {
    color: '#aaa',
    fontSize: 12,
    lineHeight: 18,
  },
  examplesBox: {
    backgroundColor: '#2a2a2a',
    padding: 12,
    borderRadius: 8,
    marginBottom: 20,
    borderLeftWidth: 4,
    borderLeftColor: '#4CAF50',
  },
  examplesTitle: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  examplesText: {
    color: '#aaa',
    fontSize: 12,
    lineHeight: 18,
  },
  historyItem: {
    backgroundColor: '#2a2a2a',
    padding: 16,
    borderRadius: 8,
    marginBottom: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#ff1744',
  },
  historyNumber: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 4,
  },
  historyTime: {
    color: '#aaa',
    fontSize: 12,
  },
});

export default App;