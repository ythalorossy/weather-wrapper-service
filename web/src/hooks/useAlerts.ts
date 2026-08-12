import { AlertsResponse, fetchAlerts, WeatherError } from '../api/weather';
import { useCityQuery } from './useCityQuery';

export function useAlerts(city: string | null) {
  return useCityQuery<AlertsResponse, WeatherError>('alerts', city, {
    fetcher: fetchAlerts,
    staleMinutes: 5,
  });
}
