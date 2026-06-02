## 1. OpenSpec

- [x] 1.1 编写 proposal、design、tasks 和 spec delta。
- [x] 1.2 运行 `openspec validate add-runtime-failure-eval-drafts --strict`。

## 2. 后端 runtime 草稿导出

- [x] 2.1 扩展 `DiagnosisEvalFixtureDraftResponse`，新增 runtime fixture 计数、列表和嵌套 DTO。
- [x] 2.2 在 `exportDiagnosisEvalFixtureDraft` 中读取作业内 runtime fallback / partial 分析并生成草稿。
- [x] 2.3 为 runtime 草稿生成归因类别、推荐动作、evidenceRefs、mustMention/mustNotMention、sourceMaterial 和 quality 元信息。
- [x] 2.4 对 failureReason 做截断和脱敏，避免输出 API Key、token 或 provider 原始敏感错误全文。

## 3. 教师端预览

- [x] 3.1 扩展前端 API 类型，增加 runtime fixture 字段。
- [x] 3.2 在教师端 Fixture 草稿预览中展示 runtime 计数、失败类别、推荐动作和 JSON 内容。

## 4. 测试与验证

- [x] 4.1 扩展 `ClassroomServiceCorrectionTest`，覆盖 runtime fallback、partial completion 和脱敏。
- [x] 4.2 运行相关后端测试。
- [x] 4.3 运行前端 typecheck、OpenSpec 校验和 `git diff --check`。
