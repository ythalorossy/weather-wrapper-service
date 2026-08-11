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

function normalize(city: string): string {
  return city.trim();
}

function sameCity(a: string, b: string): boolean {
  return a.trim().toLowerCase() === b.trim().toLowerCase();
}

export function load(): string[] {
  let raw: string | null = null;
  try {
    if (typeof window !== 'undefined') raw = window.localStorage.getItem(SAVED_KEY);
  } catch (e) {
    console.warn('localStorage.getItem failed:', e);
  }
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === 'string') : [];
  } catch {
    return [];
  }
}

export function save(list: string[]): void {
  try {
    if (typeof window !== 'undefined') window.localStorage.setItem(SAVED_KEY, JSON.stringify(list));
  } catch (e) {
    console.warn(`localStorage.setItem failed for key "${SAVED_KEY}":`, e);
  }
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
  let raw: string | null = null;
  try {
    if (typeof window !== 'undefined') raw = window.localStorage.getItem(LAST_KEY);
  } catch (e) {
    console.warn('localStorage.getItem failed:', e);
  }
  return raw;
}

export function saveLastCity(city: string | null): void {
  if (city === null || city.trim().length === 0) {
    try {
      if (typeof window !== 'undefined') window.localStorage.removeItem(LAST_KEY);
    } catch (e) {
      console.warn(`localStorage.removeItem failed for key "${LAST_KEY}":`, e);
    }
    return;
  }
  try {
    if (typeof window !== 'undefined') window.localStorage.setItem(LAST_KEY, normalize(city));
  } catch (e) {
    console.warn(`localStorage.setItem failed for key "${LAST_KEY}":`, e);
  }
}
