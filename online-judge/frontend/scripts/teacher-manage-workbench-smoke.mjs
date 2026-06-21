import { chromium } from "playwright";

const baseUrl = process.env.TEACHER_MANAGE_SMOKE_BASE_URL || "http://localhost:5173";

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

async function inspect(page, path) {
  await page.goto(`${baseUrl}${path}`, { waitUntil: "networkidle" });
  return page.evaluate(() => {
    const text = (selector) => document.querySelector(selector)?.textContent?.trim() || "";
    const count = (selector) => document.querySelectorAll(selector).length;
    const has = (selector) => Boolean(document.querySelector(selector));
    const doc = document.documentElement;
    return {
      h1: text("h1"),
      statusStrip: has(".teacher-manage-status-strip"),
      objectWorkbench: count(".management-object-workbench"),
      objectList: count(".management-object-list"),
      importDrawer: has(".management-import-drawer"),
      editorDrawer: has(".task-editor-page--embedded .editor-secondary-drawer"),
      classEmptyOption: Array.from(document.querySelectorAll("option")).some((option) => option.textContent?.trim() === "不指定班级"),
      aiEditorSectionCount: count(".standard-library-editor__section"),
      overflowX: doc.scrollWidth - doc.clientWidth
    };
  });
}

const browser = await chromium.launch({ headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1366, height: 900 } });

  const home = await inspect(page, "/app/teacher/manage");
  assert(home.h1 === "管理", "management home should keep 管理 title");
  assert(home.statusStrip, "management home should use thin status strip");
  assert(home.objectWorkbench === 0, "management home should not render object workbench");

  const classes = await inspect(page, "/app/teacher/manage/classes");
  assert(classes.objectWorkbench === 1, "classes page should render one object workbench");
  assert(classes.objectList === 1, "classes page should render class object list");
  assert(!classes.classEmptyOption, "classes import should not offer 不指定班级 as primary option");

  const problems = await inspect(page, "/app/teacher/manage/problems");
  assert(problems.objectWorkbench === 1, "problems page should render one object workbench");
  assert(problems.objectList === 1, "problems page should render problem object list");
  assert(problems.importDrawer, "problems page should keep import in a compact drawer");
  assert(!problems.editorDrawer, "embedded task editor should not render its own catalog drawer");

  const aiLibrary = await inspect(page, "/app/teacher/manage/ai-library");
  assert(aiLibrary.objectWorkbench === 1, "AI library page should render one object workbench");
  assert(aiLibrary.objectList === 1, "AI library page should render item object list");
  assert(aiLibrary.aiEditorSectionCount >= 2, "AI library editor should be grouped into compact sections");

  await page.setViewportSize({ width: 390, height: 844 });
  for (const path of ["/app/teacher/manage", "/app/teacher/manage/classes", "/app/teacher/manage/problems", "/app/teacher/manage/ai-library"]) {
    const result = await inspect(page, path);
    assert(result.overflowX === 0, `${path} should not have page-level horizontal overflow`);
  }

  console.log("teacher manage workbench smoke passed");
} finally {
  await browser.close();
}
