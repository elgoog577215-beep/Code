## ADDED Requirements

### Requirement: 教师介入建议必须暴露成效状态

系统 SHALL 在课堂复盘建议中输出教师介入成效状态，用结构化字段说明教师动作、后续证据、当前判断和下一步教学动作。

#### Scenario: 教师尚未反馈课堂复盘建议

- **WHEN** 某条课堂复盘建议没有教师反馈
- **THEN** 响应 MUST 将成效状态标记为 `NO_FEEDBACK`
- **AND** 响应 MUST 说明需要教师先采纳、调整或忽略该建议

#### Scenario: 教师采纳后学生通过

- **WHEN** 教师对某条课堂复盘建议执行 `ACCEPTED` 或 `MODIFIED`
- **AND** 该建议的示例题或证据提交之后出现同作业后续通过提交
- **THEN** 成效状态 MUST 标记为 `IMPROVED`
- **AND** 响应 MUST 包含教师反馈时间、后续提交 id、证据提交 id 和可读总结

#### Scenario: 教师采纳后仍命中同类错因

- **WHEN** 教师对某条课堂复盘建议执行 `ACCEPTED` 或 `MODIFIED`
- **AND** 后续提交仍失败并命中建议证据标签
- **THEN** 成效状态 MUST 标记为 `STILL_STUCK`
- **AND** 响应 MUST 标记需要升级或降低提示颗粒度
- **AND** 下一步动作 MUST 要求教师介入、补充更小样例或重新收集证据

#### Scenario: 教师采纳后没有后续提交

- **WHEN** 教师对某条课堂复盘建议执行 `ACCEPTED` 或 `MODIFIED`
- **AND** 反馈时间之后没有相关后续提交
- **THEN** 成效状态 MUST 标记为 `WAITING_FOLLOWUP`
- **AND** 响应 MUST 说明当前只能等待可观察证据

#### Scenario: 教师忽略建议

- **WHEN** 教师对某条课堂复盘建议执行 `DISMISSED`
- **THEN** 成效状态 MUST 标记为 `DISMISSED`
- **AND** 该建议 MUST NOT 计入教师介入失败或待升级数量

### Requirement: 教师介入闭环必须保留证据链

系统 SHALL 为教师介入成效输出可追踪证据引用，使教师能解释为什么系统判断已改善、仍卡住或等待后续。

#### Scenario: 介入成效存在后续提交

- **WHEN** 成效状态基于后续提交计算
- **THEN** 响应 MUST 包含 `evidenceSubmissionIds`
- **AND** 响应 MUST 包含 `followupSubmissionId`
- **AND** 响应 MUST 包含 `matchedTags`

#### Scenario: 介入成效需要升级

- **WHEN** 成效状态为 `STILL_STUCK`
- **THEN** 响应 MUST 将 `needsEscalation` 标记为 true
- **AND** 响应 MUST 给出面向教师的 `recommendedAction`

### Requirement: 教师工作台必须展示介入成效

教师工作台 SHALL 在课堂复盘建议卡片中展示教师介入成效状态、下一步动作和关键证据，帮助教师闭环处理 AI 建议。

#### Scenario: 教师打开包含复盘建议的作业

- **WHEN** 作业概览返回课堂复盘建议
- **THEN** 页面 MUST 展示每条建议的介入成效状态
- **AND** 页面 MUST 展示需要升级的提示
- **AND** 页面 MUST 在没有反馈或等待后续时显示非阻塞说明

#### Scenario: 介入成效字段缺失

- **WHEN** 后端暂未返回介入成效字段
- **THEN** 页面 MUST 保持兼容，继续展示原有复盘建议和反馈按钮
