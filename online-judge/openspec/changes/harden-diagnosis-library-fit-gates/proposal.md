## Why

当前 AI 诊断已经能生成较自然的学生反馈，但评测发现“语义说对、标准库 ID 为空”的情况较多。这会削弱教师统计、质量评测和标准库成长候选池的可信度。

本变更把标准库命中从“可选元数据”提升为后端门禁：命中必须绑定合法 ID，半命中和未命中才允许进入成长候选。

## What Changes

- 强化 `diagnosisDecision.libraryFit` 校验：
  - `HIT` 必须至少包含一个合法标准库 anchor ID。
  - 非 `OUT_OF_LIBRARY` anchor 使用未知 ID 时不再静默转成库外发现。
  - `MISS` 不允许携带已有标准库 ID，避免把未命中伪装成命中。
- 调整正式诊断 prompt：
  - 学生可见 `studentReport` 继续自然表达。
  - 教师/评测元数据必须通过 `diagnosisDecision` 明确 `HIT / PARTIAL / MISS` 和 anchor ID。
  - 标准库成长候选只来自 `PARTIAL / MISS / OUT_OF_LIBRARY`。
- 收紧标准库成长入口：
  - `HIT` 输出不自动生成成长候选。
  - 只有半命中、未命中或库外发现才进入候选池。

## Capabilities

### New Capabilities
- `diagnosis-library-fit-gates`: AI 诊断结果必须将标准库命中、库外发现和成长候选分开表达并通过后端门禁校验。

### Modified Capabilities
- `single-agent-ai-diagnosis`: 单诊断 Agent 的正式输出必须保持学生报告自然表达，同时提供可校验的标准库命中元数据。
- `ai-diagnosis-quality-loop`: 质量评测必须区分语义命中和标准库 ID 命中。

## Impact

- 影响后端 AI 输出校验：`AdviceGenerationOutputValidator`。
- 影响正式 prompt：`PromptTemplateRegistry`。
- 影响标准库成长候选入口：`AiStandardLibraryGrowthAgentService`。
- 影响后端测试：诊断输出校验、成长候选持久化、prompt 契约测试。
