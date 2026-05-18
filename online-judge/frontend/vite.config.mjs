import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  base: "/app/",
  plugins: [react()],
  resolve: {
    preserveSymlinks: true
  },
  server: {
    proxy: {
      "/api": "http://localhost:8081",
      "/h2-console": "http://localhost:8081"
    }
  },
  build: {
    outDir: "../src/main/resources/static/app",
    emptyOutDir: false,
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          react: ["react", "react-dom", "react-router-dom"]
        }
      }
    }
  }
});
