/**
 * Pure helpers for the saved-locations `localStorage` slice.
 *
 * Two keys are managed here:
 *   - `weather-wrapper-service:saved-cities:v1`  — JSON array of city strings
 *   - `weather-wrapper-service:last-city:v1`     — single string or null
 *
 * All helpers fail quietly: `localStorage` may be unavailable (private mode,
 * quota exceeded, SecurityError). The UI degrades to "no saved locations"
 * with no user-visible error.
 */

const SAVED_KEY = 'weather-wrapper-service:saved-cities:v1';
const LAST_KEY = 'weather-wrapper-service:last-city:v1';

function safeGet(key: string): string | null {
  try {
    return typeof window === 'undefined' ? null : window.localStorage.getItem(key);
  } catch (e) {
    console.warn('localStorage.getItem failed:', e);
    return null;
  }
}

function safeSet(key: string, value: string): void {
  try {
    if (typeof window === 'undefined') return;
    window.localStorage.setItem(key, value);
  } catch (e) {
    console.warn(`localStorage.setItem failed for key "${key}":`, e);
  }
}

function safeRemove(key: string): void {
  try {
    if (typeof window === 'undefined') return;
    window.localStorage.removeItem(key);
  } catch (e) {
    console.warn(`localStorage.removeItem failed for key "${key}":`, e);
  }
}

function normalize(city: string): string {
  return city.trim();
}

function sameCity(a: string, b: string): boolean {
  return a.trim().toLowerCase() === b.trim().toLowerCase();
}

export function load(): string[] {
  const raw = safeGet(SAVED_KEY);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === 'string') : [];
  } catch {
    return [];
  }
}

export function save(list: string[]): void {
  safeSet(SAVED_KEY, JSON.stringify(list));
}

export function add(city: string): string[] {
  const trimmed = normalize(city);
  if (!trimmed) return load();
  const current = load();
  if (current.some((existing) => sameCity(existing, trimmed))) return current;
  const next = [...current, trimmed];
  save(next);
  return next;
}

export function remove(city: string): string[] {
  const next = load().filter((existing) => !sameCity(existing, city));
  save(next);
  return next;
}

export function isSaved(city: string): boolean {
  return load().some((existing) => sameCity(existing, city));
}

export function loadLastCity(): string | null {
  return safeGet(LAST_KEY);
}

export function saveLastCity(city: string | null): void {
  if (city === null || city.trim().length === 0) {
    safeRemove(LAST_KEY);
    return;
  }
  safeSet(LAST_KEY, normalize(city));
}