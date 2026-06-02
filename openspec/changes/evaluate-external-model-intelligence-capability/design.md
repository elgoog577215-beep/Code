## Context

项目已经有 100 条复杂学生提交样本、24 条 complex live 候选和 6 个复杂质量指标。上一轮把这些指标接入了 report 与 baseline gate，但它仍偏向“本地质量门禁”：只说明某个输出是否满足 fixture 真值，还没有把“外接模型真实完成的智能诊断能力”从 fallback、本地规则和运行恢复能力中独立出来。

本轮把评测主语切换为外接模型：本地 fixture 只提供真值，AI 能力分只来自 `modelCompleted=true` 且 `fallbackUsed=false` 的输出。

## Goals / Non-Goals

**Goals:**

- 让 live model eval 报告能直接回答：外接模型是否真正具备复杂错因自主分析能力。
- 固定 14 条代表性 complex live baseline，覆盖 14 类 bugPattern。
- 将现有 6 个 complex metric 映射为面向 AI 能力的 intelligence metrics。
- 报告中显式区分 local truth、model output、model judgment、quality score 和 provider/runtime 状态。
- 强化提示协议，让模型在复杂多错因中先做主错因优先级判断，并忽略干扰信号。

**Non-Goals:**

- 不把本地规则、fallback 草稿或离线 profile 计入 AI 能力分。
- 不新增数据库字段或前端展示。
- 不一次性扩展到多模型自动比较；本轮只预留字段。
- 不用另一个外接模型作为裁判模型。

## Decisions

- intelligence report 放在测试评测侧，复用 `LiveModelEvalReport` 新增可空字段，避免打破旧报告。
- 只对 complex case 计算 intelligence metrics；非 complex live core 仍保留旧标签/证据/运行质量统计。
- 每条 complex entry 记录：
  - `localTruth`：来自 fixture 的主错因、教学优先级、必需证据、干扰信号。
  - `modelOutput`：模型实际标签、证据引用和输出摘要。
  - `modelJudgment`：是否真实模型完成、是否 fallback、是否命中能力指标。
  - `qualityScore`：只统计真实模型完成样本的 AI intelligence 分数。
- 14 条代表集通过 case id 前缀 `complex-live-01` 到 `complex-live-14` 固定，不依赖文件顺序外的随机选择。
- intelligence metrics 复用现有 complex metric 的可计算结果，命名上转成产品语义：
  - `autonomousRootCauseDiscovery` <- `primaryRootCauseHit`
  - `complexSignalPrioritization` <- `secondaryIssuesNotOverweighted`
  - `evidenceGroundedReasoning` <- `evidenceGrounded`
  - `teachingDecisionQuality` <- `teachingPriorityCorrect`
  - `distractorResistance` <- `distractingSignalsIgnored`
  - `modelSafetyAndBoundary` <- `noFullSolutionLeak`
- fallback 或异常样本仍进入运行报告，但 intelligence completed count 与 score 均不包含它们。

## Risks / Trade-offs

- [Risk] 真实 live eval 受配额、延迟和 provider 状态影响 -> 报告同时记录 provider/runtime 状态，并把 fallback 与 AI 能力分离。
- [Risk] 严格证据引用可能导致初期分数偏低 -> 这是能力瓶颈信号，不放宽为泛泛解释命中。
- [Risk] prompt 协议变长影响请求大小 -> 只增加复杂诊断优先级规则，不扩展输出 schema。
- [Risk] 14 条样本不是统计意义上的大样本 -> v1 用作可审计 baseline，后续再扩大多模型、多轮比较。
