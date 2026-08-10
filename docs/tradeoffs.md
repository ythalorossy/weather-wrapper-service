# Tradeoffs & future work

| Decision                                  | Why                                                                   | When to revisit                          |
|-------------------------------------------|-----------------------------------------------------------------------|------------------------------------------|
| Public Nominatim (no self-host)           | Single-purpose geocoder; the product is the weather wrapper, not OSM  | Self-host Photon if traffic grows        |
| Lat/lon cache key (not city name)         | "Arlington VA" vs "arlington, va" share a slot; same coords = same forecast | If you add per-user context              |
| Two-layer cache (geo + weather)           | Nominatim rate-limits at ~1 req/s; caching city→location absorbs traffic | If traffic exceeds Redis capacity        |
| Negative caching (60 s TTL on geo misses) | Bad-city floods (typos, probes) would otherwise exhaust Nominatim's rate limit | Tighten to 10–30 s for hostile traffic |
| 12-hour weather TTL / 30-day geo TTL      | NWS forecast updates hourly; lat/lon for a city rarely changes        | Tighten to 30–60 min for production      |
| No single-flight / stampede protection    | Premature for a single-user wrapper                                   | Add Caffeine in-process + per-key locks if traffic warrants |
| Spring Boot over lighter frameworks       | Matches existing stack; mature Redis/HTTP/validation/observability    | Already optimal                          |

## Future enhancements

- **OpenAPI spec** generation via springdoc-openapi ✅ done (`25e4497`)
- **Rate limiting** at the API layer (Bucket4j) to protect upstream ✅ done (`0ebfff9`)
- **Metrics** in Micrometer + Prometheus format ✅ done (`068892c`)
- **React UI** in `web/` (Vite + TanStack Query) ✅ done (`5eb7b6b`)
- ~~**Reactive variant** on WebClient~~ — covered by Java 21 virtual threads instead (cheaper, same effect)
- LICENSE file ✅ done (MIT)
- **Rate limiting** (Bucket4j distributed over Redis) ✅ done (`0ebfff9`)
- **OpenAPI spec** (springdoc-openapi) ✅ done (`25e4497`)
- **Micrometer + Prometheus metrics** ✅ done (`068892c`)
- **React UI** (Vite + TanStack Query + Tailwind v4) ✅ done (`5eb7b6b`)
- **M1 forecast expansion** (hourly + WFO metadata) ✅ done (`576eb3f`–`4b88d6c`)
- **M2 observations + alerts** (current conditions + active alerts + UI) ✅ done (`082e26f`–`f9dcf6a`)
