import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { WeatherAlert } from '../api/weather';
import { AlertsBanner } from './AlertsBanner';

const ALERT: WeatherAlert = {
  id: 'alert-1',
  event: 'Severe Thunderstorm Warning',
  severity: 'Severe',
  certainty: 'Likely',
  urgency: 'Immediate',
  category: 'Met',
  headline: 'Severe Thunderstorm Warning issued August 10',
  description: 'Severe weather is expected.',
  instruction: null,
  areaDesc: 'Arlington, VA',
  sent: '2026-08-10T12:00:00Z',
  effective: '2026-08-10T12:00:00Z',
  expires: '2026-08-10T13:00:00Z',
  webUrl: null,
};

describe('AlertsBanner', () => {
  it('preserves original case when wrapping warning in the alert title', () => {
    render(
      <AlertsBanner alerts={[ALERT]} dismissedIds={new Set()} onDismiss={vi.fn()} />,
    );

    const alertTitle = screen.getByRole('alert').querySelector('p.font-semibold')!;
    expect(alertTitle).toHaveTextContent('Severe Thunderstorm Warning');
    expect(alertTitle.querySelector('button')).toHaveTextContent('Warning');
  });

  it('preserves original case when wrapping watch in the alert title', () => {
    render(
      <AlertsBanner
        alerts={[{ ...ALERT, event: 'Severe Thunderstorm Watch' }]}
        dismissedIds={new Set()}
        onDismiss={vi.fn()}
      />,
    );

    const alertTitle = screen.getByRole('alert').querySelector('p.font-semibold')!;
    expect(alertTitle).toHaveTextContent('Severe Thunderstorm Watch');
    expect(alertTitle.querySelector('button')).toHaveTextContent('Watch');
  });
});
