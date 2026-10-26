import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    // The page calls /api/... on its own address (localhost:5173).
    // Vite passes those requests on to the Spring app, so the browser never
    // talks to another origin and CORS doesn't get in the way.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
