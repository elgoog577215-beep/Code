## ADDED Requirements

### Requirement: 学生可见建议不得伪造代码证据
系统 SHALL 在学生可见 AI 反馈中区分有代码证据、无直接代码证据和证据无效；前端 MUST NOT 因摘要、全局诊断证据或第一条建议证据而让另一条建议看起来拥有代码证据。

#### Scenario: 提升建议无直接代码证据
- **WHEN** 提升建议基于调试方法、测试策略、复杂度意识或迁移练习而没有具体代码行
- **THEN** 学生端 SHALL 展示该建议本身
- **AND** SHALL 展示“无直接代码证据”语义的空态或不展示代码证据卡

#### Scenario: 修正建议与摘要同时存在
- **WHEN** 学生反馈包含摘要和逐条修正建议
- **THEN** 学生端 SHALL 将摘要作为概览
- **AND** SHALL 只在逐条建议区域展示对应 item 自身证据
- **AND** 同一份代码证据 MUST NOT 因概览和逐条区域重复出现造成代码证据被展示两遍

### Requirement: 学生可见逐条建议数量必须来源于结构化 item
系统 SHALL 使用结构化 `repairItems` 与 `improvementItems` 作为学生逐条反馈来源；`primaryAction`、摘要文本或下一步行动只能作为概览或引导，不得替代逐条列表。

#### Scenario: 存在多条修正建议
- **WHEN** 后端返回多条有效 `repairItems`
- **THEN** 学生端 SHALL 展示多条逐条建议
- **AND** SHALL NOT 只展示 `primaryAction` 或第一条 item

#### Scenario: 下一步行动只有一条
- **WHEN** 后端返回单条下一步行动
- **THEN** 学生端 MAY 将其作为优先行动展示
- **AND** SHALL NOT 因下一步行动只有一条而隐藏多条修正或提升建议
