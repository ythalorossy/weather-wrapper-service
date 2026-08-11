import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { DiscussionCard } from './DiscussionTab';

describe('DiscussionCard', () => {
  it('renders the body in a pre-wrap block with an issued header', () => {
    render(
      <DiscussionCard
        data={{
          officeId: 'LWX',
          issuanceTime: '2026-08-10T14:35:00Z',
          body: 'KLWX AFD\n\n.SHORT TERM...\n\nDry weather through Tuesday.',
        }}
      />,
    );

    expect(screen.getByText(/Dry weather through Tuesday\./)).toBeInTheDocument();
    expect(screen.getByText(/Issued .* by LWX/)).toBeInTheDocument();
  });

  it('renders a fallback message when data is undefined', () => {
    render(<DiscussionCard data={undefined} />);
    expect(screen.getByText(/No discussion available/)).toBeInTheDocument();
  });

  it('renders a fallback message when body is empty', () => {
    render(
      <DiscussionCard
        data={{ officeId: 'LWX', issuanceTime: '2026-08-10T14:35:00Z', body: '' }}
      />,
    );
    expect(screen.getByText(/No discussion available/)).toBeInTheDocument();
  });
});
