import React, { useState, useEffect } from 'react';
import {
  SafeAreaView,
  StyleSheet,
  View,
  TextInput,
  Button,
  FlatList,
  Text,
  StatusBar,
  Alert,
  NativeModules,
  PermissionsAndroid,
  Platform,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

const { CallBlockerModule } = NativeModules;
const BLOCKLIST_KEY = 'blocklist';

const App = () => {
  const [number, setNumber] = useState('');
  const [blocklist, setBlocklist] = useState<string[]>([]);

  useEffect(() => {
    const requestPermissions = async () => {
      if (Platform.OS === 'android') {
        try {
          const granted = await PermissionsAndroid.requestMultiple([
            PermissionsAndroid.PERMISSIONS.READ_PHONE_STATE,
            PermissionsAndroid.PERMISSIONS.CALL_PHONE,
            PermissionsAndroid.PERMISSIONS.READ_CALL_LOG,
          ]);
          if (
            granted['android.permission.READ_PHONE_STATE'] === PermissionsAndroid.RESULTS.GRANTED &&
            granted['android.permission.CALL_PHONE'] === PermissionsAndroid.RESULTS.GRANTED &&
            granted['android.permission.READ_CALL_LOG'] === PermissionsAndroid.RESULTS.GRANTED
          ) {
            console.log('Permissions granted');
            await CallBlockerModule.requestRole();
          } else {
            console.log('Permissions denied');
          }
        } catch (err) {
          console.warn(err);
        }
      }
    };

    requestPermissions();
    loadBlocklist();
  }, []);

  useEffect(() => {
    CallBlockerModule.setBlocklist(blocklist);
  }, [blocklist]);

  const loadBlocklist = async () => {
    try {
      const storedBlocklist = await AsyncStorage.getItem(BLOCKLIST_KEY);
      if (storedBlocklist !== null) {
        const parsedBlocklist = JSON.parse(storedBlocklist);
        setBlocklist(parsedBlocklist);
        CallBlockerModule.setBlocklist(parsedBlocklist);
      }
    } catch (error) {
      console.error('Failed to load blocklist.', error);
    }
  };

  const saveBlocklist = async (newBlocklist: string[]) => {
    try {
      await AsyncStorage.setItem(BLOCKLIST_KEY, JSON.stringify(newBlocklist));
    }
    catch (error) {
      console.error('Failed to save blocklist.', error);
    }
  };

  const handleAddNumber = () => {
    if (number.trim() === '') {
      Alert.alert('Error', 'Please enter a phone number.');
      return;
    }
    const newBlocklist = [...blocklist, number];
    setBlocklist(newBlocklist);
    saveBlocklist(newBlocklist);
    setNumber('');
  };

  const handleDeleteNumber = (numberToDelete: string) => {
    const newBlocklist = blocklist.filter(item => item !== numberToDelete);
    setBlocklist(newBlocklist);
    saveBlocklist(newBlocklist);
    CallBlockerModule.setBlocklist(newBlocklist);
  };

  const renderItem = ({ item }: { item: string }) => (
    <View style={styles.item}>
      <Text style={styles.number}>{item}</Text>
      <Button title="Delete" onPress={() => handleDeleteNumber(item)} />
    </View>
  );

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <View style={styles.content}>
        <Text style={styles.title}>Call Blocker</Text>
        <TextInput
          style={styles.input}
          placeholder="Enter phone number"
          keyboardType="phone-pad"
          value={number}
          onChangeText={setNumber}
          maxLength={15}
        />
        <Button title="Add to Blocklist" onPress={handleAddNumber} />
        <FlatList
          style={styles.list}
          data={blocklist}
          renderItem={renderItem}
          keyExtractor={(item, index) => index.toString()}
        />
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  content: {
    flex: 1,
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
  },
  input: {
    height: 40,
    borderColor: 'gray',
    borderWidth: 1,
    marginBottom: 10,
    paddingHorizontal: 10,
  },
  list: {
    marginTop: 20,
  },
  item: {
    padding: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#ccc',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  number: {
    fontSize: 18,
    flex: 1,
  },
});

export default App;