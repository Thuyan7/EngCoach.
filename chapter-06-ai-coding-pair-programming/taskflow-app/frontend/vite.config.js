import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Dev server proxies API paths to the Spring Boot backend (ADR-002: single
// monolith on :8080), so the browser sees one origin in development and no
// CORS configuration is needed locally. Production (Vercel -> Railway) is a
// different origin: see SLICE-S5B-NOTES.md, "Known gap: CORS".
const API_TARGET = process.env.VITE_DEV_API_TARGET || "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/auth": API_TARGET,
      "/parts": API_TARGET,
      "/attempts": API_TARGET,
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: "./src/test/setup.js",
  },
});
