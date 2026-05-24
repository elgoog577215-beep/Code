## 排查结论

从 agent 工程视角，当前项目的 AI 链路已经包含：

- 输入证据构造：`DiagnosisEvidencePackage`、`ModelDiagnosisBrief`。
- 本地候选信号：`RuleSignalAnalyzer`。
- 标准库：`DiagnosisTaxonomy`、`StandardLibraryPack`。
- 外部模型 runtime：`ExternalModelAgentRuntime`、`AiReportService`。
- 输出治理：`ModelOutputValidator`、`ExternalModelOutputNormalizer`。
- 质量评测：`AssistantLiveEvalTest`、fixtures、quality gate。

主要问题不再是“没有 agent”，而是“标准库还没有把诊断裁决逻辑显式化”。外部模型看得到候选标签，却不一定知道：

- 哪类证据优先级更高。
- 隐藏测试不可见时应如何表达不确定性。
- 何时选择细粒度标签，何时只选择粗粒度标签。
- 候选信号冲突时何时保守选择 `NEEDS_MORE_EVIDENCE`。
- 诊断标签和教学动作之间的绑定关系。

## 方案

在 `StandardLibraryPack` 中新增 `decisionProtocol`，作为给外部模型的“诊断裁决说明书”。

### 字段结构

`decisionProtocol` 包含：

- `globalRules`：全局裁决原则。
- `evidencePriorityRules`：证据优先级。
- `tagSelectionRules`：标签选择规则。
- `conflictRules`：冲突和不确定性处理。
- `teachingActionRules`：教学动作绑定规则。

### 设计边界

- 不把本地规则复制成大段提示词，而是提供短规则，让模型在有限 token 内理解裁决边界。
- 不允许模型突破 `standardLibrary.issueTags` 和 `standardLibrary.fineGrainedTags`。
- 不允许模型凭空生成证据。
- 不让协议覆盖安全校验；安全仍由 prompt、validator 和 fallback 多层治理。

### Prompt 升级

诊断阶段和 single-call 阶段都增加明确规则：

- Must follow `standardLibrary.decisionProtocol`.
- Select the most evidence-supported tag, not the most common tag.
- Prefer `NEEDS_MORE_EVIDENCE` when available evidence cannot distinguish candidates.
- Use fine-grained tag only when evidence directly supports it.
- Keep uncertainty tied to hidden data and missing evidence.

教学阶段只需知道 validated diagnosis 已固定，因此不新增诊断裁决规则，只继续按已有教学动作生成提示。

## 风险与缓解

- 风险：协议太长导致 prompt 变重。  
  缓解：协议使用短列表，不放长例子。

- 风险：协议让模型过度保守。  
  缓解：规则要求“证据可区分时选择最强标签”，只在不可区分时选择 `NEEDS_MORE_EVIDENCE`。

- 风险：字段新增破坏序列化或测试。  
  缓解：只新增可选字段，不改旧字段。

## 验收标准

- `StandardLibraryPack` 输出 `decisionProtocol`。
- diagnosis judge prompt 和 single-call prompt 明确要求使用 `decisionProtocol`。
- 标准库测试覆盖协议内容。
- prompt 测试覆盖协议引用。
- targeted tests 通过。
- OpenSpec strict validate 通过。
