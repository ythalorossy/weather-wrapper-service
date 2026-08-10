import { render } from '@testing-library/react';
import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import type { HourlyWeatherResponse, HourlyForecastPeriod, SunView } from '../api/weather';
import { buildOption, HourlyChart } from './HourlyChart';

const periods: HourlyForecastPeriod[] = Array.from({ length: 48 }, (_, i) => ({
  startTime: new Date(2026, 7, 9, i).toISOString(),
  temperature: { value: 60 + Math.round(Math.sin(i / 6) * 10), unit: 'F', formatted: `${60 + Math.round(Math.sin(i / 6) * 10)}°F` },
  windSpeed: '5 mph',
  windDirection: 'NW',
  shortForecast: 'Clear',
  daytime: i % 24 < 12,
}));

const data: HourlyWeatherResponse = {
  city: 'Arlington, VA',
  resolvedLocation: { latitude: 38.88, longitude: -77.09, displayName: 'Arlington, VA' },
  forecast: { generatedAt: '', source: '', periods },
};

const sun: SunView = {
  date: '2026-08-09',
  sunriseLocal: '06:30',
  sunsetLocal: '19:45',
  dayLengthSeconds: 48000,
};

describe('buildOption', () => {
  it('returns 48 data points in the series', () => {
    const { option } = buildOption(periods);
    const seriesData = (option.series as Array<{ data: number[] }>)[0].data;
    expect(seriesData).toHaveLength(48);
  });

  it('produces a smooth line series', () => {
    const { option } = buildOption(periods);
    const series = (option.series as Array<{ type: string; smooth: boolean }>)[0];
    expect(series.type).toBe('line');
    expect(series.smooth).toBe(true);
  });

  it('uses the sky-600 line color and width 2.5', () => {
    const { option } = buildOption(periods);
    const series = (option.series as Array<{ lineStyle: { color: string; width: number } }>)[0];
    expect(series.lineStyle.color).toBe('rgb(2, 132, 199)');
    expect(series.lineStyle.width).toBe(2.5);
  });

  it('omits markLine and markArea when sun is undefined', () => {
    const { option } = buildOption(periods);
    const series = (option.series as Array<{ markLine?: unknown; markArea?: unknown }>)[0];
    expect(series.markLine).toBeUndefined();
    expect(series.markArea).toBeUndefined();
  });

  it('adds two markLine entries (sunrise + sunset) when sun is provided', () => {
    const { option } = buildOption(periods, sun);
    const series = (option.series as Array<{ markLine: { data: unknown[] } }>)[0];
    expect(series.markLine.data).toHaveLength(2);
  });

  it('adds a markArea for each contiguous daytime run', () => {
    const { option } = buildOption(periods, sun);
    const series = (option.series as Array<{ markArea: { data: unknown[] } }>)[0];
    // The fixture has two daytime runs (indices 0–11 and 24–35).
    expect(series.markArea.data).toHaveLength(2);
  });

  it('returns padded min/max bounds for the aria-label', () => {
    const { minT, maxT } = buildOption(periods);
    // Smoothed temps span roughly [50, 70]; padding of ~2 on each side.
    expect(minT).toBeLessThan(52);
    expect(maxT).toBeGreaterThan(68);
  });
});

describe('HourlyChart (stub)', () => {
  it('renders nothing when there are zero periods', () => {
    const refs = createRef<(HTMLLIElement | null)[]>();
    const empty: HourlyWeatherResponse = { ...data, forecast: { ...data.forecast, periods: [] } };
    const { container } = render(<HourlyChart data={empty} rowRefs={refs as React.RefObject<(HTMLLIElement | null)[]>} />);
    expect(container.firstChild).toBeNull();
  });
});
