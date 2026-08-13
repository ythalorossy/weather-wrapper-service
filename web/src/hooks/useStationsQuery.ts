import { useQuery, UseQueryResult } from '@tanstack/react-query';
import { fetchStations, fetchStationObservations, Station, StationObservation } from '../api/weather';

export function useStationsQuery(
  city: string,
): UseQueryResult<Station[], Error> {
  return useQuery<Station[], Error>({
    queryKey: ['stations', city],
    queryFn: () => fetchStations(city),
    enabled: !!city,
    staleTime: 30 * 60 * 1000,
  });
}

export function useStationObservationsQuery(
  stationId: string | null,
): UseQueryResult<StationObservation[], Error> {
  return useQuery<StationObservation[], Error>({
    queryKey: ['station-observations', stationId],
    queryFn: () => fetchStationObservations(stationId!),
    enabled: !!stationId,
    staleTime: 10 * 60 * 1000,
  });
}