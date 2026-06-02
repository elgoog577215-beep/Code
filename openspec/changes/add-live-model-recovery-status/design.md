## 状态模型

新增顶层字段：

- `recoveryStatus`
- `recoveryCheckCount`
- `recoveryPassedCheckCount`
- `recoveryBlockedReasonCount`
- `recoveryPassedChecks`
- `recoveryBlockedReasons`

状态含义：

- `RECOVERED`：当前报告至少有一个 entry 满足恢复 smoke 的全部关键条件。
- `BLOCKED`：报告中存在 recovery smoke recommended draft 或 runtime failure entry，但没有任何 entry 满足恢复条件。
- `NOT_APPLICABLE`：当前报告没有恢复验证上下文，例如普通结构测试或没有 runtime failure / recovery smoke recommended draft。

## 恢复成功条件

对单个 entry，必须同时满足：

- `modelCompleted=true`
- `fallbackUsed=false`
- `modelIssueTagHit=true` or `modelFineTagHit=true`
- `evidenceValid=true`
- `safetyPassed=true`

如果 entry 的 `transportMode=stream`，还必须满足：

- `streamContentChunkCount>0`

这些条件与上一轮 `recoverySmokeRequiredChecks` 保持同语义，让建议和结果闭环。

## 阻塞原因

当没有 entry 满足恢复条件时，报告输出去重后的 blocked reasons。优先从 runtime fixture draft 与 entry 生成：

- `recovery smoke pending: <caseId>`：有 recovery smoke recommendation，但本次仍未通过。
- `<caseId>: runtime fallback`
- `<caseId>: model not completed`
- `<caseId>: missing model hit`
- `<caseId>: missing evidence`
- `<caseId>: safety failed`
- `<caseId>: stream content chunk missing`
- `<caseId>: <failureReason>`：保留截断后的运行失败原因，避免只看到泛化原因。

## 控制台摘要

`liveModelSummaryLine(...)` 追加：

- `recoveryStatus`
- `recoveryBlockedReasons`

这样真实 smoke 返回 429 时，一行摘要就能看到外部模型仍未恢复。

## 边界

- 本轮不改变 baseline gate 的 pass/fail 规则。
- 本轮不把 recovery status 接入教师端质量趋势；先在 live eval 报告稳定产出。
- 不读取 API Key，不输出 provider 原始完整错误体。

## 验证

- OpenSpec strict validate。
- 无 API Key 结构测试覆盖 RECOVERED、BLOCKED、NOT_APPLICABLE。
- 真实 live smoke 当前 429 应输出 `recoveryStatus=BLOCKED`，blocked reasons 包含 runtime fallback / quota。
- Secret scan 与 diff check。
