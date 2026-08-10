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
});