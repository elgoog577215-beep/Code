## ADDED Requirements

### Requirement: 诊断系统必须生成记忆证据校准摘要

系统 MUST 在构造外部模型诊断输入时，根据当前提交证据和学生学习记忆生成结构化记忆证据校准摘要。

#### Scenario: 记忆与当前证据一致
- **WHEN** 学生学习记忆中的重复错因标签与当前提交的高置信度候选信号一致
- **THEN** `ModelDiagnosisBrief` MUST 标记该记忆为 `ALIGNED` 或等价状态，并保留当前证据和记忆证据引用

#### Scenario: 记忆与当前证据冲突
- **WHEN** 学生学习记忆中的重复错因标签与当前提交的高置信度候选信号不一致
- **THEN** `ModelDiagnosisBrief` MUST 标记该记忆为 `CONFLICTING` 或等价状态，并说明当前提交直接证据优先

#### Scenario: 记忆只能用于教学调节
- **WHEN** 学生学习记忆存在重复卡点或历史干预无效，但当前提交没有足够证据支持同一错因
- **THEN** `ModelDiagnosisBrief` MUST 标记该记忆只能用于教学动作、提示粒度或教师关注，不能作为主错因唯一依据

### Requirement: 模型主诊断必须被当前提交证据支撑

系统 MUST 校验外部模型选择的主错因和细粒度错因是否至少有一个当前提交证据、规则候选信号、编译/运行/评测事实或源码证据支撑。

#### Scenario: 模型只引用记忆证据选择主错因
- **WHEN** 外部模型输出的 `primaryIssueTag` 只由 `memory:*` evidence refs 支撑
- **THEN** 系统 MUST 拒绝该诊断输出，或将其降级为需要更多证据的本地规则结果

#### Scenario: 模型同时引用当前证据和记忆证据
- **WHEN** 外部模型输出的主错因同时引用当前提交证据和相关记忆证据
- **THEN** 系统 MAY 接受该诊断输出，并允许记忆影响教学任务粒度和教师备注

#### Scenario: 记忆冲突但模型选择记忆标签
- **WHEN** 结构化记忆校准摘要显示记忆与当前强证据冲突，且模型仍选择只被记忆支持的标签
- **THEN** 系统 MUST 拒绝该输出，并在失败原因或 trace 中记录记忆过度依赖风险

### Requirement: 记忆校准必须进入可观测诊断链路

系统 MUST 在诊断结果或诊断 trace 中保留记忆校准状态，使后续教师端统计、live eval 和问题排查能够识别模型是否正确使用学生记忆。

#### Scenario: 诊断完成后记录校准状态
- **WHEN** `DiagnosticAgentService` 完成一次诊断
- **THEN** 诊断 trace MUST 包含记忆校准状态，例如是否 aligned、conflicting、teaching-only 或 teacher-review recommended

#### Scenario: 校准建议教师复核
- **WHEN** 学生记忆显示长期重复卡点、历史干预无效或教师修正与模型候选冲突
- **THEN** 系统 MUST 在校准摘要或 trace 中标记建议教师关注
