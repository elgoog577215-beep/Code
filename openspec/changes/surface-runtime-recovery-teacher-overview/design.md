## Context

前序变更已经形成三层运行归因：

- 生产 `aiInvocation` 记录 status、failureStage、failureReason、runtimeMode 和 stream telemetry。
- runtime fixture draft 输出 recovery smoke 建议和 required checks。
- live-model-eval report / baseline regression report 输出 `recoveryStatus` 和 blocked reasons。

当前缺口是教师端 AI 质量概览只展示主导失败类型和推荐动作，没有把“恢复验证是否仍阻塞”变成结构化状态。这样教师看到 `MODEL_RUNTIME_FALLBACK` 时仍不知道系统是在等待额度恢复、stream 没有 content chunk，还是已经有新的外部模型完成样本证明恢复。

## Goals / Non-Goals

**Goals:**

- 在 `runtimeAttributionSignal` 中加入可机器读取的 recovery 状态。
- 从当前作业真实诊断样本推导恢复状态，不依赖本地评测报告文件。
- 保持与 live eval recovery checks 同语义：完成、未 fallback、模型命中、证据有效、安全通过，stream 场景必须有 content chunk。
- 为教师和维护者输出安全的阻塞原因与下一步动作，不包含密钥、raw response 或 provider 原始 body。

**Non-Goals:**

- 不改变外部模型调用、fallback、预算保护或重试策略。
- 不新增数据库字段。
- 不把 `target/ai-eval-reports/*.json` 当成生产数据源。
- 不在本轮改变 baseline gate 的 pass/fail 规则。

## Decisions

### 1. recovery 状态由当前作业样本实时派生

`AiQualityOverviewService` 遍历当前作业的 `SubmissionAnalysis`，从 `aiInvocation`、`issueTags`、`fineGrainedTags`、`evidenceRefs` 和 `answerLeakRisk` 推导恢复状态。这样教师看到的是当前作业在线样本的外部模型状态，而不是开发机上某次 eval 的快照。

### 2. 与 live eval 使用相同状态语义

状态值保持三类：

- `RECOVERED`：存在恢复上下文，并且至少一个样本满足全部恢复检查。
- `BLOCKED`：存在恢复上下文，但没有样本满足恢复检查。
- `NOT_APPLICABLE`：当前作业没有运行失败、部分完成或 recovery smoke 推荐上下文。

恢复检查包括：

- `status=MODEL_COMPLETED`
- `fallbackUsed=false`
- `modelHit=true`，即 `issueTags` 或 `fineGrainedTags` 非空
- `evidenceRefs present`
- `answerLeakRisk != HIGH`
- stream 样本追加 `streamContentChunkCount>0`

生产端没有 live eval expected tag，因此使用“模型产生结构化错因标签”作为 model hit 的在线近似。

### 3. recovery smoke 推荐只输出安全元信息

当 `recoveryStatus=BLOCKED` 时，信号输出：

- `recoverySmokeRecommended=true`
- `recoverySmokeCaseId=submission:<id>`
- `recoverySmokeRuntimeProfile=<runtimeMode 或 low-latency>`
- `recoverySmokeRequiredChecks`
- `recoveryBlockedReasons`

不输出 API Key、headers、raw prompt、raw response 或完整 provider 错误体。`failureReason` 进入 blocked reason 前只保留已有归一化短码。

### 4. MODEL_RUNTIME 维度消费 recovery 状态

运行归因的 summary 和 recommendedAction 会追加 recovery 状态说明。例如 quota + stream 无内容时，动作不仅说检查 ModelScope 额度，还会说明“当前恢复 smoke 仍阻塞，需要先用单条外部模型样本验证 content chunk 恢复”。

## Risks / Trade-offs

- [Risk] 生产样本没有 expected tag，无法像 live eval 那样验证“命中预期错因”。→ 使用结构化 `issueTags/fineGrainedTags` 非空作为在线 model hit 近似，并在文案里称为恢复证据而不是质量胜利。
- [Risk] 同一作业同时有失败和成功样本，状态可能显示 `RECOVERED` 但仍存在失败。→ 保留 failure count、primary failure 和 blocked reason 证据，recommendedAction 仍提示继续观察失败样本。
- [Risk] 老数据缺少 stream telemetry。→ 只有 `transportMode=stream` 时才要求 content chunk；老数据不会被误判为 stream 缺块。
- [Risk] 教师端信息过密。→ 本轮先在 API 和质量维度 summary/action 中结构化输出，前端可逐步精炼展示。
