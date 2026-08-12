import { CurrentConditionsResponse, fetchCurrentConditions, WeatherError } from '../api/weather';
import { useCityQuery } from './useCityQuery';

export function useCurrentConditions(city: string | null) {
  return useCityQuery<CurrentConditionsResponse, WeatherError>('conditions', city, {
    fetcher: fetchCurrentConditions,
    staleMinutes: 10,
  });
}
