import type { Observation } from '../api/weather';

interface Props {
  observation: Observation;
}

/**
 * Compact current-conditions card. Big temperature, supporting line with
 * dewpoint + humidity, and a second line with wind + pressure + free-text
 * description. Station attribution in the footer.
 *
 * Receives a non-null Observation from the parent (which gates on
 * `observation !== null`).
 */
export function CurrentConditionsCard({ observation: obs }: Props) {
  const temp = obs.temperatureFahrenheit;
  const dewpoint = obs.dewpointFahrenheit;
  const humidity = obs.relativeHumidityPercent;
  const wind = obs.windSpeedMph;
  const windDir = obs.windDirectionCompass;
  const pressure = obs.barometricPressureInHg;
  const desc = obs.textDescription;

  return (
    <article className="rounded-2xl bg-white shadow-sm border border-slate-200 px-6 py-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-wider text-slate-500">Current conditions</p>
          {temp !== null && temp !== undefined ? (
            <p className="mt-1 text-5xl font-light tabular-nums text-slate-900">
              {Math.round(temp)}°F
            </p>
          ) : (
            <p className="mt-1 text-2xl text-slate-400">—</p>
          )}
          {desc && <p className="mt-1 text-slate-700">{desc}</p>}
        </div>

        <dl className="text-right text-sm text-slate-600 space-y-1">
          {dewpoint !== null && dewpoint !== undefined && (
            <div>
              <dt className="inline text-slate-400 text-xs">dewpoint&nbsp;</dt>
              <dd className="inline tabular-nums">{Math.round(dewpoint)}°F</dd>
            </div>
          )}
          {humidity !== null && humidity !== undefined && (
            <div>
              <dt className="inline text-slate-400 text-xs">humidity&nbsp;</dt>
              <dd className="inline tabular-nums">{Math.round(humidity)}%</dd>
            </div>
          )}
        </dl>
      </div>

      <div className="mt-4 pt-4 border-t border-slate-100 grid grid-cols-2 sm:grid-cols-3 gap-3 text-sm">
        <div>
          <p className="text-xs text-slate-400">Wind</p>
          <p className="tabular-nums text-slate-700">
            {wind !== null && wind !== undefined ? `${wind.toFixed(1)} mph` : '—'}
            {windDir ? ` ${windDir}` : ''}
          </p>
        </div>
        <div>
          <p className="text-xs text-slate-400">Pressure</p>
          <p className="tabular-nums text-slate-700">
            {pressure !== null && pressure !== undefined ? `${pressure.toFixed(2)} inHg` : '—'}
          </p>
        </div>
        <div>
          <p className="text-xs text-slate-400">Observed</p>
          <p className="text-slate-700">
            {obs.timestampLocal ?? formatUtcShort(obs.timestamp)}
          </p>
        </div>
      </div>

      <p className="mt-4 text-xs text-slate-400">
        at <span className="text-slate-500">{obs.stationId}</span> · {obs.stationName}
      </p>
    </article>
  );
}

function formatUtcShort(iso: string): string {
  try {
    return new Date(iso).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
  } catch {
    return iso;
  }
}