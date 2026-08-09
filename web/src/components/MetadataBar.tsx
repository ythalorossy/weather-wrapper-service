import type { LocationMetadataResponse } from '../api/weather';

interface Props {
  data: LocationMetadataResponse;
}

/**
 * Small attribution strip beneath the city header:
 *   "Forecast from NWS Baltimore/Washington · KLWX radar"
 *
 * Links out to the NWS forecast office page. Pure presentational.
 */
export function MetadataBar({ data }: Props) {
  const { office } = data;

  return (
    <p className="text-xs text-slate-500 mt-2">
      Forecast from{' '}
      <a
        href={office.forecastOfficeUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="text-sky-700 hover:text-sky-900 underline-offset-2 hover:underline"
      >
        NWS {office.name}
      </a>
      {' · '}
      <span className="text-slate-400">{office.radarStationId} radar</span>
      {' · '}
      <span className="text-slate-400">{office.officeId}</span>
    </p>
  );
}