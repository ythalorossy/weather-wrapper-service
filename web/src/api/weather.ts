/**
 * Client for `GET /api/v1/weather?city=...`.
 *
 * Types mirror the backend `WeatherResponse` record (see
 * `backend/weather-api/src/main/java/io/ythalorossy/weatherapi/api/dto/WeatherResponse.java`).
 * Jackson serializes `Instant` as ISO-8601, so `generatedAt` is a string here.
 */

export type WeatherResponse = {
  city: string;
  resolvedLocation: {
    latitude: number;
    longitude: number;
    displayName: string;
  };
  forecast: {
    generatedAt: string;
    source: string;
    periods: ForecastPeriod[];
  };
};

export type ForecastPeriod = {
  name: string;
  temperature: {
    value: number;
    unit: string;
    formatted: string;
  };
  windSpeed: string;
  windDirection: string;
  shortForecast: string;
  detailedForecast: string;
  daytime: boolean;
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

export async function fetchWeather(city: string): Promise<WeatherResponse> {
  const url = `/api/v1/weather?city=${encodeURIComponent(city)}`;
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

  return (await response.json()) as WeatherResponse;
}