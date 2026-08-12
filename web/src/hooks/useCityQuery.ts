import { useQuery, type UseQueryOptions } from '@tanstack/react-query';

export interface CityQueryOptions<TData, TError = Error> {
  fetcher: (city: string) => Promise<TData>;
  staleMinutes?: number;
  extra?: Omit<UseQueryOptions<TData, TError, TData, [string, string | null]>, 'queryKey' | 'queryFn' | 'enabled'>;
}

export function useCityQuery<TData, TError = Error>(
  key: string,
  city: string | null,
  options: CityQueryOptions<TData, TError>,
) {
  return useQuery<TData, TError, TData, [string, string | null]>({
    queryKey: [key, city],
    queryFn: () => options.fetcher(city!),
    enabled: city !== null && city.trim().length > 0,
    ...(options.staleMinutes !== undefined ? { staleTime: options.staleMinutes * 60 * 1000 } : {}),
    ...(options.extra ?? {}),
  });
}
