## Why

当前项目已经把提交诊断切到外部模型 runtime，但真实上线能力不能只靠离线规则评测判断。我们需要一个覆盖提交诊断、Coach 追问、成长报告三类 AI 助手的在线评测闭环，区分“模型调用失败”“agent 结构失败”和“教学质量不足”。

这次变更的目标不是让离线分数更好看，而是让接入外部模型后的真实效果可测、可解释、可继续迭代。

## What Changes

- 新增统一的 AI 助手评测集格式，样本覆盖提交诊断、学生追问和学习报告三类助手。
- 新增高质量模拟评测样本，包含输入上下文、老师期望、禁止行为、评分点和教学价值说明。
- 新增后端 live eval runner，直接调用项目服务，不经过前端，生成逐样本评测报告。
- 新增评测报告结构，记录模型、prompt 版本、助手类型、调用状态、命中情况、安全情况、失败原因和输出摘要。
- 新增质量门槛测试：无外部模型时验证 fixture 结构，有 `AI_EVAL_API_KEY` 时运行真实外部模型 smoke eval。
- 根据真实测试结果做一轮结构或提示词级迭代，优先处理会影响在线效果的问题，例如调用失败不可解释、输出证据不足、追问泄题风险或报告泛化。

## Capabilities

### New Capabilities

- `external-ai-assistant-eval-loop`: 定义外部模型驱动的 AI 助手评测闭环，覆盖评测集、live eval、报告、失败归因和迭代要求。

### Modified Capabilities

- 无。

## Impact

- 后端测试与评测资源：`online-judge/src/test/java`、`online-judge/src/test/resources`。
- AI 服务评测入口：提交诊断、Coach 追问、成长报告相关服务。
- 评测报告输出：`online-judge/target/ai-eval-reports`，仅作为本地运行产物，不纳入版本控制。
- OpenSpec 文档：新增本变更的 proposal、design、spec 和 tasks。
