import type { SunView } from '../api/weather';

interface Props {
  sun: SunView;
}

/**
 * Compact "☀ 6:42 ↑ · 19:34 ↓ · 13h 52m" strip. Renders nothing when `sun`
 * is undefined (parent controls the conditional).
 */
export function SunTimesCard({ sun }: Props) {
  const day = formatDayLength(sun.dayLengthSeconds);
  return (
    <p className="mt-2 text-sm text-slate-600">
      <span aria-hidden="true">☀</span>{' '}
      <time dateTime={`${sun.date}T${sun.sunriseLocal}`}>{sun.sunriseLocal}</time>
      <span className="text-slate-400" aria-hidden="true"> ↑ </span>
      <span aria-hidden="true">·</span>
      <span className="text-slate-400" aria-hidden="true"> ↓ </span>
      <time dateTime={`${sun.date}T${sun.sunsetLocal}`}>{sun.sunsetLocal}</time>
      <span className="text-slate-400"> · {day} of daylight</span>
    </p>
  );
}

function formatDayLength(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return `${h}h ${m}m`;
}
