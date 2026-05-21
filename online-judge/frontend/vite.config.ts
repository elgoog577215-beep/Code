import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const yingqiBanner =
  "/*! owner=yingqi; signature=00f40662ae433dacddf0157fca60a279bf71a54fbf04ee7d50d3190752554b5d; claim=yingqi|wenzhong-ai-learning-platform|nboj|2026-05-19 */";

export default defineConfig({
  base: "/app/",
  plugins: [
    {
      name: "app-root-redirect",
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          const requestUrl = (req as { url?: string }).url;
          if (requestUrl === "/app") {
            res.statusCode = 302;
            res.setHeader("Location", "/app/");
            res.end();
            return;
          }
          next();
        });
      }
    },
    react()
  ],
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
        banner: yingqiBanner,
        manualChunks: {
          react: ["react", "react-dom", "react-router-dom"]
        }
      }
    }
  }
});
