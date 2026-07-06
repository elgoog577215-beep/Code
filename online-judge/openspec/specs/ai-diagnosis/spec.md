# ai-diagnosis Specification

## Purpose
TBD - created by archiving change add-free-diagnosis-candidates. Update Purpose after archive.
## Requirements
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

### Requirement: 自由诊断与标准库路径同步标注
系统 SHALL 允许单诊断 Agent 在同一次正式诊断输出中同时完成真实问题诊断和标准库路径标注；标准库路径 SHALL 辅助教学归档，不得替代模型对题目、完整代码和运行结果的独立判断。

#### Scenario: 候选携带标准库路径
- **WHEN** 模型输出 `diagnosisCandidates`
- **THEN** 每个候选 SHALL 包含最接近的标准库路径描述
- **AND** 每个候选 SHALL 标明 `libraryFit` 为 `HIT`、`PARTIAL` 或 `MISS`

#### Scenario: 具体错误点未覆盖
- **WHEN** 模型诊断出的具体错误点不在当前标准库中，但上级知识点或能力点存在
- **THEN** 候选 SHALL 标记为 `PARTIAL`
- **AND** 候选 SHALL 保留上级标准库路径
- **AND** `libraryGrowth.candidates` SHALL 包含待审核的新错误点候选

#### Scenario: 标准库不改变真实诊断
- **WHEN** 标准库候选不能解释当前代码真实问题
- **THEN** 模型 SHALL 保持自由诊断结论
- **AND** 模型 SHALL NOT 为了命中标准库而把主因改写为不符合代码语义的错误点

#### Scenario: 分层优惠最短路误诊防回归
- **WHEN** 题目要求优惠券将边权变为 `floor(w / 2)` 且学生代码在优惠转移中仍使用原边权
- **THEN** 模型 SHALL 将主因诊断为优惠转移未折半
- **AND** 模型 SHALL NOT 将主因诊断为起点状态或堆初始化错误

### Requirement: 真实模型回归必须验证路径标注
系统 SHALL 使用真实诊断样本验证正式 AI 输出是否同时命中真实主因、标准库路径和成长候选状态。

#### Scenario: 分层优惠最短路真实样本
- **WHEN** 真实模型或离线 fixture 诊断“优惠券跨层转移仍使用原边权”的提交
- **THEN** 评测 SHALL 断言主因包含优惠转移未折半
- **AND** 评测 SHALL 断言输出包含非空标准库路径
- **AND** 评测 SHALL 断言未精确命中标准库时生成待审核成长候选

#### Scenario: 标准库路径缺失
- **WHEN** 真实诊断输出包含 `diagnosisCandidates` 但缺少标准库路径
- **THEN** 评测 SHALL 标记该样本为路径标注失败

