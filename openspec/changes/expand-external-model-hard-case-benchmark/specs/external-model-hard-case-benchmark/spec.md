## ADDED Requirements

### Requirement: 复杂诊断评测库必须达到 200 条以上

系统 SHALL 提供至少 200 条可复现的复杂学生提交诊断 fixture，用于约束外接模型在复杂教育诊断场景中的主错因判断、证据扎根、干扰信号抵抗和安全提示边界。

#### Scenario: 复杂 fixture 数量达标

- **WHEN** 加载默认复杂学生提交诊断 fixture
- **THEN** fixture 数量 SHALL 大于等于 200
- **AND** `caseId` 与 `generatorSpecId` SHALL 不重复

#### Scenario: live 候选覆盖更广

- **WHEN** 从默认复杂 fixture 中筛选 `liveCandidate=true`
- **THEN** live candidate 数量 SHALL 大于等于 36
- **AND** live candidate SHALL 覆盖至少 14 类不同 bug pattern

### Requirement: 每条复杂样本必须具备足够代码规模

系统 SHALL 保证复杂评测样本不是短小玩具程序，而是足以考验外接模型筛选主因、压低干扰信号和保持证据链的长代码提交。

#### Scenario: 每条复杂样本达到最低行数

- **WHEN** 校验任一复杂学生提交 fixture
- **THEN** buggy source code 行数 SHALL 大于等于 50

#### Scenario: 大样本覆盖达到 100 行以上

- **WHEN** 统计默认复杂学生提交 fixture
- **THEN** source code 行数大于等于 100 的样本数量 SHALL 大于等于 80

### Requirement: 每条复杂样本必须有高质量 gold rubric

系统 SHALL 为每条复杂 fixture 提供可评分的教师视角 gold rubric，而不是只提供期望标签。

#### Scenario: gold rubric 字段完整

- **WHEN** 校验任一复杂学生提交 fixture
- **THEN** fixture SHALL 包含非空 `teacherExpectation`
- **AND** fixture SHALL 包含非空 `primaryRootCause`
- **AND** fixture SHALL 包含至少 2 个 `secondaryIssues`
- **AND** fixture SHALL 包含至少 2 个 `distractingSignals`
- **AND** fixture SHALL 包含非空 `expectedTeachingPriority`
- **AND** fixture SHALL 包含非空 `requiredEvidenceRefs`
- **AND** fixture SHALL 包含非空 `mustMention`
- **AND** fixture SHALL 包含防泄题 `mustNotMention`

#### Scenario: gold evidence 与真实失败绑定

- **WHEN** 校验任一复杂学生提交 fixture
- **THEN** `requiredEvidenceRefs` SHALL 包含主错因 evidenceRef
- **AND** `requiredEvidenceRefs` SHALL 至少包含一个 `judge:first_failed_case:*` 证据引用
- **AND** baseline evidenceRefs SHALL 包含所有 requiredEvidenceRefs

### Requirement: 复杂样本必须由可执行证据支撑

系统 SHALL 用确定性生成器保证复杂 fixture 的错误提交真实失败、正确解真实通过，避免只靠自然语言想象标准答案。

#### Scenario: 生成器输出可复现

- **WHEN** 运行复杂 fixture 生成器
- **THEN** 生成结果 SHALL 与提交的默认 fixture 文件完全一致

#### Scenario: 质量元数据记录执行验证

- **WHEN** 校验任一复杂学生提交 fixture
- **THEN** `quality.verifiedByExecution` SHALL 为 true
- **AND** `quality.correctSolutionVerified` SHALL 为 true
- **AND** `quality.injectedBugCount` SHALL 大于等于 3

### Requirement: 复杂样本必须覆盖多样化高质量错误

系统 SHALL 避免用浅层重复样本凑数，复杂 fixture 必须覆盖足够多的 bug pattern、主错因标签和语义来源。

#### Scenario: 错误模式覆盖达标

- **WHEN** 统计默认复杂学生提交 fixture 的 `quality.bugPattern`
- **THEN** 不同 bug pattern 数量 SHALL 大于等于 14

#### Scenario: 主错因标签覆盖达标

- **WHEN** 统计默认复杂学生提交 fixture 的 `primaryRootCause.fineGrainedTag`
- **THEN** 不同主错因细粒度标签数量 SHALL 大于等于 14

#### Scenario: 语义来源覆盖达标

- **WHEN** 对默认复杂学生提交 fixture 的学生源码进行语义去重
- **THEN** 语义来源组数量 SHALL 大于等于 28
