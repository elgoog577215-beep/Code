import assert from "node:assert/strict";
import test from "node:test";
import { chromium } from "playwright";

const baseUrl = process.env.STUDENT_HOME_BASE_URL || "http://127.0.0.1:8081";
const student = { id: 41, displayName: "Ge Kevin", classGroupId: 3, className: "温中信息技术试点班" };
const assignments = [
  {
    id: 7,
    title: "课堂编程作业",
    description: "这段作业说明不应显示在精简首页。",
    classGroupId: 3,
    className: "温中信息技术试点班",
    hintPolicy: "L2",
    status: "ACTIVE",
    endsAt: "2026-07-20T18:00:00",
    tasks: [
      { problemId: 101, title: "两数求和", difficulty: "EASY", orderIndex: 1, required: true },
      { problemId: 102, title: "回文判断", difficulty: "EASY", orderIndex: 2, required: true }
    ]
  },
  {
    id: 8,
    title: "循环练习",
    description: "另一段不应显示的作业说明。",
    classGroupId: 3,
    className: "温中信息技术试点班",
    hintPolicy: "L2",
    status: "ACTIVE",
    tasks: [{ problemId: 103, title: "阶乘计算", difficulty: "MEDIUM", orderIndex: 1, required: true }]
  }
];
const publicProblems = [
  { id: 101, title: "两数求和", difficulty: "EASY", timeLimit: 1000, memoryLimit: 65536 },
  { id: 102, title: "回文判断", difficulty: "EASY", timeLimit: 1000, memoryLimit: 65536 },
  { id: 103, title: "阶乘计算", difficulty: "MEDIUM", timeLimit: 1000, memoryLimit: 65536 }
];

async function withSignedInPage(run) {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1600, height: 1000 } });
  await context.addInitScript(value => {
    window.sessionStorage.setItem("wzai:student", JSON.stringify(value));
  }, student);
  await context.route("**/api/**", async route => {
    const path = new URL(route.request().url()).pathname;
    let body = {};
    if (path === "/api/problems/catalog") body = publicProblems;
    if (path === "/api/student/profile/41/assignments") body = assignments;
    if (path === "/api/student/assignments/7/profile/41/trajectory") body = { completedTasks: 1, totalTasks: 2 };
    if (path === "/api/student/assignments/8/profile/41/trajectory") body = { completedTasks: 0, totalTasks: 1 };
    if (path === "/api/student/profile/41/ability-profile") body = {};
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(body) });
  });

  try {
    const page = await context.newPage();
    await page.goto(`${baseUrl}/app/student`, { waitUntil: "domcontentloaded" });
    await page.locator(".student-assignment-row").first().waitFor({ state: "visible" });
    await run(page);
  } finally {
    await context.close();
    await browser.close();
  }
}

test("signed-in public bank uses the guest card style above classroom assignments", async () => {
  await withSignedInPage(async page => {
    const publicPractice = page.locator(".student-guest-practice");
    const assignmentBoard = page.locator(".student-assignment-board");
    assert.equal(await publicPractice.count(), 1, "signed-in page must reuse the guest public-practice card");
    assert.equal(await publicPractice.getAttribute("class"), "student-guest-practice");
    assert.equal(await publicPractice.locator(".student-guest-practice__difficulty").count(), 1);
    assert.equal(await publicPractice.locator(".student-guest-starters").count(), 1);
    assert.equal(await publicPractice.locator(".student-guest-starter-card").count(), 3);
    const [publicBox, assignmentBox] = await Promise.all([publicPractice.boundingBox(), assignmentBoard.boundingBox()]);
    assert.ok(publicBox && assignmentBox && publicBox.y < assignmentBox.y, "public bank must appear above assignments");
  });
});

test("each classroom assignment opens directly without selection", async () => {
  await withSignedInPage(async page => {
    const firstAssignment = page.locator(".student-assignment-row").first();
    assert.equal(await firstAssignment.evaluate(element => element.tagName), "A");
    assert.equal(await page.locator('.student-assignment-row input[type="radio"]').count(), 0);
    await Promise.all([
      page.waitForURL(url => url.pathname === "/app/student/assignments/7"),
      firstAssignment.click()
    ]);
  });
});

test("signed-in home removes secondary summaries and assignment descriptions", async () => {
  await withSignedInPage(async page => {
    assert.equal(await page.locator(".student-home-command__message span").count(), 0);
    assert.equal(await page.locator(".student-assignment-board__head p").count(), 0);
    assert.equal(await page.locator(".student-assignment-table__header").count(), 0);
    assert.equal(await page.locator(".student-assignment-row__main small").count(), 0);
    assert.equal((await page.locator("body").textContent()).includes("这段作业说明不应显示"), false);
  });
});
