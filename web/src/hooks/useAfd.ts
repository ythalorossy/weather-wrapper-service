import { fetchDiscussion, type DiscussionResponse } from '../api/weather';
import { useCityQuery } from './useCityQuery';

export function useAfd(city: string | null) {
  return useCityQuery<DiscussionResponse>('discussion', city, {
    fetcher: fetchDiscussion,
    staleMinutes: 30,
  });
}
