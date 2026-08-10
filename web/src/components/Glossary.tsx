import { useId, useState, type ReactNode } from 'react';
import glossary from '../lib/glossary.json';

interface GlossaryEntry {
  term: string;
  definition: string;
}

const GLOSSARY = glossary as Record<string, GlossaryEntry>;

interface Props {
  term: string;
  children?: ReactNode;
}

/**
 * Inline tooltip wrapper for curated glossary terms. Renders the children
 * inside a focusable button; hovering or focusing reveals a popover with
 * the definition. Unknown terms render as plain text (fail open — never
 * blocks the surrounding UI on a typo).
 */
export function Glossary({ term, children }: Props) {
  const entry = GLOSSARY[term.toLowerCase()];
  if (!entry) {
    return <>{children ?? term}</>;
  }

  const id = useId();
  const [open, setOpen] = useState(false);

  return (
    <span className="relative inline-block">
      <button
        type="button"
        aria-describedby={open ? id : undefined}
        onFocus={() => setOpen(true)}
        onBlur={() => setOpen(false)}
        onMouseEnter={() => setOpen(true)}
        onMouseLeave={() => setOpen(false)}
        className="border-b border-dotted border-slate-400 cursor-help focus:outline-none focus:bg-sky-50"
      >
        {children ?? term}
      </button>
      {open && (
        <span
          id={id}
          role="tooltip"
          className="absolute left-0 top-full z-10 mt-1 w-64 rounded-md border border-slate-200 bg-white px-3 py-2 text-xs shadow-lg"
        >
          <strong className="block mb-1">{entry.term}</strong>
          {entry.definition}
        </span>
      )}
    </span>
  );
}
