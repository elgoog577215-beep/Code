## ADDED Requirements

### Requirement: 标准库按知识树、能力点和易错点组织
系统 SHALL 支持以信息学知识树为根，将最细知识点关联到能力点，并将能力点关联到易错点。

#### Scenario: 能力点关联知识点
- **WHEN** 系统种子化标准库 v3 内容
- **THEN** 每个启用的能力点 MUST 至少关联一个信息学知识点

#### Scenario: 易错点关联能力点
- **WHEN** 系统种子化标准库 v3 内容
- **THEN** 每个启用的易错点 MUST 关联一个能力点

### Requirement: 标准库核心字段保持简洁
系统 SHALL 将能力点和易错点的核心维护字段限定为知识、能力、易错类型、定义、常见误解、前置知识、语言、严重度、版本和启用状态。

#### Scenario: 教师编辑能力点
- **WHEN** 教师编辑能力点
- **THEN** 系统 MUST 提供能力点名称、定义、学习目标、关联知识点、适用语言、难度、版本和启用状态字段

#### Scenario: 教师编辑易错点
- **WHEN** 教师编辑易错点
- **THEN** 系统 MUST 提供易错点名称、所属能力点、易错类型、定义、常见误解、关联知识点、适用语言、严重度、前置知识、版本和启用状态字段

### Requirement: 标准库不固定生成提高建议
系统 SHALL 不把提高层个性化建议作为标准库核心条目维护；提高建议 MUST 由 AI 根据题目、代码、判题结果和标准库上下文生成。

#### Scenario: 标准库提供约束而非成品建议
- **WHEN** 后端构建传给 AI 的标准库上下文
- **THEN** 上下文 MUST 重点包含知识点、能力点和易错点，而不是固定提高建议文本

### Requirement: 高频知识点具有精细样板内容
系统 SHALL 为高中信息学高频知识点提供比自动铺底更细的能力点和易错点样板。

#### Scenario: 循环边界样板
- **WHEN** 标准库种子化完成
- **THEN** 循环边界相关知识点 MUST 包含多个能力点和多个易错点

#### Scenario: DP 状态样板
- **WHEN** 标准库种子化完成
- **THEN** DP 状态定义相关知识点 MUST 包含多个能力点和多个易错点

### Requirement: 旧 AI 标准库 pack 保持兼容
系统 SHALL 在 v3 标准库存在时继续生成旧 `StandardLibraryPack` 需要的基础层候选项，避免现有 AI 诊断链路中断。

#### Scenario: 易错点映射为旧基础层候选项
- **WHEN** `StandardLibraryPackBuilder` 从数据库读取 v3 标准库
- **THEN** 启用的易错点 MUST 能作为旧 `BasicCauseOption` 候选项输出

#### Scenario: 数据库为空时回退内置库
- **WHEN** 数据库没有可用标准库条目
- **THEN** `StandardLibraryPackBuilder` MUST 回退到内置标准库

### Requirement: 教师端可管理 v3 标准库
系统 SHALL 允许教师通过既有教师登录保护的管理页查询、筛选、编辑、启用和停用能力点与易错点。

#### Scenario: 教师未登录不能访问
- **WHEN** 未登录用户访问 v3 标准库管理接口
- **THEN** 系统 MUST 返回 401

#### Scenario: 教师编辑后持久化
- **WHEN** 已登录教师修改一个易错点并保存
- **THEN** 刷新后 MUST 能读取到修改后的字段
