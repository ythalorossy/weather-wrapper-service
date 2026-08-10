import { useAfd } from '../hooks/useAfd';
import { DiscussionCard } from './DiscussionCard';

interface Props {
  city: string;
}

/**
 * Owns the useAfd query and delegates rendering to DiscussionCard.
 * Hidden while the query is pending — DiscussionCard shows a fallback
 * for both error and empty cases.
 */
export function DiscussionTab({ city }: Props) {
  const query = useAfd(city);
  if (query.isPending) {
    return <p className="text-sm text-slate-500">Loading discussion…</p>;
  }
  return <DiscussionCard data={query.data} />;
}
