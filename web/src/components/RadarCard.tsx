import { useState } from 'react';

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
            title={
              displayName
                ? `Weather radar map for ${displayName}`
                : 'Weather radar map'
            }
            src={src}
            width="100%"
            height="100%"
            style={{ border: 0 }}
            loading="lazy"
            // ponytail: W3C Referrer-Policy value not yet in React DOM lib types; drop cast when @types/react ships it.
            // @ts-expect-error - W3C "no-referrer-ferrer-when-downgrade" not in HTMLAttributeReferrerPolicy union
            referrerPolicy="no-referrer-ferrer-when-downgrade"
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
