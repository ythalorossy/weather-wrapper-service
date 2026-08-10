import { useQuery } from '@tanstack/react-query';
import { fetchDiscussion, type DiscussionResponse } from '../api/weather';

/**
 * TanStack Query wrapper for the latest AFD for a city. staleTime matches the
 * server cache TTL (30 minutes) — the user shouldn't see the spinner for
 * revalidations within the cache window.
 */
export function useAfd(city: string | null) {
  return useQuery<DiscussionResponse>({
    queryKey: ['discussion', city],
    queryFn: () => fetchDiscussion(city!),
    enabled: city !== null,
    staleTime: 30 * 60 * 1000,
  });
}
