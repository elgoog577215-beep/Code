## Why

学生端 AI 诊断截图暴露出两个相关问题：模型结构化返回的逐条建议在后处理和前端展示中可能被覆盖、重复或视觉上合并，导致教师和学生误以为系统永远只给一条建议。

项目已经要求“建议数量由证据决定”，但现有学生可见反馈规范没有约束模型输出之后的 normalizer、runtime grounding、旧视图 mapper 和前端展示层，仍可能残留单条假设。

## What Changes

- 保证学生快反馈链路在模型返回多条 `repairItems` / `improvementItems` 时，后处理只修正不可信条目，不整体覆盖有效多条结果。
- 调整运行时越界兜底和提高层漂移校正逻辑：必须保留其他证据有效的建议项，只替换或补充不可信主项。
- 调整学生端展示逻辑：摘要与逐条列表分工清楚，不重复渲染第一条建议的代码证据。
- 补充回归测试，覆盖模型多条输出经过 runtime grounding 后仍保留多条、提高层只替换漂移项、前端不重复展示第一条。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `student-visible-ai-feedback-quality`: 增加学生可见结构化建议的数量保真、后处理保留和前端展示去重要求。

## Impact

- 后端：`AiReportService` 学生快反馈后处理、运行时越界兜底、提高层漂移校正。
- 前端：`ProblemPage.tsx` 学生 AI 反馈修正建议和提升建议展示。
- 测试：`StudentAiFeedbackModelTest` 与前端问题页相关测试。
- 规范：`openspec/specs/student-visible-ai-feedback-quality` 的变更 delta。
