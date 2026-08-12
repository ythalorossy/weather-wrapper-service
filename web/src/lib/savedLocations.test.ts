import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  add,
  isSaved,
  load,
  loadLastCity,
  remove,
  save,
  saveLastCity,
} from './savedLocations';

describe('savedLocations helpers', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('returns an empty list when nothing is stored', () => {
    expect(load()).toEqual([]);
  });

  it('round-trips a list', () => {
    save(['Arlington, VA', 'Honolulu, HI']);
    expect(load()).toEqual(['Arlington, VA', 'Honolulu, HI']);
  });

  it('add is case-insensitive and trims whitespace', () => {
    add('Arlington, VA');
    add('  arlington, va  ');
    add('Honolulu, HI');
    expect(load()).toEqual(['Arlington, VA', 'Honolulu, HI']);
  });

  it('remove is case-insensitive', () => {
    add('Arlington, VA');
    remove('ARLINGTON, va');
    expect(load()).toEqual([]);
  });

  it('isSaved is case-insensitive', () => {
    add('Arlington, VA');
    expect(isSaved('arlington, va')).toBe(true);
    expect(isSaved('Honolulu')).toBe(false);
  });

  it('swallows quota errors on save', () => {
    const original = Storage.prototype.setItem;
    Storage.prototype.setItem = vi.fn(() => {
      throw new Error('QuotaExceededError');
    });
    try {
      expect(() => save(['Arlington, VA'])).not.toThrow();
    } finally {
      Storage.prototype.setItem = original;
    }
  });

  it('returns an empty list when localStorage is unavailable', () => {
    const original = Storage.prototype.getItem;
    Storage.prototype.getItem = vi.fn(() => {
      throw new Error('SecurityError');
    });
    expect(load()).toEqual([]);
    Storage.prototype.getItem = original;
  });

  it('saveLastCity + loadLastCity round-trips', () => {
    expect(loadLastCity()).toBeNull();
    saveLastCity('Arlington, VA');
    expect(loadLastCity()).toBe('Arlington, VA');
    saveLastCity(null);
    expect(loadLastCity()).toBeNull();
  });
});