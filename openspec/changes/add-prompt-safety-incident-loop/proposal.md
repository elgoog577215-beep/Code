## Why

当前系统已经能在单次诊断里识别 `answerLeakRisk=HIGH`，也能在 `HintSafetyService` 中把过度直接的提示降级并保存 `HintSafetyCheck`。但这些安全事件还没有被稳定汇总成班级级 AI 质量信号，教师很难区分“模型输出高风险”“提示已被安全降级”“Coach 回答越过证据层”等不同风险来源。

要让 AI 成为可验证的教育 agent，提示安全不能只停留在一次拦截或一次兜底文案；它需要形成可统计、可追溯、可进入评测的安全事件闭环。

## What Changes

- 新增提示安全事件分析器，按作业聚合诊断高泄题风险、`HintSafetyCheck` 安全降级记录和 Coach 回答安全风险。
- `AiQualityOverviewResponse` 新增兼容字段 `promptSafetyIncidentSignal`，输出状态、风险计数、主要风险来源、summary、recommendedAction 和 evidenceRefs。
- AI 质量概览新增 `PROMPT_SAFETY_INCIDENT_LOOP` 维度，让提示安全事件进入优先级排序和教师改进建议。
- `HintSafetyCheckRepository` 新增按提交集合查询能力，支持作业范围安全事件聚合。
- 前端 API 类型补充兼容字段和维度名称，教师端可读取新安全闭环信号。
- 增加后端测试覆盖安全降级记录、高泄题风险诊断、Coach 越界回答和 AI 质量维度证据。

## Capabilities

### New Capabilities

- `prompt-safety-incident-loop`: 覆盖提示安全事件如何从单次拦截沉淀为作业级结构化信号、教师行动建议、证据引用和回归验证。

### Modified Capabilities

无。

## Impact

- 后端：新增安全事件 analyzer，更新 `AiQualityOverviewService`、`AiQualityMetrics`、`AiQualityOverviewResponse`、`HintSafetyCheckRepository` 和相关测试。
- DTO/API：AI 质量概览新增兼容字段 `promptSafetyIncidentSignal`。
- 前端：更新 `types.ts` 和教师端维度 fallback/排序，兼容新质量维度。
- 数据：无数据库迁移；复用现有 `SubmissionAnalysis`、`HintSafetyCheck` 和 `CoachPrompt`/Coach answer quality 信号。
