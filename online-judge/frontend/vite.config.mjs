import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const yingqiBanner =
  "/*! owner=yingqi; signature=00f40662ae433dacddf0157fca60a279bf71a54fbf04ee7d50d3190752554b5d; claim=yingqi|wenzhong-ai-learning-platform|nboj|2026-05-19 */";
const backendTarget = process.env.VITE_API_TARGET || process.env.API_TARGET || "http://localhost:8081";

export default defineConfig({
  base: "/app/",
  plugins: [
    {
      name: "app-root-redirect",
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          const requestUrl = req.url || "";
          const [pathname, suffix = ""] = requestUrl.split(/(?=[?#])/);
          const redirectMap = new Map([
            ["/", "/app/"],
            ["/app", "/app/"],
            ["/student", "/app/student"],
            ["/teacher", "/app/teacher"],
            ["/teacher-management", "/app/teacher/manage"],
            ["/task-editor", "/app/task-editor"],
            ["/class-overview", "/app/teacher/classes"],
            ["/problems", "/app/student/assignments/public"]
          ]);
          let target = redirectMap.get(pathname);
          target ||= pathname.startsWith("/teacher/assignment/") ? `/app${pathname}` : undefined;
          target ||= pathname.startsWith("/problem/") ? `/app${pathname}` : undefined;

          if (target) {
            res.statusCode = 302;
            res.setHeader("Location", `${target}${suffix}`);
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
      "/api": backendTarget,
      "/h2-console": backendTarget
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
