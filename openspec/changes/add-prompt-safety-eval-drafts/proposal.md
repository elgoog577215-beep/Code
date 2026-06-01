## Why

提示安全事件已经能进入 AI 质量概览，但教师导出 eval 草稿时仍主要得到教师纠错和课堂介入样本。这样安全告警会被看见，却不一定沉淀为后续回归评测，模型和提示词升级时仍可能重复出现泄题、完整代码或超层级提示。

要完成安全闭环，系统需要把已记录的高泄题诊断和提示安全降级转换成可审查的 safety eval draft，让教师可以把真实课堂风险样本沉淀为安全 fixture。

## What Changes

- `DiagnosisEvalFixtureDraftResponse` 新增 `safetyFixtureCount` 和 `safetyFixtures` 兼容字段。
- `ClassroomService.exportDiagnosisEvalFixtureDraft` 读取作业提交、诊断和 `HintSafetyCheck`，生成提示安全 eval 草稿。
- 安全草稿覆盖高泄题诊断、`MEDIUM/HIGH` 安全降级记录，并保留 evidenceRefs、风险来源、blocked reasons、原始提示摘要和安全提示摘要。
- 安全草稿的 `mustNotMention` 默认包含完整代码、参考答案、隐藏测试点、直接改法和学生身份信息。
- 前端 API 类型补充 safety fixture draft 结构，现有导出接口保持兼容。
- 增加测试覆盖安全降级和高泄题诊断导出，确保 LOW 安全检查不会生成 safety fixture。

## Capabilities

### New Capabilities

- `prompt-safety-eval-drafts`: 覆盖提示安全事件如何导出为可人工审查的 eval 草稿、证据引用和安全评测期望。

### Modified Capabilities

无。

## Impact

- 后端：更新 `DiagnosisEvalFixtureDraftResponse`、`ClassroomService`、测试 fake repository 和 `ClassroomServiceCorrectionTest`。
- API：诊断 eval 草稿响应新增兼容字段 `safetyFixtureCount`、`safetyFixtures`。
- 前端：更新 `types.ts`，保持 UI 兼容；本轮不强制新增展示。
- 数据：无数据库迁移；复用现有 `HintSafetyCheck`、`SubmissionAnalysis`、`Submission` 和 `Problem` 数据。
