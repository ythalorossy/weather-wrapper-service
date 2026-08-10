import type { JSX } from 'react';
import { useMemo, useState, type RefObject } from 'react';
import type { HourlyWeatherResponse, SunView } from '../api/weather';

interface Props {
  data: HourlyWeatherResponse;
  rowRefs: RefObject<(HTMLLIElement | null)[]>;
  hours?: number;
  sun?: SunView;
}

const VIEW_W = 800;
const VIEW_H = 200;
const PAD_X = 16;
const PAD_Y = 16;

export function HourlyChart({ data, rowRefs, hours = 48, sun }: Props) {
  const periods = data.forecast.periods.slice(0, hours);
  const [hover, setHover] = useState<number | null>(null);

  const { points, minT, maxT } = useMemo(() => {
    if (periods.length === 0) {
      return { points: [] as Array<{ x: number; y: number; t: number }>, minT: 0, maxT: 1 };
    }
    const temps = periods.map((p) => p.temperature.value);
    const lo = Math.min(...temps);
    const hi = Math.max(...temps);
    const pad = Math.max(2, (hi - lo) * 0.1);
    const min = lo - pad;
    const max = hi + pad;
    const range = max - min || 1;
    const innerW = VIEW_W - 2 * PAD_X;
    const innerH = VIEW_H - 2 * PAD_Y;
    const step = periods.length > 1 ? innerW / (periods.length - 1) : innerW;
    const ps = periods.map((p, i) => ({
      x: PAD_X + i * step,
      y: PAD_Y + innerH - ((p.temperature.value - min) / range) * innerH,
      t: p.temperature.value,
    }));
    return { points: ps, minT: min, maxT: max };
  }, [periods]);

  if (periods.length === 0) return null;

  const path = points
    .map((p, i) => (i === 0 ? `M ${p.x} ${p.y}` : `L ${p.x} ${p.y}`))
    .join(' ');

  function onClickHour(i: number) {
    const el = rowRefs.current?.[i];
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.classList.add('ring-2', 'ring-sky-300');
      window.setTimeout(() => el.classList.remove('ring-2', 'ring-sky-300'), 1500);
    }
  }

  const ariaLabel = `Hourly temperature from ${Math.round(minT)}°F to ${Math.round(maxT)}°F over ${periods.length} hours.`;

  return (
    <div className="rounded-2xl border border-slate-200 bg-white shadow-sm p-3">
      <svg
        viewBox={`0 0 ${VIEW_W} ${VIEW_H}`}
        preserveAspectRatio="none"
        role="img"
        aria-label={ariaLabel}
        className="w-full h-32"
      >
        {sun && periods.map((p, i) => renderBand(p, i, periods, sun))}
        <path
          d={path}
          fill="none"
          stroke="rgb(2 132 199)"
          strokeWidth={2}
          vectorEffect="non-scaling-stroke"
        />
        {points.map((p, i) => (
          <g key={`hover-${i}`}>
            <rect
              data-testid="hour-target"
              x={p.x - (VIEW_W / periods.length) / 2}
              y={PAD_Y}
              width={VIEW_W / periods.length}
              height={VIEW_H - 2 * PAD_Y}
              fill="transparent"
              role="button"
              aria-label={`Hour ${i}`}
              tabIndex={-1}
              onMouseEnter={() => setHover(i)}
              onMouseLeave={() => setHover(null)}
              onFocus={() => setHover(i)}
              onBlur={() => setHover(null)}
              onClick={() => onClickHour(i)}
            />
            {hover === i && (
              <g aria-hidden="true">
                <circle cx={p.x} cy={p.y} r={4} fill="rgb(2 132 199)" />
                <text
                  x={p.x}
                  y={PAD_Y - 4}
                  textAnchor="middle"
                  fontSize={12}
                  fill="rgb(15 23 42)"
                >
                  {new Date(periods[i].startTime).toLocaleTimeString(undefined, {
                    hour: 'numeric',
                  })}
                  {' · '}
                  {Math.round(p.t)}°F
                </text>
              </g>
            )}
          </g>
        ))}
      </svg>
    </div>
  );
}

function renderBand(
  p: { daytime: boolean; startTime: string },
  i: number,
  periods: Array<{ daytime: boolean; startTime: string }>,
  _sun: SunView,
): JSX.Element | null {
  if (!p.daytime) return null;
  let endI = i;
  while (endI + 1 < periods.length && periods[endI + 1].daytime) endI++;
  const innerW = VIEW_W - 2 * PAD_X;
  const step = periods.length > 1 ? innerW / (periods.length - 1) : innerW;
  const x = PAD_X + i * step - step / 2;
  const w = Math.max(step, (endI - i + 1) * step);
  return <rect key={`band-${i}`} x={x} y={PAD_Y} width={w} height={VIEW_H - 2 * PAD_Y} fill="rgb(224 242 254)" opacity={0.6} aria-hidden="true" />;
}
