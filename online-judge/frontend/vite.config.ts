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
          const requestUrl = (req as { url?: string }).url || "";
          const [pathname, suffix = ""] = requestUrl.split(/(?=[?#])/);
          const redirectMap = new Map([
            ["/", "/app/"],
            ["/app", "/app/"],
            ["/student", "/app/student"],
            ["/teacher", "/app/teacher"],
            ["/teacher-management", "/app/teacher-management"],
            ["/task-editor", "/app/task-editor"],
            ["/class-overview", "/app/class-overview"],
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
