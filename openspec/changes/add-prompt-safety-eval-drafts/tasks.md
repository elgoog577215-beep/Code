## 1. 响应模型与取数

- [x] 1.1 扩展 `DiagnosisEvalFixtureDraftResponse`，新增 `safetyFixtureCount` 和 `safetyFixtures`。
- [x] 1.2 为 `ClassroomService` 注入 `HintSafetyCheckRepository` 并读取作业范围安全检查。
- [x] 1.3 保持没有安全事件时旧导出字段兼容。

## 2. 安全 fixture draft 生成

- [x] 2.1 按 submissionId 合并高泄题诊断和中高风险 `HintSafetyCheck`。
- [x] 2.2 生成包含 riskSources、riskLevel、blockedReasons、hint previews、evidenceRefs、mustNotMention 和 quality 的安全草稿。
- [x] 2.3 过滤 LOW 安全检查并限制安全草稿数量。

## 3. 前端类型兼容

- [x] 3.1 更新前端 API 类型，补充 safety fixture draft 字段。

## 4. 测试

- [x] 4.1 扩展 `ClassroomServiceCorrectionTest`，覆盖高泄题诊断导出 safety fixture。
- [x] 4.2 覆盖 `HintSafetyCheck` 中高风险导出、同提交来源合并和 LOW 检查过滤。

## 5. 验证

- [x] 5.1 运行 `openspec validate add-prompt-safety-eval-drafts --strict`。
- [x] 5.2 运行 ClassroomService eval 草稿相关后端测试。
- [x] 5.3 运行后端编译、前端 typecheck 和 `git diff --check`。
