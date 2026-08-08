import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    host: true,
    proxy: {
      // Forward the Spring Boot API so the React app can call `/api/v1/weather`
      // with no CORS friction. Backend must be running on :8080 (e.g. `docker compose up`).
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});