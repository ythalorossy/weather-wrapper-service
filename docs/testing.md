# Testing

```bash
cd backend
mvn verify
```

Test layout:
- **Unit:** domain value objects, use-case orchestration (mocked ports incl. `Cache<V>`)
- **Helper:** `CacheAsideTest` (4 tests for `getOrLoad` hit/miss/exception paths + `requirePositive`)
- **Adapter integration:** `RedisJsonCache<V>` (Testcontainers Redis),
  `NominatimGeocodingProvider` (WireMock), NWS providers (WireMock),
  `ComputedSunTimesProvider` (incl. Reykjavik polar-day boundary check)
