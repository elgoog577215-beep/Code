## MODIFIED Requirements

### Requirement: 结构化建议数量应在后处理和展示中保真
系统 SHALL 将模型返回的 `repairItems` 与 `improvementItems` 作为学生可见逐条反馈的主数据源；后端后处理和前端展示 MUST 保留证据有效的多条结构化建议，不得因摘要或主项展示把多条结果整体压缩为一条。

#### Scenario: 后处理保留多条结构化建议
- **WHEN** 模型返回多条互不重复且未触发安全风险的修正项或提升项
- **THEN** normalizer 和提高层校正 SHALL 保留这些有效条目
- **AND** 仅删除或标记被判定为不可信、跑偏、重复或证据无效的条目
- **AND** 系统 MUST NOT 生成单个本地兜底条目整体覆盖有效的多条模型输出

#### Scenario: 学生端展示不重复第一条建议
- **WHEN** 学生反馈同时包含 `studentReport` 摘要和逐条 `repairItems` 或 `improvementItems`
- **THEN** 学生端 SHALL 将摘要作为概览展示
- **AND** 学生端 SHALL 在逐条列表中展示结构化建议及其证据
- **AND** 同一条建议的标题、正文或代码证据 MUST NOT 因摘要区域和逐条区域重复渲染而出现两遍
