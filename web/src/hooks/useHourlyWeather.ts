import { fetchHourlyWeather, HourlyWeatherResponse, WeatherError } from '../api/weather';
import { useCityQuery } from './useCityQuery';

export function useHourlyWeather(city: string | null) {
  return useCityQuery<HourlyWeatherResponse, WeatherError>('hourly-weather', city, {
    fetcher: fetchHourlyWeather,
    staleMinutes: 30,
  });
}
