## 设计目标

让真实模型诊断评测具备与 assistant live eval 一致的样本间冷却能力，并把冷却配置写入结构化报告，减少 429 / rate limit 噪声对质量判断的污染。

## 状态字段

`LiveModelEvalReport` 顶层新增：

- `caseDelayMs`：本次 live model eval 从 `AI_EVAL_CASE_DELAY_MS` 解析出的样本间等待毫秒数，负数或非法值按 0 处理。
- `delayedCaseCount`：本次报告中实际需要等待的 case 数。若 `caseDelayMs <= 0` 或 case 数小于等于 1，则为 0；否则为 `caseCount - 1`。

这些字段只描述评测实验条件，不参与单 case 的模型质量命中计算。

## 执行流程

`liveModelSmokeProducesPerCaseReportWhenEnabled()` 保持原有入口：

1. 读取 `AI_EVAL_CASE_DELAY_MS`。
2. 从第二个 case 开始，在调用 `DiagnosticAgentService.diagnose(...)` 前等待指定毫秒数。
3. 继续按原逻辑收集 latency、fallback、stream telemetry、model hit 和 recovery status。
4. 汇总报告时写入 `caseDelayMs` 与 `delayedCaseCount`。

等待发生在 case 之间，不计入单 case `latencyMs`，这样报告仍然表达模型调用本身耗时，而不是评测调度等待。

## 控制台摘要

`liveModelSummaryLine(...)` 追加：

- `caseDelayMs`
- `delayedCases`

这样运行真实 ModelScope smoke 时，一行摘要就能看到本次是否启用了冷却。

## 边界

- 不新增 provider 专属环境变量，复用已有 `AI_EVAL_CASE_DELAY_MS`，保持 assistant 和 model eval 一致。
- 不改变 `AI_EVAL_SMOKE_LIMIT`、baseline regression gate、quality baseline draft 生成规则。
- 不隐藏 quota / rate limit / fallback，报告仍通过 recovery status 和 blocked reasons 表达真实失败原因。
- 不输出 API Key 或 provider 原始敏感错误体。

## 验证

- OpenSpec strict validate。
- 结构测试覆盖 report 字段与 summary 文本。
- 相关后端测试通过。
- Secret scan 确认本轮文件未写入真实 token。
- `git diff --check` 确认无空白问题。
