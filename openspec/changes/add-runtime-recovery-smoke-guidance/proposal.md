## Why

当前 quota/rate limit runtime draft 已经会推荐 offline runtime profile eval，但“额度或 provider 恢复后该如何用最小真实 live eval 证明外部模型重新参与”仍主要藏在自然语言 `expectedRuntimeAction` 中。这样维护者需要人工读建议，CI 或报告消费者无法稳定判断下一步恢复验证是什么。

本轮把恢复后 smoke 验证沉淀为结构化字段，让 quota、budget guard、provider error 等外部调用受限样本都能明确给出下一次真实模型恢复验证入口。

## What Changes

- live eval runtime fixture draft 新增 recovery smoke guidance 字段：
  - 是否推荐恢复 smoke。
  - smoke caseId。
  - smoke runtime profile。
  - smoke command hint。
  - smoke required checks。
- quota/rate limit、budget guard、provider error、timeout 和 stream no-content 场景生成 recovery smoke guidance。
- 教师端 runtime fixture draft 同步暴露这些字段，让归因草稿不只告诉老师“哪里失败”，也告诉维护者“恢复后如何验证”。
- 保持现有 `offlineProfile*` 字段语义不变：offline profile 用于额度不可用期间的请求体积/结构校验，recovery smoke 用于额度恢复后的真实模型参与验证。

## Capabilities

### New Capabilities
- `runtime-recovery-smoke-guidance`: 覆盖 runtime fixture draft 如何结构化表达外部模型恢复后的最小 live smoke 验证步骤。

### Modified Capabilities
- `live-eval-runtime-draft-export`: live eval runtime draft 增加 recovery smoke guidance 字段。
- `runtime-failure-eval-drafts`: 教师端 runtime fixture draft 增加 recovery smoke guidance 字段。

## Impact

- 测试 DTO：`LiveEvalRuntimeFixtureDraft` 新增 recovery smoke guidance 字段。
- 测试工厂：`LiveEvalRuntimeFixtureDraftFactory` 为 model runtime failures 填充 recovery smoke guidance。
- 生产 DTO：`DiagnosisEvalFixtureDraftResponse.RuntimeFixtureDraft` 新增同名字段。
- 生产服务：`ClassroomService` runtime fixture draft 导出填充 recovery smoke guidance。
- 测试：扩展 runtime draft factory 与教师端 fixture draft 测试。
