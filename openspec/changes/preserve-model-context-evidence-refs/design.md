## Context

当前外部模型 runtime 已经通过 `ModelDiagnosisBrief` 和 `StandardLibraryPack` 限制模型只能引用 brief 中的证据。模型输出合法并通过校验后，`AiReportService` 会生成新的 `SubmissionAnalysisResponse`。随后 `DiagnosticAgentService` 做标签归一化、低置信保护、教师校准、学习轨迹和提示安全处理。

真实 baseline 对比显示：模型成功输出时，`analysis.getEvidenceRefs()` 可能只保留模型主动选择的部分 refs，而 rule signal 或 evidence package 中的稳定上下文 refs 可能不再出现在最终 response 中。

## Goals / Non-Goals

**Goals:**

- 外部模型成功后保留稳定上下文证据 refs。
- 让 live eval report 的 `actualEvidenceRefs` 更稳定，能支持 baseline regression gate。
- 不改变模型调用、prompt、标签选择或 fallback 语义。
- 不把隐藏测试输入输出、密钥或 provider 原始错误写入证据 refs。

**Non-Goals:**

- 不要求模型逐条引用所有 evidence package 细节。
- 不让 baseline gate 接收 fixture 上下文做隐式放行。
- 不改变生产 API schema。

## Decisions

### 合并在 DiagnosticAgentService 完成

`DiagnosticAgentService` 同时拥有最终分析、`RuleSignalResult` 和 `DiagnosisEvidencePackage`，是保留上下文证据的最小位置。这样无论外部模型 runtime 是单阶段还是多阶段，最终 response 都经过同一个稳定证据保留步骤。

### 只合并证据引用，不覆盖模型判断

本变更只补充 `evidenceRefs`，不把 rule signal 的标签重新强行覆盖模型输出。模型的标签仍经过标准库校验、taxonomy 归一化和现有校准逻辑。

### evidence package 摘要证据作为上下文来源

使用 `DiagnosisEvidencePackageReader.summarizePersisted` 同等语义的证据来源，至少可保留 `verdict:*`、`judge:first_failed_case:*`、`judge:hidden_failure`、`problem:*` 等稳定 refs。对于 report 体积，沿用去重并限制数量。

## Risks / Trade-offs

- [Risk] refs 数量增加，报告不够聚焦。→ 限制最大数量，模型输出 refs 排在前面。
- [Risk] rule signal evidence 太宽泛。→ 只合并 refs，不据此改变质量命中；baseline gate 仍要求具体 mustKeep。
- [Risk] 隐藏测试证据误泄露。→ evidence refs 只包含标识符，不包含隐藏输入输出；现有 evidence package 构建已承担隐藏数据保护。
