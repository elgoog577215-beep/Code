## 1. 安全事件信号模型

- [x] 1.1 新增提示安全事件 analyzer，聚合高泄题诊断、安全降级记录和 Coach 安全风险。
- [x] 1.2 在 `AiQualityOverviewResponse` 新增 `promptSafetyIncidentSignal` 兼容字段。
- [x] 1.3 扩展 `HintSafetyCheckRepository`，支持按提交集合读取安全检查。

## 2. AI 质量概览闭环

- [x] 2.1 更新 `AiQualityOverviewService` 装配提示安全事件信号。
- [x] 2.2 更新 `AiQualityMetrics` 与质量维度，新增 `PROMPT_SAFETY_INCIDENT_LOOP`。
- [x] 2.3 保持原 `HINT_SAFETY`、高泄题计数、优先级排序和 eval readiness 兼容。

## 3. 前端类型兼容

- [x] 3.1 更新前端 API 类型，补充 `PromptSafetyIncidentSignal`。
- [x] 3.2 更新教师端维度 fallback 和优先级排序，兼容新维度。

## 4. 测试

- [x] 4.1 扩展 `AiQualityOverviewServiceTest`，覆盖安全降级、高泄题诊断和 Coach 安全风险聚合。
- [x] 4.2 覆盖 `PROMPT_SAFETY_INCIDENT_LOOP` 状态、证据引用和 improvement priority。

## 5. 验证

- [x] 5.1 运行 `openspec validate add-prompt-safety-incident-loop --strict`。
- [x] 5.2 运行 AI 质量相关后端测试。
- [x] 5.3 运行后端编译、前端 typecheck 和 `git diff --check`。
