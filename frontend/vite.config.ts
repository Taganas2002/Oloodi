/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // Keep SSE streams open through the Vite dev proxy.
        timeout: 0,
        proxyTimeout: 0,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    exclude: ['**/node_modules/**', '**/e2e/**', '**/measurements/**', '**/dist/**'],
  },
})
