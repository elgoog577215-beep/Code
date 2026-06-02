## Context

`AiQualityTrendService` 当前按作业聚合 submission、analysis、teacher correction、class review feedback 和 `HintSafetyCheck`。提示安全趋势由三项组成：高泄题诊断、提示安全降级、提示安全高风险降级。Coach 安全拒绝虽然已经在 `CoachInteractionAnalyzer` 和作业概览中可见，但趋势服务没有读取 `CoachPrompt`。

## Goals / Non-Goals

**Goals:**

- 在跨作业趋势中统计 Coach 模型追问安全拒绝次数。
- 保持 `promptSafetyIncidentCount` 作为总安全事件数，并新增 `coachSafetyRejectionCount` 作为可解释子项。
- 在 assignment point 和 source segment 上都暴露 Coach 安全拒绝计数。
- 教师端展示 Coach 安全回退，让趋势卡片能直接提示模型追问安全问题。

**Non-Goals:**

- 不新增新的趋势页面或图表组件。
- 不改变 Coach 安全拒绝的落库结构。
- 不把 Coach 安全拒绝与 HintSafetyCheck 混为同一个降级指标。

## Decisions

### 新增独立字段

新增 `coachSafetyRejectionCount`，而不是只把它累加进 `promptSafetyIncidentCount`。这样既保留总安全事件率，又能区分来源，避免教师误以为所有安全事件都来自学生提示或诊断提示。

### 按 submissionId 归属到作业与 source segment

趋势服务已经有 submissionId 到 analysis/source segment 的映射。Coach prompt 按 submissionId 读取后，作业点按作业提交集合过滤；source segment 按同 submission 的分析来源归类，缺少分析时归到 `UNKNOWN|unknown`。

### 前端复用现有趋势卡片

教师端只新增一个紧凑指标和 assignment badge，不引入新面板。趋势仍保持扫描式工作台风格。

## Risks / Trade-offs

- [Risk] 同一提交多轮 Coach 安全拒绝会按 prompt 次数计数。→ 这是有意选择，趋势关注安全门触发频度；草稿导出仍按 submission 合并。
- [Risk] 无 analysis 的 Coach prompt 只能归到 UNKNOWN source segment。→ 保证事件不丢失，同时不伪造模型来源。
