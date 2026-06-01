## Context

教师工作台当前按选中作业加载 `aiQualityOverview`，并在 AI 质量区展示当前作业的维度告警、eval readiness 和诊断 eval 候选样本。后端已经提供 `/api/teacher/ai-quality/trend`，包含跨作业分析样本、教师校正、source segment、诊断 eval 候选，以及课堂介入 eval candidate、waiting、improved、shifted、still stuck 计数。

当前缺口是：这些跨作业趋势只存在于 API 和类型层，老师在工作台中无法发现长期质量走势，也无法知道哪些课堂介入反馈已经具备沉淀价值。

## Goals / Non-Goals

**Goals:**

- 在教师工作台 AI 质量区加载并展示跨作业 AI 质量趋势。
- 让课堂介入 eval candidate、still stuck、waiting followup 成为醒目的可行动信号。
- 展示少量 assignment point 和 source segment，帮助老师定位趋势来源。
- 保持当前作业详情、eval 候选样本和 fixture 草稿预览流程不变。
- 保持 UI 克制、紧凑、可扫描，适合教师重复使用。

**Non-Goals:**

- 不新增后端接口或数据库字段。
- 不新增独立趋势页面。
- 不在本轮做图表库或复杂可视化。
- 不改变当前作业 AI 质量的排序和候选加载行为。

## Decisions

### Decision 1: 趋势放在 AI 质量区的 summary 下方

跨作业趋势与当前作业质量属于同一教师决策流：先看当前作业是否需要处理，再看长期趋势是否需要沉淀 eval 或复盘策略。因此趋势摘要放在 AI 质量 summary 之后、details 之前，避免埋进折叠详情。

替代方案是放进 details 内部，但会降低 discoverability，老师可能只看到当前作业的质量状态而错过跨作业 still stuck 信号。

### Decision 2: 独立 loading/error，不阻塞当前作业质量

趋势 API 是跨作业视角，失败时不应影响当前作业 AI 质量。组件维护单独的 `aiQualityTrend`、`aiQualityTrendLoading`、`aiQualityTrendError` 状态，并在 `loadAll` 时读取。

### Decision 3: 使用指标条 + 最近作业点 + source segment

第一版不引入图表库，而是用当前设计系统内的紧凑指标和列表：

- 指标条：跨作业样本、诊断 eval、课堂介入、仍卡同类问题、等待后续。
- 作业点：展示最近 4 个 assignment point 的介入候选、still stuck、低置信和校正。
- source segment：展示前 3 个来源版本的样本、校正、低置信和泄题风险。

这能覆盖“规模、行动优先级、来源定位”三类教师判断，同时避免信息过载。

## Risks / Trade-offs

- [Risk] 趋势指标过多导致教师端阅读负担增加。-> Mitigation: 默认只展示摘要和少量 top items，完整诊断候选仍放在 details。
- [Risk] 趋势 API 失败造成误解。-> Mitigation: 独立错误态，明确当前作业质量仍可用。
- [Risk] 无图表时趋势感不足。-> Mitigation: 先显示可行动计数和最近作业点，后续若需要再添加折线图或 sparkline。

