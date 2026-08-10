import { type RefObject } from 'react';
import type { HourlyWeatherResponse } from '../api/weather';

interface Props {
  data: HourlyWeatherResponse;
  rowRefs?: RefObject<(HTMLLIElement | null)[]>;
  hours?: number;
}

/**
 * Compact hourly forecast list. One row per hour, with the time, temperature,
 * short forecast, and a day/night glyph.
 *
 * If `rowRefs` is provided, each <li> is registered into the array at its
 * own index so a sibling chart can scroll-to-row on click.
 */
export function HourlyList({ data, rowRefs, hours = 24 }: Props) {
  const periods = data.forecast.periods.slice(0, hours);

  return (
    <ul className="divide-y divide-slate-100 rounded-2xl bg-white shadow-sm border border-slate-200 overflow-hidden">
      {periods.map((period, idx) => (
        <li
          key={`${period.startTime}-${idx}`}
          ref={(el) => {
            if (rowRefs?.current) rowRefs.current[idx] = el;
          }}
          className="px-4 py-2.5 grid grid-cols-[5rem_4rem_1fr_auto] items-center gap-3 text-sm"
        >
          <time
            dateTime={period.startTime}
            className="text-slate-600 tabular-nums"
          >
            {formatHour(period.startTime, data.resolvedLocation.displayName)}
          </time>
          <span className="text-right text-slate-400 text-xs uppercase tracking-wider">
            {period.daytime ? '☀' : '☾'}
          </span>
          <span className="text-slate-700 truncate">
            {period.shortForecast}
          </span>
          <span className="text-right font-light tabular-nums text-slate-900">
            {period.temperature.formatted}
          </span>
        </li>
      ))}
    </ul>
  );
}

function formatHour(iso: string, _displayName: string): string {
  const date = new Date(iso);
  return date.toLocaleTimeString(undefined, {
    hour: 'numeric',
    hour12: true,
  });
}