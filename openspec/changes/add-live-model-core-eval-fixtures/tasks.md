## 1. OpenSpec

- [x] 1.1 新增 `live-model-core-eval-fixtures` 能力规格。
- [x] 1.2 修改 `external-model-call-stability`，说明 live-model smoke 支持 case id 过滤。

## 2. Fixture 数据

- [x] 2.1 新增 `live-model-core-cases.json`。
- [x] 2.2 首批覆盖至少 6 个核心教育诊断错因。
- [x] 2.3 每条样本包含 case id、期望标签、证据要求、must mention / must not mention 和质量说明。

## 3. Loader 与 live eval 接入

- [x] 3.1 新增 `LiveModelCoreEvalFixtureLoader`。
- [x] 3.2 `ModelDiagnosisEvalTest#allEvalCases()` 纳入 core set。
- [x] 3.3 live smoke 支持 `AI_EVAL_CASE_IDS` 过滤。

## 4. 验证

- [x] 4.1 补充结构测试覆盖 core set 加载和 case id 过滤。
- [x] 4.2 运行相关后端测试。
- [x] 4.3 运行 OpenSpec strict validate、secret scan 和 `git diff --check`。
- [x] 4.4 可行时运行真实 ModelScope 小样本 smoke，并对比最新报告。
