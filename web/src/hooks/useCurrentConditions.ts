import { useQuery } from '@tanstack/react-query';
import { CurrentConditionsResponse, fetchCurrentConditions, WeatherError } from '../api/weather';

export function useCurrentConditions(city: string | null) {
  return useQuery<CurrentConditionsResponse, WeatherError>({
    queryKey: ['conditions', city],
    queryFn: () => fetchCurrentConditions(city!),
    enabled: city !== null && city.trim().length > 0,
    // Match backend's 10-minute cache TTL. Station data refreshes every 5-15
    // min; 10 min strikes a balance between freshness and request load.
    staleTime: 10 * 60 * 1000,
  });
}