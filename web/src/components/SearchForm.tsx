import { FormEvent, useState, useEffect } from 'react';

interface Props {
  onSubmit: (city: string) => void;
  isFetching: boolean;
  initialValue: string;
}

export function SearchForm({ onSubmit, isFetching, initialValue }: Props) {
  const [value, setValue] = useState(initialValue);

  useEffect(() => {
    setValue(initialValue);
  }, [initialValue]);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmed = value.trim();
    if (trimmed.length === 0) return;
    onSubmit(trimmed);
  }

  return (
    <form onSubmit={handleSubmit} className="w-full max-w-2xl flex gap-2">
      <input
        type="text"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="Enter a city, e.g. Arlington, VA"
        aria-label="City"
        className="flex-1 rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-base shadow-sm placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-sky-500"
      />
      <button
        type="submit"
        disabled={isFetching || value.trim().length === 0}
        className="rounded-lg bg-sky-600 px-5 py-2.5 font-medium text-white shadow-sm hover:bg-sky-700 focus:outline-none focus:ring-2 focus:ring-sky-500 disabled:bg-slate-300 disabled:cursor-not-allowed transition-colors"
      >
        {isFetching ? 'Searching…' : 'Search'}
      </button>
    </form>
  );
}