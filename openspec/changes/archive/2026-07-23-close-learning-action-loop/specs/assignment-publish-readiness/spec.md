## ADDED Requirements

### Requirement: 作业发布前必须检查题目诊断就绪度
系统 SHALL 在教师发布作业前检查所有所选题目的测试用例、已审核测试点语义、知识与能力路径和隐藏揭示策略，并 SHALL 以每题 blocker 与 warning 返回结果。

#### Scenario: 题目缺少测试用例
- **WHEN** 教师尝试发布包含零个测试用例的题目
- **THEN** 预检 SHALL 返回 blocker
- **AND** 后端 MUST NOT 创建或更新为正式发布状态

#### Scenario: 正式测试点缺少审核语义
- **WHEN** 所选题目的测试点缺少稳定语义 code、审核状态、学习目标或合法标准库路径
- **THEN** 预检 SHALL 返回对应题目和具体缺失项
- **AND** 正式发布 SHALL 被阻止

#### Scenario: 题目满足硬门禁但教学覆盖较薄
- **WHEN** 题目可以可靠判题但测试意图类型或教学说明不足
- **THEN** 预检 SHALL 返回 warning
- **AND** 系统 SHALL 允许教师在知情后发布

### Requirement: 草稿保存与正式发布必须采用不同门禁
教师 SHALL 能保存包含未完成题目的草稿；正式发布 SHALL 在后端事务中重新执行就绪检查，客户端预检结果 MUST NOT 替代后端门禁。

#### Scenario: 保存未就绪草稿
- **WHEN** 创建请求状态为 `DRAFT` 且包含质量 blocker
- **THEN** 系统 SHALL 允许保存草稿
- **AND** 页面 SHALL 保留 blocker 供后续修复

#### Scenario: 绕过客户端直接发布
- **WHEN** 客户端未调用预检而直接提交 `ACTIVE` 创建请求
- **THEN** 后端 SHALL 重新检查所选题目
- **AND** 存在 blocker 时请求 SHALL 被拒绝

### Requirement: 发布预检界面必须双语且可访问
预检摘要、问题级 blocker、warning、空状态和操作文案 SHALL 同步提供中文与英文，并 SHALL 通过文本与图标表达严重程度。

#### Scenario: 英文模式检查未就绪作业
- **WHEN** 教师在英文模式选择包含 blocker 的题目
- **THEN** 页面 SHALL 使用英文展示问题、严重程度和修复方向
- **AND** 状态 MUST NOT 仅依赖颜色表达
