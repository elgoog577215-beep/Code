## MODIFIED Requirements

### Requirement: 管理员必须具有独立治理界面
平台管理员、学校管理员和教师 SHALL 使用独立登录入口、导航与工作台；直接刷新任一 `/app/**` 深层地址 SHALL 返回前端应用而非 404。

#### Scenario: 管理员处理待审核教师
- **WHEN** 学校管理员打开本校教师申请列表
- **THEN** 页面 SHALL 显示待审核申请及批准、拒绝操作

#### Scenario: 学校管理员登录成功
- **WHEN** 校管从 school-admin portal 登录且无需强制改密
- **THEN** 前端 SHALL 跳转到学校管理员概览
- **AND** 页面 MUST NOT 展示教师写操作或平台治理入口

#### Scenario: 英文窄屏查看额度分配
- **WHEN** 用户在英文模式和手机宽度打开学校管理员额度页
- **THEN** 表单、状态和错误 SHALL 使用英文且无横向遮挡
