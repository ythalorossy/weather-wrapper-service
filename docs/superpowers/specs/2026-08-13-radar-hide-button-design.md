# Radar Map — Hide Toggle

**Status:** design approved, ready for plan
**Date:** 2026-08-13
**Owner:** web
**Parent spec:** `docs/superpowers/specs/2026-08-12-radar-map-design.md`

## Goal

Today `RadarCard` shows the RainViewer iframe on first click and offers no
way to dismiss it again until the user navigates away. Add a way to hide
the radar map from inside the card, so the user can reclaim the vertical
space without losing their city selection.

## Non-goals

- Persisting the open/closed state across reloads (URL hash, `localStorage`).
  Each refresh returns to the hidden state — same lazy-load spirit as the
  original.
- A floating close-X overlay on the iframe. One control surface is enough.
- Touching the upstream M4 "KLWX radar" link in `MetadataBar.tsx`.
- Any backend / API change.

## Design

### Single-file edit: `web/src/components/RadarCard.tsx`

Replace the conditional render `{!loaded && <button>...</button>}` with
a single always-present toggle button whose label and `onClick` flip on
`loaded`.

```tsx
const [loaded, setLoaded] = useState(false);
// …
<button
  type="button"
  onClick={() => setLoaded(v => !v)}
  className="text-xs underline hover:text-slate-700"
>
  {loaded ? 'Hide radar map' : 'Show radar map'}
</button>
```

The fallback helper text ("Loads RainViewer (~1 MB) only when you open
it.") continues to render only when `loaded` is `false` — unchanged.

### Behavior

- Hidden (default) → click → iframe mounts, label becomes "Hide radar
  map". `loaded` flips to `true`.
- Shown → click → iframe unmounts, label becomes "Show radar map".
  `loaded` flips to `false`.

Clicking the toggle while loaded also releases the iframe DOM and stops
its network activity — matches the lazy-load intent of the original
design (don't pay the ~1 MB cost when the user isn't looking).

### Out-of-scope follow-ups, updated

The 2026-08-12 spec lists "Replace button-only state with a persistent
toggle (URL hash or `localStorage`)" as a future-work bullet. This spec
addresses the *visible* half of that bullet (in-app toggle). The
*persisted* half (survives reload) remains future work and is still
explicitly out of scope here.

## Error handling

- **City changes mid-session**: behavior unchanged from the 2026-08-12
  spec. `RadarCard` is unmounted and remounted on city change (via the
  `daily.data && (...)` gate in `App.tsx`); internal `loaded` state
  resets to `false`. Acceptable.

No new failure modes introduced.

## Tests

Extend `web/src/components/RadarCard.test.tsx` (no new files):

1. Existing tests stay. The `getByRole('button', { name: /show radar
   map/i })` queries still match both states because the regex is case
   insensitive and uses no anchors — verified by reading the existing
   test file.
2. **New** — clicking the button when loaded hides the iframe and reverts
   the button label to "Show radar map".
3. **New** — clicking the button twice is a no-op round-trip: button
   label and iframe presence return to the starting state.

No snapshot. No new fixtures.

## Documentation

- `docs/ui.md` — add one sentence noting the toggle behavior. If the
  file already lists `RadarCard` without mentioning hide, add the
  sentence; otherwise skip.
- `docs/tradeoffs.md` — drop the "Replace button-only state with a
  persistent toggle" line from future-work, or amend it to read
  "persist the toggle across reloads (URL hash or `localStorage`)" if
  the user-visible toggle is now considered done.
- `docs/superpowers/specs/2026-08-12-radar-map-design.md` — no edit
  needed; this spec is its child.

## Rollout

One PR, one file plus tests. Verify with:

```
cd web
npm run typecheck
npx vitest run src/components/RadarCard.test.tsx
npm test
npm run build
```

Manual smoke: load any city → "Show radar map" button present → click →
iframe loads → label becomes "Hide radar map" → click → iframe gone,
label reverts. Confirm the page layout reclaims the space (no leftover
gray box).

## Acceptance

- `npm test` green, including the two new cases.
- `npm run build` clean.
- Visual: toggling collapses the iframe and reclaims its vertical space.