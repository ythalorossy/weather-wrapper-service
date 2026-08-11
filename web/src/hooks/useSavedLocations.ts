import { useCallback, useSyncExternalStore } from 'react';
import {
  add as addToStorage,
  isSaved as isSavedInStorage,
  load,
  loadLastCity,
  remove as removeFromStorage,
} from '../lib/savedLocations';

const subscribers = new Set<() => void>();

function emit(): void {
  subscribers.forEach((cb) => cb());
}

function subscribe(cb: () => void): () => void {
  subscribers.add(cb);
  const onStorage = (e: StorageEvent) => {
    if (e.key === null || e.key.startsWith('weather-wrapper-service:')) {
      cb();
    }
  };
  window.addEventListener('storage', onStorage);
  return () => {
    subscribers.delete(cb);
    window.removeEventListener('storage', onStorage);
  };
}

function getSnapshot(): string {
  return JSON.stringify(load());
}

export function useSavedLocations() {
  const json = useSyncExternalStore(subscribe, getSnapshot, getSnapshot);
  const saved = JSON.parse(json) as string[];

  const add = useCallback((city: string) => {
    addToStorage(city);
    emit();
  }, []);

  const remove = useCallback((city: string) => {
    removeFromStorage(city);
    emit();
  }, []);

  const isSaved = useCallback((city: string) => isSavedInStorage(city), []);

  return { saved, add, remove, isSaved };
}

export function useLastCity() {
  const lastCity = useSyncExternalStore(
    subscribe,
    () => loadLastCity() ?? '',
    () => loadLastCity() ?? '',
  );
  return { lastCity: lastCity || null };
}
