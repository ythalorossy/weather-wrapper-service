import { useQuery } from '@tanstack/react-query';
import { fetchHourlyWeather, HourlyWeatherResponse, WeatherError } from '../api/weather';

export const HOURLY_WEATHER_QUERY_KEY = 'hourly-weather';

export function useHourlyWeather(city: string | null) {
  return useQuery<HourlyWeatherResponse, WeatherError>({
    queryKey: [HOURLY_WEATHER_QUERY_KEY, city],
    queryFn: () => fetchHourlyWeather(city!),
    enabled: city !== null && city.trim().length > 0,
    // Hourly forecast changes less often than every minute — match backend TTL.
    staleTime: 30 * 60 * 1000,
  });
}