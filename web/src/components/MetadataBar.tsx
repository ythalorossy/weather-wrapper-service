import type { LocationMetadataResponse } from '../api/weather';
import { SunTimesCard } from './SunTimesCard';

interface Props {
  data: LocationMetadataResponse;
}

/**
 * "Forecast from NWS Baltimore/Washington · KLWX radar · LWX" strip with an
 * optional sunrise/sunset line beneath. Renders nothing different when
 * `data.sun` is undefined; the strip just stays short.
 *
 * The radar station id links out to the new radar.weather.gov SPA,
 * pre-loaded with the station.
 */
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
      {sun && <SunTimesCard sun={sun} />}
    </div>
  );
}