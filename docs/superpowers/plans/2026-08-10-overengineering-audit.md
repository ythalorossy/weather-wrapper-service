# Over-engineering Cleanup — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate the 27 over-engineering findings from the repo-wide audit without changing observable behavior. Three independent plans, each ships working software and lands as its own PR.

**Architecture:**
- **Plan A (Cache Layer):** Replace 6 cache ports + 7 adapters + 6 use-case helpers with one generic `Cache<V>` port + one `RedisJsonCache<V>` adapter + one `CacheAside` helper. Crosses 3 Maven modules.
- **Plan B (Backend Cleanup):** Consolidate the 3 NWS providers (shared `/points` lookup + HTTP-error translator), simplify DTOs and domain models (drop duplicated views, redundant resolvers, key methods, hand-rolled normalization, the reflection on `SunriseResult`, the URL-builder ceremony, the duplicated Javadoc on `RestClientConfig`, and the pre-existing test failure in `ComputedSunTimesProviderTest.returnsEmptyForReykjavikMidSummer`).
- **Plan C (Web + Delete Sweep):** Delete checked-in build output, dev logs, ephemeral Copilot state, per-task report; trim web duplication in hooks/components/libs.

**Tech Stack:**
- Backend: Java 21, Spring Boot 3.3.5, JUnit 5, AssertJ, WireMock, Testcontainers Redis. No dependency changes.
- Frontend: React 19, TypeScript 5.7 (strict), Vite 6, TanStack Query 5, Tailwind 4.

## Global Constraints

- Behavior byte-identical post-refactor (no API response changes, no metric renames, no cache-key changes).
- Tests: TDD on new code (generic `Cache<V>`, `RedisJsonCache<V>`, `CacheAside`, `NwsClient`, `NwsPointsService`). For pure deletions/shrinks, rely on existing tests — they must keep passing.
- Java records preferred; pattern matching for `instanceof` and `switch` (Java 21).
- All commits use `refactor(scope):` or `chore(scope):` prefix per repo convention.
- No new dependencies; no dependencies removed.
- Each plan is a self-contained PR (separate branch + PR per plan).
- Net expected: ~1,200–1,500 LOC removed across both codebases.

## Sub-plans

- [`2026-08-10-overengineering-audit-A.md`](./2026-08-10-overengineering-audit-A.md) — Generic cache layer (highest blast radius; cross-module).
- [`2026-08-10-overengineering-audit-B.md`](./2026-08-10-overengineering-audit-B.md) — Backend NWS, DTO, and model cleanup.
- [`2026-08-10-overengineering-audit-C.md`](./2026-08-10-overengineering-audit-C.md) — Web cleanup and delete-only sweep.
