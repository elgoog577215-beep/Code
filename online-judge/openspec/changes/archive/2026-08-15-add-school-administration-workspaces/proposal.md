## Why

当前全局管理员直接审核所有教师并逐个调整额度，学校名称却只是教师自填文本，无法形成可信的学校归属、校内治理或额度总量约束。试点扩展到多所学校前，必须把平台治理、学校治理和教师教学拆成三个清晰的信任边界。

## What Changes

- **BREAKING**：账号角色由 `ADMIN/TEACHER` 改为 `PLATFORM_ADMIN/SCHOOL_ADMIN/TEACHER`，管理员与教师使用独立入口和工作台。
- 新增学校实体、唯一有效校管账号和只存哈希的学校注册码；教师注册必须通过注册码绑定学校。
- 平台管理员只管理学校、校管账号、学校额度池以及全平台题目审核，不再审核普通教师或读取教学明细。
- 学校管理员审核本校教师、分配教师额度，并只读查看和导出本校完整教学数据。
- AI 额度改为“学校自然月硬额度池 -> 教师自然月分配”，并发下禁止超分和超扣。
- 停用学校时撤销校管、教师和学生会话，禁止登录、提交和外部 AI 调用，保留历史数据。
- 受控清理全部历史教师账号及其教学数据；已发布公共/共建题转交平台管理员后保留。

## Capabilities

### New Capabilities

- `school-organization-governance`：学校、校管账号、注册码、状态和三级角色边界。
- `school-admin-teaching-visibility`：学校管理员对本校教学数据的完整只读和导出能力。
- `school-ai-quota-pooling`：学校月度硬额度池、教师分配和并发不变量。

### Modified Capabilities

- `teacher-account-governance`：教师注册从自由文本学校改为注册码归属和学校审核。
- `teacher-data-isolation`：教师所有权边界上增加校管只读学校范围，平台管理员不获得教学旁路。
- `teacher-ai-quota-metering`：教师额度由默认 500 次改为学校管理员从学校池分配，默认 0 次。
- `teacher-console-ui`：拆分平台、学校和教师三个独立入口与工作台。
- `schema-migration-and-trial-readiness`：增加受控历史清理、学校约束和学校级 readiness。

## Impact

- 后端新增 organization 模块、学校级权限上下文、平台/校管 API、学校额度服务和只读教学投影。
- 数据库新增 `schools`、`school_ai_quotas`，并为账号和 AI 用量增加学校归属；Flyway 使用 V6–V8 扩展、清理、收紧约束。
- 前端新增两个管理员登录入口和工作台，教师注册字段改为学校注册码，旧 `/api/admin/**` 保留一版受限别名。
- 部署必须在迁移前备份并显式提供 `DELETE_ALL_LEGACY_TEACHERS` 清理确认；缺少任一证明时失败关闭。
