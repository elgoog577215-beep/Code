## Why

真实 ModelScope assistant live eval 的 baseline regression gate 抓到一类新问题：外部模型诊断 3/3 完成、无 fallback、错因标签和教学动作都命中，但当前报告缺少上一份成功 baseline 中的 `verdict:wrong_answer` 这类稳定判题证据引用。该证据并不是模型凭空生成的内容，而是 evidence package 和 rule signal 已经提供的上下文事实。

如果模型成功输出后覆盖掉部分稳定上下文证据，教师端可解释性会变弱，后续 baseline 回归也会把“模型表达差异”和“系统证据丢失”混在一起。本变更让外部模型成功后的诊断结果继续保留规则/判题上下文证据。

## What Changes

- `DiagnosticAgentService` 在外部模型增强后合并模型输出证据、rule signal 证据和 evidence package 摘要证据。
- 合并后的证据继续去重、限量，并保留模型输出的结构化标签和教学动作。
- live eval report 的 `actualEvidenceRefs` 因此能稳定包含 verdict、首个失败测试点、题目/规则信号等上下文证据。
- baseline regression gate 不需要放宽；它仍检查 mustKeep，但 current report 会提供稳定证据链。
- 新增无需 API Key 的结构测试，验证模型成功路径不会丢失 `verdict:*` 等上下文证据。

## Capabilities

### New Capabilities

- `model-context-evidence-refs`: 约束外部模型成功参与后，诊断结果必须保留稳定上下文证据引用，用于教师解释和 live eval baseline 回归。

### Modified Capabilities

- 无。

## Impact

- 生产诊断链：`DiagnosticAgentService`
- 测试：诊断 agent / 外部 runtime 相关测试新增上下文证据保留断言
- 评测：assistant live eval baseline regression gate 可继续使用严格 mustKeep
- 风险：证据 refs 变多；通过去重和限量控制报告体积
