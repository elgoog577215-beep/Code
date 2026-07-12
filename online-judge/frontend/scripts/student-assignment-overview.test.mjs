import assert from "node:assert/strict";
import test from "node:test";
import { chromium } from "playwright";

const baseUrl = process.env.STUDENT_ASSIGNMENT_BASE_URL || "http://127.0.0.1:8081";
const student = { id: 41, displayName: "Ge Kevin", classGroupId: 3, className: "温中信息技术试点班" };
const assignment = {
  id: 7,
  title: "try",
  description: "这段说明应从精简页面移除。",
  classGroupId: 3,
  className: "温中信息技术试点班",
  hintPolicy: "L2",
  status: "ACTIVE",
  endsAt: null,
  tasks: [
    { problemId: 101, title: "两数求和", difficulty: "EASY", orderIndex: 1, required: true },
    { problemId: 102, title: "回文判断", difficulty: "EASY", orderIndex: 2, required: true },
    { problemId: 103, title: "阶乘计算", difficulty: "MEDIUM", orderIndex: 3, required: true }
  ]
};
const trajectory = {
  completedTasks: 1,
  totalTasks: 3,
  totalAttempts: 4,
  tasks: [
    { problemId: 101, title: "两数求和", difficulty: "EASY", attemptCount: 2, passed: true, submissions: [] },
    { problemId: 102, title: "回文判断", difficulty: "EASY", attemptCount: 2, passed: false, submissions: [] },
    { problemId: 103, title: "阶乘计算", difficulty: "MEDIUM", attemptCount: 0, passed: false, submissions: [] }
  ]
};

test("assignment overview keeps only the core navigation, progress, and direct task list", async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1600, height: 1000 } });
  await context.addInitScript(value => {
    window.sessionStorage.setItem("wzai:student", JSON.stringify(value));
    window.sessionStorage.setItem("wzai:student:7", JSON.stringify(value));
  }, student);
  await context.route("**/api/**", async route => {
    const path = new URL(route.request().url()).pathname;
    const body = path === "/api/student/profile/41/assignments"
      ? [assignment]
      : path === "/api/student/assignments/7/profile/41/trajectory"
        ? trajectory
        : {};
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(body) });
  });

  try {
    const page = await context.newPage();
    await page.goto(`${baseUrl}/app/student/assignments/7`, { waitUntil: "domcontentloaded" });
    await page.locator(".student-assignment-progress-row").first().waitFor({ state: "visible" });

    assert.equal(await page.locator(".student-assignment-insights-tabs a").count(), 3);
    assert.equal(await page.locator(".student-assignment-insights-title > span").count(), 0);
    assert.equal(await page.locator(".student-assignment-insights-meta").count(), 1);
    assert.equal((await page.locator(".student-assignment-insights-meta").textContent())?.trim(), "温中信息技术试点班");
    assert.equal((await page.locator("body").textContent()).includes("截止时间"), false);
    assert.equal(await page.locator(".student-assignment-summary-band").count(), 0);
    assert.equal(await page.locator(".student-assignment-compact-progress").count(), 1);
    assert.equal((await page.locator(".student-assignment-compact-progress").textContent()).includes("总提交 4"), true);
    assert.equal(await page.locator(".student-assignment-note-band").count(), 0);
    assert.equal(await page.getByRole("heading", { name: "题目进度" }).count(), 0);
    assert.equal(await page.locator(".student-assignment-progress-header > span").count(), 5);
    assert.equal(await page.locator(".student-assignment-progress-row").first().evaluate(element => element.tagName), "A");
    assert.equal(await page.locator(".student-assignment-latest-verdict").count(), 0);
    assert.equal((await page.locator(".student-assignment-attempts").first().textContent())?.trim(), "2");
  } finally {
    await context.close();
    await browser.close();
  }
});
