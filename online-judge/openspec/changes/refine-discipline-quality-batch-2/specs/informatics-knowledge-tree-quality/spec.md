## ADDED Requirements

### Requirement: 后续知识点精修批次必须由生产使用证据和领域缺口驱动
知识树内容精修 SHALL 优先选择已经被启用标准库条目引用、仍使用已知模板描述且位于薄弱领域或课堂高频路径的知识点，并 SHALL 保存独立批次版本以便复核质量增量。

#### Scenario: 选择第二批精修节点
- **WHEN** 系统从 MATH、ENG、BASIC 选择第二批知识点
- **THEN** 候选 SHALL 是启用的 `KNOWLEDGE_POINT` 且仍使用模板描述
- **AND** 排序 SHALL 使用规范化能力点、易错点和提升点的不同条目引用数
- **AND** 人工审校 SHALL 优先最低覆盖领域与课堂高频入口，而不是按领域平均凑数

#### Scenario: 保存第二批人工精修内容
- **WHEN** 一个知识点进入第二批正式精修
- **THEN** 系统 SHALL 更新概念边界、可观察学习目标、具体典型任务、有效别名和真实前置知识
- **AND** `library_version` SHALL 标记为 `informatics-knowledge-discipline-v2`
- **AND** 系统 SHALL NOT 只对原模板做同义改写
