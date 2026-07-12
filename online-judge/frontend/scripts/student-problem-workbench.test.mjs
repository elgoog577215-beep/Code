import assert from "node:assert/strict";
import test from "node:test";
import { chromium } from "playwright";

const baseUrl = process.env.STUDENT_PROBLEM_BASE_URL || "http://127.0.0.1:8081";
const viewportWidth = Number(process.env.STUDENT_PROBLEM_VIEWPORT_WIDTH || 1600);
const viewportHeight = Number(process.env.STUDENT_PROBLEM_VIEWPORT_HEIGHT || 1000);
const student = { id: 41, displayName: "NothingK", classGroupId: 3, className: "温中信息技术试点班" };
const assignment = {
  id: 7,
  title: "try",
  classGroupId: 3,
  className: "温中信息技术试点班",
  hintPolicy: "L2",
  status: "ACTIVE",
  tasks: [
    { problemId: 101, title: "两数求和", difficulty: "EASY", orderIndex: 1, required: true },
    { problemId: 102, title: "回文判断", difficulty: "EASY", orderIndex: 2, required: true },
    { problemId: 103, title: "FizzBuzz", difficulty: "EASY", orderIndex: 3, required: true },
    { problemId: 104, title: "阶乘计算", difficulty: "MEDIUM", orderIndex: 4, required: true }
  ]
};
const problem = {
  id: 101,
  title: "两数求和",
  description: "## 题目描述\n给定两个整数 `a` 和 `b`，输出它们的和。\n\n## 输入格式\n一行两个整数。\n\n## 输出格式\n输出一个整数。",
  difficulty: "EASY",
  timeLimit: 1000,
  memoryLimit: 131072,
  starterCode: "a, b = map(int, input().split())\nprint(a + b)",
  sampleTestCases: [{ input: "3 5", expectedOutput: "8" }]
};
const trajectory = {
  assignment,
  student,
  completedTasks: 0,
  totalTasks: 4,
  totalAttempts: 0,
  repeatedIssueCount: 0,
  recentIssueDistribution: [],
  abilitySummary: [],
  tasks: assignment.tasks.map(task => ({ ...task, attemptCount: 0, passed: false, submissions: [] }))
};

test("problem workbench has persistent navigation, resizable split panels, and collapsible code", async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: {
      width: viewportWidth,
      height: viewportHeight
    }
  });
  await context.addInitScript(value => {
    window.localStorage.setItem("wzai:theme", "light");
    window.sessionStorage.setItem("wzai:student", JSON.stringify(value));
    window.sessionStorage.setItem("wzai:student:7", JSON.stringify(value));
  }, student);
  await context.route("**/api/**", async route => {
    const path = new URL(route.request().url()).pathname;
    const body = path === "/api/problems/101"
      ? problem
      : path === "/api/student/profile/41/assignments"
        ? [assignment]
        : path === "/api/student/assignments/7/profile/41/trajectory"
          ? trajectory
          : path === "/api/submissions/problem/101/history-summary"
            ? []
            : {};
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(body) });
  });

  try {
    const page = await context.newPage();
    const browserErrors = [];
    page.on("console", message => {
      if (message.type() === "error") browserErrors.push(message.text());
    });
    page.on("pageerror", error => browserErrors.push(error.message));
    await page.goto(`${baseUrl}/app/student/assignments/7/problems/101?studentProfileId=41`, { waitUntil: "domcontentloaded" });
    await page.locator(".problem-main-split").waitFor({ state: "visible", timeout: 10000 });
    await page.locator(".problem-assignment-header").waitFor({ state: "visible", timeout: 10000 });

    assert.equal(await page.locator(".problem-assignment-header").count(), 1);
    assert.equal((await page.locator(".problem-assignment-header h1").textContent())?.trim(), "try");
    assert.equal(await page.locator(".problem-assignment-header .student-assignment-insights-meta").count(), 2);
    assert.equal((await page.locator(".problem-assignment-header .student-assignment-profile strong").textContent())?.trim(), "NothingK");
    assert.equal(await page.locator(".problem-back-link").count(), 0);
    const pageFrame = await page.locator(".problem-page").boundingBox();
    const appHeaderFrame = await page.locator(".app-header").boundingBox();
    assert.ok(pageFrame);
    assert.ok(appHeaderFrame);
    assert.ok(pageFrame.x <= 1, JSON.stringify(pageFrame));
    assert.ok(pageFrame.width >= viewportWidth - 2, JSON.stringify(pageFrame));
    assert.ok(Math.abs(pageFrame.y - (appHeaderFrame.y + appHeaderFrame.height)) <= 1, JSON.stringify({ pageFrame, appHeaderFrame }));
    assert.equal(await page.locator(".problem-workbench-rail a").count(), 4);
    assert.equal((await page.locator(".problem-workbench-rail a.is-active").textContent())?.trim(), "题目");
    assert.equal(await page.locator(".problem-main-split > .panel--statement").count(), 1);
    assert.equal(await page.locator(".problem-main-split > .panel--editor").count(), 1);
    if (process.env.STUDENT_PROBLEM_SCREENSHOT) {
      await page.screenshot({ path: process.env.STUDENT_PROBLEM_SCREENSHOT, fullPage: true });
    }

    const separator = page.getByRole("separator", { name: "调整题目和代码宽度" });
    if (viewportWidth > 980) {
      const statementBefore = await page.locator(".panel--statement").boundingBox();
      const editorBefore = await page.locator(".panel--editor").boundingBox();
      const splitBefore = await page.locator(".problem-main-split").evaluate(element => ({
        style: element.getAttribute("style"),
        columns: getComputedStyle(element).gridTemplateColumns,
        width: element.getBoundingClientRect().width
      }));
      const separatorBox = await separator.boundingBox();
      assert.ok(statementBefore && editorBefore && separatorBox);
      await page.mouse.move(separatorBox.x + separatorBox.width / 2, separatorBox.y + 100);
      await page.mouse.down();
      await page.mouse.move(separatorBox.x + 140, separatorBox.y + 100, { steps: 6 });
      await page.mouse.up();
      const statementAfter = await page.locator(".panel--statement").boundingBox();
      const editorAfter = await page.locator(".panel--editor").boundingBox();
      assert.ok(statementAfter && editorAfter);
      assert.ok(statementAfter.width > statementBefore.width + 60, JSON.stringify({ splitBefore, statementBefore, statementAfter }));
      assert.ok(editorAfter.width < editorBefore.width - 60, JSON.stringify({ editorBefore, editorAfter }));
    } else {
      assert.equal(await separator.isVisible(), false);
    }

    await page.getByRole("button", { name: "收起代码" }).click();
    assert.equal(await page.locator(".problem-main-split > .panel--editor").count(), 0);
    if (process.env.STUDENT_PROBLEM_COLLAPSED_SCREENSHOT) {
      await page.screenshot({ path: process.env.STUDENT_PROBLEM_COLLAPSED_SCREENSHOT, fullPage: true });
    }
    await page.getByRole("button", { name: "展开代码" }).click();
    await page.locator(".problem-main-split > .panel--editor").waitFor({ state: "visible" });
    assert.equal(await page.getByRole("button", { name: "提交代码" }).count(), 1);
    assert.deepEqual(browserErrors, []);

  } finally {
    await context.close();
    await browser.close();
  }
});
