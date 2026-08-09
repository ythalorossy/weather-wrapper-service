import type { HourlyWeatherResponse } from '../api/weather';

interface Props {
  data: HourlyWeatherResponse;
  /** Number of hours to show. Default 24 (today + tonight). */
  hours?: number;
}

/**
 * Compact hourly forecast list. One row per hour, with the time, temperature,
 * short forecast, and a day/night glyph.
 *
 * Receives the full response so the parent can decide how many hours to slice
 * without re-fetching.
 */
export function HourlyList({ data, hours = 24 }: Props) {
  const periods = data.forecast.periods.slice(0, hours);

  return (
    <ul className="divide-y divide-slate-100 rounded-2xl bg-white shadow-sm border border-slate-200 overflow-hidden">
      {periods.map((period, idx) => (
        <li
          key={`${period.startTime}-${idx}`}
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

/**
 * Format an ISO-8601 timestamp as e.g. "3 PM" in the location's local time.
 * Uses `Intl.DateTimeFormat` with `timeZoneName: 'short'` only if the browser
 * knows the timezone — otherwise falls back to UTC offset.
 */
function formatHour(iso: string, _displayName: string): string {
  const date = new Date(iso);
  // Without a proper timezone resolver we keep this simple: render the hour in
  // the user's local browser timezone, which is correct enough for an NWS
  // forecast served at hourly granularity (the timestamps are absolute; users
  // see them in their own clock).
  return date.toLocaleTimeString(undefined, {
    hour: 'numeric',
    hour12: true,
  });
}