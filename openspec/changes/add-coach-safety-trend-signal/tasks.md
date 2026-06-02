## 1. OpenSpec

- [x] 1.1 编写 proposal、design、tasks 和 spec delta。
- [x] 1.2 运行 `openspec validate add-coach-safety-trend-signal --strict`。

## 2. 后端趋势统计

- [x] 2.1 在 `AiQualityTrendService` 中注入并读取 `CoachPromptRepository`。
- [x] 2.2 为总览、assignment point 和 source segment 统计 `coachSafetyRejectionCount`。
- [x] 2.3 保持 `promptSafetyIncidentCount` 包含 Coach 安全拒绝，同时 `promptSafetyDowngradeCount` 只统计 HintSafetyCheck。
- [x] 2.4 扩展 `AiQualityTrendResponse` DTO。

## 3. 前端展示

- [x] 3.1 更新前端 API 类型。
- [x] 3.2 在教师端 AI 趋势卡片展示 Coach 安全回退总数。
- [x] 3.3 在作业趋势点展示 Coach 安全 badge。

## 4. 测试与验证

- [x] 4.1 扩展 `AiQualityTrendServiceTest` 覆盖 Coach 安全拒绝趋势统计。
- [x] 4.2 运行 OpenSpec strict 校验和相关后端测试。
- [x] 4.3 运行后端编译、前端 typecheck 和 `git diff --check`。
