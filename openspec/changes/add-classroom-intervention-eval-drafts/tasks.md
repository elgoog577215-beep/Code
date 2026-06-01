## 1. 后端草稿结构

- [x] 1.1 扩展 `DiagnosisEvalFixtureDraftResponse`，新增课堂介入草稿计数与列表。
- [x] 1.2 新增 `InterventionFixtureDraft` DTO，包含 source、suggestionKey、feedback、impact、证据和评测期望字段。

## 2. 导出逻辑

- [x] 2.1 在 `ClassroomService.exportDiagnosisEvalFixtureDraft` 中读取同作业反馈、提交、分析和问题上下文。
- [x] 2.2 为普通 `ClassReviewSuggestion` 反馈生成 `class-review-intervention-draft`。
- [x] 2.3 为 `strategy:` 前缀反馈生成 `class-strategy-intervention-draft`。
- [x] 2.4 为草稿生成 `mustMention`、`mustNotMention`、`expectedTeachingActions` 和 `quality.evalPurpose`。

## 3. 前端预览

- [x] 3.1 更新前端 API 类型，加入 `interventionFixtureCount` 和 `interventionFixtures`。
- [x] 3.2 更新教师端 fixture 草稿预览，显示诊断草稿和课堂介入草稿数量。
- [x] 3.3 JSON 预览同时输出诊断草稿与课堂介入草稿。

## 4. 验证

- [x] 4.1 扩展 `ClassroomServiceCorrectionTest`，覆盖教师讲评建议改善草稿。
- [x] 4.2 扩展 `ClassroomServiceCorrectionTest`，覆盖班级策略仍卡同类问题草稿。
- [x] 4.3 运行 OpenSpec 严格校验、相关后端测试、后端编译、前端 typecheck 和 diff 检查。
