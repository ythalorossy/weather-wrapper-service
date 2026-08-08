import { useQuery } from '@tanstack/react-query';
import { fetchWeather, WeatherError, WeatherResponse } from '../api/weather';

const WEATHER_QUERY_KEY = 'weather' as const;

export function useWeatherQuery(city: string | null) {
  return useQuery<WeatherResponse, WeatherError>({
    queryKey: [WEATHER_QUERY_KEY, city],
    queryFn: () => fetchWeather(city!),
    enabled: city !== null && city.trim().length > 0,
  });
}