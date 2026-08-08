import { useState } from 'react';
import { useWeatherQuery } from './hooks/useWeatherQuery';
import { SearchForm } from './components/SearchForm';
import { ForecastCard } from './components/ForecastCard';

export default function App() {
  const [city, setCity] = useState<string | null>(null);
  const { data, isPending, isError, error, isFetching } = useWeatherQuery(city);

  return (
    <div className="min-h-screen text-slate-900 flex flex-col items-center px-4 py-12">
      <header className="text-center mb-8">
        <h1 className="text-3xl font-semibold tracking-tight">Weather</h1>
        <p className="text-slate-500 mt-1">Forecasts from the National Weather Service</p>
      </header>

      <SearchForm onSubmit={setCity} isFetching={isFetching} initialValue={city ?? ''} />

      <main className="mt-8 w-full max-w-2xl">
        {city === null && (
          <p className="text-center text-slate-400 py-12">
            Enter a city to see the forecast.
          </p>
        )}

        {city !== null && isPending && (
          <p className="text-center text-slate-500 py-12">
            Loading forecast for <span className="font-medium">{city}</span>…
          </p>
        )}

        {city !== null && isError && (
          <div
            role="alert"
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-red-800"
          >
            <p className="font-medium">Couldn&apos;t load forecast</p>
            <p className="text-sm mt-1">{error.message}</p>
          </div>
        )}

        {data && <ForecastCard data={data} />}
      </main>
    </div>
  );
}