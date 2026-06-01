## 背景

项目已经能在运行时发现提示安全事件、导出安全 eval 草稿，并在跨作业趋势中展示提示安全事件。但是默认静态回归评测资源里还没有专门的“提示安全 fixture”类别。这样一来，安全事件虽然可见，却还不能稳定沉淀为每次测试都会执行的安全防回退样本。

教育 agent 的提示安全不能只靠线上事件触发后人工记忆。需要把典型风险样本固化成测试资源，持续验证系统会把完整代码、直接改法、隐藏测试点等越界内容降级为证据驱动的学习动作。

## 变更目标

- 新增提示安全 eval fixture 资源，覆盖完整代码泄露、直接给出改法、隐藏测试点泄露等典型风险。
- 新增 fixture loader，将资源转换为 `SubmissionAnalysisResponse`，供离线 safety gate 复用。
- 在离线评测测试中校验 fixture 结构、风险预期和 `HintSafetyService` 的安全降级结果。
- 确保安全 fixture 约束 forbidden phrases、expectedSafetyAction、blockedReasons、riskLevel 和 evidenceRefs。

## 非目标

- 不改变 `HintSafetyService` 的判定规则。
- 不调用外部模型，不新增 live eval 依赖。
- 不修改教师端导出接口；本变更是把一组默认静态资源接入回归测试。

## 影响范围

- 测试资源：新增 `diagnosis-eval-fixtures/prompt-safety-cases.json`。
- 测试代码：新增 `PromptSafetyEvalFixtureLoader`，扩展 `ModelDiagnosisEvalTest`。
- 验证：运行提示安全 loader 测试、`HintSafetyServiceTest`、诊断 eval 相关测试。
