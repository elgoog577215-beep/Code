## ADDED Requirements

### Requirement: 标准库必须复用既有知识树
AI 标准库规范结构 SHALL 复用 `informatics_knowledge_nodes` 作为知识树来源，并 SHALL 在能力点、易错点和提升点中通过知识节点 code 关联到该知识树。

#### Scenario: 能力点关联知识节点
- **WHEN** 标准库同步能力点种子
- **THEN** 系统 SHALL 保存能力点到独立能力点表，并 SHALL 保留该能力点关联的知识节点 code

#### Scenario: 不重复创建知识树
- **WHEN** 标准库规范结构初始化
- **THEN** 系统 SHALL NOT 创建另一套标准库专用知识树表来重复表达大章节、小章节和知识点

### Requirement: 标准库必须按能力易错提升分表存储
AI 标准库规范结构 SHALL 将能力点、易错点和提升点存储为独立实体，其中易错点 SHALL 归属于能力点，提升点 SHALL 归属于能力点或知识节点。

#### Scenario: 易错点归属能力点
- **WHEN** 同步一个 `MISTAKE_POINT` 或兼容 `BASIC_CAUSE` 条目
- **THEN** 系统 SHALL 将它保存为规范易错点，并 SHALL 记录它关联的能力点 code

#### Scenario: 提升点归属能力或知识点
- **WHEN** 同步一个 `IMPROVEMENT_POINT` 条目
- **THEN** 系统 SHALL 将它保存为规范提升点，并 SHALL 记录它关联的能力点 code、知识节点 code 或二者之一

### Requirement: 新结构不得引入独立证据模式表
AI 标准库规范结构 SHALL NOT 新增独立证据模式表；标准库 SHALL 只保存教学知识结构和轻量自然语言诊断提示，当前提交证据 SHALL 来自提交代码、评测结果、错误日志和 AI 分析输出。

#### Scenario: 同步旧证据字段
- **WHEN** 旧扁平条目包含 `evidenceSignals`、`commonCodePatterns`、`judgeSignals` 或 `requiredEvidence`
- **THEN** 规范结构同步 SHALL NOT 将这些字段写入独立证据模式表

### Requirement: 旧扁平条目必须可映射到新结构
系统 SHALL 保存旧 `layer/code` 与新结构目标之间的映射，使历史数据和旧接口可以兼容新主结构。

#### Scenario: 旧能力点映射
- **WHEN** 一个旧 `SKILL_UNIT` 种子被同步到规范能力点
- **THEN** 系统 SHALL 保存从旧 `SKILL_UNIT/code` 到规范能力点的映射

#### Scenario: 旧基础错因映射
- **WHEN** 一个旧 `BASIC_CAUSE` 种子被同步到规范结构
- **THEN** 系统 SHALL 保存从旧 `BASIC_CAUSE/code` 到规范易错点的映射

### Requirement: 标准库同步必须幂等
规范标准库同步 SHALL 可重复执行，重复执行不得产生重复能力点、易错点、提升点或旧映射。

#### Scenario: 重复运行同步器
- **WHEN** 标准库同步器连续执行两次
- **THEN** 第二次执行 SHALL NOT 增加重复规范条目，并 SHALL 保持已有 code 唯一
