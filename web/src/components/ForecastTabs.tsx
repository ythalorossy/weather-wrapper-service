import { useState } from 'react';
import type { HourlyWeatherResponse, WeatherResponse } from '../api/weather';
import { ForecastCard } from './ForecastCard';
import { HourlyList } from './HourlyList';

type Mode = 'daily' | 'hourly';

interface Props {
  daily: WeatherResponse;
  hourly: HourlyWeatherResponse;
}

/**
 * Tabbed container for the two forecast views. Renders the Daily tab by default
 * (matches the pre-M1 single-card layout), with a switch to the Hourly view.
 *
 * Owns its own tab state; the parent doesn't need to know which mode is shown.
 */
export function ForecastTabs({ daily, hourly }: Props) {
  const [mode, setMode] = useState<Mode>('daily');

  return (
    <div>
      <div role="tablist" aria-label="Forecast view" className="flex gap-1 mb-4 border-b border-slate-200">
        <button
          role="tab"
          type="button"
          aria-selected={mode === 'daily'}
          onClick={() => setMode('daily')}
          className={`px-3 py-2 text-sm font-medium -mb-px border-b-2 transition-colors ${
            mode === 'daily'
              ? 'border-sky-600 text-sky-700'
              : 'border-transparent text-slate-500 hover:text-slate-700'
          }`}
        >
          Daily
        </button>
        <button
          role="tab"
          type="button"
          aria-selected={mode === 'hourly'}
          onClick={() => setMode('hourly')}
          className={`px-3 py-2 text-sm font-medium -mb-px border-b-2 transition-colors ${
            mode === 'hourly'
              ? 'border-sky-600 text-sky-700'
              : 'border-transparent text-slate-500 hover:text-slate-700'
          }`}
        >
          Hourly
        </button>
      </div>

      <div role="tabpanel">
        {mode === 'daily' ? (
          <ForecastCard data={daily} />
        ) : (
          <HourlyList data={hourly} />
        )}
      </div>
    </div>
  );
}