## Why

项目已经能根据学生画像生成重做、新题和复盘推荐，也能记录曝光、点击、进入题目和后续提交。但推荐仍主要是“动作建议”，缺少可验证的学习假设、完成信号和失败后的调度策略，教师也很难判断推荐到底是在促进迁移、只是被点击，还是让学生继续卡在同类错因。

本轮目标是把学生推荐升级为学习闭环：每条推荐都明确它要验证什么、成功标准是什么、失败后下一步该降级还是教师介入，并把这些信号沉淀到推荐效果看板中。

## What Changes

- 扩展学生推荐项，新增学习假设、预期完成信号、推荐策略、风险等级和失败后的 fallback 动作。
- 记录推荐曝光、点击、进入题目和提交事件时，保留推荐策略与预期完成信号，避免效果分析只能依赖自然语言 reason。
- 强化推荐生成逻辑，让历史推荐效果影响下一次推荐：同类推荐后仍命中相同错因时，优先降级为复盘或更小台阶。
- 扩展推荐效果概览，新增策略级效果统计、未完成学习信号、需要教师介入的反馈项和推荐闭环摘要。
- 保持兼容：只新增 DTO 和事件字段，不移除现有字段；旧数据缺少新增字段时按未知策略处理。
- 补充后端测试和前端共享类型，验证推荐假设、事件沉淀和效果反馈不是文案偶然结果。

## Capabilities

### New Capabilities

- `recommendation-learning-loop`: 定义学生推荐的学习假设、完成信号、策略降级和效果反馈闭环。

### Modified Capabilities

- `ai-quality-feedback-loop`: 将推荐闭环效果纳入 AI 能力质量判断，尤其关注推荐后同类错因未改善、推荐被点击但无提交、推荐策略需要教师介入等信号。

## Impact

- 后端推荐链路：`StudentRecommendationService`、`StudentRecommendationEventService`、`RecommendationEffectivenessService`。
- 领域对象和 DTO：`StudentRecommendationEvent`、`StudentRecommendationResponse`、`RecommendationEffectivenessResponse`。
- 前端共享类型：推荐项和推荐效果概览新增兼容字段。
- 测试：扩展推荐生成、推荐事件记录和推荐效果概览测试。
- OpenSpec：新增推荐学习闭环能力，并扩展 AI 质量反馈闭环能力。
