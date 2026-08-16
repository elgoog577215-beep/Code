## ADDED Requirements

### Requirement: 标准库挂接结果必须提供证据相关的应用场景语境
系统 SHALL 在标准库逐层挂接完成后，把与已选知识点和正式能力点相关的有界课堂—竞赛场景放入学生建议生成使用的同一 `StandardLibraryPack`；场景 SHALL 作为理解教学任务、验证动作和竞赛迁移的参考语境，MUST NOT 成为新的诊断 Agent、强制命中 ID 或当前提交证据。

#### Scenario: 已选能力存在成对场景
- **WHEN** 某个问题已经挂接到带应用场景的正式能力点
- **THEN** 学生建议阶段 SHALL 能读取该能力对应的课堂检查和竞赛迁移语境
- **AND** 建议仍 SHALL 引用核心诊断 issue 和当前提交的合法 evidenceRefs

#### Scenario: 已选能力没有相关场景
- **WHEN** 某个问题的已选能力没有启用场景
- **THEN** 系统 SHALL 继续使用知识点、能力点、易错点和提升点生成建议
- **AND** 场景缺失 MUST NOT 把该问题改成 `ATTACHMENT_FAILED` 或阻断其他问题

#### Scenario: 场景语境与提交证据冲突
- **WHEN** 场景中的常见失败与当前代码和判题事实不一致
- **THEN** AI MUST 以当前问题、代码和判题证据为准
- **AND** AI MUST NOT 为匹配场景而虚构易错点命中或标准库 ID
