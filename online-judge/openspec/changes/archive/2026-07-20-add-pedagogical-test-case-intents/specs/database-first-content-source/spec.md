## ADDED Requirements

### Requirement: 正式测试点语义必须以数据库为主库
系统 SHALL 将正式测试点的评测意图、学习目标、竞赛角色、揭示策略和标准库映射保存在正式数据库中，并 SHALL 只通过教师治理流程或可审计版本化迁移发布。

#### Scenario: 发布一批正式测试点语义
- **WHEN** 开发者精修现有正式题目的测试点语义
- **THEN** 内容 SHALL 通过未发布的 Flyway 迁移写入
- **AND** Java seed、测试 fixture 和运行时初始化器 SHALL NOT 成为生产语义来源

#### Scenario: 教师新建未评审测试点
- **WHEN** 教师通过管理流程新建测试点但尚未完成语义审核
- **THEN** 系统 MAY 暂存缺少正式语义的测试点
- **AND** 该测试点 SHALL NOT 通过正式内容发布门禁

