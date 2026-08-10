# Tech stack

| Layer            | Choice                                                       |
|------------------|--------------------------------------------------------------|
| Language         | Java 21                                                      |
| Framework        | Spring Boot 3.3.5 (web, validation, actuator, data-redis)    |
| Build            | Maven (multi-module)                                         |
| Cache            | Redis 7 via Spring Data Redis (Lettuce)                      |
| HTTP client      | Spring `RestClient` (Spring Framework 6.1+)                  |
| Geocoding        | Public Nominatim (no key, rate-limited to ~1 req/s)          |
| Weather          | NWS `api.weather.gov` (no key, US-only, lat/lon, requires UA)|
| Tests            | JUnit 5, Mockito, AssertJ, WireMock 3, Testcontainers (Redis)|
| Containerization | Multi-stage Docker (Maven 3.9 + Temurin 21 JRE)              |
