# Replace SVG HourlyChart with Apache ECharts

**Status:** design approved, ready for plan
**Date:** 2026-08-10
**Owner:** web
**Scope:** single component, no backend changes

## Goal

Replace the hand-rolled SVG temperature chart in `web/src/components/HourlyChart.tsx`
with an Apache ECharts implementation that preserves all existing features
(hover tooltip, click-to-scroll, sunrise/sunset markers, daytime band shading)
and remains accessible.

## Non-goals

- Adding new chart types (daily min/max, wind, alerts). Future work.
- A shared chart wrapper / theme / provider abstraction. One-off swap.
- Animation tuning beyond `animation: false`. The current SVG chart is static.
- Resize-observer wiring. The wrapper `<div>` already constrains height; ECharts auto-fits.
- Replacing other potential SVG content in the codebase — only `HourlyChart.tsx` qualifies as a chart.

## Why ECharts (recap)

- **Built-in `smooth: true`** — no plugin needed to match the current 3-point moving-average curve.
- **`markLine` + `markArea`** — first-class support for vertical sunrise/sunset lines
  and daytime shading, which the SVG version assembles by hand.
- **Custom `tooltip.formatter`** — replaces the manual hover circle + text DOM.
- **Single dep pair** (`echarts` + `echarts-for-react`) covers everything; Chart.js
  would need `chartjs-plugin-annotation` + custom tooltip code on top of the core lib.

---

## 1. Dependencies

Add to `web/package.json` dependencies (latest 5.x):

```jsonc
{
  "dependencies": {
    "echarts": "^5.5.0",
    "echarts-for-react": "^3.0.2"
  }
}
```

No backend changes. No `vite.config.ts` changes. ECharts is imported as a regular
ESM module and tree-shakes via `echarts-for-react`'s default `echarts` import.

## 2. Component API

Public props stay identical so `ForecastTabs.tsx:77` requires no changes:

```ts
interface Props {
  data: HourlyWeatherResponse;
  rowRefs: RefObject<(HTMLLIElement | null)[]>;
  hours?: number;
  sun?: SunView;
}
```

`export function HourlyChart(...)` is preserved.

## 3. SVG → ECharts feature mapping

Every visible behaviour of the current SVG component maps to one ECharts option
field, built inside a single `useMemo`:

| Current SVG feature (file:line) | ECharts equivalent |
|---|---|
| 3-point moving average smoothing (`HourlyChart.tsx:28-32`) | Pre-computed `smoothTemps` in `useMemo`; fed as `series.data`. `series.smooth: true` interpolates the curve. |
| `<path d="M … L …">` (`HourlyChart.tsx:160-168`) | `series.type: 'line'`, `series.data: smoothTemps`, `lineStyle: { color: 'rgb(2,132,199)', width: 2.5 }`. |
| Vertical sunrise/sunset dashed lines + `↑ sunrise` / `↓ sunset` labels (`HourlyChart.tsx:93-145`) | `series.markLine.data`: two entries `{ xAxis, label: { formatter, color }, lineStyle: { type: 'dashed', color: 'rgb(251,191,36)' } }`. x position computed from `parseLocalTime(sun.sunriseLocal)` / `sunsetLocal` mapped onto the xAxis range. |
| Daytime band shading via `renderBand` (`HourlyChart.tsx:225-239`) | `series.markArea.data`: pairs of `{ xAxis: startIdx, itemStyle: { color: 'rgb(224,242,254)', opacity: 0.8 } }` covering each contiguous run of `daytime: true`. |
| 2-hour gridlines + bottom hour labels (`HourlyChart.tsx:148-181`) | `xAxis: { type: 'category', data: labels, axisLabel: { interval: 1, formatter: (v, i) => i % 2 === 0 ? v : '' } }`; `splitLine: { show: true }` for vertical gridlines. |
| Hover circle + time/temp text (`HourlyChart.tsx:200-217`) | `tooltip: { trigger: 'axis', formatter: (params) => `${params[0].axisValue} · ${Math.round(params[0].data)}°F` }`. ECharts draws the indicator automatically. |
| Click on hour rect → `rowRefs.current[i].scrollIntoView()` (`HourlyChart.tsx:70-77, 198`) | `onEvents={{ click: (p) => onClickHour(p.dataIndex) }}` on `<ReactECharts>`. `onClickHour` keeps the existing `scrollIntoView` + transient `ring-2 ring-sky-300` highlight. |
| `<svg role="img" aria-label="…">` (`HourlyChart.tsx:84-89`) | Wrapper `<div role="img" aria-label={…}>`; the static label string is unchanged (`Hourly temperature from X°F to Y°F over N hours.`). |
| `viewBox` + `preserveAspectRatio="none"` for full-width stretch (`HourlyChart.tsx:84-87`) | Wrapper `<div className="w-full h-40">`; `<ReactECharts style={{ height: '100%' }} />`. |

`onClickHour` is hoisted out of the render body so it's a stable reference passed
to `onEvents`. (ECharts re-binds on every render otherwise — fine functionally, but
avoidable.)

## 4. Component skeleton

