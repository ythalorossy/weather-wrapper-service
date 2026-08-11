import { useQuery } from '@tanstack/react-query';
import { AlertsResponse, fetchAlerts, WeatherError } from '../api/weather';

export function useAlerts(city: string | null) {
  return useQuery<AlertsResponse, WeatherError>({
    queryKey: ['alerts', city],
    queryFn: () => fetchAlerts(city!),
    enabled: city !== null && city.trim().length > 0,
    // Alerts change fast; backend cache is 5 minutes. Match it on the client
    // so TanStack Query doesn't refetch in the background more often than the
    // backend would actually return new data.
    staleTime: 5 * 60 * 1000,
  });
}