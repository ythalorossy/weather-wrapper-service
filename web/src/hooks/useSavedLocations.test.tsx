import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { useLastCity, useSavedLocations } from './useSavedLocations';

describe('useSavedLocations', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('returns an empty list initially', () => {
    const { result } = renderHook(() => useSavedLocations());
    expect(result.current.saved).toEqual([]);
  });

  it('add appends a city', () => {
    const { result } = renderHook(() => useSavedLocations());
    act(() => result.current.add('Arlington, VA'));
    expect(result.current.saved).toEqual(['Arlington, VA']);
    expect(result.current.isSaved('Arlington, VA')).toBe(true);
  });

  it('remove drops a city case-insensitively', () => {
    const { result } = renderHook(() => useSavedLocations());
    act(() => result.current.add('Arlington, VA'));
    act(() => result.current.add('Honolulu, HI'));
    act(() => result.current.remove('arlington, va'));
    expect(result.current.saved).toEqual(['Honolulu, HI']);
  });

  it('two subscribers see the same change', () => {
    const a = renderHook(() => useSavedLocations());
    const b = renderHook(() => useSavedLocations());

    act(() => a.result.current.add('Arlington, VA'));

    expect(b.result.current.saved).toEqual(['Arlington, VA']);
  });
});

describe('useLastCity', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('returns null when nothing is stored', () => {
    const { result } = renderHook(() => useLastCity());
    expect(result.current.lastCity).toBeNull();
  });

  it('returns the stored value on mount', () => {
    localStorage.setItem('weather-wrapper-service:last-city:v1', 'Honolulu, HI');
    const { result } = renderHook(() => useLastCity());
    expect(result.current.lastCity).toBe('Honolulu, HI');
  });
});
