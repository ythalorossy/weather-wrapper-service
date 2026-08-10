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
const VIEW_H = 240;
const PAD_X = 16;
const PAD_Y = 16;
const BOTTOM_MARGIN = 24;

export function HourlyChart({ data, rowRefs, hours = 48, sun }: Props) {
  const periods = data.forecast.periods.slice(0, hours);
  const [hover, setHover] = useState<number | null>(null);

  const { points, minT, maxT, gridLines } = useMemo(() => {
    if (periods.length === 0) {
      return { points: [] as Array<{ x: number; y: number; t: number }>, minT: 0, maxT: 1, gridLines: [] as Array<{ x: number; label: string }> };
    }
    // Smooth temps with 3-point moving average for a natural curve
    const rawTemps = periods.map((p) => p.temperature.value);
    const smoothTemps = rawTemps.map((t, i) => {
      if (i === 0) return (t + rawTemps[1]) / 2;
      if (i === rawTemps.length - 1) return (t + rawTemps[rawTemps.length - 2]) / 2;
      return (rawTemps[i - 1] + t + rawTemps[i + 1]) / 3;
    });
    const lo = Math.min(...smoothTemps);
    const hi = Math.max(...smoothTemps);
    const pad = Math.max(2, (hi - lo) * 0.1);
    const min = lo - pad;
    const max = hi + pad;
    const range = max - min || 1;
    const chartInnerH = VIEW_H - PAD_Y - BOTTOM_MARGIN - PAD_Y;
    const innerW = VIEW_W - 2 * PAD_X;
    const step = periods.length > 1 ? innerW / (periods.length - 1) : innerW;
    const ps = periods.map((p, i) => ({
      x: PAD_X + i * step,
      y: PAD_Y + chartInnerH - ((smoothTemps[i] - min) / range) * chartInnerH,
      t: p.temperature.value,
    }));

    // Grid lines at 2-hour intervals
    const hourStep = 2;
    const gl: Array<{ x: number; label: string }> = [];
    for (let h = 0; h <= 24; h += hourStep) {
      const idx = Math.round((h / 24) * (periods.length - 1));
      if (idx >= 0 && idx < periods.length) {
        const date = new Date(periods[idx].startTime);
        const label = date.toLocaleTimeString(undefined, { hour: 'numeric', hour12: true });
        gl.push({ x: PAD_X + idx * step, label });
      }
    }

    return { points: ps, minT: min, maxT: max, gridLines: gl };
  }, [periods]);

  if (periods.length === 0) return null;

  // Straight lines - data is pre-smoothed via moving average
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
  const chartBottom = VIEW_H - PAD_Y;

  return (
    <div className="rounded-2xl border border-slate-200 bg-white shadow-sm p-3">
      <svg
        viewBox={`0 0 ${VIEW_W} ${VIEW_H}`}
        preserveAspectRatio="none"
        role="img"
        aria-label={ariaLabel}
        className="w-full h-40"
      >
        {sun && periods.map((p, i) => renderBand(p, i, periods, sun))}
        {/* Sunrise/sunset vertical lines */}
        {sun && (() => {
          if (periods.length === 0) return null;
          const innerW = VIEW_W - 2 * PAD_X;
          const totalHours = periods.length;

          const sunriseMins = parseLocalTime(sun.sunriseLocal);
          const sunsetMins = parseLocalTime(sun.sunsetLocal);

          const sunriseX = PAD_X + (sunriseMins / 60 / totalHours) * innerW;
          const sunsetX = PAD_X + (sunsetMins / 60 / totalHours) * innerW;

          return (
            <>
              <line
                x1={sunriseX}
                y1={PAD_Y}
                x2={sunriseX}
                y2={chartBottom - BOTTOM_MARGIN}
                stroke="rgb(251 191 36)"
                strokeWidth={2}
                strokeDasharray="4 2"
                vectorEffect="non-scaling-stroke"
              />
              <text
                x={sunriseX + 4}
                y={PAD_Y + 14}
                fontSize={10}
                fill="rgb(217 119 6)"
                fontWeight="500"
              >
                ↑ sunrise
              </text>
              <line
                x1={sunsetX}
                y1={PAD_Y}
                x2={sunsetX}
                y2={chartBottom - BOTTOM_MARGIN}
                stroke="rgb(251 191 36)"
                strokeWidth={2}
                strokeDasharray="4 2"
                vectorEffect="non-scaling-stroke"
              />
              <text
                x={sunsetX + 4}
                y={PAD_Y + 14}
                fontSize={10}
                fill="rgb(217 119 6)"
                fontWeight="500"
              >
                ↓ sunset
              </text>
            </>
          );
        })()}
        {/* Vertical grid lines at 2-hour intervals */}
        {gridLines.map((gl) => (
          <line
            key={`grid-${gl.x}`}
            x1={gl.x}
            y1={PAD_Y}
            x2={gl.x}
            y2={chartBottom - BOTTOM_MARGIN}
            stroke="rgb(203 213 225)"
            strokeWidth={1}
            vectorEffect="non-scaling-stroke"
          />
        ))}
        <path
          d={path}
          fill="none"
          stroke="rgb(2 132 199)"
          strokeWidth={2.5}
          strokeLinecap="round"
          strokeLinejoin="round"
          vectorEffect="non-scaling-stroke"
        />
        {/* Hour labels */}
        {gridLines.map((gl) => (
          <text
            key={`label-${gl.x}`}
            x={gl.x}
            y={VIEW_H - 4}
            textAnchor="middle"
            fontSize={10}
            fill="rgb(100 116 139)"
          >
            {gl.label}
          </text>
        ))}
        {points.map((p, i) => (
          <g key={`hover-${i}`}>
            <rect
              data-testid="hour-target"
              x={p.x - (VIEW_W / periods.length) / 2}
              y={PAD_Y}
              width={VIEW_W / periods.length}
              height={chartBottom - BOTTOM_MARGIN - PAD_Y}
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
  return <rect key={`band-${i}`} x={x} y={PAD_Y} width={w} height={VIEW_H - PAD_Y - BOTTOM_MARGIN - PAD_Y} fill="rgb(224 242 254)" opacity={0.8} aria-hidden="true" />;
}

function parseLocalTime(time: string): number {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}
