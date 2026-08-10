import { useSavedLocations } from '../hooks/useSavedLocations';

interface Props {
  active: string | null;
  onSelect: (city: string) => void;
}

/**
 * Horizontal pill row above the search form showing all saved cities.
 * Renders nothing when the saved set is empty. The active city (current
 * `city` state) is highlighted; clicking a pill calls `onSelect(city)`.
 *
 * Reads the saved set from `useSavedLocations` so it stays in sync with
 * `SaveLocationButton` toggles elsewhere in the tree.
 */
export function SavedLocationsPills({ active, onSelect }: Props) {
  const { saved, remove } = useSavedLocations();

  if (saved.length === 0) return null;

  return (
    <div className="mb-3 flex flex-wrap gap-2 justify-center" role="navigation" aria-label="Saved locations">
      {saved.map((city) => {
        const isActive = active !== null && active.trim().toLowerCase() === city.trim().toLowerCase();
        return (
          <div
            key={city}
            className={`inline-flex items-center gap-1 rounded-full border px-3 py-1 text-sm transition-colors ${
              isActive
                ? 'border-sky-600 text-sky-700 font-semibold'
                : 'border-slate-300 text-slate-700 hover:border-slate-400'
            }`}
          >
            <button
              type="button"
              onClick={() => onSelect(city)}
              className="focus:outline-none focus:underline"
            >
              {city}
            </button>
            <button
              type="button"
              aria-label={`Remove ${city}`}
              onClick={() => remove(city)}
              className="ml-1 text-slate-400 hover:text-slate-600 focus:outline-none"
            >
              ×
            </button>
          </div>
        );
      })}
    </div>
  );
}