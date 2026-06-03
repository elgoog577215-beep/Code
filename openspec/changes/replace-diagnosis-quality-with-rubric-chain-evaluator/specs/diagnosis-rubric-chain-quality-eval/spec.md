## ADDED Requirements

### Requirement: 复杂诊断评测必须使用 gold rubric

系统 SHALL 为每条复杂学生提交 fixture 生成可审计的 `DiagnosisQualityRubric`，作为诊断质量评估的权威标准答案。

#### Scenario: 复杂 fixture 生成完整 rubric

- **GIVEN** 一条复杂学生提交 fixture
- **WHEN** 系统生成诊断质量 rubric
- **THEN** rubric SHALL 包含标准主因标签、细粒度标签、主因理由、必需证据、必须覆盖概念、干扰项、安全禁区和期望教学优先级
- **AND** rubric SHALL 不依赖外部大模型动态裁判

### Requirement: 诊断质量必须由链路评判器统一判分

系统 SHALL 使用 `DiagnosisChainRubricEvaluator` 作为复杂外接模型诊断质量的主评判器。

#### Scenario: 完整正确诊断链路通过

- **GIVEN** 模型输出引用了正确证据、命中主因、压低干扰项、给出可操作教学动作且不泄题
- **WHEN** 链路评判器评分
- **THEN** `overallVerdict` SHALL be `PASS`
- **AND** `score` SHALL be `1.0`

#### Scenario: 主因猜对但证据错误仍失败

- **GIVEN** 模型输出命中主因标签但缺失必需证据
- **WHEN** 链路评判器评分
- **THEN** `evidenceVerdict` SHALL be `FAIL`
- **AND** `overallVerdict` SHALL be `FAIL`

#### Scenario: 教学话术好但主因错误仍失败

- **GIVEN** 模型输出有可操作教学动作但选择了错误主因
- **WHEN** 链路评判器评分
- **THEN** `rootCauseVerdict` SHALL be `FAIL`
- **AND** `overallVerdict` SHALL be `FAIL`

#### Scenario: 泄露答案边界必须失败

- **GIVEN** 模型输出包含完整代码、参考答案、隐藏测试点等禁区内容
- **WHEN** 链路评判器评分
- **THEN** `safetyVerdict` SHALL be `FAIL`
- **AND** `overallVerdict` SHALL be `FAIL`

### Requirement: report 主质量口径必须切换到 rubric chain

系统 SHALL 在 live/offline eval report 的主摘要中使用 rubric chain quality 作为复杂诊断质量主指标。

#### Scenario: report 汇总链路质量

- **WHEN** live model eval report 汇总复杂 case
- **THEN** summary SHALL include rubric chain evaluated count、passed count、average score、stage pass/fail counts
- **AND** console summary SHALL show rubric chain quality instead of legacy scattered quality groups

#### Scenario: fallback 不进入模型正确率

- **GIVEN** 外部模型失败后本地 fallback 命中正确标签
- **WHEN** report 汇总模型诊断质量
- **THEN** 该 case SHALL NOT count as rubric chain evaluated
- **AND** 该 case SHALL remain visible in runtime/fallback status

### Requirement: legacy 质量指标不得作为主结论

系统 SHALL 保留必要 legacy 字段以兼容历史报告，但 SHALL NOT use legacy scattered metrics as the primary diagnosis quality conclusion.

#### Scenario: 主摘要排除 legacy 质量组

- **WHEN** report 生成 console summary
- **THEN** summary SHALL NOT present `complexQuality`、`intelligenceQuality`、`modelTraceQuality`、`studentFeedbackQuality` as primary quality groups
- **AND** rubric chain quality SHALL be the only primary complex diagnosis quality conclusion
