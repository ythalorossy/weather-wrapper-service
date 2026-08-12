import { fetchWeather, WeatherError, WeatherResponse } from '../api/weather';
import { useCityQuery } from './useCityQuery';

export function useWeatherQuery(city: string | null) {
  return useCityQuery<WeatherResponse, WeatherError>('weather', city, {
    fetcher: fetchWeather,
  });
}
