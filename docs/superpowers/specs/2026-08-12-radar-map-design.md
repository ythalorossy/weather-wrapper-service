# Radar Map Embed

**Status:** design approved, ready for plan
**Date:** 2026-08-12
**Owner:** web

## Goal

Add an in-app interactive weather radar preview to the dashboard. The M4
slice ("KLWX radar" link in `MetadataBar.tsx:30`) already opens the NWS
radar SPA in a new tab; this slice surfaces a radar *preview* inside the
app, without paying the page-weight cost on initial render.

User opens a city → sees forecast, conditions, alerts, **and** a small
radar card. The card starts as a button; the third-party iframe loads
only on click.

## Non-goals

- Closest-radar-station lookup (already covered by the WFO's
  `radarStationId`).
- Map controls, layers toggling, time scrubbing. RainViewer's defaults
  are sufficient.
- Replacing the M4 "KLWX radar" link. Both surfaces remain.
- Offline / fallback radar source. If RainViewer is unreachable, the
  iframe shows its own broken-state UI — no custom error UI.

## Design

### New component: `web/src/components/RadarCard.tsx`

```tsx
interface RadarCardProps {
  latitude: number;
  longitude: number;
  displayName?: string;
}

export function RadarCard({ latitude, longitude, displayName }: RadarCardProps) {
  const [loaded, setLoaded] = useState(false);
  const src =
    `https://www.rainviewer.com/map.html` +
    `?loc=${latitude},${longitude},8` +
    `&oFa=1&c=3&layer=radar&sm=1&sn=1`;

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4">
      <header className="flex items-baseline justify-between mb-2">
        <h3 className="text-sm uppercase tracking-wider text-slate-500">
          Radar{displayName ? ` — ${displayName}` : ''}
        </h3>
        {!loaded && (
          <button
            type="button"
            onClick={() => setLoaded(true)}
            className="text-xs underline hover:text-slate-700"
          >
            Show radar map
          </button>
        )}
      </header>
      {loaded ? (
        <div className="relative w-full h-80 overflow-hidden rounded">
          <iframe
            title={`Weather radar map${displayName ? ` for ${displayName}` : ''}`}
            src={src}
            width="100%"
            height="100%"
            style={{ border: 0 }}
            loading="lazy"
          />
        </div>
      ) : (
        <p className="text-xs text-slate-400">
          Loads RainViewer (~1&nbsp;MB) only when you open it.
        </p>
      )}
    </section>
  );
}
```

### Wiring

One line in `App.tsx`, between `CurrentConditionsCard` and `ForecastTabs`
(inside the `daily.data && (...)` block):

```tsx
{daily.data.resolvedLocation && (
  <RadarCard
    latitude={daily.data.resolvedLocation.latitude}
    longitude={daily.data.resolvedLocation.longitude}
    displayName={daily.data.resolvedLocation.displayName}
  />
)}
```

`resolvedLocation` is guaranteed on `WeatherResponse` (it carries the
geocoded coordinates for the resolved city), so the gate never trips in
practice. The check is a type-narrowing nicety, not a runtime guard.

### URL params — cleanup of the prior plan

The existing `docs/superpowers/plans/2026-08-11-radar-map.md` URL had
two bugs:

1. Duplicate `o` parameter: `o=3` and `o=1` both present (last one
   wins — ambiguous and a copy-paste smell).
2. Unknown param `k=1` with no documented meaning on RainViewer.

Cleaned URL (RainViewer defaults, no duplicates):

```
https://www.rainviewer.com/map.html?loc={lat},{lon},8&oFa=1&c=3&layer=radar&sm=1&sn=1
```

| Param  | Meaning |
|--------|---------|
| `loc`  | center (`lat,lon,zoom`) |
| `oFa`  | fade animation between radar frames |
| `c`    | color scheme (3 = universal blue) |
| `layer`| `radar` (vs satellite) |
| `sm`   | show map controls |
| `sn`   | show cities |

## Error handling

- **RainViewer unreachable**: iframe shows its own broken-state UI. No
  custom error UI in the card — adding one is a separate, larger piece
  (needs iframe `onload` + fallback render path).
- **No lat/lon**: impossible — `resolvedLocation` is non-null on the
  response type, and the JSX gate above short-circuits to nothing
  otherwise.
- **City changes mid-session**: button state stays `loaded`. Next mount
  (unmount + remount via the `daily.data && ...` guard when `city`
  changes and `daily.data` refetches) resets state. Acceptable: keeps
  the radar visible across re-queries of the same city; costs a wasted
  RainViewer load if the user switches cities, which they will do rarely.

## Tests

`web/src/components/RadarCard.test.tsx`:

1. Renders the "Show radar map" button and the helper text by default.
   Asserts the iframe is **not** in the document.
2. After clicking the button, the iframe mounts with `src` containing
   both `latitude` and `longitude` and the cleaned URL prefix
   (`https://www.rainviewer.com/map.html?loc=`).
3. `title` attribute includes `displayName` when provided; falls back
   to a generic title otherwise.

No snapshot. No new fixtures — `render(<RadarCard ... />)` directly.

## Documentation

- `docs/ui.md` — add `RadarCard` to the component map.
- `docs/milestones.md` — new row **M6 — Radar map embed** marked ✅.
- `docs/tradeoffs.md` — drop the "Embedded radar map preview" line from
  the future-work section (now done).
- `README.md` — no change.

## Rollout

One PR. Verify with:

```
npm run test web/src/components/RadarCard.test.tsx
npm run test
npm run build
```

Manual smoke: `docker compose up -d --build` → search "Arlington VA" →
see "Show radar map" button → click → RainViewer iframe loads centered
on Arlington → switch to "Honolulu, HI" → button resets, new center.

## Acceptance

- `npm run test` green.
- `npm run build` clean.
- Card appears for every resolved city, button defaults to closed.

## Out-of-scope follow-ups

- Replace button-only state with a persistent toggle (URL hash or
  `localStorage`) so the user's choice survives reloads.
- Custom error UI when RainViewer is unreachable.
- Layer / time controls beyond RainViewer's defaults.
- Replacing the M4 "KLWX radar" link with the embedded map (would lose
  the "open in new tab with full NWS controls" affordance).