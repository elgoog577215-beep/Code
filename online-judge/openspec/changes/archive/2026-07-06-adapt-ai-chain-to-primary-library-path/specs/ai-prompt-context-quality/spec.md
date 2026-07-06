## MODIFIED Requirements

### Requirement: 诊断报告提示词应分离学生报告与后端元数据
系统 SHALL 在正式诊断提示词中要求模型先生成学生自然语言报告，再提供后端审计和标准库 metadata；学生可见报告 SHALL NOT 机械堆叠内部结构字段。

#### Scenario: 生成正式诊断报告
- **WHEN** 模型使用 `diagnosis-report-v2` 生成诊断结果
- **THEN** prompt SHALL 明确 `studentReport` 是学生优先阅读的自然反馈
- **AND** prompt SHALL 明确 `diagnosisDecision`、`diagnosisCandidates`、`teacherTrace` 和 `libraryGrowth` 是后端审计与标准库成长信息

#### Scenario: 多条建议存在
- **WHEN** 当前提交存在多个独立错因或提升方向
- **THEN** prompt SHALL 要求逐条列表由证据决定数量
- **AND** prompt SHALL NOT 要求固定一条或把多条问题硬塞进单个字符串

#### Scenario: 标准库包含主路径和相关路径
- **WHEN** `standardLibrary.knowledgeGroups` 或兼容列表包含 `primaryKnowledgeNodeCode` 与 `relatedKnowledgeNodeCodes`
- **THEN** prompt SHALL 要求模型优先使用主路径理解知识父子关系
- **AND** prompt SHALL 要求模型把相关路径视为辅助上下文，而不是独立错因或独立学生标签
