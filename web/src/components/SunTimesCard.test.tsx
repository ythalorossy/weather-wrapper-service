import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { SunTimesCard } from './SunTimesCard';

describe('SunTimesCard', () => {
  it('renders sunrise and sunset times plus day length', () => {
    render(
      <SunTimesCard
        sun={{
          date: '2026-08-09',
          sunriseLocal: '06:42',
          sunsetLocal: '19:34',
          dayLengthSeconds: 48720, // 13h 32m
        }}
      />,
    );

    expect(screen.getByText('06:42')).toBeInTheDocument();
    expect(screen.getByText('19:34')).toBeInTheDocument();
    expect(screen.getByText(/13h 32m/)).toBeInTheDocument();
  });

  it('formats very short day lengths', () => {
    render(
      <SunTimesCard
        sun={{
          date: '2026-12-21',
          sunriseLocal: '07:30',
          sunsetLocal: '16:45',
          dayLengthSeconds: 33300, // 9h 15m
        }}
      />,
    );
    expect(screen.getByText(/9h 15m/)).toBeInTheDocument();
  });
});
