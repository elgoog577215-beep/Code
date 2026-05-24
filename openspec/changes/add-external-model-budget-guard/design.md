## Background

当前系统已经可以真实调用外部模型，但连续调用多类助手时会遇到供应商额度和限流问题。真实学校部署中，这不是边缘问题：一个班级同时提交、追问、生成报告时，外部模型 API 的可用性会直接决定 AI 助手是否“真实可用”。

现有问题是调用失败被分散处理：

- `AiReportService` 自己分类异常。
- `CoachAgentService` 自己分类异常。
- live eval 根据字符串再做一次分类。
- 当供应商已经返回 quota/rate limit，后续助手仍继续请求。

## Goals

- 统一外部模型错误分类，减少重复字符串判断。
- 在 JVM 级别加入轻量预算保护，避免连续额度/限流错误后继续调用。
- 保持兜底输出可解释：清楚说明是外部模型预算/限流，不是学生诊断能力或教学逻辑失败。
- 让 live eval 后续迭代能判断下一步应该优化额度/路由/调用节流，而不是误改 prompt。

## Non-Goals

- 不实现完整分布式限流。
- 不引入 Redis、数据库迁移或复杂队列。
- 不改变外部模型供应商。
- 不隐藏真实失败：短路保护也必须在 failureReason 中留下原因。

## Architecture

```text
AI 服务准备调用外部模型
        |
        v
ExternalModelBudgetGuard.canAttempt()
        |
        +-- false --> 快速兜底，failureReason=BUDGET_GUARD_OPEN:<reason>
        |
        v
真实调用供应商 API
        |
        v
ExternalModelFailureClassifier.classify(exception/body/status)
        |
        +-- quota/rate limit --> recordFailure，短期开启 guard
        +-- transient timeout --> 只分类，不长期短路
        +-- success --> recordSuccess，可清除 guard
```

## Design Decisions

### 1. JVM 级短路优先于复杂限流

当前项目还没到分布式生产部署阶段，先用轻量组件解决本地和单实例服务中最明显的问题。后续如果部署多实例，再把 guard 状态迁移到 Redis 或网关层。

### 2. 只对 quota/rate limit 开启短路

timeout、invalid JSON、单次 API error 不一定代表后续请求必失败。额度和限流错误更适合短时间保护，避免连续撞接口。

### 3. 保留人工可读失败原因

AI 能力评测不是只看成功率。报告必须说明是 `INSUFFICIENT_QUOTA`、`RATE_LIMITED` 还是 `BUDGET_GUARD_OPEN`，让下一轮优化能选对方向。

## Testing

- 单元测试错误分类器。
- 单元测试预算保护器在 quota/rate limit 后打开，在成功后关闭。
- 测试提交诊断和 Coach 在 guard 打开时快速兜底并保留失败原因。
- 运行 assistant live eval 结构测试；真实外部模型测试使用更节制的样本数和 delay。
