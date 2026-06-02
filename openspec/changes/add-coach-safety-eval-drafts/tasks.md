## 1. OpenSpec

- [x] 1.1 编写 proposal、design、tasks 和 spec delta。
- [x] 1.2 运行 `openspec validate add-coach-safety-eval-drafts --strict`。

## 2. 后端草稿生成

- [x] 2.1 在 `exportDiagnosisEvalFixtureDraft` 中读取作业提交对应的 CoachPrompt。
- [x] 2.2 扩展安全草稿构建逻辑，纳入 `SAFETY_REJECTED` Coach prompt。
- [x] 2.3 为 Coach 安全拒绝生成 risk source、blocked reason、risk level、evidenceRefs、sourceMaterial 和 quality 元信息。
- [x] 2.4 保证同 submissionId 的诊断安全、提示安全和 Coach 安全来源合并为一条草稿。

## 3. 测试

- [x] 3.1 扩展 `ClassroomServiceCorrectionTest`，覆盖 Coach 安全拒绝导出为 safety fixture draft。
- [x] 3.2 扩展合并测试，确认同 submissionId 多类安全来源合并且 evidenceRefs 保留 Coach 来源。
- [x] 3.3 确认导出草稿不暴露不安全模型草稿全文，只展示安全 fallback 与脱敏描述。

## 4. 验证

- [x] 4.1 运行 `openspec validate add-coach-safety-eval-drafts --strict`。
- [x] 4.2 运行相关后端测试。
- [x] 4.3 运行后端编译、前端 typecheck 和 `git diff --check`。
