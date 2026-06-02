## 设计目标

baseline regression report 表达“当前 report 对比上一份 baseline 的结果”。它现在已有：

- baseline/current report path
- case 覆盖数
- violations
- final/model/fallback hit counts

但它缺少 recovery status。对于 live-model-eval，这会让 429、quota、provider failure 与真正模型质量退化混在一起。

## 字段设计

新增字段：

- `currentRecoveryStatus`
- `currentRecoveryCheckCount`
- `currentRecoveryPassedCheckCount`
- `currentRecoveryBlockedReasonCount`
- `currentRecoveryBlockedReasons`

不复制 `recoveryPassedChecks`，因为 regression report 的主要消费场景是快速判定是否可比；通过检查列表可在 current report 中查看。这里保留 blocked reasons，优先服务“为什么本次不可比/仍阻塞”。

## 填充规则

- `fromModel(...)` 且 current 不为空时，从 `LiveModelEvalReport` 复制字段。
- `currentRecoveryBlockedReasons` 做 defensive copy；null 时为空列表。
- `fromAssistant(...)` 不填这些字段，避免 assistant 多助手报告与 model diagnosis recovery status 混用。

## 边界

- 不改变 baseline gate 的 pass/fail 规则。
- 不根据 recovery status 自动跳过 violations；本轮只增强报告解释力。
- 不写入 API Key、provider 原始 body 或完整模型输出。

## 验证

- OpenSpec strict validate。
- 结构测试覆盖 BLOCKED recovery status 被复制到 regression report。
- 真实 live smoke + baseline regression report 验证当前 429 会写出 `currentRecoveryStatus=BLOCKED`。
- Secret scan 和 diff check。
