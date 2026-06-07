## ADDED Requirements

### Requirement: 后端必须区分教学主链路和内部支撑链路

系统 SHALL 将后端能力分为教学主链路、可简化链路、内部支撑链路和后续清理链路，避免所有 AI/评测/运行状态都进入学生和教师主体验。

#### Scenario: 能力分类清晰

- **WHEN** 审计 submission/classroom 相关服务、DTO 和接口
- **THEN** 每一项 SHALL 被标记为 `keep-main`、`simplify`、`internal-only` 或 `remove-later`
- **AND** 每一项 SHALL 给出分类理由

#### Scenario: 内部能力不得默认进入主链路

- **WHEN** 新增或修改 AI runtime、eval、fallback、profile、smoke、quality dimension、loop analyzer 相关能力
- **THEN** 系统 SHALL 默认将其归入 `internal-only`
- **AND** 若要进入学生或教师主链路，必须说明它如何直接服务学生下一步动作或教师课堂判断

### Requirement: 学生主链路必须保持短而稳定

系统 SHALL 让学生主链路只围绕作业、题目、提交、判题事实和学生 AI 反馈组织。

#### Scenario: 学生提交失败

- **WHEN** 学生提交代码失败
- **THEN** 系统 SHALL 继续返回判题 verdict 和测试点事实
- **AND** 学生 AI 反馈 SHALL 通过独立学生反馈接口提供
- **AND** 旧 analysis 大对象 SHALL NOT 重新成为学生端主建议来源

#### Scenario: AI 反馈失败

- **WHEN** 学生 AI 反馈超时、失败或安全拒绝
- **THEN** 系统 SHALL 返回简单明确状态
- **AND** 判题事实 SHALL 不受影响
- **AND** 系统 SHALL NOT 用本地规则文案冒充模型建议

### Requirement: 教师主链路必须服务课堂判断

系统 SHALL 让教师主链路只围绕作业、班级概况、需关注学生、共性问题和课堂动作组织。

#### Scenario: 教师查看课堂

- **WHEN** 教师打开课堂工作台
- **THEN** 主信息 SHALL 优先支持判断谁需要关注、卡在哪里、共性问题是什么、下一步课堂动作是什么
- **AND** AI 质量、运行状态、评测证据 SHALL 作为辅助或内部信息展示

#### Scenario: 工程状态下沉

- **WHEN** 教师端需要展示 AI 运行或质量信息
- **THEN** 页面 SHALL 使用教师可理解的中文表达
- **AND** `fallback`、`smoke`、`profile`、`BLOCKED`、`RECOVERED`、`NOT_COMPARABLE` 等工程术语 SHALL NOT 作为主文案出现

### Requirement: 收敛变更必须保护现有兼容性

系统 SHALL 在收敛后端复杂度时保护现有判题、提交、学生 AI 反馈、教师课堂接口和测试。

#### Scenario: 第一轮收敛不删除接口

- **WHEN** 第一轮 `simplify-teaching-backend-core` 实施完成
- **THEN** 它 SHALL NOT 删除已被前端或测试依赖的 API
- **AND** 它 SHALL NOT 改变数据库表结构
- **AND** 它 SHALL NOT 改变模型调用语义

#### Scenario: 删除候选必须先审计

- **WHEN** 某个服务、DTO、字段或接口被标记为 `remove-later`
- **THEN** 审计文档 SHALL 说明删除前置条件
- **AND** 后续删除 SHALL 先确认没有前端、测试或业务调用依赖

### Requirement: 后端审计必须可用于下一轮执行

系统 SHALL 产出能指导后续执行的后端主链路审计，而不是只写抽象原则。

#### Scenario: 审计文档包含执行信息

- **WHEN** 查看后端审计文档
- **THEN** 文档 SHALL 至少覆盖 submission application、submission dto、submission API、classroom application、classroom dto 和 classroom API
- **AND** 文档 SHALL 列出第一批建议保留、下沉、简化和后续清理候选
- **AND** 文档 SHALL 给出下一轮推荐执行顺序
