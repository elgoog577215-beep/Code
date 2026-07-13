import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import test from "node:test";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "..");
const componentPath = join(frontendRoot, "src", "features", "growth", "SingleProblemGrowthDashboard.tsx");
const stylesheetPath = join(frontendRoot, "src", "features", "growth", "SingleProblemGrowthDashboard.css");
const componentSource = readFileSync(componentPath, "utf8");

test("growth dashboard keeps every existing information section", () => {
  const requiredTranslationKeys = [
    "growthDashboard.metrics.submissions",
    "growthDashboard.metrics.effective",
    "growthDashboard.metrics.tests",
    "growthDashboard.metrics.unresolved",
    "growthDashboard.trendTitle",
    "growthDashboard.knowledgeTitle",
    "growthDashboard.evolutionTitle",
    "growthDashboard.matrixTitle"
  ];

  requiredTranslationKeys.forEach(key => {
    assert.match(componentSource, new RegExp(key.replaceAll(".", "\\.")), `missing ${key}`);
  });
});

test("growth dashboard uses the radial concept without changing surrounding chrome", () => {
  assert.match(componentSource, /import "\.\/SingleProblemGrowthDashboard\.css";/);
  assert.match(componentSource, /growth-dashboard__metric-icon/);
  assert.match(componentSource, /growth-dashboard__trend-insight/);
  assert.match(componentSource, /growth-dashboard__analytics-grid/);
  assert.match(componentSource, /growth-dashboard__knowledge-list/);
  assert.match(componentSource, /growth-dashboard__donut/);
  assert.match(componentSource, /<PieChart>/);
  assert.doesNotMatch(componentSource, /<BarChart/);
  assert.ok(existsSync(stylesheetPath), "dedicated dashboard stylesheet should exist");

  const stylesheet = readFileSync(stylesheetPath, "utf8");
  assert.match(stylesheet, /\.growth-dashboard__metric\s*\{/);
  assert.match(stylesheet, /\.growth-dashboard__trend-insight\s*\{/);
  assert.match(stylesheet, /\.growth-dashboard__analytics-grid\s*\{/);
  assert.match(stylesheet, /\.growth-dashboard__donut\s*\{/);
  assert.match(stylesheet, /@media \(max-width: 900px\)/);
  assert.doesNotMatch(stylesheet, /\.app-header|\.problem-result-modal|\.student-assignment-side-nav/);
});
