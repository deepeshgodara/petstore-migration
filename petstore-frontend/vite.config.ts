import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    host: true,
    proxy: {
      '/api/v1/categories': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/v1/products': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/v1/items': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/v1/orders': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/v1/migration': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
    },
  },
});
