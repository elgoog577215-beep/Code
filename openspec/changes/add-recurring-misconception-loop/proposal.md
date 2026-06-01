## Why

当前系统已经能处理单次错因、多次提交轨迹、Coach 理解证据、推荐效果、教师介入和 AC 后迁移，但仍缺少一个稳定表达“同一学生跨题或跨作业反复卡在同一细粒度误区”的结构化状态。对教育 agent 来说，这类复发模式比单次失败更像真正需要干预的学习瓶颈：学生可能每次都能局部修好代码，却没有把底层概念或判断步骤迁移出去。

本变更把“反复同类误区”升级为可观察、可推荐、可给教师解释、可进入质量评测的状态变量，补齐长期学习记忆从诊断辅助到教学决策的闭环。

## What Changes

- 新增 `recurringMisconceptionSignal`，表达复发错因、能力点、跨题/跨作业证据、复发强度、建议动作和教师关注标记。
- 在学生能力画像中输出该信号，让学生端和推荐系统不需要从自然语言摘要里重新推断长期薄弱点。
- 让推荐服务优先消费复发信号，生成 `MISCONCEPTION_REPAIR` 或 `TEACHER_REVIEW_RECOMMENDED` 策略，避免继续单纯加新题。
- 在教师作业概览中展示复发误区学生数、班级摘要和学生行信号，帮助教师区分“本次卡住”和“长期反复卡住”。
- 在 AI 质量概览中新增 `RECURRING_MISCONCEPTION_LOOP` 维度，检查系统是否把长期复发模式转成复盘、推荐和教师介入动作。
- 增加后端测试和前端类型验证，覆盖跨题复发、跨作业复发、非复发不误报、推荐消费和质量维度。

## Capabilities

### New Capabilities
- `recurring-misconception-loop`: 覆盖学生长期复发误区信号、学生画像输出、推荐消费、教师可见状态和 AI 质量评测。

### Modified Capabilities
- 无。

## Impact

- 后端：新增确定性分析器，影响 `StudentAbilityProfileService`、`ClassroomService`、`StudentRecommendationService`、`AiQualityOverviewService` 和相关 DTO。
- 前端：共享 API 类型、学生作业/练习页可选展示、教师工作台学生行和摘要展示。
- 测试：新增或更新能力画像、推荐服务、教师概览、AI 质量概览相关测试。
- 数据库：不新增迁移；第一版从现有提交、诊断分析、题目标签、Coach/推荐事件动态推导。
