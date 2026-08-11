# Plan C — Web Cleanup + Delete Sweep Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Delete checked-in build output, dev logs, ephemeral Copilot state, per-task report, and dead placeholders. Trim web duplication in hooks, components, and the storage library. Eliminates ~150 LOC of web code + a few hundred KB of accidentally-committed artifacts.

**Architecture:** Mostly deletions plus small in-place edits to React hooks and components. No new abstractions; one component file is inlined into another.

**Tech Stack:** React 19, TypeScript 5.7 (strict), Vite 6, TanStack Query 5, Tailwind 4.

## Global Constraints

- See [`2026-08-10-overengineering-audit.md`](./2026-08-10-overengineering-audit.md) global constraints.
- Frontend tests use vitest + @testing-library/react (already configured per M3).
- Frontend never reads/writes localStorage outside `web/src/lib/savedLocations.ts` (M3 1.2 rule) — preserves after C4.
- All commits use `refactor(web):` or `chore(repo):` per repo convention.

## Files

**Delete:**
- `web/dev.log`
- `web/dist/` (entire directory)
- `web/.gitkeep`
- `.github/modernize/` (entire directory)
- `task-2.11-report.md`

**Modify:**
- `.gitignore` (add patterns)
- `web/src/hooks/useAlerts.ts`
- `web/src/hooks/useCurrentConditions.ts`
- `web/src/hooks/useSavedLocations.ts`
- `web/src/App.tsx`
- `web/src/components/HourlyChart.tsx`
- `web/src/components/DiscussionCard.tsx`
- `web/src/components/DiscussionTab.tsx`
- `web/src/lib/savedLocations.ts`

---

### Task C1 — Delete-only sweep

**Files:**
- Modify: `.gitignore`
- Delete: `web/dev.log`, `web/dist/`, `web/.gitkeep`, `.github/modernize/`, `task-2.11-report.md`

- [ ] **Step 1: Add patterns to `.gitignore`**

  Append (after the existing `web/` block):
  ```
  # Vite dev output / logs (should not be committed)
  web/dev.log
  web/dist/

  # GitHub Copilot session state (ephemeral)
  .github/modernize/
  ```

- [ ] **Step 2: Remove the artifacts**

  ```bash
  git rm -r web/dist/
  git rm web/dev.log web/.gitkeep task-2.11-report.md
  git rm -r .github/modernize/
  ```

- [ ] **Step 3: Confirm `git status` is clean**

  Run: `git status`
  Expected: only the 5 deletions + `.gitignore` modification.

- [ ] **Step 4: Commit**

  ```bash
  git add .gitignore
  git add -u web/ .github/ task-2.11-report.md
  git commit -m "chore(repo): remove dev logs, build output, ephemeral session state, dead placeholders"
  ```

---

### Task C2 — Web hook shrinks

**Files:**
- Modify: `web/src/hooks/useAlerts.ts`, `web/src/hooks/useCurrentConditions.ts`, `web/src/hooks/useSavedLocations.ts`

- [ ] **Step 1: Read the three hook files**

  Read `useAlerts.ts`, `useCurrentConditions.ts`, and `useSavedLocations.ts` to confirm:
  - `ALERTS_QUERY_KEY` and `CONDITIONS_QUERY_KEY` are placeholder constants (`'***'`) that are not imported elsewhere.
  - `useSavedLocations.ts` lines 53 and 64 use `void` to silence an unused-import lint on names that are not actually imported (or are already used elsewhere).

- [ ] **Step 2: Delete unused constants**

  In `useAlerts.ts`: delete the `export const ALERTS_QUERY_KEY = '***';` line. Keep the literal `'alerts'` in the `queryKey: ['alerts', city]` array.
  In `useCurrentConditions.ts`: same, with `'conditions'`.

- [ ] **Step 3: Fix `useSavedLocations.ts`**

  - Delete any unused imports (the `void isSavedInStorage;` and `void saveLastCity;` lines are workarounds, not real usages).
  - Replace the local `isSaved` logic in `useSavedLocations` with a direct call to `savedLocations.isSaved(...)` from `web/src/lib/savedLocations.ts`. Delete the duplicate code.

- [ ] **Step 4: Run frontend tests and build**

  Run: `cd web && npm run test && npm run build`
  Expected: all tests pass, build clean.

