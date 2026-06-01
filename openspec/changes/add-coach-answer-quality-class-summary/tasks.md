## 1. 响应结构

- [x] 1.1 在 `AssignmentOverviewResponse` 新增 `CoachAnswerQualityClassSummary` 字段。
- [x] 1.2 在前端 `AssignmentOverview` 类型新增 `coachAnswerQualitySummary`。

## 2. 后端汇总

- [x] 2.1 在 `ClassroomService` 基于学生最新 `latestCoachInteraction.answerQualitySignal` 汇总 Coach 回答质量。
- [x] 2.2 输出 prompted、answered、verifiable、transferReady、evidenceInsufficient、safetyRisk、teacherAttention 等计数。
- [x] 2.3 输出 dominantGap、summary、recommendedAction 和 evidenceRefs。

## 3. 教师端展示

- [x] 3.1 在教师工作台展示 Coach 回答质量摘要卡片。
- [x] 3.2 补充卡片样式、移动端约束和暗色主题兼容。

## 4. 验证

- [x] 4.1 扩展后端测试，覆盖证据不足、可验证、可迁移和疑似越界聚合。
- [x] 4.2 运行 OpenSpec 严格校验、相关后端测试、后端编译、前端 typecheck、浏览器视觉检查和 diff 检查。
