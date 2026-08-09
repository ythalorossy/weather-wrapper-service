import { useState } from 'react';
import type { WeatherAlert } from '../api/weather';

interface Props {
  alerts: WeatherAlert[];
}

const SEVERITY_STYLES: Record<WeatherAlert['severity'], string> = {
  Extreme: 'bg-red-100 text-red-900',
  Severe: 'bg-orange-100 text-orange-900',
  Moderate: 'bg-yellow-50 text-yellow-900',
  Minor: 'bg-sky-50 text-sky-900',
  Unknown: 'bg-slate-100 text-slate-700',
};

/**
 * Full list of active alerts. Each alert is collapsible: clicking the header
 * toggles between summary and the long description / instruction.
 */
export function AlertList({ alerts }: Props) {
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  if (alerts.length === 0) {
    return (
      <p className="text-sm text-slate-400 py-4">No active alerts.</p>
    );
  }

  function toggle(id: string) {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  return (
    <ul className="divide-y divide-slate-100 rounded-2xl bg-white shadow-sm border border-slate-200 overflow-hidden">
      {alerts.map((a) => {
        const isOpen = expanded.has(a.id);
        return (
          <li key={a.id} className="px-4 py-3">
            <button
              type="button"
              onClick={() => toggle(a.id)}
              className="w-full text-left flex items-start gap-3"
              aria-expanded={isOpen}
            >
              <span
                className={`shrink-0 text-xs px-2 py-0.5 rounded-full font-medium ${SEVERITY_STYLES[a.severity]}`}
              >
                {a.severity}
              </span>
              <span className="flex-1 min-w-0">
                <span className="block font-medium">{a.event}</span>
                <span className="block text-sm text-slate-600 truncate">{a.headline}</span>
                <span className="block text-xs text-slate-400 mt-0.5">{a.areaDesc}</span>
              </span>
              <span className="shrink-0 text-slate-400 text-xs">
                {isOpen ? '▾' : '▸'}
              </span>
            </button>
            {isOpen && (
              <div className="mt-3 pl-1 text-sm text-slate-700 space-y-2">
                <p className="whitespace-pre-wrap">{a.description}</p>
                {a.instruction && (
                  <p className="rounded bg-slate-50 px-3 py-2 text-slate-800">
                    <strong>Action: </strong>
                    {a.instruction}
                  </p>
                )}
                <div className="text-xs text-slate-500 space-y-0.5 pt-2 border-t border-slate-100">
                  <p>Certainty: {a.certainty} · Urgency: {a.urgency}</p>
                  <p>
                    Effective: {new Date(a.effective).toLocaleString()} —
                    Expires: {new Date(a.expires).toLocaleString()}
                  </p>
                  {a.webUrl && (
                    <p>
                      <a
                        href={a.webUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-sky-700 hover:text-sky-900 underline-offset-2 hover:underline"
                      >
                        View full alert on weather.gov ↗
                      </a>
                    </p>
                  )}
                </div>
              </div>
            )}
          </li>
        );
      })}
    </ul>
  );
}