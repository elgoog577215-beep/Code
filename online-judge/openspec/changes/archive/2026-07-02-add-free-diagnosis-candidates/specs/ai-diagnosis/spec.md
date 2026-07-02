# AI 诊断输出契约规格

## ADDED Requirements

### Requirement: 自由诊断候选层

系统 SHALL 在 `diagnosis-report-v2` 输出中支持 `diagnosisCandidates` 字段，用于记录单诊断 Agent 的自由诊断候选。

#### Scenario: 模型先自由列出问题再映射标准库

- GIVEN 单诊断 Agent 收到题目、代码、判题结果、证据和树形标准库包
- WHEN 模型生成诊断输出
- THEN 输出 SHOULD 包含 `diagnosisCandidates`
- AND 每个候选 SHOULD 说明问题名称、层级、标准库匹配状态、证据、理由和置信度
- AND 学生端 SHALL 继续优先展示 `studentReport`

#### Scenario: 候选命中标准库

- GIVEN `diagnosisCandidates` 中某候选的 `libraryFit` 为 `HIT` 或 `PARTIAL`
- WHEN 该候选填写 `anchorId`
- THEN 后端 SHALL 校验该 ID 存在于当前 `StandardLibraryPack`

#### Scenario: 候选为库外发现

- GIVEN `diagnosisCandidates` 中某候选的 `anchorType` 为 `OUT_OF_LIBRARY`
- THEN 该候选的 `anchorId` SHALL 为空
- AND 候选 SHALL 引用合法 `evidenceRefs`
- AND 该信息 MAY 被用于 `libraryGrowth.candidates`

#### Scenario: 最终决策仍受门禁保护

- GIVEN `diagnosisDecision.libraryFit` 为 `HIT`
- THEN `diagnosisDecision.anchors` SHALL 至少包含一个合法标准库 ID

- GIVEN `diagnosisDecision.libraryFit` 为 `MISS`
- THEN `diagnosisDecision.anchors` SHALL NOT 包含已知标准库 ID

### Requirement: 学生报告自然可读

系统 SHALL 保持 `studentReport` 作为学生端主输出，且不把 `diagnosisCandidates`、标准库命中评审或库外成长评审直接展示给学生。

#### Scenario: 学生端不展示后台诊断候选

- GIVEN 模型输出包含 `diagnosisCandidates`
- WHEN 系统生成学生端反馈
- THEN 学生端 SHALL 优先展示 `studentReport`
- AND 学生端 SHALL NOT 直接展示标准库命中评审或库外成长评审
