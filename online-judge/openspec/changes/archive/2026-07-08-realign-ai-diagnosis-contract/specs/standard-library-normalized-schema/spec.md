## ADDED Requirements

### Requirement: 标准库成长候选必须由后端补齐来源和证据线索
系统 SHALL 在保存 AI 标准库成长候选时由后端补齐当前题目、当前提交、诊断命中状态和证据线索；模型 MAY 提出候选内容，但 MUST NOT 被要求可靠填写后端来源 ID。

#### Scenario: 保存成长候选
- **WHEN** 正式诊断输出有效标准库成长候选
- **THEN** 后端 SHALL 使用当前诊断上下文补齐 `sourceProblemId`、`sourceSubmissionId` 或等价来源字段
- **AND** 后端 SHALL 保存该候选关联的证据 refs 或无直接代码证据状态

#### Scenario: 模型填错来源 ID
- **WHEN** 模型返回的来源题目或提交 ID 与当前诊断上下文不一致
- **THEN** 后端 SHALL 以当前后端上下文为准
- **AND** SHALL NOT 使用模型返回的错误来源污染候选池

### Requirement: 成长候选必须聚合同类重复发现
系统 SHALL 对同一路径、同类名称或同类标准库建议的成长候选进行聚合，保留出现次数、最近来源、证据线索和相似条目，而不是每次诊断都生成孤立候选。

#### Scenario: 重复候选出现
- **WHEN** 新诊断产生与已有待审核候选同路径且同类的候选
- **THEN** 后端 SHALL 增加出现次数或追加来源线索
- **AND** SHALL 保持候选为教师可审核记录

#### Scenario: 相似但不确定候选
- **WHEN** 新候选与已有候选或正式库条目相似但无法自动合并
- **THEN** 后端 SHALL 标记相似关系
- **AND** SHALL 留给教师在候选池中合并、编辑、拒绝或忽略
