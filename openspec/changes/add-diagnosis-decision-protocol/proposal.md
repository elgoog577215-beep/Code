## Why

当前外部模型链路已经具备调用稳定性、single-call、输出标准化和严格校验，但提示词和标准库仍主要告诉模型“输出什么格式”，没有充分告诉模型“如何在多个错因候选之间做教学裁决”。  
这会导致真实外部模型在证据冲突、隐藏测试不可见或候选标签相近时发生分类漂移，最终影响学生提示和教师统计的准确性。

## What Changes

- 在 `StandardLibraryPack` 中新增 `decisionProtocol`，把诊断裁决规则显式传给外部模型。
- `decisionProtocol` 包含：全局裁决原则、证据强度规则、标签选择规则、冲突处理规则和教学动作绑定规则。
- 升级 diagnosis judge 和 single-call prompt，强制模型按 `standardLibrary.decisionProtocol` 做错因裁决。
- 保持兼容：不改变现有响应 DTO，不改变前端接口，不改变默认 runtime 模式。
- 补充测试验证标准库包含协议，prompt 引用协议，runtime 仍能通过模型输出校验。

## Capabilities

### New Capabilities

- `diagnosis-decision-protocol`: 定义外部模型如何使用标准库中的诊断裁决协议来选择错因标签、证据引用和教学动作。

### Modified Capabilities

- 无。

## Impact

- 后端 AI 标准库：`StandardLibraryPack`、`StandardLibraryPackBuilder`。
- 外部模型提示词：`PromptTemplateRegistry`。
- 测试：标准库构建、prompt contract、runtime targeted tests。
- 不影响数据库结构和前端渲染。
