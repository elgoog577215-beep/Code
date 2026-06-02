## Why

项目已经具备外接模型、标准库、证据链、fallback、live eval、baseline 和 runtime telemetry 等能力，但这些研发层复杂度开始影响产品判断。学生真正需要的是提交失败后知道“当前最该处理什么”和“下一步怎么验证”，而不是理解 taxonomy、promptVersion、provider、latency 或 baseline。

本变更的目标不是继续扩 AI 能力，而是收束产品主链路：把学生主体验、教师洞察和研发评测明确分层，让复杂能力留在该在的位置。

## What Changes

- 明确产品北极星：学生每次失败提交后，都能在不泄题的前提下，明确知道当前错误点和下一步可验证动作。
- 确认 `studentFeedback` 是学生端唯一主展示契约，旧字段只做兜底或折叠详情。
- 将后端 AI 主链路收束为固定 6 步：判题结果、证据摘要、本地候选信号、外接模型尝试、安全与结构校验、学生双通道反馈。
- 将复杂能力下沉：教师端看教学洞察，研发层保留 live eval、baseline、runtime telemetry 和 fallback attribution。
- 新增主架构文档与 OpenSpec change 分层索引，帮助后续开发判断能力应该进入哪一层。
- 新增学生反馈契约测试，保证任何分析结果都能生成至少一个当前错误点和一个下一步动作。

## Capabilities

### New Capabilities

- `ai-product-main-loop`: 系统 SHALL use `studentFeedback` as the primary student-facing AI feedback contract and keep runtime/evaluation internals out of the student main surface.

### Modified Capabilities

- `student-facing-dual-channel-ai-feedback`: 学生端反馈 SHALL remain focused on blocking issues, improvement opportunities, and next learning action.
- `external-model-intelligence-eval`: 外接模型评测 SHALL be treated as研发评测层能力，不作为学生端主展示内容。

## Impact

- 后端：不新增 DTO，不迁移数据库，补契约测试和文档化主链路。
- 前端：保持 `studentFeedback` 主展示，确认内部复杂字段不进入第一视觉层。
- 文档：新增 AI 主链路架构文档和 OpenSpec 分层索引。
- 评测：保留现有复杂评测，但产品目标只采纳少数核心学生反馈指标。
