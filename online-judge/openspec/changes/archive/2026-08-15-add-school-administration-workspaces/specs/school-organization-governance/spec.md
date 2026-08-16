## Purpose

建立平台、学校和教师三级身份与可信学校归属，使教师治理能够下放到唯一校管账号，同时阻止平台和其他学校越权读取教学明细。

## ADDED Requirements

### Requirement: 平台必须创建并治理学校
平台管理员 SHALL 能创建、停用和恢复学校，并 SHALL 为每所学校指定恰好一个当前有效的学校管理员账号。

#### Scenario: 平台创建学校
- **WHEN** 平台管理员提交合法学校名称和校管账号资料
- **THEN** 系统 SHALL 原子创建学校和校管账号
- **AND** 临时密码与学校注册码 SHALL 只返回一次且不得写入日志

### Requirement: 教师必须通过学校注册码归属
教师注册 SHALL 使用当前有效学校注册码解析学校，MUST NOT 接受客户端自由指定学校 ID 或学校名称。

#### Scenario: 使用已轮换注册码注册
- **WHEN** 教师提交学校已轮换的旧注册码
- **THEN** 系统 SHALL 返回 `INVALID_SCHOOL_CODE`
- **AND** 系统 MUST NOT 创建账号或暴露学校信息

### Requirement: 学校管理员只能治理本校教师
学校管理员 SHALL 只能审核、拒绝、停用、恢复和重置本校教师账号，平台管理员 SHALL 不执行普通教师日常治理。

#### Scenario: 校管猜测其他学校教师 ID
- **WHEN** 学校管理员请求其他学校教师的状态或密码操作
- **THEN** 系统 SHALL 返回 403 或 404
- **AND** 响应 MUST NOT 暴露目标教师所属学校

### Requirement: 停用学校必须冻结整校
平台停用学校时 SHALL 撤销校管、教师和学生会话，并 SHALL 阻止新登录、提交和外部 AI 调用。

#### Scenario: 已登录学生在学校停用后提交
- **WHEN** 学生会话所属学校变为 `SUSPENDED`
- **THEN** 提交 SHALL 被拒绝并返回 `SCHOOL_SUSPENDED`
- **AND** 历史提交 SHALL 保留
