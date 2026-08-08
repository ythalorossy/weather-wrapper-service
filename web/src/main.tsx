import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from './App';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Backend caches weather for 12h and geocoding for 30d.
      // Match on the client so quick re-queries are instant, and stale
      // entries revalidate quietly in the background.
      staleTime: 30 * 60 * 1000, // 30 min
      gcTime: 60 * 60 * 1000, // 60 min
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
);