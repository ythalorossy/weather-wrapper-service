import { useRef, useState } from 'react';
import type { HourlyWeatherResponse, SunView, WeatherResponse } from '../api/weather';
import { ForecastCard } from './ForecastCard';
import { HourlyList } from './HourlyList';
import { HourlyChart } from './HourlyChart';

type Mode = 'daily' | 'hourly';

interface Props {
  daily: WeatherResponse;
  hourly: HourlyWeatherResponse;
  sun?: SunView;
}

/**
 * Tabbed container for the two forecast views. Renders the Daily tab by default
 * with a switch to the Hourly view. The Hourly tab pairs an SVG chart
 * (click-to-scroll) with the existing tabular list.
 */
export function ForecastTabs({ daily, hourly, sun }: Props) {
  const [mode, setMode] = useState<Mode>('daily');
  const rowRefs = useRef<(HTMLLIElement | null)[]>([]);

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
          <div className="space-y-3">
            {hourly.forecast.periods.length > 0 && (
              <HourlyChart data={hourly} rowRefs={rowRefs} sun={sun} />
            )}
            <HourlyList data={hourly} rowRefs={rowRefs} />
          </div>
        )}
      </div>
    </div>
  );
}