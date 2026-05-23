## Why

当前 AI 诊断链已经有本地规则、证据包、标准库和离线评测，但外部大模型调用仍接近“单次长 prompt 生成完整诊断”。这种方式在 live eval 中出现超时、回退和质量不可观测，无法满足学校和政府场景对稳定、可解释、可评测的教育 AI 要求。

本变更要把外部模型调用从“补充文本生成”升级为 workflow-first 的教育 agent runtime：先压缩证据、裁剪标准库，再分阶段调用模型，并把每次调用的质量、回退和教学安全显式记录。

## What Changes

- 新增外部模型教育 agent runtime 的能力边界，负责组织模型输入、提示词契约、标准库注入、分阶段模型调用、输出校验和评测报告。
- 将当前单次大 prompt 拆成至少两个模型任务：错因裁决和教学表达，降低单次调用复杂度。
- 新增模型输入摘要 `ModelDiagnosisBrief`，只给模型判断所需的题目摘要、关键代码、评测事实、候选信号和允许标签。
- 新增标准库裁剪包 `StandardLibraryPack`，只注入当前样本相关的错因标签、细粒度标签、教学动作、安全约束和常见误区。
- 新增 live model eval report，逐条记录模型是否真正参与、耗时、JSON 合规、标签命中、证据引用、安全结果和失败原因。
- 保留本地规则 agent 作为兜底，但任何回退都必须显式标记为 `RULE_FALLBACK`，不能计为外部模型成功。
- 不引入破坏性 API 变更；现有学生端和教师端可继续消费原有诊断结果。

## Capabilities

### New Capabilities

- `external-model-education-agent-runtime`: 定义外部大模型在教育诊断中的可控调用链，包括上下文压缩、标准库注入、提示词契约、分阶段调用、输出校验、回退标记和 live eval 报告。

### Modified Capabilities

- 无。

## Impact

- 后端 AI 诊断链：`AiReportService`、`DiagnosticAgentService` 及其周边模型调用逻辑。
- 标准库与证据结构：`DiagnosisTaxonomy`、规则信号、诊断证据包、学生提示计划和教师备注。
- 评测体系：`ModelDiagnosisEvalTest`、教师校正 fixture、学生提示 fixture，以及新增 live model eval 报告。
- 配置项：外部模型、超时、最大输出长度、是否启用分阶段 runtime 等可配置项。
- 运维可观测性：模型调用 trace、fallback 状态、延迟和失败原因将成为教师端质量分析或后续日志分析的基础。
