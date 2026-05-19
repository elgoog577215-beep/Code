import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  base: "/app/",
  plugins: [
    {
      name: "app-root-redirect",
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          if (req.url === "/app") {
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
