# school-admin-teaching-visibility Specification

## Purpose
允许学校管理员监督本校完整教学过程，同时用只读接口、学校范围校验和审计约束保护学生个人信息与教师教学所有权。
## Requirements
### Requirement: 校管必须能只读查看本校教学明细
学校管理员 SHALL 能查看和导出本校教师、班级、名单、作业、提交、代码、成绩和分析，但 MUST NOT 修改任何教学资源。

#### Scenario: 校管读取本校提交详情
- **WHEN** 学校管理员请求本校教师作业下的学生提交
- **THEN** 系统 SHALL 返回提交与分析只读投影
- **AND** 系统 SHALL 记录不含学生姓名、学号和代码正文的访问审计

#### Scenario: 校管调用教师写接口
- **WHEN** `SCHOOL_ADMIN` 请求创建、修改或删除班级、作业、题目或提交
- **THEN** 系统 SHALL 返回 `FORBIDDEN`

### Requirement: 平台不得通过应用读取教学明细
平台管理员 SHALL 只能查看学校级账号数和额度汇总，MUST NOT 通过平台 API 获取普通教师或学生明细。

#### Scenario: 平台管理员请求校管教学接口
- **WHEN** `PLATFORM_ADMIN` 调用 `/api/school-admin/teaching/**`
- **THEN** 系统 SHALL 返回 `SCHOOL_ADMIN_REQUIRED`
