## MODIFIED Requirements

### Requirement: 教师账号必须经所属学校审核后使用
系统 SHALL 接受用户名、密码、姓名和学校注册码的教师注册申请，并 SHALL 仅允许所属学校为 `ACTIVE` 且账号状态为 `ACTIVE` 的教师进入教师工作台。

#### Scenario: 新教师提交注册
- **WHEN** 合法账号资料和有效学校注册码提交注册
- **THEN** 系统 SHALL 把申请绑定到注册码对应学校并返回 `PENDING`
- **AND** 只有该校学校管理员 SHALL 能看到和审核申请

### Requirement: 账号会话必须带有角色与学校边界
账号登录 SHALL 指定目标 portal，成功会话 SHALL 包含角色以及校管/教师的学校 ID；角色与 portal 不一致时 MUST NOT 创建会话。

#### Scenario: 教师账号尝试登录平台管理入口
- **WHEN** `TEACHER` 使用正确凭据请求 `PLATFORM_ADMIN` portal
- **THEN** 系统 SHALL 返回 `PORTAL_ROLE_MISMATCH`
- **AND** 系统 MUST NOT 设置会话 Cookie
