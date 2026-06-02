## Context

现有 baseline regression report 有两类信号：

- `status`：baseline gate 是否发现 violations。
- `currentRecoveryStatus`：当前 live-model-eval 是否完成 recovery checks。

最新真实报告显示：

- `status=PASSED`
- `comparedCaseCount=0`
- `currentRecoveryStatus=BLOCKED`
- `currentModelIssueTagHitCount=0`
- `currentFallbackIssueTagHitCount=1`

这说明 gate 没发现 regression，但当前报告并不能证明外部模型质量稳定，因为命中来自 fallback，真实模型没有完成。

## Goals / Non-Goals

**Goals:**

- 明确区分 pass/fail 与 comparability。
- 在 report 中输出机器可读的可比性状态和原因。
- 保留 blocked reasons，帮助维护者知道是 quota、stream、model hit 还是 case 覆盖导致不可比。

**Non-Goals:**

- 不改变 baseline gate violations 计算。
- 不让 recovery blocked 自动导致 `status=FAILED`。
- 不读取或输出 API Key、provider 原始响应体、raw prompt 或 raw response。

## Decisions

### 1. 新增 comparabilityStatus，而不是复用 status

`status=PASSED` 仍表示没有 baseline violation；`comparabilityStatus=NOT_COMPARABLE` 表示这次结果不能用于判断真实外部模型质量。两者并列，避免破坏现有 CI 或脚本。

### 2. 状态枚举

- `COMPARABLE`：有可比 case，且当前 recovery 未阻塞。
- `PARTIAL`：有部分对比，但存在覆盖不足或 recovery 风险。
- `NOT_COMPARABLE`：没有可比 case，或当前 recovery blocked，或模型命中为 0 且 fallback 命中存在。

### 3. 原因生成规则

原因使用短字符串，不包含密钥和 provider 原始 body：

- `no compared cases`
- `current recovery blocked`
- `model hits missing; fallback hits present`
- `current report missing`
- `baseline missing`
- `violations present`

若 recovery blocked reasons 已存在，追加前几条具体 blocked reason。

## Risks / Trade-offs

- [Risk] 旧脚本只看 `status`，仍可能忽略 comparability。→ 保持字段名明确，并在真实报告里输出。
- [Risk] PARTIAL 与 NOT_COMPARABLE 边界过严。→ 对 recovery blocked 和 model hit 缺失采取严格不可比；对覆盖不足但已有比较保留 PARTIAL。
- [Risk] assistant live eval 没有 model recovery 状态。→ assistant report 仍输出 comparability，但只基于 case 覆盖和 violations。
