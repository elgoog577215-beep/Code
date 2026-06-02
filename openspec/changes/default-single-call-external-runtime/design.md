## Context

当前外接模型能力已经有两条路径：

- `staged`：先调用 `diagnosis-judge-v2`，再调用 `teaching-hint-v1`。
- `single-call`：一次调用 `diagnosis-and-teaching-v2`，同时返回诊断和教学提示。

历史 live eval 显示 DeepSeek-V4-Pro 的 staged 路径在第二阶段更容易触发额度/限流。由于 Pursue goal 的重点是“真实外部模型参与后的效果”，默认路径应优先降低请求次数，并把每次调用的模型状态沉淀为可评测信号。

## Decisions

### 1. 默认 runtime 调整为 single-call

`ai.external-runtime-mode` 的默认值改为 `single-call`：

- 生产与 live eval 默认只发一次外部模型请求。
- 仍可通过 `AI_EXTERNAL_RUNTIME_MODE=staged` 或配置项回滚 staged。
- 已有 staged 测试保留，用显式设置覆盖默认值。

### 2. aiInvocation 增加结构化归因字段

新增字段：

- `runtimeMode`: `single-call`、`staged` 或 `legacy-long-prompt`。
- `failureStage`: 如 `DIAGNOSIS_AND_TEACHING`、`DIAGNOSIS_JUDGE`、`TEACHING_HINT`、`SUBMISSION_ANALYSIS`、`GROWTH_REPORT`。
- `failureReason`: `ModelStageFailureReason` 的字符串，如 `INSUFFICIENT_QUOTA`、`RATE_LIMITED`、`SAFETY_RISK`、`INVALID_TAG`。

这样 live eval、教师端质量统计和后续运维分析不需要再从中文说明里猜测失败原因。

### 3. agent 包装必须保留外部模型归因

`DiagnosticAgentService.resolveInvocation` 会继续补 `agentVersion`，但必须保留已有 `runtimeMode`、`failureStage`、`failureReason`。如果没有外部模型记录，才使用本地规则默认值。

### 4. live eval 优先读结构化字段

`ModelDiagnosisEvalTest` 与 `AssistantLiveEvalTest` 的 failureReason 生成逻辑优先使用：

```text
status + ":" + failureStage + ":" + failureReason
```

只有旧报告没有结构化字段时，才回退解析 `uncertainty` 或 trace。

## Risks

- 默认 single-call 可能让一个请求里的输出字段更多，单次响应更长。通过保留 staged 回滚、现有 validation 和 live eval 控制风险。
- 老数据没有新增字段。读取逻辑必须兼容缺失字段。
- 当前工作区已有大量未归档 AI 安全改动。本变更只触碰外部模型 runtime 相关文件，避免扩大冲突面。

## Verification

- `openspec validate default-single-call-external-runtime --strict`
- 后端相关测试：
  - `AiReportServiceExternalRuntimeTest`
  - `DiagnosisReportReaderTest`
  - `ModelDiagnosisEvalTest`
  - `AssistantLiveEvalTest`
- 至少运行一次受控 live eval，确认当前 key 下默认 single-call 的真实模型调用状态，并记录 completed/fallback/failureReason。
- `git diff --check`
