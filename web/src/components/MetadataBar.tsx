import type { LocationMetadataResponse } from '../api/weather';

interface Props {
  data: LocationMetadataResponse;
}

function formatDayLength(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return `${h}h ${m}m`;
}

export function MetadataBar({ data }: Props) {
  const { office, sun } = data;
  return (
    <div className="mt-2">
      <p className="text-xs text-slate-500">
        Forecast from{' '}
        <a
          className="underline hover:text-slate-700"
          href={office.forecastOfficeUrl}
          target="_blank"
          rel="noreferrer"
        >
          {office.name}
        </a>
        {' · '}
        <a
          className="underline hover:text-slate-700"
          href={`https://radar.weather.gov/?station=${office.radarStationId}`}
          target="_blank"
          rel="noopener noreferrer"
        >
          {office.radarStationId} radar
        </a>
        {' · '}
        {office.officeId}
      </p>
      {sun && (
        <p className="mt-2 text-sm text-slate-600">
          <span aria-hidden="true">☀</span>{' '}
          <time dateTime={`${sun.date}T${sun.sunriseLocal}`}>{sun.sunriseLocal}</time>
          <span className="text-slate-400" aria-hidden="true"> ↑ </span>
          <span aria-hidden="true">·</span>
          <span className="text-slate-400" aria-hidden="true"> ↓ </span>
          <time dateTime={`${sun.date}T${sun.sunsetLocal}`}>{sun.sunsetLocal}</time>
          <span className="text-slate-400"> · {formatDayLength(sun.dayLengthSeconds)} of daylight</span>
        </p>
      )}
    </div>
  );
}
