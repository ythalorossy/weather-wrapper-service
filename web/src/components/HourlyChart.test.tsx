import { render, screen } from '@testing-library/react';
import { createRef, type RefObject } from 'react';
import { describe, expect, it, vi } from 'vitest';
import type { HourlyWeatherResponse, HourlyForecastPeriod, SunView } from '../api/weather';
import { buildOption, HourlyChart, onClickHour } from './HourlyChart';

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
    expect(series.markArea.data).toHaveLength(2);
  });

  it('returns padded min/max bounds for the aria-label', () => {
    const { minT, maxT } = buildOption(periods);
    expect(minT).toBeLessThan(52);
    expect(maxT).toBeGreaterThan(68);
  });
});

describe('HourlyChart', () => {
  it('renders the chart wrapper with an aria-label', () => {
    const refs = createRef<(HTMLLIElement | null)[]>();
    render(<HourlyChart data={data} rowRefs={refs as RefObject<(HTMLLIElement | null)[]>} />);
    const wrapper = screen.getByTestId('hourly-chart');
    expect(wrapper).toBeInTheDocument();
    expect(wrapper).toHaveAttribute('role', 'img');
    expect(wrapper.getAttribute('aria-label')).toMatch(/Hourly temperature from .+°F to .+°F over 48 hours\./);
  });

  it('renders nothing when there are zero periods', () => {
    const refs = createRef<(HTMLLIElement | null)[]>();
    const empty: HourlyWeatherResponse = { ...data, forecast: { ...data.forecast, periods: [] } };
    const { container } = render(<HourlyChart data={empty} rowRefs={refs as RefObject<(HTMLLIElement | null)[]>} />);
    expect(container.firstChild).toBeNull();
  });

  it('does not crash when sun is undefined', () => {
    const refs = createRef<(HTMLLIElement | null)[]>();
    expect(() =>
      render(<HourlyChart data={data} rowRefs={refs as RefObject<(HTMLLIElement | null)[]>} />),
    ).not.toThrow();
  });

  // Fallback: SVG click dispatch is unreliable in jsdom (document.elementFromPoint unavailable).
  // Calling the exported onClickHour helper directly.
  it('clicking hour index 5 scrolls the matching list row into view', () => {
    const row5 = document.createElement('li');
    const scrollSpy = vi.fn();
    row5.scrollIntoView = scrollSpy;
    const refs = {
      current: Array.from({ length: 48 }, () => null) as (HTMLLIElement | null)[],
    } as RefObject<(HTMLLIElement | null)[]>;
    refs.current![5] = row5;

    render(<HourlyChart data={data} rowRefs={refs} />);
    onClickHour(refs, 5);

    expect(scrollSpy).toHaveBeenCalled();
  });
});
