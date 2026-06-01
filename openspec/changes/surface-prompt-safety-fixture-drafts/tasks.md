## 1. OpenSpec

- [x] 1.1 编写 proposal、design、tasks 和 spec delta。
- [x] 1.2 运行 `openspec validate surface-prompt-safety-fixture-drafts --strict`。

## 2. 教师端可见性

- [x] 2.1 扩展 Fixture 草稿预览入口计数，纳入 `safetyFixtureCount`。
- [x] 2.2 在统计区展示提示安全草稿数量。
- [x] 2.3 在预览区展示安全草稿风险摘要、待审查提交和主要 blocked reasons。
- [x] 2.4 在 JSON 预览中包含 `safetyFixtures`。

## 3. 样式与兼容

- [x] 3.1 补充紧凑安全摘要样式，适配亮暗主题和移动端。
- [x] 3.2 保持没有安全草稿时的现有展示兼容。

## 4. 验证

- [x] 4.1 运行 `openspec validate surface-prompt-safety-fixture-drafts --strict`。
- [x] 4.2 运行前端 typecheck。
- [x] 4.3 运行 `git diff --check`。
