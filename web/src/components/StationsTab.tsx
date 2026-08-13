import { useState } from 'react';
import { useStationsQuery, useStationObservationsQuery } from '../hooks/useStationsQuery';

type Props = {
  city: string;
};

export function StationsTab({ city }: Props) {
  const stationsQuery = useStationsQuery(city);
  const [selectedStationId, setSelectedStationId] = useState<string | null>(null);
  const observationsQuery = useStationObservationsQuery(selectedStationId);

  if (stationsQuery.isLoading) {
    return <div className="p-4 text-gray-500">Loading stations…</div>;
  }

  if (stationsQuery.isError) {
    return (
      <div className="p-4 text-red-600">
        Failed to load stations: {stationsQuery.error?.message ?? 'Unknown error'}
      </div>
    );
  }

  const stations = stationsQuery.data ?? [];

  return (
    <div className="p-4 space-y-4">
      <h2 className="text-lg font-semibold">Nearby Observation Stations</h2>

      {stations.length === 0 ? (
        <p className="text-gray-500">No stations found for this location.</p>
      ) : (
        <ul className="divide-y divide-gray-200">
          {stations.map((s) => (
            <li
              key={s.stationId}
              className={`p-3 cursor-pointer hover:bg-gray-50 ${
                selectedStationId === s.stationId ? 'bg-blue-50' : ''
              }`}
              onClick={() => setSelectedStationId(s.stationId)}
            >
              <div className="font-medium">{s.name}</div>
              <div className="text-sm text-gray-500">
                {s.stationId} — {s.latitude.toFixed(2)}, {s.longitude.toFixed(2)}
              </div>
            </li>
          ))}
        </ul>
      )}

      {selectedStationId && (
        <div className="mt-6 border-t pt-4">
          <h3 className="text-md font-semibold mb-2">
            Recent Observations — {selectedStationId}
          </h3>
          {observationsQuery.isLoading && <p>Loading observations…</p>}
          {observationsQuery.isError && (
            <p className="text-red-600">Failed to load observations.</p>
          )}
          {observationsQuery.data && (
            <ul className="space-y-3">
              {observationsQuery.data.map((obs, idx) => (
                <li key={idx} className="bg-gray-50 p-3 rounded">
                  <div className="text-sm text-gray-600">{obs.timestamp}</div>
                  <div className="font-medium">
                    {obs.temperature.formatted} — Wind {obs.windSpeed} {obs.windDirection}
                  </div>
                  {obs.humidity !== null && (
                    <div className="text-sm">Humidity: {obs.humidity}%</div>
                  )}
                  {obs.barometricPressure !== null && (
                    <div className="text-sm">Pressure: {obs.barometricPressure} inHg</div>
                  )}
                  <details className="mt-2">
                    <summary className="cursor-pointer text-sm text-blue-600">
                      Raw METAR
                    </summary>
                    <pre className="mt-1 text-xs bg-white p-2 rounded overflow-x-auto">
                      {obs.rawMessage}
                    </pre>
                  </details>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}