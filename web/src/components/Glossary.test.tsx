import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Glossary } from './Glossary';

describe('Glossary', () => {
  it('renders the children and reveals the tooltip on hover', () => {
    render(<Glossary term="dBZ">dBZ</Glossary>);

    const button = screen.getByRole('button', { name: 'dBZ' });
    expect(button).toBeInTheDocument();

    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    fireEvent.mouseEnter(button);
    expect(screen.getByRole('tooltip')).toHaveTextContent(/radar reflectivity/);
    fireEvent.mouseLeave(button);
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  });

  it('renders the tooltip on focus', () => {
    render(<Glossary term="POP">POP</Glossary>);

    const button = screen.getByRole('button', { name: 'POP' });
    fireEvent.focus(button);
    expect(screen.getByRole('tooltip')).toHaveTextContent(/Probability of Precipitation/);
  });

  it('renders plain text when the term is unknown', () => {
    render(<Glossary term="quantum">{`quantum`}</Glossary>);

    // No button — falls through to plain text rendering.
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    expect(screen.getByText('quantum')).toBeInTheDocument();
  });
});
