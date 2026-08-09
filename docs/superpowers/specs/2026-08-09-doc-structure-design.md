# Documentation Restructure — Design Spec

**Date:** 2026-08-09
**Status:** Approved
**Scope:** Split the monolithic `README.md` into a `docs/` folder, add `docs/superpowers/` for design specs and implementation plans, and reduce the root `README.md` to a short overview + table of contents.

## Goals

- Make the documentation navigable for three audiences: end users (UI + API consumers), developers (contributors), and AI agents (e.g., superpowers skills).
- Reduce `README.md` from ~676 lines to ~40 lines.
- Establish a dedicated location for design specs and implementation plans produced via the superpowers workflow (brainstorming → spec → plan → implementation).
- Keep a single source of truth; avoid duplication.

## Non-goals

- Changing any application code or behavior.
- Rewriting documentation content beyond moving and lightly adapting headings.
- Adding new documentation beyond what already exists (except the new `docs/ui.md` to give the frontend its own home).

## File structure

```
weather-wrapper-service/
├── README.md                              # Short overview + TOC
├── docs/
│   ├── architecture.md                    # Mermaid flowchart, request flow, negative caching
│   ├── use-cases.md                       # Sequence diagrams, path matrices
│   ├── tech-stack.md                      # Tech stack table
│   ├── module-layout.md                   # Backend module tree
│   ├── ui.md                              # NEW — UI architecture, components, state mgmt
│   ├── running.md                         # Run locally (Docker + without Docker)
│   ├── configuration.md                   # Config table + env vars
│   ├── api.md                             # API reference (endpoints, statuses, example)
│   ├── testing.md                         # Test layout + commands
│   ├── observability.md                   # Prometheus metrics
│   ├── rate-limiting.md                   # Bucket4j
│   ├── concurrency.md                     # Virtual threads
│   ├── tradeoffs.md                       # Tradeoffs table + future enhancements
│   ├── milestones.md                      # M1–M4 milestones
│   └── superpowers/
│       ├── specs/                         # Design specs (this file lives here)
│       └── plans/                         # Implementation plans
└── ...
```

## `README.md` (revised)

~30–40 lines. Includes:

- Title + one-sentence description (UI + API).
- Two-line "where things run" block (UI on `:5173`, API on `:8080`).
- Quick start (`docker compose up --build` + `npm run dev`).
- Architecture at a glance (ASCII or simple mermaid).
- Table of contents linking to every file in `docs/`.
- License one-liner.

## Content mapping

| Current README section                              | New file                  |
|----------------------------------------------------|---------------------------|
| Title, description, quick start, license           | `README.md` (revised)     |
| Milestones (M1–M4)                                 | `docs/milestones.md`      |
| Architecture (flowchart, request flow, neg. cache) | `docs/architecture.md`    |
| Use case sequences (sequence diagrams, matrices)   | `docs/use-cases.md`       |
| Tech stack table                                   | `docs/tech-stack.md`      |
| Module layout tree                                 | `docs/module-layout.md`   |
| Running locally (Docker, without Docker)           | `docs/running.md`         |
| Configuration table + env vars                     | `docs/configuration.md`   |
| API reference (endpoints, statuses, example)       | `docs/api.md`             |
| Testing (mvn verify, test layout)                  | `docs/testing.md`         |
| Observability (Prometheus, metrics tables)         | `docs/observability.md`   |
| Rate limiting (Bucket4j)                           | `docs/rate-limiting.md`   |
| Concurrency model (virtual threads)                | `docs/concurrency.md`     |
| Tradeoffs & future work + Future enhancements      | `docs/tradeoffs.md`       |
| UI components (currently scattered in milestones)  | `docs/ui.md` (NEW)        |

**Notes:**

- Mermaid diagrams move with their parent section.
- UI component details (ForecastTabs, MetadataBar, CurrentConditionsCard, AlertsBanner, etc.) move from milestones to `docs/ui.md`.
- `docs/ui.md` includes: tech stack (Vite, React, TanStack Query, Tailwind v4), folder structure (`web/src/`), component tree, query hooks, build commands.

## `docs/superpowers/specs/` and `docs/superpowers/plans/`

- `specs/` — Design specs produced by the **brainstorming** skill. One file per spec: `YYYY-MM-DD-<topic>-design.md`.
- `plans/` — Implementation plans produced by the **writing-plans** skill. One file per plan: `YYYY-MM-DD-<topic>-plan.md`.
- The first spec is this file: `docs/superpowers/specs/2026-08-09-doc-structure-design.md`.
- The first plan (created later) will be `docs/superpowers/plans/2026-08-09-doc-structure-plan.md`.
- Both folders are committed to git; each plan references the spec it implements.

## Migration plan

1. Create `docs/` at repo root.
2. Create `docs/superpowers/specs/` and `docs/superpowers/plans/`.
3. For each row in the content mapping table: copy the source content from `README.md` into the new file. Adapt headings (e.g., `## Architecture` → `# Architecture`). Add a one-line front-matter comment if needed (e.g., `<!-- Source: README.md — extracted 2026-08-09 -->`).
4. Extract UI-specific content (currently inside the Milestones section) into the new `docs/ui.md`.
5. Replace `README.md` with the new short version (overview + TOC).
6. Verify all relative links in the new files resolve correctly (e.g., `docs/architecture.md` is linked from `README.md`).
7. Commit with message: `docs: split README into docs/ folder, add superpowers/specs + plans`.
8. (Optional) Add a CI check that fails if any file in `docs/` contains a broken relative link.

## Risks & mitigations

- **Broken links:** mitigated by manual verification in step 6 and optional CI check in step 8.
- **Lost context during extraction:** mitigated by copying content verbatim and only adapting headings.
- **Merge conflicts with in-flight work:** the migration should be done on a dedicated branch (e.g., `docs/restructure`) and merged after review.

## Success criteria

- `README.md` is ≤ 50 lines.
- Every section of the old README has a clear home in `docs/`.
- All relative links from `README.md` to `docs/` resolve.
- `docs/superpowers/specs/` and `docs/superpowers/plans/` exist and are empty except for this spec and the future plan.
- No content is lost (every paragraph from the old README appears somewhere in the new docs).