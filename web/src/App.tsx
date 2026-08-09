import { useState } from 'react';
import { useWeatherQuery } from './hooks/useWeatherQuery';
import { useHourlyWeather } from './hooks/useHourlyWeather';
import { useLocationMetadata } from './hooks/useLocationMetadata';
import { useCurrentConditions } from './hooks/useCurrentConditions';
import { useAlerts } from './hooks/useAlerts';
import { SearchForm } from './components/SearchForm';
import { ForecastTabs } from './components/ForecastTabs';
import { MetadataBar } from './components/MetadataBar';
import { CurrentConditionsCard } from './components/CurrentConditionsCard';
import { AlertsBanner } from './components/AlertsBanner';
import { AlertList } from './components/AlertList';

export default function App() {
  const [city, setCity] = useState<string | null>(null);
  // Per-session dismissed alert ids (not persisted — page refresh resets).
  const [dismissedAlertIds, setDismissedAlertIds] = useState<Set<string>>(
    () => new Set(),
  );

  const daily = useWeatherQuery(city);
  const hourly = useHourlyWeather(city);
  const metadata = useLocationMetadata(city);
  const conditions = useCurrentConditions(city);
  const alerts = useAlerts(city);

  function dismissAlert(id: string) {
    setDismissedAlertIds((prev) => {
      const next = new Set(prev);
      next.add(id);
      return next;
    });
  }

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

      <main className="mt-8 w-full max-w-2xl space-y-6">
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

        {alerts.data && alerts.data.alerts.length > 0 && (
          <AlertsBanner
            alerts={alerts.data.alerts}
            dismissedIds={dismissedAlertIds}
            onDismiss={dismissAlert}
          />
        )}

        {daily.data && (
          <>
            <header>
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

            {conditions.data?.observation && (
              <CurrentConditionsCard observation={conditions.data.observation} />
            )}

            {hourly.data ? (
              <ForecastTabs daily={daily.data} hourly={hourly.data} />
            ) : (
              <ForecastTabs
                daily={daily.data}
                hourly={{
                  city: daily.data.city,
                  resolvedLocation: daily.data.resolvedLocation,
                  forecast: { generatedAt: '', source: '', periods: [] },
                }}
              />
            )}

            {alerts.data && alerts.data.alerts.length > 0 && (
              <section>
                <h3 className="text-sm uppercase tracking-wider text-slate-500 mb-2">
                  All active alerts ({alerts.data.alerts.length})
                </h3>
                <AlertList alerts={alerts.data.alerts} />
              </section>
            )}
          </>
        )}
      </main>
    </div>
  );
}