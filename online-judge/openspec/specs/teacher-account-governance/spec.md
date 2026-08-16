# teacher-account-governance Specification

## Purpose
为多教师试用提供可审核、可撤销、可追踪且完整留痕的教师与管理员身份治理，替代无法区分责任主体的统一口令，并覆盖账号全生命周期的安全控制。
## Requirements
### Requirement: 教师账号必须经审核后使用
系统 SHALL 接受用户名、密码、姓名和学校注册码的教师注册申请，并 SHALL 仅允许所属学校为 `ACTIVE` 且账号状态为 `ACTIVE` 的教师进入教师工作台。

#### Scenario: 新教师提交注册
- **WHEN** 合法账号资料和有效学校注册码提交注册
- **THEN** 系统 SHALL 把申请绑定到注册码对应学校并返回 `PENDING`
- **AND** 只有该校学校管理员 SHALL 能看到和审核申请

#### Scenario: 待审核教师尝试登录
- **WHEN** `PENDING` 账号提交正确密码
- **THEN** 系统 SHALL 拒绝登录并返回 `ACCOUNT_PENDING`

### Requirement: 管理员必须治理账号生命周期
学校管理员 SHALL 只能审核、拒绝、停用、恢复和重置本校教师；平台管理员 SHALL 管理学校及学校管理员，但 MUST NOT 管理普通教师。

#### Scenario: 管理员批准申请
- **WHEN** 学校管理员批准一项本校待审核申请
- **THEN** 账号状态 SHALL 变为 `ACTIVE`
- **AND** 系统 SHALL 记录审核人和审核时间

#### Scenario: 管理员重置密码
- **WHEN** 学校管理员为本校教师生成临时密码
- **THEN** 所有既有教师会话 SHALL 立即失效
- **AND** 账号 SHALL 在下次登录后强制修改密码

#### Scenario: 校管尝试管理其他学校教师
- **WHEN** 学校管理员猜测其他学校教师 ID
- **THEN** 系统 SHALL 返回 403 或 404

### Requirement: 教师会话必须安全且可撤销
账号登录 SHALL 指定目标 portal，成功会话 SHALL 使用 HttpOnly Cookie 保存随机令牌，服务端 SHALL 仅保存令牌哈希并包含角色以及校管/教师的学校 ID；角色与 portal 不一致时 MUST NOT 创建会话，改密、重置、拒绝、停用或整校停用时 SHALL 撤销相关会话。

#### Scenario: 教师成功登录
- **WHEN** `ACTIVE` 账号从与角色匹配的 portal 提交正确用户名和密码
- **THEN** 系统 SHALL 创建数据库会话并设置 `HttpOnly`、`SameSite=Lax` Cookie
- **AND** 生产环境 Cookie SHALL 设置 `Secure`

#### Scenario: 教师账号尝试登录平台管理入口
- **WHEN** `TEACHER` 使用正确凭据请求 `PLATFORM_ADMIN` portal
- **THEN** 系统 SHALL 返回 `PORTAL_ROLE_MISMATCH`
- **AND** 系统 MUST NOT 设置会话 Cookie

#### Scenario: 连续登录失败
- **WHEN** 同一账号连续失败达到 5 次
- **THEN** 系统 SHALL 锁定登录 15 分钟

### Requirement: 管理接口必须要求管理员角色
`/api/platform-admin/**` SHALL 只允许平台管理员，`/api/school-admin/**` SHALL 只允许学校管理员；兼容 `/api/admin/**` SHALL 按治理对象应用同样角色和学校范围并记录审计事件。

#### Scenario: 普通教师调用管理接口
- **WHEN** `TEACHER` 角色请求任一管理接口
- **THEN** 系统 SHALL 返回 `FORBIDDEN`
