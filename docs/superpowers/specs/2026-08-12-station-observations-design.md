# Station Observations & Raw METAR Design Spec

## Overview
Add comprehensive station-level weather data from NWS (`api.weather.gov`), including nearby reporting stations list, decoded metrics, raw METAR text, and observation trends, exposed via new backend endpoints and a dedicated "Stations" tab in the React frontend.

## 1. Domain & Ports Layer (`weather-domain`)
- **Models**:
  - `Station`: `String stationId`, `String name`, `double latitude`, `double longitude`.
  - `StationObservation`: `String stationId`, `Instant timestamp`, `Temperature temperature`, `Integer humidity`, `String windSpeed`, `String windDirection`, `String rawMessage`, `Double barometricPressure`.
- **Ports**:
  - `StationsProvider`: `List<Station> getStations(String gridId, int gridX, int gridY)`
  - `StationObservationProvider`: `List<StationObservation> getObservations(String stationId)`

## 2. Application Layer (`weather-application`)
- **Use Cases**:
  - `GetStationsUseCase`: Resolves city to gridpoint, checks `Cache<List<Station>>`, calls `StationsProvider` on miss, caches for 1 hour.
  - `GetStationObservationsUseCase`: Checks `Cache<List<StationObservation>>`, calls `StationObservationProvider` on miss, caches for 10 minutes.
- **Cache-Aside**: Utilizes generic `Cache<V>` and `CacheAside.getOrLoad`.

## 3. Infrastructure & API Layer (`weather-infrastructure` & `weather-api`)
- **Adapters**:
  - `NwsStationsProvider`: Calls `/gridpoints/{gridId}/{x},{y}/stations`.
  - `NwsStationObservationProvider`: Calls `/stations/{stationId}/observations`.
- **Endpoints**:
  - `GET /api/v1/weather/stations?city=...`
  - `GET /api/v1/weather/stations/{stationId}/observations`

## 4. Frontend Layer (`web/`)
- **API**: Extend `web/src/api/weather.ts` with station fetch methods.
- **Hooks**: `useStationsQuery` and `useStationObservationsQuery`.
- **Components**: `StationsTab`, `StationList`, `StationDetailCard` (with raw METAR drawer and observation trends).
