import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const pageSource = readFileSync(new URL("../src/features/student/StudentPage.tsx", import.meta.url), "utf8");
const i18nSource = readFileSync(new URL("../src/shared/i18n.tsx", import.meta.url), "utf8");
const styleSource = readFileSync(new URL("../src/styles.css", import.meta.url), "utf8");

test("student home consumes the canonical recommendation API", () => {
  assert.match(pageSource, /api\.studentRecommendations\(student\.id\)/);
  assert.doesNotMatch(pageSource, /reviewCards\?\.\[0\]/);
});

test("next learning keeps a single primary action and completion signal", () => {
  assert.match(pageSource, /const primary = items\[0\]/);
  assert.match(pageSource, /primary\.expectedCompletionSignal/);
  assert.match(pageSource, /items\.slice\(1\)/);
});

test("recommendation route carries identity and tracking token", () => {
  assert.match(pageSource, /studentProfileId/);
  assert.match(pageSource, /recommendationToken/);
  assert.match(pageSource, /recordRecommendationEvent/);
});

test("loading empty and failure states remain distinct", () => {
  assert.match(pageSource, /recommendationLoading/);
  assert.match(pageSource, /recommendationFailed/);
  assert.match(pageSource, /!primary/);
  assert.match(pageSource, /setRecommendationReload/);
});

test("next learning has Chinese English and responsive contracts", () => {
  assert.equal((i18nSource.match(/nextLearning:/g) || []).length, 2);
  assert.match(i18nSource, /本轮下一步/);
  assert.match(i18nSource, /Your Next Step/);
  assert.match(styleSource, /\.student-next-learning/);
  assert.match(styleSource, /@media \(max-width: 560px\)/);
});
