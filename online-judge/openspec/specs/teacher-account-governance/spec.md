# teacher-account-governance Specification

## Purpose
为多教师试用提供可审核、可撤销、可追踪且完整留痕的教师与管理员身份治理，替代无法区分责任主体的统一口令，并覆盖账号全生命周期的安全控制。
## Requirements
### Requirement: 教师账号必须经审核后使用
系统 SHALL 接受用户名、密码、姓名和学校的教师注册申请，并 SHALL 仅允许状态为 `ACTIVE` 的账号进入教师工作台。

#### Scenario: 新教师提交注册
- **WHEN** 合法且未占用的规范化用户名提交注册
- **THEN** 系统 SHALL 返回申请 ID 和 `PENDING`
- **AND** 系统 MUST NOT 自动创建已登录会话

#### Scenario: 待审核教师尝试登录
- **WHEN** `PENDING` 账号提交正确密码
- **THEN** 系统 SHALL 拒绝登录并返回 `ACCOUNT_PENDING`

### Requirement: 管理员必须治理账号生命周期
管理员 SHALL 能审核、拒绝、停用、恢复教师，并 SHALL 能生成仅返回一次的临时密码。

#### Scenario: 管理员批准申请
- **WHEN** 管理员批准一项待审核申请
- **THEN** 账号状态 SHALL 变为 `ACTIVE`
- **AND** 系统 SHALL 记录审核人和审核时间

#### Scenario: 管理员重置密码
- **WHEN** 管理员为教师生成临时密码
- **THEN** 所有既有教师会话 SHALL 立即失效
- **AND** 账号 SHALL 在下次登录后强制修改密码

### Requirement: 教师会话必须安全且可撤销
教师登录 SHALL 使用 HttpOnly Cookie 保存随机令牌，服务端 SHALL 仅保存令牌哈希，并 SHALL 在改密、重置、拒绝或停用时撤销全部会话。

#### Scenario: 教师成功登录
- **WHEN** `ACTIVE` 教师提交正确用户名和密码
- **THEN** 系统 SHALL 创建数据库会话并设置 `HttpOnly`、`SameSite=Lax` Cookie
- **AND** 生产环境 Cookie SHALL 设置 `Secure`

#### Scenario: 连续登录失败
- **WHEN** 同一账号连续失败达到 5 次
- **THEN** 系统 SHALL 锁定登录 15 分钟

### Requirement: 管理接口必须要求管理员角色
所有 `/api/admin/**` 接口 SHALL 要求有效管理员会话，并 SHALL 记录账号审核、状态变化和密码重置审计事件。

#### Scenario: 普通教师调用管理接口
- **WHEN** `TEACHER` 角色请求任一管理接口
- **THEN** 系统 SHALL 返回 `FORBIDDEN`
