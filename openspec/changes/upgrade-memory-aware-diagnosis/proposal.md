## Why

现有教育 AI agent 已经把学生长期学习记忆接入 `DiagnosisEvidencePackage -> ModelDiagnosisBrief -> prompt/standard library` 主链路，但系统目前主要是“让外部模型看到记忆”，还没有程序化判断模型是否正确使用记忆。

下一步需要把记忆能力升级成“可校准的辅助证据”：记忆能帮助个性化教学和教师关注，但不能覆盖当前提交的编译、运行、评测和源码直接证据。

## What Changes

- 在现有诊断主链路上新增记忆证据策略，判断学生记忆与当前提交证据是一致、冲突、无关还是仅能作为教学辅助。
- 扩展 `ModelDiagnosisBrief`，输出机器可读的记忆使用摘要，而不是只给模型一段自然语言 `learningMemorySummary`。
- 新增诊断校准结果，检查模型选择的主错因和细粒度错因是否被当前证据支撑，是否过度依赖 `memory:*` 证据。
- 更新 `ModelOutputValidator`，对“只引用记忆证据却选择主错因”的模型输出进行拒绝或降级，避免外部模型被学生画像带偏。
- 更新 `StandardLibraryPackBuilder` 和 `PromptTemplateRegistry`，强化“当前证据优先、记忆用于教学调节”的决策协议。
- 增加记忆一致、记忆冲突、记忆无关、历史干预无效、教师修正校准等测试样本，验证外部模型链路不是只在离线规则下变好。

## Capabilities

### New Capabilities

- `memory-aware-diagnosis-calibration`: 定义学生学习记忆如何作为辅助证据进入诊断校准，包含记忆相关性、记忆冲突、模型输出证据支撑和教师复核建议。

### Modified Capabilities

- `external-model-education-agent-runtime`: 外部模型诊断 runtime 的输出校验要求增加“记忆不能单独支撑主诊断”的约束。

## Impact

- 后端 AI 诊断链：`DiagnosisEvidencePackage`、`ModelDiagnosisBrief`、`ModelDiagnosisBriefBuilder`、`ModelOutputValidator`、`StandardLibraryPackBuilder`、`PromptTemplateRegistry`、`DiagnosticAgentService`。
- 外部模型输入输出协议：新增兼容字段，不删除旧字段；旧报告和旧 prompt 版本继续可读。
- 测试：新增或更新 brief 构建、模型输出校验、标准库、提示词和 agent 编排测试。
- 数据库：不新增表，不做迁移，不把历史代码全文塞进 prompt。
