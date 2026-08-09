import { useState } from 'react';
import { useWeatherQuery } from './hooks/useWeatherQuery';
import { useHourlyWeather } from './hooks/useHourlyWeather';
import { useLocationMetadata } from './hooks/useLocationMetadata';
import { SearchForm } from './components/SearchForm';
import { ForecastTabs } from './components/ForecastTabs';
import { MetadataBar } from './components/MetadataBar';

export default function App() {
  const [city, setCity] = useState<string | null>(null);
  const daily = useWeatherQuery(city);
  const hourly = useHourlyWeather(city);
  const metadata = useLocationMetadata(city);

  return (
    <div className="min-h-screen text-slate-900 flex flex-col items-center px-4 py-12">
      <header className="text-center mb-8">
        <h1 className="text-3xl font-semibold tracking-tight">Weather</h1>
        <p className="text-slate-500 mt-1">Forecasts from the National Weather Service</p>
      </header>

      <SearchForm
        onSubmit={setCity}
        isFetching={daily.isFetching}
        initialValue={city ?? ''}
      />

      <main className="mt-8 w-full max-w-2xl">
        {city === null && (
          <p className="text-center text-slate-400 py-12">
            Enter a city to see the forecast.
          </p>
        )}

        {city !== null && daily.isPending && (
          <p className="text-center text-slate-500 py-12">
            Loading forecast for <span className="font-medium">{city}</span>…
          </p>
        )}

        {city !== null && daily.isError && (
          <div
            role="alert"
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-red-800"
          >
            <p className="font-medium">Couldn&apos;t load forecast</p>
            <p className="text-sm mt-1">{daily.error.message}</p>
          </div>
        )}

        {daily.data && (
          <>
            <header className="mb-3">
              <p className="text-xs uppercase tracking-wider text-slate-500">Forecast for</p>
              <h2 className="text-xl font-semibold mt-1">{daily.data.city}</h2>
              <p className="text-sm text-slate-500 mt-1">
                {daily.data.resolvedLocation.displayName}
              </p>
              {metadata.data && <MetadataBar data={metadata.data} />}
              {metadata.isError && (
                <p className="text-xs text-slate-400 mt-2">
                  Couldn&apos;t load forecast office info.
                </p>
              )}
            </header>

            {hourly.data ? (
              <ForecastTabs daily={daily.data} hourly={hourly.data} />
            ) : (
              // Hourly data is non-blocking — the daily card is enough while it loads.
              <ForecastTabs
                daily={daily.data}
                hourly={{
                  city: daily.data.city,
                  resolvedLocation: daily.data.resolvedLocation,
                  forecast: {
                    generatedAt: '',
                    source: '',
                    periods: [],
                  },
                }}
              />
            )}
          </>
        )}
      </main>
    </div>
  );
}