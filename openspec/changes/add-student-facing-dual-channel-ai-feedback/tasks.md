## 1. OpenSpec

- [x] 1.1 创建 `student-facing-dual-channel-ai-feedback` 能力规格。
- [x] 1.2 写明学生端双通道输出、fallback 语义、标准库扩展和评测指标。

## 2. 后端学生反馈结构

- [x] 2.1 在 `SubmissionAnalysisResponse` 中新增 `studentFeedback` 及嵌套 DTO。
- [x] 2.2 新增 `StudentFeedbackAssembler`，从本地诊断结果生成安全的双通道反馈。
- [x] 2.3 在 `DiagnosticAgentService` / `AiReportService` 返回前填充 `studentFeedback`，保持旧字段兼容。

## 3. 标准库与外接模型协议

- [x] 3.1 扩展 `StandardLibraryPack` 和 builder，输出 improvement taxonomy 与 student feedback rules。
- [x] 3.2 升级 prompt 到 `diagnosis-and-teaching-v3`，要求输出 `studentFeedback`。
- [x] 3.3 扩展 `ExternalModelStagePayloads`、normalizer 和 validator，校验学生反馈标签、证据引用和安全边界。
- [x] 3.4 支持模型部分完成时保留有效诊断并本地重写学生反馈。

## 4. 评测与样本

- [x] 4.1 为 14 条 `complex-live-*` 代表样本补充学生反馈期望。
- [x] 4.2 新增学生反馈质量指标：blockingPrimaryHit、secondaryIssueBalanced、improvementOpportunityUseful、evidenceGrounded、studentActionable、noSolutionLeak、fallbackHonesty。
- [x] 4.3 扩展 live eval report，区分 localTruth、modelOutput、studentFeedback 和 student feedback quality score。

## 5. 测试与验证

- [x] 5.1 增加 DTO 结构、标准库、prompt、validator 和 assembler 单测。
- [x] 5.2 增加多查询前缀和端到端诊断测试，验证当前错误点、次要问题和继续提升点。
- [x] 5.3 运行相关 Maven 测试。
- [x] 5.4 运行 `openspec validate add-student-facing-dual-channel-ai-feedback --strict`。
- [x] 5.5 运行 secret scan 和 `git diff --check`。
