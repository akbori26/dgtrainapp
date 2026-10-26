import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  build: {
    // npm run build puts the page where Spring Boot serves static files,
    // so http://localhost:8080 shows the game and the jar contains it.
    outDir: '../src/main/resources/static',
    // The folder is outside frontend/, so Vite must be told it may empty it first
    emptyOutDir: true,
  },
  server: {
    // The page calls /api/... on its own address (localhost:5173).
    // Vite passes those requests on to the Spring app, so the browser never
    // talks to another origin and CORS doesn't get in the way.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
