## ADDED Requirements

### Requirement: 提示词必须表达统一树和诊断层关系
正式 AI 诊断 prompt SHALL 明确知识树只到知识点，能力点和易错点属于知识点以下的诊断层，并 SHALL 要求模型不要把知识树和标准库解释成两套平行数据库。

#### Scenario: 生成诊断报告 prompt
- **WHEN** 系统构造 `diagnosis-report-v2` prompt
- **THEN** prompt SHALL 明确 `standardLibrary.knowledgeGroups` 的主链路是“知识点 -> 能力点 -> 易错点/提升点”
- **AND** prompt SHALL 明确知识点回答“学什么”，能力点回答“会做什么”，易错点回答“常错在哪里”
- **AND** prompt SHALL 要求学生可见知识路径优先使用知识点中文路径

#### Scenario: 候选包包含相关知识
- **WHEN** 标准库候选包含相关知识节点或相邻易错点
- **THEN** prompt SHALL 要求模型把它们作为上下文参考
- **AND** prompt SHALL NOT 要求模型把相关知识节点平铺成多个独立错因