```tsx
import ReactECharts from 'echarts-for-react';
import type { EChartsOption } from 'echarts';
import { useMemo, useRef, type RefObject } from 'react';
import type { HourlyWeatherResponse, SunView } from '../api/weather';

interface Props {
  data: HourlyWeatherResponse;
  rowRefs: RefObject<(HTMLLIElement | null)[]>;
  hours?: number;
  sun?: SunView;
}

export function HourlyChart({ data, rowRefs, hours = 48, sun }: Props) {
  const chartRef = useRef<ReactECharts>(null);
  const periods = data.forecast.periods.slice(0, hours);

  const { option, minT, maxT } = useMemo(
    () => buildOption(periods, sun),
    [periods, sun],
  );

  if (periods.length === 0) return null;

  const ariaLabel = `Hourly temperature from ${Math.round(minT)}°F to ${Math.round(maxT)}°F over ${periods.length} hours.`;

  function onClickHour(i: number) {
    const el = rowRefs.current?.[i];
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.classList.add('ring-2', 'ring-sky-300');
      window.setTimeout(() => el.classList.remove('ring-2', 'ring-sky-300'), 1500);
    }
  }

  return (
    <div
      role="img"
      aria-label={ariaLabel}
      data-testid="hourly-chart"
      className="rounded-2xl border border-slate-200 bg-white shadow-sm p-3 w-full h-40"
    >
      <ReactECharts
        ref={chartRef}
        option={option}
        onEvents={{ click: (p) => onClickHour(p.dataIndex) }}
        opts={{ renderer: 'svg' }}
        style={{ height: '100%', width: '100%' }}
        notMerge
        lazyUpdate
      />
    </div>
  );
}

function buildOption(
  periods: HourlyForecastPeriod[],
  sun?: SunView,
): { option: EChartsOption; minT: number; maxT: number } {
  // Smooth temps with 3-point moving average (same as current SVG component),
  // compute padded min/max for the y-axis, then assemble the EChartsOption
  // per the table in section 3. Returns both the option and the rounded bounds
  // so the caller can build the aria-label without recomputing.
}
```

`buildOption` is a module-level function so the test file can import it directly
and assert on the produced option without rendering the component (used by the
"renders 48 hover targets for 48 hours" test replacement).

### Renderer choice: SVG (not canvas)

`opts.renderer = 'svg'`. Reasoning:

- 48 data points × ~12 chart elements per point is well within SVG's comfort zone; performance is a non-issue.
- SVG keeps axis labels and tooltip text accessible to screen readers.
- The existing component was SVG; visual fidelity to the prior design is closer.

## 5. Error handling

- **Empty `periods`**: return `null` (unchanged from current behaviour).
- **Missing `sun`**: omit `markLine` and `markArea` blocks via conditional spread — temperature line still renders.
- **Malformed data**: no new paths. ECharts drops `null`/`NaN` silently, and the data comes from a typed backend response we already trust.
- **ECharts load failure**: none — the library is bundled, not fetched at runtime.

## 6. Tests

`web/src/components/HourlyChart.test.tsx` is rewritten to match the new surface.
Same `vitest` + `@testing-library/react` setup; no new test infra.

| Existing test | Replacement |
|---|---|
| `renders 48 hover targets for 48 hours` | Assert `getByTestId('hourly-chart')` exists; call `buildOption(data.forecast.periods)` and assert `result.series[0].data.length === 48`. |
| `renders nothing when there are zero periods` | Unchanged — `container.firstChild` still null. |
| `does not crash when sun is undefined` | Unchanged. |
| `click on hour 5 calls scrollIntoView on rowRefs[5]` | Get the ECharts instance via `chartRef.current.getEchartsInstance()`, then call `instance._zr.handler.dispatch('click', { target: instance.getDataIndex(5) })` (or use `dispatchAction({ type: 'highlight', … })` + a synthesized click via `fireEvent.click` on the chart container — implementation chooses the simpler path that passes). Assert `scrollSpy` called. |

If the synthesized-click approach proves flaky, fall back to extracting
`onClickHour` into a named export and calling it directly with `5`. Document the
fallback choice in the test file.

## 7. Documentation updates

- `docs/ui.md` — `HourlyChart` row notes "Apache ECharts (SVG renderer)" instead of "Hand-rolled SVG".
- `docs/tradeoffs.md` (if it discusses the SVG chart) — update to reflect the swap.
- No README or architecture doc change required; the component API is unchanged.

## 8. Rollout

Single PR, frontend only.

| Verify with | Expected |
|---|---|
| `npm run test` | All HourlyChart tests pass. No other web tests regress. |
| `npm run build` | Clean; new bundle size delta logged in PR description. |
| `docker compose up -d --build` + visit `http://localhost:5173`, search "Arlington, VA", switch to Hourly tab | Chart renders identically: blue line, amber dashed sunrise/sunset lines, sky-blue daytime bands, hover tooltip showing time + temp, click scrolls list row + flashes ring. |

## 9. Acceptance

- All current HourlyChart tests rewritten and passing.
- No regression in `npm run build` (typecheck + bundle).
- Manual smoke confirms identical visual + interactive behaviour for Arlington VA
  with and without sun metadata, and with 0/1/48 periods.
- ECharts bundle delta documented in the PR (current chart is ~0 KB runtime; new
  bundle should land somewhere under ~200 KB gzipped; if larger, note the actual
  size and call out the trade-off).

## 10. Out-of-scope follow-ups

- Additional chart types (daily min/max, wind speed, alerts timeline).
- Shared chart-theme object / ECharts provider — premature until a second chart exists.
- Animated transitions on `periods` updates — ECharts supports this but the current UX is static.
- True closest-radar lookup (separate work, M4 slice 4.1 already shipped a link).