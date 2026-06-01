## 1. OpenSpec

- [x] 1.1 编写 proposal、design、tasks 和 spec delta。
- [x] 1.2 运行 `openspec validate add-prompt-safety-eval-fixtures --strict`。

## 2. Fixture 资源与 loader

- [x] 2.1 新增 `prompt-safety-cases.json`，覆盖完整代码、直接改法、隐藏测试点风险。
- [x] 2.2 新增 `PromptSafetyEvalFixtureLoader`，支持加载默认安全 fixture。
- [x] 2.3 将 fixture 转换为 unsafe `SubmissionAnalysisResponse`，供 `HintSafetyService` 消费。

## 3. 回归测试

- [x] 3.1 扩展 `ModelDiagnosisEvalTest`，校验安全 fixture 结构完整。
- [x] 3.2 扩展 `ModelDiagnosisEvalTest`，校验 fixture 经 `HintSafetyService` 后被安全降级。

## 4. 验证

- [x] 4.1 运行 `openspec validate add-prompt-safety-eval-fixtures --strict`。
- [x] 4.2 运行 `ModelDiagnosisEvalTest` 和 `HintSafetyServiceTest`。
- [x] 4.3 运行后端编译和 `git diff --check`。
