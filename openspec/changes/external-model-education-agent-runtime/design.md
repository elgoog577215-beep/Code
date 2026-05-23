## Context

当前项目的 AI 诊断链已经具备本地规则信号、诊断标准库、证据包、学生提示计划、学习轨迹和离线评测集。它的问题不在于“完全没有 agent”，而在于外部大模型调用仍主要由 `AiReportService` 通过一个长 prompt 一次性完成错因分析、学生提示、教师备注、学习干预、行级问题和报告生成。

live eval 已暴露出两个关键事实：第一，ModelScope API 可以连通；第二，长 prompt 调用在 DeepSeek-V4-Pro 上会出现高延迟、超时和规则回退。对于学校和政府场景，外部模型调用必须可控、可解释、可降级、可评测，不能把模型慢或不稳定简单归因给基础模型。

本设计借鉴成熟 agent 产品和框架的共同做法：优先使用可预测 workflow，而不是完全自主 agent；把上下文压缩成任务输入；用标准库约束模型输出；每个阶段都有 trace、校验和回退；教师校正进入反馈闭环。

## Goals / Non-Goals

**Goals:**

- 将外部模型调用拆成可控的教育诊断 workflow，降低单次 prompt 复杂度。
- 新增 `ModelDiagnosisBrief`，让模型只看到完成当前任务必要的题目摘要、关键代码、评测事实、候选信号和允许标签。
- 新增 `StandardLibraryPack`，按候选错因裁剪标准库，而不是把完整标签列表硬编码进 prompt。
- 将模型任务拆成至少两个阶段：错因裁决和教学表达。
- 对每个模型阶段执行 JSON、标签、证据引用和泄题风险校验。
- 生成 live model eval report，逐条记录模型真实表现，区分模型完成和规则兜底。
- 保留本地规则 agent 作为兜底，确保外部 API 异常时学生仍能得到基础提示。

**Non-Goals:**

- 不训练或微调基础大模型。
- 不引入完全自主多 agent 系统。
- 不改变前端主要交互流程。
- 不把教师校正自动写入主标准库；教师校正先作为评测和候选规则来源。
- 不在本变更中承诺某个特定外部模型一定满足生产 SLA。

## Decisions

### Decision 1: 使用 workflow-first，而不是单 prompt 或全自主 agent

采用固定流程：

```text
评测事实
-> 本地证据提取
-> ModelDiagnosisBrief
-> StandardLibraryPack
-> 阶段 A：错因裁决
-> 程序校验
-> 阶段 B：教学表达
-> 安全校验
-> 诊断结果合成
-> trace 与 eval report
```

理由：教育诊断需要稳定和可解释，workflow 能明确每一步的责任和失败边界。全自主 agent 会增加不可控性；单 prompt 则已经被 live eval 证明容易超时且难定位问题。

备选方案：继续优化当前长 prompt。拒绝原因是它会让提示词、标准库、行级分析、教学表达和报告生成继续耦合，后续难以评测和回滚。

### Decision 2: 模型先做裁决，再做表达

阶段 A 只输出结构化裁决：

- `primaryIssueTag`
- `fineGrainedTag`
- `evidenceRefs`
- `confidence`
- `uncertainty`
- `needsMoreEvidence`

阶段 B 使用阶段 A 的结果生成：

- `studentHint`
- `studentHintPlan`
- `learningInterventionPlan`
- `teacherNote`
- `answerLeakRisk`

理由：错因判断和教学表达是不同任务。拆开以后，阶段 A 更短、更容易评测；阶段 B 不需要重新读完整上下文，只需要围绕已校验结论生成教学语言。

### Decision 3: 标准库按需裁剪后注入

新增 `StandardLibraryPackBuilder`，根据规则信号、题目知识点、教师校正和 verdict 构造小型标准库包。模型每次只看到相关候选项，例如 3 到 6 个粗粒度错因、对应细粒度标签、教学动作、常见误区和禁止泄题规则。

理由：完整标签列表放进 prompt 会增加上下文噪声，也让模型更容易自由发挥。裁剪包能提升约束力，并让每次模型选择的边界可审计。

### Decision 4: 输入上下文使用 brief，而不是完整对象

新增 `ModelDiagnosisBriefBuilder`，将输入压缩为：

