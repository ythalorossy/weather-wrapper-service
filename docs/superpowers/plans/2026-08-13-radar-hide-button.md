# Radar Map — Hide Toggle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give `RadarCard` a way to dismiss the loaded RainViewer iframe (current behavior: click "Show radar map" → iframe loads → no way to hide it without leaving the page).

**Architecture:** Replace the conditional `{!loaded && <button>Show radar map</button>}` in `RadarCard.tsx` with an always-present toggle button whose label and `onClick` flip on `loaded`. Same `useState<boolean>` — no new state, no props, no new files.

**Tech Stack:** React 19, TypeScript, Vitest, Testing Library (fireEvent).

**Spec:** `docs/superpowers/specs/2026-08-13-radar-hide-button-design.md`.

## Global Constraints

- One source file modified: `web/src/components/RadarCard.tsx`. No new components, no new files.
- One test file extended: `web/src/components/RadarCard.test.tsx`. No new test files.
- All three existing tests in `RadarCard.test.tsx` must remain green without modification (they all assert before any click — verified against the regex `/show radar map/i`).
- No new npm dependencies. No backend / API changes.
- Button keeps the same classes (`text-xs underline hover:text-slate-700`) and `type="button"`.
- Verify with: `cd web && npm run typecheck && npx vitest run src/components/RadarCard.test.tsx && npm run build`.

---

### Task 1: Add hide toggle to `RadarCard` (TDD)

**Files:**
- Modify: `web/src/components/RadarCard.tsx:23-31` (replace the conditional button with a toggle)
- Modify: `web/src/components/RadarCard.test.tsx` (append two new test cases)

**Interfaces:**
- Consumes: existing `RadarCardProps` (`latitude`, `longitude`, `displayName?`); existing `useState<boolean>` for `loaded`.
- Produces: `<RadarCard … />` whose header always contains a single button labelled `Show radar map` (default) or `Hide radar map` (after click), and whose body renders the iframe only when `loaded` is true.

- [ ] **Step 1: Append the two new failing tests**

Append the following two `it(...)` blocks inside the existing `describe('RadarCard', …)` in `web/src/components/RadarCard.test.tsx` (after the third existing test). Do not touch the three existing tests.

```tsx
  it('hides the iframe and reverts the button label when clicked while loaded', () => {
    render(
      <RadarCard
        latitude={38.8816}
        longitude={-77.0910}
        displayName="Arlington, VA"
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: /show radar map/i }));
    expect(screen.queryByTitle(/weather radar map/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /hide radar map/i }));
    expect(screen.queryByTitle(/weather radar map/i)).not.toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /show radar map/i }),
    ).toBeInTheDocument();
  });

  it('toggle is a no-op round-trip: click → click returns to initial state', () => {
    render(<RadarCard latitude={38.8816} longitude={-77.0910} />);

    fireEvent.click(screen.getByRole('button', { name: /show radar map/i }));
    expect(screen.queryByTitle(/weather radar map/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /hide radar map/i }));
    expect(screen.queryByTitle(/weather radar map/i)).not.toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /show radar map/i }),
    ).toBeInTheDocument();
  });
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd web && npx vitest run src/components/RadarCard.test.tsx`
Expected: the two new tests FAIL with an error about `getByRole('button', { name: /hide radar map/i })` (no such button exists yet — only the conditional "Show radar map" button is rendered). The three pre-existing tests stay green.

- [ ] **Step 3: Update `RadarCard.tsx` to make the new tests pass**

In `web/src/components/RadarCard.tsx`, replace lines 23–31:

```tsx
        {!loaded && (
          <button
            type="button"
            onClick={() => setLoaded(true)}
            className="text-xs underline hover:text-slate-700"
          >
            Show radar map
          </button>
        )}
```

with:

```tsx
        <button
          type="button"
          onClick={() => setLoaded((v) => !v)}
          className="text-xs underline hover:text-slate-700"
        >
          {loaded ? 'Hide radar map' : 'Show radar map'}
        </button>
```

Do not change anything else in the file (the helper `<p>` block on lines 49–51 is already gated on `loaded` and stays correct).

- [ ] **Step 4: Run tests to verify all five pass**

Run: `cd web && npx vitest run src/components/RadarCard.test.tsx`
Expected: all five tests in `describe('RadarCard', …)` PASS (3 existing + 2 new).

- [ ] **Step 5: Typecheck and full build**

Run: `cd web && npm run typecheck && npm run build`
Expected: typecheck clean, Vite build succeeds with no new warnings.

- [ ] **Step 6: Commit**

```bash
git add web/src/components/RadarCard.tsx web/src/components/RadarCard.test.tsx
git commit -m "feat(web): toggle button to hide radar map in RadarCard"
```

- [ ] **Step 7: Update docs**

- `docs/ui.md` — locate the `RadarCard` entry (added by the 2026-08-12 radar-map spec). If it does not yet mention hide behavior, append one sentence: `Click the button again to dismiss the iframe and reclaim the vertical space.`
- `docs/tradeoffs.md` — locate the future-work bullet that currently reads `Replace button-only state with a persistent toggle (URL hash or \`localStorage\`) so the user's choice survives reloads.` (added by the 2026-08-12 spec). Replace it with: `Persist the radar open/closed choice across reloads (URL hash or \`localStorage\`).`
- Do not touch `docs/superpowers/specs/2026-08-12-radar-map-design.md` (this spec is its child).

If `docs/ui.md` has no `RadarCard` entry yet, skip that step (the parent spec's rollout was incomplete; out of scope to fix here).

- [ ] **Step 8: Commit docs**

```bash
git add docs/ui.md docs/tradeoffs.md
git commit -m "docs: note radar hide toggle and trim future-work bullet"
```

---

## Self-Review

- Spec coverage: Goal (hide button) → Task 1 Step 3. Non-goals (no persistence, no X overlay) → absent from task body by design. Out-of-scope follow-up parent-spec update → Task 1 Step 7. Tests → Task 1 Step 1. Rollout/verify commands → Task 1 Steps 4–5. Acceptance → covered by Steps 4 and 5.
- Placeholder scan: no TBD / TODO / "implement later" / "similar to" patterns. Each step has concrete code or concrete commands.
- Type / name consistency: `RadarCard`, `latitude`, `longitude`, `displayName`, `loaded`, `setLoaded` — all match between the existing component, the existing tests, and the new test code.