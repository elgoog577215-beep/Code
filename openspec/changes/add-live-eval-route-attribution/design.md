## Context

当前外部模型链路已经具备三层基础能力：单次诊断可走 `single-call` 低预算模式，运行失败会被区分为配额、限流、超时等原因，生产服务也支持主路由和备用 OpenAI-compatible 路由。但 live eval 报告条目仍主要记录 `model`、`status` 和 `fallbackUsed`，缺少实际 provider/model/route role。

这会导致一个关键盲区：当备用路由配置后，整体完成率提升了，也无法从每条样本看出是主路由恢复、备用路由接管，还是本地兜底被误算。教育 agent 的后续优化需要基于真实外部模型输出继续迭代，必须把路由归因变成报告的一等字段。

## Goals / Non-Goals

**Goals:**

- 每条 live eval entry 记录实际 provider、实际 model 和路由角色。
- 诊断类 entry 优先从 `SubmissionAnalysisResponse.AiInvocation` 读取真实调用信息。
- 无法获得真实调用元数据的 assistant 类型，不伪造精确归因；使用评测默认 route 信息并保持字段语义明确。
- 保持 JSON 报告向后兼容，只新增字段，不删除旧字段。
- 增加测试验证新增字段可以写入、回读和用于分析。

**Non-Goals:**

- 不改变生产 `SubmissionAnalysisResponse` 的学生可见内容。
- 不重写 `AiReportService` 或 `CoachAgentService` 的调用策略。
- 不在本轮为 Coach/Growth Report 设计完整 invocation DTO；如果未来需要，可单独把调用元数据扩展到对应返回对象。
- 不把本地兜底结果计入外部模型质量。

## Decisions

### 1. 在 live eval entry 中新增归因字段

新增字段建议为：

- `actualProvider`
- `actualModel`
- `routeRole`

原因：旧字段 `model` 保持为本次评测目标模型或默认模型，避免破坏旧报告消费方；新增字段表达真实执行归因。`routeRole` 使用 `PRIMARY`、`FALLBACK`、`UNKNOWN`、`LOCAL_FALLBACK` 这类稳定枚举字符串，方便后续聚合。

替代方案是直接改写 `model` 字段为实际模型，但这会影响旧统计口径，不适合在已有报告结构上做。

### 2. 诊断 entry 从 `AiInvocation` 获取真实归因

`AiReportService` 已经把成功路由写入 `AiInvocation.provider/model`，因此诊断类 live eval 可以直接使用这两个字段。`fallbackUsed=true` 时 route role 标为 `LOCAL_FALLBACK`，避免把本地规则兜底误认为外部模型输出。

替代方案是在 live eval 外层按配置猜测路由，但这无法区分主路由失败后备用路由成功。

### 3. Coach/Growth Report 保持保守归因

当前 Coach 和成长报告返回对象没有统一 `AiInvocation`。本轮只给它们补充默认 provider/model 和 `PRIMARY` 或 `LOCAL_FALLBACK`，并通过字段语义说明这不是完整调用追踪。这样不会为了报告字段强行改业务 DTO。

替代方案是立即扩展所有 assistant 返回对象，但改动面更大，容易偏离本轮“评测归因”目标。

## Risks / Trade-offs

- [Risk] 新字段可能被误解为所有 assistant 类型都有精确路由追踪。→ Mitigation：诊断类使用真实 `AiInvocation`，其他类型只在当前能力范围内保守标注，后续可单独扩展。
- [Risk] JSON 消费方未适配新增字段。→ Mitigation：只新增字段，不删除旧字段；Jackson/前端消费默认兼容。
- [Risk] route role 推断和 provider/model 字段不一致。→ Mitigation：新增测试覆盖主路由、备用路由、本地兜底和报告回读。
