## Purpose

用班级码和严格名单建立课堂级学生身份边界，并让全班作业和指定学生作业都具有一致、可解释的可见与统计口径。

## ADDED Requirements

### Requirement: 班级加入码必须只保存哈希
系统 SHALL 在创建班级或轮换加入码时仅返回一次明文，并 SHALL 只持久化不可逆哈希。

#### Scenario: 教师轮换班级码
- **WHEN** 班级所有者请求轮换加入码
- **THEN** 系统 SHALL 返回新的明文码一次
- **AND** 旧码 SHALL 立即不能用于新登录

### Requirement: 学生必须严格匹配有效名单
学生登录 SHALL 同时匹配班级码、姓名、学号和 `ACTIVE` 名单记录；系统 MUST NOT 在登录时自动创建学生。

#### Scenario: 名单完全匹配
- **WHEN** 学生提交正确班级码、姓名和学号且名单状态为 `ACTIVE`
- **THEN** 系统 SHALL 创建 HttpOnly 学生会话

#### Scenario: 名单不匹配或需修复
- **WHEN** 任一身份字段错误、名单停用或状态为 `NEEDS_REVIEW`
- **THEN** 系统 SHALL 返回 `ROSTER_MISMATCH`
- **AND** 学生记录数量 SHALL 保持不变

### Requirement: 名单导入必须以学号为身份键
名单导入 SHALL 要求学号，并 SHALL 在同一班级内以学号更新既有学生；旧数据缺少学号时 SHALL 标记为 `NEEDS_REVIEW`。

#### Scenario: 重复导入相同学号
- **WHEN** 教师向同一班级重复导入相同学号
- **THEN** 系统 SHALL 更新该学生资料而非新增重复身份

### Requirement: 作业必须明确目标模式
作业 SHALL 使用 `CLASS` 动态覆盖当前有效名单，或使用 `STUDENTS` 保存创建时明确的同班学生集合。

#### Scenario: 全班作业后新增学生
- **WHEN** `CLASS` 作业仍有效且班级新增一名 `ACTIVE` 学生
- **THEN** 新学生 SHALL 能看到该作业
- **AND** 当前完成率分母 SHALL 包含该学生

#### Scenario: 创建定向作业
- **WHEN** 教师选择 `STUDENTS` 并提交学生集合
- **THEN** 所有接收人 SHALL 属于该作业班级和当前教师
- **AND** 非接收学生访问时 SHALL 返回 `ASSIGNMENT_NOT_TARGETED`

### Requirement: 提交必须通过统一作业范围校验
学生查看或提交作业时 SHALL 验证目标范围、题目归属和有效时间窗口；移除或转班 SHALL 阻止后续访问但保留历史提交。

#### Scenario: 历史学生已离开班级
- **WHEN** 已有提交的学生被停用或转班
- **THEN** 历史提交 SHALL 保留并标记为非当前名单历史记录
- **AND** 当前完成率分母 MUST NOT 包含该学生

