# API

The full machine-readable spec is served by springdoc-openapi:

| URL                                       | Purpose                          |
|-------------------------------------------|----------------------------------|
| <http://localhost:8080/v3/api-docs>       | OpenAPI 3 spec, JSON             |
| <http://localhost:8080/v3/api-docs.yaml>  | OpenAPI 3 spec, YAML             |
| <http://localhost:8080/swagger-ui/index.html> | Interactive Swagger UI        |

## `GET /api/v1/weather?city={city}`

12-hour-block forecast (5–7 days, day+night periods).

| Status | When                                            | Body                                     |
|--------|-------------------------------------------------|------------------------------------------|
| `200`  | Success                                         | `WeatherResponse` (see below)            |
| `400`  | `city` is missing or blank                      | RFC 9457 `ProblemDetail`                 |
| `404`  | City not found by Nominatim                     | `ProblemDetail` with `city` property     |
| `502`  | NWS unreachable or returned a non-success status| `ProblemDetail`                          |

## `GET /api/v1/weather/hourly?city={city}`

Fine-grained hourly forecast (up to ~156 hours). Sibling to the daily endpoint above, served from the same NWS gridpoint pipeline but at a finer time resolution.

| Status | When                                            | Body                                     |
|--------|-------------------------------------------------|------------------------------------------|
| `200`  | Success                                         | `HourlyWeatherResponse`                  |
| `400`  | `city` is missing or blank                      | RFC 9457 `ProblemDetail`                 |
| `404`  | City not found by Nominatim                     | `ProblemDetail`                          |
| `502`  | NWS unreachable or returned a non-success status| `ProblemDetail`                          |

## `GET /api/v1/weather/metadata?city={city}`

NWS Weather Forecast Office (WFO) info for the resolved city: issuing office id, human-readable name, timezone, radar station, and forecast-office page URL. Used by the UI's "Forecast from NWS Baltimore/Washington · Sunrise 6:42, sunset 19:34" strip.

| Status | When                                            | Body                                     |
|--------|-------------------------------------------------|------------------------------------------|
| `200`  | Success                                         | `LocationMetadataResponse`               |
| `400`  | `city` is missing or blank                      | RFC 9457 `ProblemDetail`                 |
| `404`  | City not found by Nominatim                     | `ProblemDetail`                          |
| `502`  | NWS unreachable or returned a non-success status| `ProblemDetail`                          |

## `GET /api/conditions?city={city}`

Current conditions from the nearest NWS observation station. Returns the latest reported values: temperature, dewpoint, wind speed + direction, relative humidity, barometric pressure, free-text description, and the station attribution. 10-minute cache.

| Status | When                                            | Body                                     |
|--------|-------------------------------------------------|------------------------------------------|
| `200`  | Success (may have `observation: null` if no station found nearby) | `CurrentConditionsResponse` |
| `400`  | `city` is missing or blank                      | RFC 9457 `ProblemDetail`                 |
| `404`  | City not found by Nominatim                     | `ProblemDetail`                          |
| `502`  | NWS unreachable or returned a non-success status| `ProblemDetail`                          |

## `GET /api/v1/alerts?city={city}`

Active NWS weather alerts at the resolved city. Empty list (200, not 404) when no alerts are active. Each alert carries the CAP-IP severity / certainty / urgency / category plus headline, long-form description, recommended instructions, affected area, and validity window. 5-minute cache.

| Status | When                                            | Body                                     |
|--------|-------------------------------------------------|------------------------------------------|
| `200`  | Success (alerts list may be empty)              | `AlertsResponse`                          |
| `400`  | `city` is missing or blank                      | RFC 9457 `ProblemDetail`                 |
| `404`  | City not found by Nominatim                     | `ProblemDetail`                          |
| `502`  | NWS unreachable or returned a non-success status| `ProblemDetail`                          |

## Example response

```json
{
  "city": "Arlington, VA",
  "resolvedLocation": {
    "latitude": 38.8816,
    "longitude": -77.0910,
    "displayName": "Arlington, Arlington County, Virginia, United States"
  },
  "forecast": {
    "generatedAt": "2026-08-07T12:00:00Z",
    "source": "National Weather Service (api.weather.gov)",
    "periods": [
      {
        "name": "Today",
        "temperature": { "value": 85, "unit": "FAHRENHEIT", "formatted": "85°F" },
        "windSpeed": "5 mph",
        "windDirection": "NW",
        "shortForecast": "Sunny",
        "detailedForecast": "Sunny, with a high near 85. Northwest wind around 5 mph.",
        "daytime": true
      }
    ]
  }
}
```
