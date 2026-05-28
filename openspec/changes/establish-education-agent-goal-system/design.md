## Context

当前项目的教育 AI agent 已经不是单一 prompt：它包含题目、提交、评测结果、规则信号、标准库、外部模型 runtime、输出校验、学生学习记忆、Coach 追问、成长报告和教师端统计。最近一次真实 live eval 暴露了两个问题：

- ModelScope 外部模型存在额度或限流风险，导致大量 `MODEL_RUNTIME_FALLBACK`。
- 旧报告容易把本地兜底命中误读成外部模型质量命中。

因此，本轮目标系统不能只写“准确率高”，必须把外部模型完成率、诊断质量、学生改善、教师价值和安全边界拆成分层指标，并让评测报告能持续显示目标缺口。

## Goals / Non-Goals

**Goals:**

- 建立一个足够远大的北极星目标：学生下一次更会改，教师下一节更会教，系统越用越懂学生。
- 建立第一阶段可执行目标：外部模型稳定完成、长代码评测集、主错因命中、细粒度错因命中、教学动作、安全、fallback 口径。
- 在 live eval 报告中加入目标快照和目标缺口，让每次评测都能回答“离目标还差多少”。
- 保持现有主链路，不另建评测系统、记忆系统或教师统计旁路。
- 给后续阶段留下任务入口：学生下一次提交改善率、教师端共性错因聚类、教师修正反哺标准库。

**Non-Goals:**

- 不在本轮一次性生成 100/500/1000 条评测样本。
- 不把外部模型换成新的供应商，也不把配额问题伪装成 prompt 问题。
- 不新增数据库表，不改生产接口，不重写教师端页面。
- 不用外部模型当裁判模型；评测仍以 fixture、老师期望和规则 rubric 为主。

## Decisions

### 1. 目标系统分五层，而不是只设一个准确率

采用五层目标：

```text
北极星目标
-> 第一阶段：外部模型诊断可用
-> 第二阶段：学生学习效果可见
-> 第三阶段：教师教学价值可见
-> 长期成熟化：数据、模型、反馈闭环规模化
```

理由：单一准确率会把教育 agent 降级成分类器，忽略学生是否真的改善、教师是否能用、系统是否能沉淀。

### 2. live eval 报告加入目标快照

`AssistantLiveEvalReport` 新增 `goalSnapshot`，包含：

- `phase`
- `externalCompletionRate`
- `runtimeFailureRate`
- `signalHitRate`
- `evidenceValidRate`
- `safetyPassRate`
- `teachingActionValidRate`
- 对应目标值
- `goalGaps`
- `coverageGaps`
- `nextOptimizationFocus`
- `nextAction`

这样报告不会只给计数，还会直接指出当前最大短板。例如外部模型完成率低于 90%，应优先处理模型额度、限流、低预算路径或供应商配置，而不是盲目改 prompt。

同时报告新增 `sampleProfile`，记录本次评测覆盖了哪些助手类型、case id、诊断样本数和 20 行以上长代码诊断样本数。这样 3 条 smoke 全通过只能说明链路修复有效；只有覆盖数量达到目标底线时，才能把结果作为更强的整体质量证据。

### 3. live eval 默认使用低预算单次调用

生产配置仍保持 `staged` 默认值，便于分阶段定位诊断和教学表达问题。但 live eval 的默认目标是先验证外部模型在线可用性和完成率，因此测试侧默认使用 `single-call`，减少每条提交从两次模型请求变成一次模型请求。需要深度分阶段排查时，可以显式设置 `AI_EVAL_EXTERNAL_RUNTIME_MODE=staged`。

### 4. live eval 共享容量门禁

同一轮 live eval 里的 `AiReportService` 和 `CoachAgentService` 使用同一个 `ExternalModelBudgetGuard`。一旦某个助手遇到 `INSUFFICIENT_QUOTA` 或 `RATE_LIMITED`，后续调用会快速记录为 `BUDGET_GUARD_OPEN`，避免 10 条或 100 条评测继续消耗无效请求。报告新增 `failureReasonCounts`，让容量问题、限流问题和模型输出质量问题分开看。

报告还新增 `routeProfile`，记录主路由和备用 OpenAI-compatible 路由是否配置。如果运行失败率高且没有备用路由，`nextOptimizationFocus` 应指向 `MODEL_ROUTE_CONFIGURATION`，而不是继续要求调 prompt。

### 5. fallback 仍记录文本，但不计入模型质量

本地兜底代表系统韧性，不代表外部模型能力。完成率和运行失败率以全部请求样本为分母；信号命中率、证据有效率和教学动作有效率以 `completedOutput=true` 的模型输出为分母。fallback 条目保留 `outputDetail` 和失败原因，方便人工复盘，但不会把 quota、限流或超时误归因为 prompt、标准库或 validator 质量问题。

### 6. 长代码样本作为外部模型诊断底线

外部模型诊断能力不能用 5 行玩具代码评估。第一阶段要求 live eval 中至少有 10 条 20 行以上诊断样本；目标是逐步扩展到 100 条高质量长代码样本。

### 7. 学生改善率和教师价值先进入目标文档，后进入实现

学生下一次提交改善率、教师端共性错因聚类、教师修正反哺标准库都很重要，但跨越提交历史、课堂业务和教师端统计。本轮先写成 spec 与任务拆解，避免半截实现造成旁路；后续单独按主链路接入。

## Risks / Trade-offs

- [Risk] 指标过多导致执行发散。
  Mitigation：第一阶段只落地 live eval 目标快照和长代码门槛，后续阶段按 OpenSpec 逐步推进。

- [Risk] 外部模型配额不足导致无法验证质量。
  Mitigation：报告区分运行失败和质量失败，先优化完成率，再判断 prompt 和标准库质量。

- [Risk] 目标值过早写死。
  Mitigation：目标值作为阶段门槛，可通过环境变量覆盖；默认值用于持续回归和趋势判断。

- [Risk] 只看评测集导致过拟合。
  Mitigation：评测集必须持续引入教师修正、真实学生提交和隐藏变体，报告保留老师期望和迭代建议。

## Migration Plan

1. 新增 OpenSpec 目标系统文档和 spec。
2. 扩展 live eval report，加入目标快照与目标缺口。
3. 补充 quality gate 测试，验证 fallback 不计质量、目标缺口可解释。
4. 更新项目记忆，作为后续 AI 优化的默认方向。
5. 后续按目标系统继续推进 100 条长代码评测集、学生改善率和教师端价值指标。