- 题目摘要和必要约束。
- 带行号的关键代码片段。
- verdict、编译/运行错误、首个失败样例或可见输出差异。
- 本地规则候选信号和证据引用。
- 学习轨迹摘要。
- 禁止暴露的隐藏数据说明。

理由：模型不应重新处理完整业务对象。brief 让模型专注于裁决，降低 token、延迟和格式失败概率。

### Decision 5: live eval 从“测试是否通过”升级为“逐条质量报告”

新增 live eval report，记录：

- `caseId`
- `model`
- `promptVersion`
- `stage`
- `latencyMs`
- `status`
- `fallbackUsed`
- `jsonValid`
- `expectedIssueTagHit`
- `expectedFineTagHit`
- `evidenceValid`
- `safetyPassed`
- `failureReason`

理由：批量外部模型评测可能超时、限流或部分失败。只用 JUnit 成败会丢失诊断信息。逐条报告能支持后续模型选择、prompt 迭代和教师端质量分析。

### Decision 6: 失败回退必须显式记录

外部模型阶段失败时，系统继续使用本地规则结果，但必须写入 `aiInvocation.status=RULE_FALLBACK`、`fallbackUsed=true` 和明确失败原因。live eval 中 fallback 不能计为模型成功。

理由：本地规则兜底是产品稳定性能力，不是外部模型质量。混淆两者会误导评测。

## Risks / Trade-offs

- 模型阶段拆分会增加调用次数 -> 通过阶段 A 成功后才触发阶段 B、缓存阶段 A、支持配置开关控制。
- 标准库裁剪错误可能限制模型发现真实错因 -> 裁剪包必须包含 `NEEDS_MORE_EVIDENCE`，并允许模型在证据充分时选择标准库内的非候选标签，但必须说明证据。
- live eval 成本增加 -> 默认只跑小样本 smoke；完整评测需要显式开启并输出报告。
- brief 过度压缩导致证据不足 -> brief builder 必须保留首个失败样例、关键代码行、rule signal reason 和不确定性字段。
- 外部模型仍可能超时或限流 -> 支持超时配置、失败原因记录、规则兜底和后续模型路由。
- 改动跨多个后端模块 -> 通过新增 runtime 组件渐进接入，保留旧 `AiReportService` 路径作为回滚方案。

## Migration Plan

1. 新增数据结构和 builder，不改变现有 API。
2. 新增 prompt registry、阶段 A/B 模板和输出 validator。
3. 新增 runtime，以配置开关接入 `DiagnosticAgentService` 或 `AiReportService`。
4. 新增 live eval report，并先用小样本验证外部模型调用链。
5. 保留原有本地规则和旧模型增强路径，必要时通过配置回滚。

## Open Questions

- 默认配置已按当前产品选择切回 `deepseek-ai/DeepSeek-V4-Pro`，并默认使用 `stream=true` 协议；上线前仍需要用 live eval 报告验证额度、延迟、回退率和稳定性。
- 阶段 B 是否必须每次调用，还是仅在阶段 A 低置信度或教师端需要时调用，需要根据延迟与成本确定。真实 small live eval 已显示阶段 A 可通过流式协议完成，但阶段 B 在当前账号下触发 `INSUFFICIENT_QUOTA`，因此阶段 B 条件触发和缓存策略应优先评估。
- 教师校正如何进入标准库审核流程，本变更只要求进入 eval 和候选规则，不直接自动合并。

## Online Integration Update

本变更的下一步目标不是继续增强离线规则效果，而是让真实外部模型调用优先走新 runtime。生产路径应按以下策略推进：

1. 当 `ai.external-runtime-enabled=true` 时，提交诊断的外部模型增强优先使用 `ExternalModelAgentRuntime` 准备 brief、standard library pack 和两阶段 prompt。
2. 阶段 A 调用 `diagnosis-judge-v1`，只让模型裁决错因、细粒度错因、证据、置信度和不确定性。
3. 阶段 A 通过校验后，阶段 B 调用 `teaching-hint-v1`，只生成学生提示、提示计划、学习干预计划和教师备注。
4. 阶段 A/B 任一失败时，返回本地规则结果，并写入 `MODEL_RUNTIME_FALLBACK` 或等价状态；live eval 必须把它计为 fallback。
5. 旧长 prompt 路径只在 `ai.external-runtime-enabled=false` 时保留为兼容路径，不再作为默认优化方向。
