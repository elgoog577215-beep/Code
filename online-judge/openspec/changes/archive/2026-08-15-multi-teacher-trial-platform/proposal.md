## Why

当前平台仍以学校统一口令和全局共享教学数据运行，无法安全地交给多位教师试用。要进入教研群推广，必须先建立可审核的教师身份、严格的数据归属、名单制学生入口、可治理题库以及可审计的 AI 免费额度。

## What Changes

- **BREAKING**：生产环境停用学校统一教师口令，改为用户名密码注册、管理员审核和数据库会话。
- **BREAKING**：取消公开班级枚举和学生登录时自动创建身份，学生必须以班级码、姓名、学号严格匹配有效名单。
- 为班级、作业、学生和分析链路建立教师归属，并在仓储查询与服务层同时校验归属。
- 作业支持动态全班目标和明确学生集合，并统一校验可见、提交、时间窗口与统计分母。
- 建立公共、共建、私有三级题库及不可变版本、审核、快照与归档规则。
- 建立统一 AI Provider 网关、按教师自然月计量的 500 次默认额度、幂等扣费和失败不扣费规则。
- 引入 Flyway 接管正式数据库结构，生产 Hibernate 仅校验结构，并扩展备份、迁移演练、审计、readiness 和指标。
- 首版明确不实现支付、对外 API Token、教师自带 Key、短信/邮箱找回或学校组织协作。

## Capabilities

### New Capabilities

- `teacher-account-governance`：教师注册、审核、账号生命周期、密码与数据库会话安全。
- `teacher-data-isolation`：教学数据归属、管理员边界和跨教师越权防护。
- `strict-roster-and-assignment-targeting`：班级码、严格名单登录、动态全班与定向作业。
- `problem-bank-scope-and-versioning`：三级题库、审核、不可变版本、作业快照和归档。
- `teacher-ai-quota-metering`：统一 Provider 网关、月度额度、幂等计量和降级行为。
- `schema-migration-and-trial-readiness`：Flyway 基线、生产结构校验、迁移演练和试用 readiness。

### Modified Capabilities

- `teacher-console-ui`：教师工作台增加账号状态、独立工作区、三级题库与额度信息。
- `teacher-ai-feedback-effect-loop`：课堂 AI 调用必须关联教师额度，并在额度耗尽时保留基础评测闭环。

## Impact

- 后端新增 Spring Security、Flyway、认证与管理模块，并修改课堂、题库、提交、AI、readiness 和审计链路。
- 数据库新增教师账号/会话、学生会话、作业接收人、题目版本、额度、用量和审计表，并为现有教学数据补充归属。
- 前端新增教师注册登录、管理员审核、名单/目标选择、三级题库与额度界面；学生认证改用 HttpOnly Cookie。
- 部署新增 bootstrap 管理员环境变量、迁移前备份与 Flyway 校验；现有 16 道题迁移为平台公共已发布题。
