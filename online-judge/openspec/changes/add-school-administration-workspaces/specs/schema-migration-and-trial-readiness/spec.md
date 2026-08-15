## MODIFIED Requirements

### Requirement: 历史教师清理必须受备份和确认门禁保护
系统 SHALL 在扩展学校结构后永久清理所有旧教师账号及其教学数据，但 MUST 在存在可验证备份证明和精确确认值 `DELETE_ALL_LEGACY_TEACHERS` 时才执行；已发布公共/共建题 SHALL 转交平台管理员并保留。

#### Scenario: 缺少清理确认
- **WHEN** 数据库仍有旧教师且部署未提供精确确认值
- **THEN** 迁移 SHALL 失败并停止应用
- **AND** 旧数据 MUST NOT 被部分删除

### Requirement: Readiness 必须覆盖学校组织不变量
Readiness SHALL 检查有效平台管理员、活动学校唯一校管、账号学校归属、额度超分、学校注册码和遗留角色。

#### Scenario: 学校教师分配总量超过学校额度
- **WHEN** readiness 发现任一学校当月超分
- **THEN** 系统 SHALL 标记不可推广并报告学校 ID 与计数而不泄漏教师或学生明细
