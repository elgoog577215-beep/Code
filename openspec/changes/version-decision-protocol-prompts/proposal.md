## Why

上一轮已经把 `decisionProtocol` 加入标准库和诊断 prompt，但运行记录仍使用旧 promptVersion。  
这会让教师端质量统计、live eval 报告和后续 A/B 对比无法区分“旧提示词”和“带诊断裁决协议的新提示词”，影响长期迭代判断。

## What Changes

- 将诊断裁决阶段 prompt 版本升级为 `diagnosis-judge-v2`。
- 将 single-call prompt 版本升级为 `diagnosis-and-teaching-v2`。
- staged 组合版本升级为 `diagnosis-judge-v2+teaching-hint-v1`。
- 保留旧版本模板常量，避免历史报告含义丢失；运行时默认使用新版本。
- 更新测试，确保 `aiInvocation.promptVersion` 和 live eval 可观测到新版本。

## Capabilities

### New Capabilities

- `prompt-version-observability`: 定义 AI prompt 语义变化后必须更新稳定版本号，并在运行记录和评测报告中可观测。

### Modified Capabilities

- 无。

## Impact

- 后端 prompt 模板：`PromptTemplateRegistry`。
- 外部模型 runtime 版本记录：`AiReportService`、`ExternalModelAgentRuntime`。
- 测试：prompt contract、runtime invocation、live eval 报告。
- 不改前端，不改数据库，不改 API 响应结构。
