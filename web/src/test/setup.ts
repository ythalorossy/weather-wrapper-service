import '@testing-library/jest-dom/vitest';

if (typeof globalThis.localStorage?.clear !== 'function') {
  Object.defineProperty(globalThis, 'localStorage', {
    get() {
      return (globalThis as any).window?._localStorage;
    },
    configurable: true,
  });
}