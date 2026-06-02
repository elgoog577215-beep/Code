## Why

Coach 安全拒绝现在已经能进入单个作业的 Coach 质量汇总，也能导出为安全 eval 草稿。但跨作业 AI 质量趋势仍只统计诊断高泄题风险和提示安全降级，没有把 Coach 模型追问被安全门拒绝纳入时间维度。

教师需要知道安全问题是在改善还是变差，尤其是模型追问安全门是否频繁触发。把 Coach 安全拒绝接入趋势层，可以让教师在跨作业视角中区分“学生提示安全风险”“提示降级事件”和“Coach 模型安全回退”。

## What Changes

- `AiQualityTrendService` 读取作业提交对应的 CoachPrompt，统计 `modelFailureReason=SAFETY_REJECTED`。
- `AiQualityTrendResponse`、作业趋势点和来源分段新增 `coachSafetyRejectionCount`。
- `promptSafetyIncidentCount` 保持安全事件总数语义，新增 Coach 安全拒绝计数会纳入总安全事件。
- 教师端 AI 趋势卡片展示 Coach 安全回退总数，并在作业趋势点上显示对应 badge。
- 扩展趋势测试，覆盖跨作业、单作业和 source segment 的 Coach 安全拒绝统计。

## Capabilities

### New Capabilities

- `coach-safety-trend-signal`: 覆盖 Coach 模型安全拒绝事件在 AI 质量趋势中的统计、响应字段和教师端展示。

### Modified Capabilities

- 无。

## Impact

- 后端服务：`AiQualityTrendService` 增加 `CoachPromptRepository` 依赖和趋势计数。
- 后端 DTO：`AiQualityTrendResponse` 增加兼容字段。
- 前端类型与教师页：新增安全回退指标展示。
- 测试：扩展 `AiQualityTrendServiceTest`，前端运行 typecheck。
