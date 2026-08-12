import { useCallback, useSyncExternalStore } from 'react';
import {
  add as addToStorage,
  isSaved as isSavedInStorage,
  load,
  loadLastCity,
  remove as removeFromStorage,
  saveLastCity,
} from '../lib/savedLocations';

const SAVED_KEY = 'weather-wrapper-service:saved-cities:v1';
const LAST_KEY = 'weather-wrapper-service:last-city:v1';

let savedRawCache: string | null = '';
let savedCache: string[] = [];

function subscribe(cb: () => void): () => void {
  const onStorage = (e: StorageEvent) => {
    if (e.key === null || e.key.startsWith('weather-wrapper-service:')) {
      savedRawCache = '';
      cb();
    }
  };
  window.addEventListener('storage', onStorage);
  return () => window.removeEventListener('storage', onStorage);
}

function getSavedSnapshot(): string[] {
  const raw = localStorage.getItem(SAVED_KEY);
  if (raw !== savedRawCache) {
    savedRawCache = raw;
    savedCache = load();
  }
  return savedCache;
}

function getLastCitySnapshot(): string | null {
  return loadLastCity();
}

export function useSavedLocations() {
  const saved = useSyncExternalStore(subscribe, getSavedSnapshot, getSavedSnapshot);

  const add = useCallback((city: string) => {
    addToStorage(city);
    window.dispatchEvent(new StorageEvent('storage', { key: SAVED_KEY }));
  }, []);

  const remove = useCallback((city: string) => {
    removeFromStorage(city);
    window.dispatchEvent(new StorageEvent('storage', { key: SAVED_KEY }));
  }, []);

  const isSaved = useCallback((city: string) => isSavedInStorage(city), []);

  return { saved, add, remove, isSaved };
}

export function useLastCity() {
  const lastCity = useSyncExternalStore(subscribe, getLastCitySnapshot, getLastCitySnapshot);
  const setLastCity = useCallback((city: string | null) => {
    saveLastCity(city);
    window.dispatchEvent(new StorageEvent('storage', { key: LAST_KEY }));
  }, []);
  return { lastCity: lastCity || null, setLastCity };
}