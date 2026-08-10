# Testing

```bash
cd backend
mvn verify
```

Test layout:
- **Unit:** domain value objects, `GetWeatherUseCase` orchestration (mocked ports)
- **Adapter integration:** `RedisWeatherCache` (Testcontainers Redis),
  `NominatimGeocodingProvider` (WireMock), `NwsWeatherProvider` (WireMock)
