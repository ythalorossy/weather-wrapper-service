/**
 * Client for the weather API.
 *
 * Three endpoints, all under `/api/v1/weather`:
 * - `GET /api/v1/weather?city=...`     — 12-hour-block forecast
 * - `GET /api/v1/weather/hourly?city=...` — hourly forecast (~156 hours)
 * - `GET /api/v1/weather/metadata?city=...` — NWS WFO info
 *
 * Types mirror the backend records in
 * `backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/dto/`.
 * Jackson serializes `Instant` as ISO-8601 strings.
 */

export type ResolvedLocation = {
  latitude: number;
  longitude: number;
  displayName: string;
};

export type Temperature = {
  value: number;
  unit: string;
  formatted: string;
};

export type WeatherResponse = {
  city: string;
  resolvedLocation: ResolvedLocation;
  forecast: {
    generatedAt: string;
    source: string;
    periods: ForecastPeriod[];
  };
};

export type ForecastPeriod = {
  name: string;
  temperature: Temperature;
  windSpeed: string;
  windDirection: string;
  shortForecast: string;
  detailedForecast: string;
  daytime: boolean;
};

export type HourlyWeatherResponse = {
  city: string;
  resolvedLocation: ResolvedLocation;
  forecast: {
    generatedAt: string;
    source: string;
    periods: HourlyForecastPeriod[];
  };
};

export type HourlyForecastPeriod = {
  startTime: string;
  temperature: Temperature;
  windSpeed: string;
  windDirection: string;
  shortForecast: string;
  daytime: boolean;
};

export type SunView = {
  date: string;             // ISO date "YYYY-MM-DD" in location timezone
  sunriseLocal: string;     // "HH:mm"
  sunsetLocal: string;      // "HH:mm"
  dayLengthSeconds: number;
};

export type LocationMetadataResponse = {
  city: string;
  resolvedLocation: ResolvedLocation;
  office: {
    officeId: string;
    name: string;
    radarStationId: string;
    timezoneId: string;
    forecastOfficeUrl: string;
  };
  sun?: SunView;
};

// ----- /api/v1/conditions -----

export type CurrentConditionsResponse = {
  city: string;
  resolvedLocation: ResolvedLocation;
  observation: Observation | null;
};

export type Observation = {
  stationId: string;
  stationName: string;
  timestamp: string;
  timestampLocal: string | null;
  temperatureFahrenheit: number | null;
  dewpointFahrenheit: number | null;
  windSpeedMph: number | null;
  windDirectionDegrees: number | null;
  windDirectionCompass: string | null;
  relativeHumidityPercent: number | null;
  barometricPressureInHg: number | null;
  textDescription: string | null;
};

// ----- /api/v1/alerts -----

export type AlertsResponse = {
  city: string;
  resolvedLocation: ResolvedLocation;
  alerts: WeatherAlert[];
};

export type WeatherAlert = {
  id: string;
  event: string;
  severity: 'Extreme' | 'Severe' | 'Moderate' | 'Minor' | 'Unknown';
  certainty: 'Observed' | 'Likely' | 'Possible' | 'Unlikely' | 'Unknown';
  urgency: 'Immediate' | 'Expected' | 'Future' | 'Past' | 'Unknown';
  category:
    | 'Met'
    | 'Health'
    | 'Security'
    | 'Safety'
    | 'Hydrological'
    | 'Marine'
    | 'Fire'
    | 'Quality'
    | 'Aviation'
    | 'Law'
    | 'Unknown';
  headline: string;
  description: string;
  instruction: string | null;
  areaDesc: string;
  sent: string;
  effective: string;
  expires: string;
  webUrl: string | null;
};

export class WeatherError extends Error {
  readonly status: number;
  readonly problem?: { title?: string; detail?: string };

  constructor(
    message: string,
    status: number,
    problem?: { title?: string; detail?: string },
  ) {
    super(message);
    this.name = 'WeatherError';
    this.status = status;
    this.problem = problem;
  }
}

async function fetchJson<T>(url: string): Promise<T> {
  const response = await fetch(url, {
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) {
    let problem: { title?: string; detail?: string } | undefined;
    try {
      problem = await response.json();
    } catch {
      // Body wasn't JSON — fall back to status text.
    }
    throw new WeatherError(
      problem?.detail ?? problem?.title ?? `Request failed with status ${response.status}`,
      response.status,
      problem,
    );
  }
  return (await response.json()) as T;
}

export async function fetchWeather(city: string): Promise<WeatherResponse> {
  return fetchJson<WeatherResponse>(
    `/api/v1/weather?city=${encodeURIComponent(city)}`,
  );
}

export async function fetchHourlyWeather(city: string): Promise<HourlyWeatherResponse> {
  return fetchJson<HourlyWeatherResponse>(
    `/api/v1/weather/hourly?city=${encodeURIComponent(city)}`,
  );
}

export async function fetchLocationMetadata(city: string): Promise<LocationMetadataResponse> {
  return fetchJson<LocationMetadataResponse>(
    `/api/v1/weather/metadata?city=${encodeURIComponent(city)}`,
  );
}

export async function fetchCurrentConditions(city: string): Promise<CurrentConditionsResponse> {
  return fetchJson<CurrentConditionsResponse>(
    `/api/v1/conditions?city=${encodeURIComponent(city)}`,
  );
}

export async function fetchAlerts(city: string): Promise<AlertsResponse> {
  return fetchJson<AlertsResponse>(`/api/v1/alerts?city=${encodeURIComponent(city)}`);
}

export type DiscussionResponse = {
  officeId: string;
  issuanceTime: string; // ISO
  body: string;
};

export async function fetchDiscussion(city: string): Promise<DiscussionResponse> {
  return fetchJson<DiscussionResponse>(
    `/api/v1/weather/forecast/discussion?city=${encodeURIComponent(city)}`,
  );
}

// ----- /api/v1/weather/stations -----

export type Station = {
  stationId: string;
  name: string;
  latitude: number;
  longitude: number;
};

export type StationObservation = {
  stationId: string;
  timestamp: string;
  temperature: Temperature;
  humidity: number | null;
  windSpeed: string;
  windDirection: string;
  rawMessage: string;
  barometricPressure: number | null;
};

export async function fetchStations(city: string): Promise<Station[]> {
  return fetchJson<Station[]>(
    `/api/v1/weather/stations?city=${encodeURIComponent(city)}`,
  );
}

export async function fetchStationObservations(stationId: string): Promise<StationObservation[]> {
  return fetchJson<StationObservation[]>(
    `/api/v1/weather/stations/${encodeURIComponent(stationId)}/observations`,
  );
}