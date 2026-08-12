import ReactECharts from 'echarts-for-react';
import type { EChartsOption } from 'echarts';
import type { HourlyForecastPeriod, HourlyWeatherResponse, SunView } from '../api/weather';
import { useMemo, type JSX, type RefObject } from 'react';

export interface BuildOptionResult {
  option: EChartsOption;
  minT: number;
  maxT: number;
}

/**
 * Pure builder for the ECharts option object used by `HourlyChart`.
 * Exported so tests can assert the produced option directly.
 */
export function buildOption(periods: HourlyForecastPeriod[], sun?: SunView): BuildOptionResult {
  const rawTemps = periods.map((p) => p.temperature.value);
  const smoothTemps = rawTemps.map((t, i) => {
    if (i === 0) return (t + rawTemps[1]) / 2;
    if (i === rawTemps.length - 1) return (t + rawTemps[rawTemps.length - 2]) / 2;
    return (rawTemps[i - 1] + t + rawTemps[i + 1]) / 3;
  });

  const lo = Math.min(...smoothTemps);
  const hi = Math.max(...smoothTemps);
  const pad = Math.max(2, (hi - lo) * 0.1);
  const minT = lo - pad;
  const maxT = hi + pad;

  const xLabels = periods.map((p) =>
    new Date(p.startTime).toLocaleTimeString(undefined, { hour: 'numeric', hour12: true }),
  );

  const series: Record<string, unknown> = {
    type: 'line',
    smooth: true,
    data: smoothTemps,
    lineStyle: { color: 'rgb(2, 132, 199)', width: 2.5 },
  };

  if (sun) {
    const sunriseDate = parseLocalDateTime(sun.date, sun.sunriseLocal);
    const sunsetDate = parseLocalDateTime(sun.date, sun.sunsetLocal);
    const sunriseIdx = findNearestPeriodIndex(periods, sunriseDate);
    const sunsetIdx = findNearestPeriodIndex(periods, sunsetDate);

    series.markLine = {
      symbol: 'none',
      data: [
        { xAxis: sunriseIdx, label: { formatter: '↑ sunrise', color: 'rgb(217, 119, 6)' }, lineStyle: { type: 'dashed', color: 'rgb(251, 191, 36)' } },
        { xAxis: sunsetIdx, label: { formatter: '↓ sunset', color: 'rgb(217, 119, 6)' }, lineStyle: { type: 'dashed', color: 'rgb(251, 191, 36)' } },
      ],
    };

    const runs = contiguousDaytimeRuns(periods);
    series.markArea = {
      itemStyle: { color: 'rgb(224, 242, 254)', opacity: 0.8 },
      data: runs.map(([start, end]) => [
        { xAxis: start, itemStyle: { color: 'rgb(224, 242, 254)', opacity: 0.8 } },
        { xAxis: end },
      ]),
    };
  }

  const option: EChartsOption = {
    animation: false,
    grid: { left: 16, right: 16, top: 16, bottom: 24, containLabel: true },
    xAxis: {
      type: 'category',
      data: xLabels,
      boundaryGap: false,
      axisLabel: {
        interval: (idx: number) => idx % 2 === 0,
        fontSize: 10,
        color: 'rgb(100, 116, 139)',
      },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      min: minT,
      max: maxT,
      axisLabel: { fontSize: 10, color: 'rgb(100, 116, 139)', formatter: (v: number) => `${Math.round(v)}°` },
      splitLine: { lineStyle: { color: 'rgb(226, 232, 240)' } },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const p = (params as { axisValue: string; data: number }[])[0];
        return `${p.axisValue} · ${Math.round(p.data)}°F`;
      },
    },
    series: [series],
  };

  return { option, minT, maxT };
}

function parseLocalDateTime(dateStr: string, timeStr: string): Date {
  return new Date(`${dateStr}T${timeStr}:00`);
}

function toLocalDateStr(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function findNearestPeriodIndex(periods: HourlyForecastPeriod[], target: Date): number {
  let nearest = 0;
  let minDiff = Infinity;
  periods.forEach((p, i) => {
    const diff = Math.abs(new Date(p.startTime).getTime() - target.getTime());
    if (diff < minDiff) {
      minDiff = diff;
      nearest = i;
    }
  });
  return nearest;
}

function contiguousDaytimeRuns(periods: HourlyForecastPeriod[]): Array<[number, number]> {
  const runs: Array<[number, number]> = [];
  let start: number | null = null;
  periods.forEach((p, i) => {
    if (p.daytime && start === null) start = i;
    if ((!p.daytime || i === periods.length - 1) && start !== null) {
      runs.push([start, p.daytime ? i : i - 1]);
      start = null;
    }
  });
  return runs;
}

export interface Props {
  data: HourlyWeatherResponse;
  rowRefs: RefObject<(HTMLLIElement | null)[]>;
  hours?: number;
  sun?: SunView;
  today?: boolean;
}

/**
 * Hourly temperature chart.
 *
 * Renders a 48-hour temperature line with sunrise/sunset markers and
 * daytime band shading when `sun` is provided. Hover reveals a tooltip
 * (time + temp); click on any hour scrolls the matching list row into
 * view via `rowRefs`.
 */
export function HourlyChart({ data, rowRefs, hours = 48, sun, today }: Props): JSX.Element | null {
  const rawPeriods = data.forecast.periods;
  const periods = today && sun
    ? rawPeriods.filter((p) => toLocalDateStr(new Date(p.startTime)) === sun.date).slice(0, 24)
    : rawPeriods.slice(0, hours);

  const { option, minT, maxT } = useMemo(
    () => buildOption(periods, sun),
    [periods, sun],
  );

  if (periods.length === 0) return null;

  const ariaLabel = `Hourly temperature from ${Math.round(minT)}°F to ${Math.round(maxT)}°F over ${periods.length} hours.`;

  return (
    <div
      data-testid="hourly-chart"
      role="img"
      aria-label={ariaLabel}
      className="rounded-2xl border border-slate-200 bg-white shadow-sm p-3 w-full h-40"
    >
      <ReactECharts
        option={option}
        onEvents={{ click: (p: { dataIndex: number }) => onClickHour(rowRefs, p.dataIndex) }}
        opts={{ renderer: 'svg' }}
        style={{ height: '100%', width: '100%' }}
        lazyUpdate
      />
    </div>
  );
}

export function onClickHour(rowRefs: RefObject<(HTMLLIElement | null)[]>, i: number): void {
  const el = rowRefs.current?.[i];
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    el.classList.add('ring-2', 'ring-sky-300');
    window.setTimeout(() => el.classList.remove('ring-2', 'ring-sky-300'), 1500);
  }
}
