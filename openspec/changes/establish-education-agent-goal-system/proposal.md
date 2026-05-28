## Why

当前教育 AI agent 已经具备外部模型调用、诊断标准库、学习记忆、教师统计和 live eval 雏形，但缺少一套统一目标系统来约束“什么叫真的变强”。如果没有北极星目标、阶段指标和评测口径，后续优化容易只追求单次诊断分数、提示词表面表达或本地兜底命中，无法证明学生和教师真实受益。

本变更要把目标从“AI 诊断准确”升级为“AI 能让学生下一次更会改、让教师更会教、让系统越用越懂学生”，并把目标写进可执行、可评测、可持续迭代的工程闭环。

## What Changes

- 新增教育 AI agent 目标体系，明确北极星目标、第一阶段在线可用目标、第二阶段学生学习效果目标、第三阶段教师教学价值目标和长期成熟化目标。
- 将目标映射到 live eval 报告字段，区分外部模型完成率、运行失败率、质量命中率、证据有效率、安全通过率、教学动作有效率和目标缺口。
- 收紧长代码诊断评测集门槛：外部模型诊断能力评估必须优先使用 20 行以上真实或半真实代码样本，不能用短代码样本代表上线质量。
- 明确本地兜底与外部模型能力的统计边界：fallback 可以证明系统可用性，但不能计入外部模型质量。
- 沉淀下一阶段任务拆解：外部模型稳定性、100 条长代码评测集、学生下一次提交改善率、教师端共性错因统计和教师修正反馈闭环。
- 更新项目记忆，记录目标体系和评测口径，避免后续迭代偏离教育结果。

## Capabilities

### New Capabilities

- `education-agent-goal-system`: 定义教育 AI agent 的目标分层、量化指标、评测口径、报告字段和迭代闭环。

### Modified Capabilities

- `external-ai-assistant-eval-loop`: live eval 报告需要呈现目标缺口，并且质量统计只计算外部模型完成输出。

## Impact

- OpenSpec 文档：新增 `establish-education-agent-goal-system` change，作为后续长期优化的目标约束。
- 后端评测代码：`AssistantLiveEvalReport`、`AssistantLiveEvalTest`、`AssistantLiveEvalQualityGate`、相关测试。
- 评测资源：`assistant-live-cases.json` 的长代码样本门槛继续作为外部模型诊断测试底线。
- 项目记忆：`docs/ai-memory/项目决策.md` 记录目标系统与防误判口径。
- 生产 API：不引入破坏性变更，不新增数据库表，不改变学生端或教师端现有接口。
