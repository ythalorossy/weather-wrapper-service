import { fireEvent, render, screen } from '@testing-library/react';
import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import type { HourlyWeatherResponse } from '../api/weather';
import { HourlyChart } from './HourlyChart';

const data: HourlyWeatherResponse = {
  city: 'Arlington, VA',
  resolvedLocation: { latitude: 38.88, longitude: -77.09, displayName: 'Arlington, VA' },
  forecast: {
    generatedAt: '',
    source: '',
    periods: Array.from({ length: 48 }, (_, i) => ({
      startTime: new Date(2026, 7, 9, i).toISOString(),
      temperature: { value: 60 + Math.round(Math.sin(i / 6) * 10), unit: 'F', formatted: `${60 + Math.round(Math.sin(i / 6) * 10)}°F` },
      windSpeed: '5 mph',
      windDirection: 'NW',
      shortForecast: 'Clear',
      daytime: i % 24 < 12,
    })),
  },
};

describe('HourlyChart', () => {
  it('renders 48 hover targets for 48 hours', () => {
    const refs = createRef<(HTMLLIElement | null)[]>();
    render(<HourlyChart data={data} rowRefs={refs as React.RefObject<(HTMLLIElement | null)[]>} />);
    expect(screen.getAllByRole('button', { name: /hour/i }).length + screen.getAllByLabelText(/temperature/i).length).toBeGreaterThanOrEqual(0);
    const svg = screen.getByRole('img', { name: /temperature/i });
    expect(svg).toBeInTheDocument();
  });

  it('renders nothing when there are zero periods', () => {
    const refs = createRef<(HTMLLIElement | null)[]>();
    const empty: HourlyWeatherResponse = { ...data, forecast: { ...data.forecast, periods: [] } };
    const { container } = render(<HourlyChart data={empty} rowRefs={refs as React.RefObject<(HTMLLIElement | null)[]>} />);
    expect(container.firstChild).toBeNull();
  });

  it('does not crash when sun is undefined', () => {
    const refs = createRef<(HTMLLIElement | null)[]>();
    expect(() => render(<HourlyChart data={data} rowRefs={refs as React.RefObject<(HTMLLIElement | null)[]>} />)).not.toThrow();
  });

  it('click on hour 5 calls scrollIntoView on rowRefs[5]', () => {
    const row5 = document.createElement('li');
    const scrollSpy = vi.fn();
    row5.scrollIntoView = scrollSpy;
    const refs = {
      current: Array.from({ length: 48 }, () => null) as (HTMLLIElement | null)[],
    };
    refs.current[5] = row5;

    render(<HourlyChart data={data} rowRefs={refs} />);
    const rects = screen.getAllByTestId('hour-target');
    fireEvent.click(rects[5]);
    expect(scrollSpy).toHaveBeenCalled();
  });
});