- [ ] **Step 5: Commit**

  ```bash
  git add web/src/hooks/
  git commit -m "refactor(web): drop unused query key constants and isSaved duplication"
  ```

---

### Task C3 — `HourlyChart.tsx` duplicate `onClickHour`

**Files:**
- Modify: `web/src/components/HourlyChart.tsx`

- [ ] **Step 1: Read the file**

  Confirm lines 171-178 (local `onClickHour`) and lines 199-206 (exported `onClickHour`) have byte-identical bodies.

- [ ] **Step 2: Delete the local copy**

  Delete the local `function onClickHour(...) { ... }` declaration (lines 171-178). Inline the call site that referenced the local copy to use the exported function.

- [ ] **Step 3: Run frontend tests and build**

  Run: `cd web && npm run test && npm run build`
  Expected: pass, clean.

- [ ] **Step 4: Commit**

  ```bash
  git add web/src/components/HourlyChart.tsx
  git commit -m "refactor(web): drop duplicate onClickHour in HourlyChart"
  ```

---

### Task C4 — `App.tsx` last-city dedup

**Files:**
- Modify: `web/src/App.tsx`

- [ ] **Step 1: Read the file**

  Locate the `useState<string|null>` + manual `localStorage` listener block (~lines 17, 20-35). Confirm `useLastCity()` is exported from `web/src/hooks/useSavedLocations.ts`.

- [ ] **Step 2: Replace the manual plumbing**

  Delete the `useState`, the `useEffect` listening to `storage` events, and any direct `localStorage.getItem(...)` calls. Replace with:
  ```tsx
  const lastCity = useLastCity();
  ```

  Use `lastCity` wherever the previous `useState` value was read.

- [ ] **Step 3: Run frontend tests and build**

  Run: `cd web && npm run test && npm run build`
  Expected: pass, clean.

- [ ] **Step 4: Commit**

  ```bash
  git add web/src/App.tsx
  git commit -m "refactor(web): use useLastCity hook in App.tsx"
  ```

---

### Task C5 — `savedLocations.ts` simplification

**Files:**
- Modify: `web/src/lib/savedLocations.ts`

- [ ] **Step 1: Read the file**

  Confirm there are 3 wrapper functions: `safeGet`, `safeSet`, `safeRemove`, each wrapping a single `localStorage` call with try/catch.

- [ ] **Step 2: Inline the wrappers**

  At each call site of `safeGet`/`safeSet`/`safeRemove`, replace with an inline `try { localStorage.… } catch { … }` block. Delete the 3 wrapper functions.

- [ ] **Step 3: Run frontend tests and build**

  Run: `cd web && npm run test && npm run build`
  Expected: pass, clean. Net LOC: ~30 removed.

- [ ] **Step 4: Commit**

  ```bash
  git add web/src/lib/savedLocations.ts
  git commit -m "refactor(web): inline safeGet/safeSet/safeRemove in savedLocations"
  ```

---

### Task C6 — `DiscussionCard` + `DiscussionTab` merge

**Files:**
- Modify: `web/src/components/DiscussionTab.tsx`
- Delete: `web/src/components/DiscussionCard.tsx`

- [ ] **Step 1: Read both files**

  Read `DiscussionCard.tsx` and `DiscussionTab.tsx` to confirm the split: one owns the query, one renders. Confirm `DiscussionCard` is only consumed by `DiscussionTab`.

- [ ] **Step 2: Inline the rendering logic**

  Move the JSX from `DiscussionCard` into `DiscussionTab` (where the query lives). Update imports.

- [ ] **Step 3: Delete `DiscussionCard.tsx`**

  ```bash
  git rm web/src/components/DiscussionCard.tsx
  ```

- [ ] **Step 4: Run frontend tests and build**

  Run: `cd web && npm run test && npm run build`
  Expected: pass, clean.

- [ ] **Step 5: Commit**

  ```bash
  git add web/src/components/
  git commit -m "refactor(web): merge DiscussionCard into DiscussionTab"
  ```

---

## Acceptance

- `git status` shows only the deletions expected (C1) plus a clean tree after C2-C6.
- `npm run test` and `npm run build` pass throughout.
- Net diff: ~150 LOC of web code removed + the deleted artifacts (which were never intended to be committed).
