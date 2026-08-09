import type { WeatherAlert } from '../api/weather';

interface Props {
  alerts: WeatherAlert[];
  /** Currently-dismissed alert ids. Banner hides matching alerts. */
  dismissedIds: Set<string>;
  onDismiss: (id: string) => void;
}

const SEVERITY_STYLES: Record<WeatherAlert['severity'], string> = {
  Extreme: 'bg-red-100 border-red-400 text-red-900',
  Severe: 'bg-orange-100 border-orange-400 text-orange-900',
  Moderate: 'bg-yellow-50 border-yellow-400 text-yellow-900',
  Minor: 'bg-sky-50 border-sky-400 text-sky-900',
  Unknown: 'bg-slate-100 border-slate-400 text-slate-900',
};

/**
 * Top-of-page alert banner. Shows the single most severe active alert with a
 * dismiss button. The full list (if more than one) lives in AlertList.
 *
 * If all alerts are dismissed (or there are none), the banner is hidden.
 */
export function AlertsBanner({ alerts, dismissedIds, onDismiss }: Props) {
  const visible = alerts.filter((a) => !dismissedIds.has(a.id));
  if (visible.length === 0) return null;

  // Pick the most severe visible alert.
  const priority: Record<WeatherAlert['severity'], number> = {
    Extreme: 0, Severe: 1, Moderate: 2, Minor: 3, Unknown: 4,
  };
  const top = [...visible].sort(
    (a, b) => priority[a.severity] - priority[b.severity] ||
              (a.urgency === 'Immediate' ? -1 : 1) - (b.urgency === 'Immediate' ? -1 : 1),
  )[0];

  const extra = visible.length - 1;
  const styles = SEVERITY_STYLES[top.severity];

  return (
    <div
      role="alert"
      className={`rounded-lg border-l-4 ${styles} px-4 py-3 flex items-start gap-3`}
    >
      <div className="flex-1 min-w-0">
        <p className="font-semibold">{top.event}</p>
        <p className="text-sm mt-0.5 line-clamp-2">{top.headline}</p>
        {extra > 0 && (
          <p className="text-xs mt-1 opacity-80">
            +{extra} more active alert{extra > 1 ? 's' : ''}
          </p>
        )}
      </div>
      <button
        type="button"
        onClick={() => onDismiss(top.id)}
        className="shrink-0 text-xs px-2 py-1 rounded hover:bg-black/5 transition-colors"
        aria-label={`Dismiss ${top.event} alert`}
      >
        Dismiss
      </button>
    </div>
  );
}