import type { DiscussionResponse } from '../api/weather';

interface Props {
  data: DiscussionResponse | undefined;
}

/**
 * Renders the AFD body in a whitespace-pre-wrap block (NWS uses \n\n for
 * paragraph breaks). Shows "No discussion available" when the body is empty
 * or undefined — the typical case for non-US cities.
 */
export function DiscussionCard({ data }: Props) {
  if (!data || !data.body) {
    return (
      <p className="text-sm text-slate-500">No discussion available.</p>
    );
  }

  const issued = new Date(data.issuanceTime).toLocaleString();

  return (
    <article>
      <p className="text-xs text-slate-500 mb-2">
        Issued {issued} by {data.officeId}
      </p>
      <div className="whitespace-pre-wrap text-sm leading-relaxed text-slate-800 max-h-[60vh] overflow-y-auto rounded border border-slate-200 bg-slate-50 px-4 py-3">
        {data.body}
      </div>
    </article>
  );
}
