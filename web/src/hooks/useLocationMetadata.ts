import { fetchLocationMetadata, LocationMetadataResponse, WeatherError } from '../api/weather';
import { useCityQuery } from './useCityQuery';

export function useLocationMetadata(city: string | null) {
  return useCityQuery<LocationMetadataResponse, WeatherError>('location-metadata', city, {
    fetcher: fetchLocationMetadata,
    staleMinutes: 60 * 24,
  });
}
