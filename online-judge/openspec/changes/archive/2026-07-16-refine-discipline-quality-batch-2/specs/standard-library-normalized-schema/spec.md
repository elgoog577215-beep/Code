## ADDED Requirements

### Requirement: 新增正式提升点必须同步规范化主表和兼容读取结构
通过数据库迁移新增正式提升点时，系统 SHALL 使用同一稳定 code 同步规范化提升点、启用平铺快照和 legacy mapping，并 SHALL 保持能力点、知识点和关联错因归属一致。

#### Scenario: 新增第二批提升点
- **WHEN** V3 为缺少提升点的正式能力点新增提升路径
- **THEN** `ai_standard_improvement_points` SHALL 保存训练目标、练习策略、学生收益、教师解释和关联错因
- **AND** `ai_standard_library_items` SHALL 保存同 code 的启用 `IMPROVEMENT_POINT` 快照
- **AND** `ai_standard_library_legacy_mappings` SHALL 保存同 code 的 `MAPPED` 映射

#### Scenario: 从精确知识点读取新增提升点
- **WHEN** 教师或 AI 展开第二批提升点的主知识节点诊断层
- **THEN** 系统 SHALL 返回其所属启用能力点和该提升点
- **AND** 导航结果 SHALL 保留提升点的主知识节点、相关知识节点和关联错因

### Requirement: 提升点必须提供可执行且可验证的迁移练习
正式提升点 SHALL 描述学生修复当前错误后如何练习、用什么边界或状态检查结果以及教师如何观察掌握情况，不得只重复能力点名称或使用抽象鼓励语。

#### Scenario: 审核第二批提升内容
- **WHEN** 第二批提升点进入正式数据库
- **THEN** 每条提升点 SHALL 关联至少一个启用易错点
- **AND** `practice_strategy` SHALL 包含可执行动作或自测样例类别
- **AND** `teacher_explanation` SHALL 包含可观察的讲解或验收方法
