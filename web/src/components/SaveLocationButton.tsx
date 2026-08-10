import { useSavedLocations } from '../hooks/useSavedLocations';

interface Props {
  city: string;
}

/**
 * Star toggle on the forecast header. Renders nothing when no city is set
 * (parent shouldn't render it before `daily.data` resolves anyway). Filled
 * when the city is in the saved set, outline when not.
 */
export function SaveLocationButton({ city }: Props) {
  const { saved, add, remove, isSaved } = useSavedLocations();
  if (!city) return null;

  const isCurrentlySaved = isSaved(city);
  const label = isCurrentlySaved ? `Remove ${city} from saved locations` : `Save ${city}`;

  function toggle() {
    if (isCurrentlySaved) {
      remove(city);
    } else {
      add(city);
    }
  }

  // Reference `saved` so devtools shows the live saved list (used by the pills).
  void saved;

  return (
    <button
      type="button"
      aria-label={label}
      aria-pressed={isCurrentlySaved}
      onClick={toggle}
      className="ml-2 text-2xl leading-none text-amber-400 hover:text-amber-500 focus:outline-none"
    >
      {isCurrentlySaved ? '★' : '☆'}
    </button>
  );
}
