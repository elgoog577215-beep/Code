import { rm } from "node:fs/promises";
import { resolve } from "node:path";

const outDir = resolve(import.meta.dirname, "../../src/main/resources/static/app");
try {
  await rm(outDir, { recursive: true, force: true });
} catch (error) {
  if (error && typeof error === "object" && "code" in error && error.code === "EPERM") {
    console.warn(`[clean-build] ${outDir} is locked; continuing so Vite can overwrite current assets.`);
  } else {
    throw error;
  }
}
