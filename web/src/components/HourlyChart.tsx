import type { EChartsOption } from 'echarts';
import type { HourlyForecastPeriod, HourlyWeatherResponse, SunView } from '../api/weather';
import type { RefObject } from 'react';

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
    const sunriseMins = parseLocalTime(sun.sunriseLocal);
    const sunsetMins = parseLocalTime(sun.sunsetLocal);
    const sunriseIdx = clamp(Math.round((sunriseMins / 60 / 24) * (periods.length - 1)), 0, periods.length - 1);
    const sunsetIdx = clamp(Math.round((sunsetMins / 60 / 24) * (periods.length - 1)), 0, periods.length - 1);

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

function parseLocalTime(time: string): number {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}

function clamp(n: number, lo: number, hi: number): number {
  return Math.max(lo, Math.min(hi, n));
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
}

/**
 * Stub — Task 3 replaces this with the real ECharts component.
 */
export function HourlyChart({ data }: Props): null {
  if (data.forecast.periods.length === 0) return null;
  return null;
}
