## Why

项目已经能识别教师讲评建议、班级教学策略、教师反馈和后续学习成效，但这些课堂闭环证据目前主要停留在页面状态与质量看板里。诊断 eval 草稿导出只覆盖教师校正样本，无法把“AI 建议是否有效”“教师采纳后学生是否改善”“仍卡同类问题是否应升级”沉淀为可回归评测资产。

教育 agent 的长期能力提升需要把运行中的课堂证据转成评测样本。否则每轮优化只能看当次页面结果，无法稳定防止建议质量、证据引用和后续行动判断退化。

## What Changes

- 扩展诊断 eval fixture 草稿响应，新增课堂介入成效草稿，覆盖教师讲评建议反馈和班级教学策略反馈。
- 复用现有 `ClassReviewFeedback`、提交、诊断分析和 impact analyzer，不新增数据库表。
- 导出草稿时同时包含：
  - 原始 AI 建议或班级策略 key。
  - 教师反馈动作。
  - 后续提交成效状态。
  - 期望教学动作、应提及证据、禁止泄题项和质量目的。
- 教师端 fixture 草稿预览显示诊断校正草稿数量和课堂介入草稿数量，帮助老师知道哪些运行证据可以沉淀进 eval。
- 增加后端测试，覆盖改善、仍卡同类问题、策略反馈草稿和无反馈时不导出的边界。

## Capabilities

### New Capabilities

- `classroom-intervention-eval-drafts`: 把课堂讲评建议和班级教学策略的执行成效导出为 eval 草稿候选。

### Modified Capabilities

- `diagnosis-eval-fixture-draft`: 在保持教师校正诊断草稿兼容的基础上，增加课堂介入成效草稿列表与计数。

## Impact

- 后端：更新 `DiagnosisEvalFixtureDraftResponse`、`ClassroomService.exportDiagnosisEvalFixtureDraft` 和相关测试。
- API：沿用现有 `/api/teacher/assignments/{id}/diagnosis-eval-fixture-draft` 端点，新增响应字段；旧前端可忽略新增字段。
- 前端：教师端 eval 草稿预览增加课堂介入草稿计数和 JSON 输出。
- 数据：无数据库迁移。
- 兼容性：`fixtures` 原字段仍表示教师诊断校正草稿；新增 `interventionFixtures` 表示课堂介入成效草稿。
