## 1. OpenSpec

- [x] 1.1 编写 proposal、design、tasks 和 spec delta。
- [x] 1.2 运行 `openspec validate add-coach-safety-rejection-signal --strict`。

## 2. 持久化与响应

- [x] 2.1 扩展 `CoachPrompt`，新增模型失败原因和模型泄题风险 nullable 字段。
- [x] 2.2 扩展 `CoachPromptResponse`，返回模型安全元数据。
- [x] 2.3 更新 `CoachPromptService`，保存 `CoachDraft.failureReason` 与 `answerLeakRisk`。
- [x] 2.4 更新前端 API 类型以保持响应字段可消费。
- [x] 2.5 将 Coach 安全拒绝计数接入班级 Coach 质量汇总和教师端指标。

## 3. 交互摘要信号

- [x] 3.1 扩展 `CoachInteractionSummaryResponse`，新增 `CoachSafetyRejectionSignal`。
- [x] 3.2 更新 `CoachInteractionAnalyzer`，统计 `SAFETY_REJECTED`、生成 evidenceRefs 和教师关注标记。
- [x] 3.3 保持学生回答质量 `SAFETY_RISK` 与模型安全拒绝语义分离。

## 4. 测试

- [x] 4.1 扩展 `CoachPromptServiceTest`，覆盖安全拒绝落库和响应字段。
- [x] 4.2 新增或扩展 `CoachInteractionAnalyzer` 测试，覆盖安全拒绝摘要信号。
- [x] 4.3 运行相关后端测试。

## 5. 验证

- [x] 5.1 运行 `openspec validate add-coach-safety-rejection-signal --strict`。
- [x] 5.2 运行后端编译。
- [x] 5.3 运行前端 typecheck。
- [x] 5.4 运行 `git diff --check`。
