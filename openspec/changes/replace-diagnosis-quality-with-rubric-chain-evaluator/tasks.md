## 1. OpenSpec

- [x] 1.1 创建 `diagnosis-rubric-chain-quality-eval` 能力规格，明确 gold rubric、链路评判和 report 主口径。
- [x] 1.2 运行 `openspec validate replace-diagnosis-quality-with-rubric-chain-evaluator --strict`。

## 2. Rubric 与链路评判器

- [x] 2.1 新增 `DiagnosisQualityRubric`，从复杂 fixture 生成权威 gold rubric。
- [x] 2.2 新增 `DiagnosisChainRubricEvaluator`，输出 evidence/rootCause/distractor/teaching/safety/overall verdict、failedReasons 和 score。
- [x] 2.3 明确正确率口径：真实模型完成且未 fallback，证据、主因、安全和教学动作通过才算正确。

## 3. Report 主质量口径切换

- [x] 3.1 扩展 `LiveModelEvalReport` entry/summary/qualityScore，加入 rubric chain quality 字段。
- [x] 3.2 更新 `ModelDiagnosisEvalTest` 聚合逻辑，使用 rubric chain quality 作为主摘要。
- [x] 3.3 将旧散指标从 console 主摘要移除，仅保留 legacy 兼容字段。

## 4. 测试

- [x] 4.1 补充 rubric 生成测试，确认 210 条复杂 fixture 都能生成完整 rubric。
- [x] 4.2 补充 evaluator 单元测试，覆盖正确链路、证据错误、主因错误、次要问题过重和泄题失败。
- [x] 4.3 补充 report 测试，确认主摘要展示 rubric chain quality，fallback 不进入模型正确率，failedReasons 能定位阶段。

## 5. 验证

- [x] 5.1 运行相关 Maven 测试：`./mvnw -pl online-judge -Dtest=ComplexStudentSubmissionEvalFixtureTest,ModelDiagnosisEvalTest test`。
- [x] 5.2 运行 `git diff --check`。
- [x] 5.3 运行精确 secret scan，确认本轮改动没有 token、Authorization 或 Bearer 泄露。
