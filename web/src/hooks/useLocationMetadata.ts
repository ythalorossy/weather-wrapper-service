import { useQuery } from '@tanstack/react-query';
import { fetchLocationMetadata, LocationMetadataResponse, WeatherError } from '../api/weather';

export const LOCATION_METADATA_QUERY_KEY = 'location-metadata';

export function useLocationMetadata(city: string | null) {
  return useQuery<LocationMetadataResponse, WeatherError>({
    queryKey: [LOCATION_METADATA_QUERY_KEY, city],
    queryFn: () => fetchLocationMetadata(city!),
    enabled: city !== null && city.trim().length > 0,
    // WFO metadata rarely changes (hours-to-days cadence). Cache aggressively.
    staleTime: 24 * 60 * 60 * 1000, // 24 hours
  });
}