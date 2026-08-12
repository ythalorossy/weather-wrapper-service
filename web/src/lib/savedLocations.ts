/**
 * Pure helpers for the saved-locations localStorage slice.
 *
 * Two keys: `weather-wrapper-service:saved-cities:v1` and
 * `weather-wrapper-service:last-city:v1`. Errors from localStorage
 * (quota exceeded, SecurityError) are swallowed — the UI degrades to
 * "no saved locations" silently.
 */

const SAVED_KEY = 'weather-wrapper-service:saved-cities:v1';
const LAST_KEY = 'weather-wrapper-service:last-city:v1';

function normalize(city: string): string {
  return city.trim();
}

function sameCity(a: string, b: string): boolean {
  return a.trim().toLowerCase() === b.trim().toLowerCase();
}

function safeGet(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function safeSet(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    // quota exceeded / SecurityError — degrade silently
  }
}

function safeRemove(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    // quota exceeded / SecurityError — degrade silently
  }
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