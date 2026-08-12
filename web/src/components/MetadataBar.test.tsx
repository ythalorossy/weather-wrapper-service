import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { MetadataBar } from './MetadataBar';
import type { LocationMetadataResponse } from '../api/weather';

const FIXTURE: LocationMetadataResponse = {
  city: 'Arlington, VA',
  resolvedLocation: { latitude: 38.8816, longitude: -77.0910, displayName: 'Arlington, VA' },
  office: {
    officeId: 'LWX',
    name: 'NWS Baltimore/Washington',
    radarStationId: 'KLWX',
    timezoneId: 'America/New_York',
    forecastOfficeUrl: 'https://www.weather.gov/lwx',
  },
};

const FIXTURE_WITH_SUN: LocationMetadataResponse = {
  ...FIXTURE,
  sun: {
    date: '2026-08-09',
    sunriseLocal: '06:42',
    sunsetLocal: '19:34',
    dayLengthSeconds: 48720,
  },
};

const FIXTURE_WITH_SHORT_DAY: LocationMetadataResponse = {
  ...FIXTURE,
  sun: {
    date: '2026-12-21',
    sunriseLocal: '07:30',
    sunsetLocal: '16:45',
    dayLengthSeconds: 33300,
  },
};

describe('MetadataBar', () => {
  it('renders the radar station id as a link to radar.weather.gov', () => {
    render(<MetadataBar data={FIXTURE} />);

    const radar = screen.getByRole('link', { name: /KLWX radar/ });
    expect(radar).toBeInTheDocument();
    expect(radar).toHaveAttribute('href', 'https://radar.weather.gov/?station=KLWX');
    expect(radar).toHaveAttribute('target', '_blank');
    expect(radar).toHaveAttribute('rel', expect.stringMatching(/noopener/));
  });

  it('keeps the office name as a separate link to the WFO page', () => {
    render(<MetadataBar data={FIXTURE} />);

    const office = screen.getByRole('link', { name: 'NWS Baltimore/Washington' });
    expect(office).toHaveAttribute('href', 'https://www.weather.gov/lwx');
  });

  it('renders sunrise and sunset times plus day length when sun is present', () => {
    render(<MetadataBar data={FIXTURE_WITH_SUN} />);

    expect(screen.getByText('06:42')).toBeInTheDocument();
    expect(screen.getByText('19:34')).toBeInTheDocument();
    expect(screen.getByText(/13h 32m of daylight/)).toBeInTheDocument();
  });

  it('renders a short winter day length as 9h 15m of daylight', () => {
    render(<MetadataBar data={FIXTURE_WITH_SHORT_DAY} />);

    expect(screen.getByText(/9h 15m of daylight/)).toBeInTheDocument();
  });
});