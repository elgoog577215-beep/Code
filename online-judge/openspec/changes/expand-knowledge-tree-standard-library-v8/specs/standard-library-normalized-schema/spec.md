## ADDED Requirements

### Requirement: 手写提升点必须进入规范标准库结构
AI 标准库 SHALL 支持手写提升点 seed 被同步到 `ai_standard_improvement_points`，并 SHALL 在候选包中作为结构化提升方向参与诊断建议。

#### Scenario: 同步手写提升点
- **WHEN** 标准库同步器处理 V8 `IMPROVEMENT_POINT` seed
- **THEN** 系统 SHALL 保存该提升点到规范提升点表
- **AND** SHALL 保留其能力点归属、知识节点路径、提升目标、练习策略、学生收益和教师解释

#### Scenario: 提升点进入候选包
- **WHEN** 系统根据规范标准库生成 AI 诊断候选包
- **THEN** V8 手写提升点 SHALL 出现在 `improvementPoints`
- **AND** 若其能力点位于结构化知识邻域内，SHALL 能被放入对应知识组

### Requirement: V8 手写易错点与提升点必须具备细颗粒质量
V8 标准库手写条目 SHALL 避免空泛命名，并 SHALL 以可观察症状、具体误解、修复策略或提升练习表达可教学颗粒度。

#### Scenario: 易错点具备具体诊断信息
- **WHEN** V8 `MISTAKE_POINT` seed 被校验
- **THEN** 该易错点 SHALL 关联合法能力点和知识节点
- **AND** 其名称 SHALL 描述具体错误行为，而不是泛化为“理解或应用偏差”
- **AND** 其 commonMisconception SHALL 解释学生为什么会犯该错

#### Scenario: 提升点具备具体提升方向
- **WHEN** V8 `IMPROVEMENT_POINT` seed 被校验
- **THEN** 该提升点 SHALL 关联合法知识节点
- **AND** SHALL 包含适用场景、学生收益和教师解释
- **AND** SHALL 关联相关易错点，帮助教师从修错过渡到提升
