import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { RadarCard } from './RadarCard';

describe('RadarCard', () => {
  it('renders a Show radar map button by default and no iframe', () => {
    render(
      <RadarCard
        latitude={38.8816}
        longitude={-77.0910}
        displayName="Arlington, VA"
      />,
    );

    expect(screen.getByRole('button', { name: /show radar map/i })).toBeInTheDocument();
    expect(screen.queryByTitle(/weather radar map/i)).not.toBeInTheDocument();
    expect(screen.getByText(/Loads RainViewer/i)).toBeInTheDocument();
  });

  it('mounts the iframe with the resolved coordinates after clicking the button', () => {
    render(
      <RadarCard
        latitude={38.8816}
        longitude={-77.0910}
        displayName="Arlington, VA"
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: /show radar map/i }));

    const iframe = screen.getByTitle(/weather radar map/i) as HTMLIFrameElement;
    expect(iframe).toBeInTheDocument();
    expect(iframe.src).toContain('https://www.rainviewer.com/map.html?loc=38.8816,-77.091,8');
    expect(iframe.src).toContain('oFa=1');
    expect(iframe.src).toContain('c=3');
    expect(iframe.src).toContain('layer=radar');
    expect(iframe.src).toContain('sm=1');
    expect(iframe.src).toContain('sn=1');
  });

  it('uses a generic iframe title when displayName is not provided', () => {
    render(<RadarCard latitude={21.3069} longitude={-157.8583} />);

    fireEvent.click(screen.getByRole('button', { name: /show radar map/i }));

    const iframe = screen.getByTitle(/weather radar map/i) as HTMLIFrameElement;
    expect(iframe.title).toBe('Weather radar map');
    expect(iframe.src).toContain('21.3069,-157.8583');
  });
});
