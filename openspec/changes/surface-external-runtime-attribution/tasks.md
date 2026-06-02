## 1. OpenSpec 与契约

- [x] 1.1 定义外部模型运行归因趋势 proposal、design、spec 和任务清单。
- [x] 1.2 通过 `openspec validate surface-external-runtime-attribution --strict`。

## 2. 后端趋势归因

- [x] 2.1 扩展 `AiQualityTrendResponse` 顶层、作业点和来源片段字段。
- [x] 2.2 调整 `AiQualityTrendService`，按 `aiInvocation` 统计模型完成、部分完成、运行失败和失败率。
- [x] 2.3 将 source segment 分组键扩展到 `runtimeMode`、`failureStage` 和 `failureReason`。
- [x] 2.4 更新 `AiQualityTrendServiceTest` 覆盖顶层、作业点和来源片段归因。

## 3. 教师端展示

- [x] 3.1 更新前端 API 类型，兼容趋势新增字段。
- [x] 3.2 在教师工作台 AI 质量趋势区展示模型失败、部分完成、作业级 badge 和来源级失败归因。

## 4. 验证

- [x] 4.1 运行后端趋势相关测试。
- [x] 4.2 运行前端 typecheck。
- [x] 4.3 运行 `git diff --check`。
- [x] 4.4 对照最近 live eval 报告说明本轮未改变外部调用行为，仅提升真实模型归因可见性。
