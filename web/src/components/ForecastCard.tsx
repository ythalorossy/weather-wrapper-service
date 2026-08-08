import type { WeatherResponse } from '../api/weather';

interface Props {
  data: WeatherResponse;
}

export function ForecastCard({ data }: Props) {
  return (
    <article className="rounded-2xl bg-white shadow-sm border border-slate-200 overflow-hidden">
      <header className="px-6 py-5 border-b border-slate-100 bg-gradient-to-br from-sky-50 to-white">
        <p className="text-xs uppercase tracking-wider text-slate-500">Forecast for</p>
        <h2 className="text-xl font-semibold mt-1">{data.city}</h2>
        <p className="text-sm text-slate-500 mt-1">{data.resolvedLocation.displayName}</p>
        <p className="text-xs text-slate-400 mt-2">
          {data.forecast.source} · generated{' '}
          {new Date(data.forecast.generatedAt).toLocaleString()}
        </p>
      </header>

      <ul className="divide-y divide-slate-100">
        {data.forecast.periods.map((period) => (
          <li
            key={period.name}
            className="px-6 py-4 flex items-start justify-between gap-4"
          >
            <div className="flex-1 min-w-0">
              <p className="font-medium">
                {period.name} {period.daytime ? '☀' : '☾'}
              </p>
              <p className="text-sm text-slate-600 mt-0.5">{period.shortForecast}</p>
              <p className="text-xs text-slate-500 mt-1">
                Wind {period.windSpeed} {period.windDirection}
              </p>
              <p className="text-xs text-slate-400 mt-2 leading-relaxed">
                {period.detailedForecast}
              </p>
            </div>
            <div className="text-right shrink-0">
              <p className="text-3xl font-light tabular-nums text-slate-900">
                {period.temperature.formatted}
              </p>
            </div>
          </li>
        ))}
      </ul>
    </article>
  );
}